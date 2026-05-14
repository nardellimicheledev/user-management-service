package it.intesigroup.interview.usermanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.intesigroup.interview.usermanagement.enums.ApplicationRoleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

// §2.1 req. 3 – "Aggiungere un utente con i suoi ruoli" – campi §1.1.
// L'email è inclusa qui ma assente in UpdateUserRequest: §1.1 "L'email è un campo non modificabile".
@Schema(description = "Dati per la creazione di un nuovo utente")
public record CreateUserRequest(
        @Schema(description = "Username univoco", example = "mario.rossi")
        @NotBlank @Size(max = 100)
        String username,

        @Schema(description = "Email — non modificabile dopo la creazione", example = "mario.rossi@example.com")
        @NotBlank @Email @Size(max = 255)
        String email,

        @Schema(description = "Codice fiscale — 16 caratteri alfanumerici", example = "RSSMRA80A01H501Z")
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9]{16}$", message = "taxCode must contain 16 alphanumeric characters")
        String taxCode,

        @Schema(description = "Nome", example = "Mario")
        @NotBlank @Size(max = 100)
        String name,

        @Schema(description = "Cognome", example = "Rossi")
        @NotBlank @Size(max = 100)
        String surname,

        @Schema(description = "Uno o più ruoli applicativi", example = "[\"DEVELOPER\",\"REPORTER\"]")
        @NotEmpty
        Set<@NotNull ApplicationRoleType> roles
) {
}
