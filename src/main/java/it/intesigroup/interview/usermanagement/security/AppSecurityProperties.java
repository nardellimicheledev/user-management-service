package it.intesigroup.interview.usermanagement.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

// §2.2.1 – Legge app.security.client-id da application.yml (valore: "demo-task").
// Serve al KeycloakJwtAuthenticationConverter per estrarre i ruoli client-specific dal JWT.
@ConfigurationProperties(prefix = "app.security")
public record AppSecurityProperties(String clientId) {
}
