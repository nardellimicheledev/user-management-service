package it.intesigroup.interview.usermanagement.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("User Management Service")
                        .description("API REST per la gestione anagrafica utenti. " +
                                "Autenticazione tramite JWT Keycloak — ottenere il token dall'IDP e inserirlo nel pulsante 'Authorize'.")
                        .version("1.0.0"))
                // Registra lo schema JWT Bearer utilizzabile dal pulsante "Authorize" di Swagger UI
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                // Applica il requisito di sicurezza a tutti gli endpoint per default
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
