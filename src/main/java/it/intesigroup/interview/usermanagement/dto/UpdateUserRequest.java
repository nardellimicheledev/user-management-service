package it.intesigroup.interview.usermanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.intesigroup.interview.usermanagement.enums.ApplicationRoleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

// §2.1 req. 4 – "Modificare un utente esistente o un suo ruolo".
// L'email è deliberatamente assente: §1.1 "L'email è un campo non modificabile".
@Schema(description = "Dati per la modifica di un utente esistente (email esclusa — non modificabile)")
public record UpdateUserRequest(
        @Schema(description = "Username", example = "mario.rossi.updated")
        @NotBlank @Size(max = 100)
        String username,

        @Schema(description = "Codice fiscale — 16 caratteri alfanumerici", example = "RSSMRA80A01H501Z")
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9]{16}$", message = "taxCode must contain 16 alphanumeric characters")
        String taxCode,

        @Schema(description = "Nome", example = "Mario")
        @NotBlank @Size(max = 100)
        String name,

        @Schema(description = "Cognome", example = "Rossi Aggiornato")
        @NotBlank @Size(max = 100)
        String surname,

        @Schema(description = "Nuovi ruoli applicativi — sostituiscono quelli esistenti", example = "[\"OWNER\",\"MAINTAINER\"]")
        @NotEmpty
        Set<@NotNull ApplicationRoleType> roles
) {
}
