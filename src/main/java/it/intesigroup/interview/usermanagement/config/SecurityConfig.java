package it.intesigroup.interview.usermanagement.config;

import it.intesigroup.interview.usermanagement.security.AppSecurityProperties;
import it.intesigroup.interview.usermanagement.security.KeycloakJwtAuthenticationConverter;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

// §2.2.1 – Configura il servizio come Resource Server OAuth2 con autenticazione JWT Keycloak.
// @EnableMethodSecurity abilita @PreAuthorize sui metodi del controller (§2.2.2 RBAC).
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AppSecurityProperties properties) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                // STATELESS: nessuna sessione HTTP, il JWT viene rivalidato a ogni richiesta
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/health").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                        .jwtAuthenticationConverter(new KeycloakJwtAuthenticationConverter(properties.clientId()))
                ))
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler((request, response, exception) -> {
                            log.warn("Access denied: {} {} - principal={}", request.getMethod(), request.getRequestURI(), request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "unknown");
                            response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        })
                        .authenticationEntryPoint((request, response, exception) -> {
                            log.warn("Unauthorized: {} {} - {}", request.getMethod(), request.getRequestURI(), exception.getMessage());
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        })
                )
                .build();
    }
}
