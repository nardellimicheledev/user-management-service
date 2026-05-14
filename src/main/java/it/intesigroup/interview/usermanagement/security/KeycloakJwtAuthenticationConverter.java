package it.intesigroup.interview.usermanagement.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final String clientId;

    public KeycloakJwtAuthenticationConverter(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Set<String> authorityNames = new HashSet<>();
        collectScopes(jwt, authorityNames);
        collectRealmRoles(jwt, authorityNames);
        collectClientRoles(jwt, authorityNames);
        collectGenericPermissions(jwt, authorityNames);
        mapRolesToPermissions(authorityNames);

        Collection<GrantedAuthority> authorities = authorityNames.stream()
                .filter(Objects::nonNull)
                .filter(authority -> !authority.isBlank())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());

        String principalName = jwt.getClaimAsString("preferred_username");
        if (principalName == null || principalName.isBlank()) {
            principalName = jwt.getSubject();
        }
        return new JwtAuthenticationToken(jwt, authorities, principalName);
    }

    private void collectScopes(Jwt jwt, Set<String> authorities) {
        addClaimValues(jwt.getClaim("scope"), authorities);
        addClaimValues(jwt.getClaim("scp"), authorities);
    }

    @SuppressWarnings("unchecked")
    private void collectRealmRoles(Jwt jwt, Set<String> authorities) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null) {
            addClaimValues(realmAccess.get("roles"), authorities);
        }
    }

    @SuppressWarnings("unchecked")
    private void collectClientRoles(Jwt jwt, Set<String> authorities) {
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess == null) {
            return;
        }
        Object clientAccessValue = resourceAccess.get(clientId);
        if (clientAccessValue instanceof Map<?, ?> clientAccess) {
            addClaimValues(clientAccess.get("roles"), authorities);
        }
    }

    private void collectGenericPermissions(Jwt jwt, Set<String> authorities) {
        addClaimValues(jwt.getClaim("permissions"), authorities);
    }

    private void mapRolesToPermissions(Set<String> authorities) {
        List<String> toAdd = new ArrayList<>();
        if (authorities.contains("ADMIN")) {
            toAdd.addAll(List.of("read_user", "create_user", "update_user", "delete_user"));
        }
        if (authorities.contains("OPERATOR")) {
            toAdd.addAll(List.of("read_user", "create_user", "update_user"));
        }
        if (authorities.contains("USER")) {
            toAdd.add("read_user");
        }
        authorities.addAll(toAdd);
    }

    private void addClaimValues(Object value, Set<String> authorities) {
        if (value == null) {
            return;
        }
        if (value instanceof String stringValue) {
            for (String token : stringValue.split(" ")) {
                if (!token.isBlank()) {
                    authorities.add(token.trim());
                }
            }
            return;
        }
        if (value instanceof Collection<?> collection) {
            collection.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .filter(item -> !item.isBlank())
                    .forEach(authorities::add);
        }
    }
}
