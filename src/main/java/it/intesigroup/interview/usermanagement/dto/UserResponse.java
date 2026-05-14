package it.intesigroup.interview.usermanagement.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import it.intesigroup.interview.usermanagement.enums.ApplicationRoleType;

import java.util.Set;
import java.util.UUID;

// §2.2.3 – NON_NULL esclude dal JSON i campi che il mapper imposta a null in base al ruolo chiamante:
//   OPERATOR: taxCode=null  |  USER: taxCode=null, roles=null
@Schema(description = "Dati utente restituiti dalla API — i campi taxCode e roles possono essere assenti in base al ruolo del chiamante (§2.2.3)")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
        @Schema(description = "ID univoco utente") UUID id,
        @Schema(description = "Username", example = "mario.rossi") String username,
        @Schema(description = "Email", example = "mario.rossi@example.com") String email,
        @Schema(description = "Codice fiscale — visibile solo ad ADMIN", example = "RSSMRA80A01H501Z") String taxCode,
        @Schema(description = "Nome", example = "Mario") String name,
        @Schema(description = "Cognome", example = "Rossi") String surname,
        @Schema(description = "Ruoli applicativi — visibili ad ADMIN e OPERATOR") Set<ApplicationRoleType> roles
) {
}
