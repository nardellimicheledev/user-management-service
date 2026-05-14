package it.intesigroup.interview.usermanagement;

import it.intesigroup.interview.usermanagement.security.KeycloakJwtAuthenticationConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakJwtAuthenticationConverterTest {

    private KeycloakJwtAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        converter = new KeycloakJwtAuthenticationConverter("demo-task");
    }

    @Test
    void shouldMapAdminRoleToAllPermissions() {
        Jwt jwt = buildJwt("admin_user", Map.of("roles", List.of("ADMIN")), null);

        JwtAuthenticationToken token = (JwtAuthenticationToken) converter.convert(jwt);
        Set<String> authorities = extractAuthorities(token);

        assertThat(authorities).contains("ADMIN", "read_user", "create_user", "update_user", "delete_user");
    }

    @Test
    void shouldMapOperatorRoleToReadCreateUpdate() {
        Jwt jwt = buildJwt("creator_user", Map.of("roles", List.of("OPERATOR")), null);

        JwtAuthenticationToken token = (JwtAuthenticationToken) converter.convert(jwt);
        Set<String> authorities = extractAuthorities(token);

        assertThat(authorities).contains("OPERATOR", "read_user", "create_user", "update_user");
        assertThat(authorities).doesNotContain("delete_user");
    }

    @Test
    void shouldMapUserRoleToReadOnly() {
        Jwt jwt = buildJwt("reader_user", Map.of("roles", List.of("USER")), null);

        JwtAuthenticationToken token = (JwtAuthenticationToken) converter.convert(jwt);
        Set<String> authorities = extractAuthorities(token);

        assertThat(authorities).contains("USER", "read_user");
        assertThat(authorities).doesNotContain("create_user", "update_user", "delete_user");
    }

    @Test
    void shouldExtractRolesFromClientResourceAccess() {
        Jwt jwt = buildJwt("admin_user", null, Map.of("roles", List.of("ADMIN")));

        JwtAuthenticationToken token = (JwtAuthenticationToken) converter.convert(jwt);
        Set<String> authorities = extractAuthorities(token);

        assertThat(authorities).contains("ADMIN", "read_user", "create_user", "update_user", "delete_user");
    }

    @Test
    void shouldUsePreferredUsernameAsPrincipalName() {
        Jwt jwt = buildJwt("mario.rossi", Map.of("roles", List.of("USER")), null);

        JwtAuthenticationToken token = (JwtAuthenticationToken) converter.convert(jwt);

        assertThat(token.getName()).isEqualTo("mario.rossi");
    }

    @Test
    void shouldFallbackToSubjectWhenPreferredUsernameIsAbsent() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("sub-123")
                .claim("realm_access", Map.of("roles", List.of("USER")))
                .build();

        JwtAuthenticationToken token = (JwtAuthenticationToken) converter.convert(jwt);

        assertThat(token.getName()).isEqualTo("sub-123");
    }

    @Test
    void shouldHandleJwtWithNoRoles() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("sub-123")
                .claim("preferred_username", "anonymous")
                .build();

        JwtAuthenticationToken token = (JwtAuthenticationToken) converter.convert(jwt);
        Set<String> authorities = extractAuthorities(token);

        assertThat(authorities).doesNotContain("read_user", "create_user", "update_user", "delete_user");
    }

    private Jwt buildJwt(String preferredUsername, Map<String, Object> realmRoles, Map<String, Object> clientRoles) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("sub-" + preferredUsername)
                .claim("preferred_username", preferredUsername);

        if (realmRoles != null) {
            builder.claim("realm_access", realmRoles);
        }
        if (clientRoles != null) {
            builder.claim("resource_access", Map.of("demo-task", clientRoles));
        }
        return builder.build();
    }

    private Set<String> extractAuthorities(JwtAuthenticationToken token) {
        return token.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}
