package it.intesigroup.interview.usermanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.intesigroup.interview.usermanagement.dto.CreateUserRequest;
import it.intesigroup.interview.usermanagement.dto.UpdateUserRequest;
import it.intesigroup.interview.usermanagement.dto.UserResponse;
import it.intesigroup.interview.usermanagement.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

// §2.1 – Espone i 5 requisiti funzionali base come endpoint REST su /api/users.
// §2.2.1 – Richiede un JWT valido Keycloak su ogni chiamata (configurato in SecurityConfig).
// §2.2.2 – @PreAuthorize controlla i permessi estratti dal token prima di eseguire il metodo.
@Tag(name = "Users", description = "Gestione anagrafica utenti (§2.1)")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // §2.1 req. 1 – ADMIN, OPERATOR e USER hanno tutti 'read_user'
    @Operation(summary = "Lista utenti", description = "Restituisce tutti gli utenti. I campi visibili dipendono dal ruolo (§2.2.3).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista restituita"),
            @ApiResponse(responseCode = "401", description = "Token mancante o non valido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Permesso 'read_user' assente", content = @Content)
    })
    @GetMapping
    @PreAuthorize("hasAuthority('read_user')")
    public List<UserResponse> findAll() {
        log.debug("GET /api/users");
        return userService.findAll();
    }

    // §2.1 req. 2
    @Operation(summary = "Dettaglio utente", description = "Restituisce un singolo utente per ID. I campi visibili dipendono dal ruolo (§2.2.3).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Utente trovato"),
            @ApiResponse(responseCode = "401", description = "Token mancante o non valido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Permesso 'read_user' assente", content = @Content),
            @ApiResponse(responseCode = "404", description = "Utente non trovato",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('read_user')")
    public UserResponse findById(
            @Parameter(description = "UUID dell'utente") @PathVariable UUID id) {
        log.debug("GET /api/users/{}", id);
        return userService.findById(id);
    }

    // §2.1 req. 3 – Solo ADMIN e OPERATOR hanno 'create_user'
    @Operation(summary = "Crea utente", description = "Crea un nuovo utente con i ruoli indicati. L'email non è modificabile dopo la creazione (§1.1).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Utente creato"),
            @ApiResponse(responseCode = "400", description = "Dati non validi (validazione fallita)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Token mancante o non valido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Permesso 'create_user' assente", content = @Content),
            @ApiResponse(responseCode = "409", description = "Email già esistente",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    @PreAuthorize("hasAuthority('create_user')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        log.debug("POST /api/users email={}", request.email());
        UserResponse response = userService.create(request);
        log.debug("POST /api/users -> 201 id={}", response.id());
        return ResponseEntity.created(URI.create("/api/users/" + response.id())).body(response);
    }

    // §2.1 req. 4 – Solo ADMIN e OPERATOR hanno 'update_user'
    @Operation(summary = "Modifica utente", description = "Aggiorna username, taxCode, nome, cognome e ruoli. L'email non è modificabile (§1.1).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Utente aggiornato"),
            @ApiResponse(responseCode = "400", description = "Dati non validi",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Token mancante o non valido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Permesso 'update_user' assente", content = @Content),
            @ApiResponse(responseCode = "404", description = "Utente non trovato",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('update_user')")
    public UserResponse update(
            @Parameter(description = "UUID dell'utente") @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        log.debug("PUT /api/users/{}", id);
        return userService.update(id, request);
    }

    // §2.1 req. 5 – Solo ADMIN ha 'delete_user'
    @Operation(summary = "Elimina utente", description = "Elimina definitivamente l'utente. Solo il ruolo ADMIN ha il permesso 'delete_user'.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Utente eliminato"),
            @ApiResponse(responseCode = "401", description = "Token mancante o non valido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Permesso 'delete_user' assente", content = @Content),
            @ApiResponse(responseCode = "404", description = "Utente non trovato",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('delete_user')")
    public ResponseEntity<Void> delete(
            @Parameter(description = "UUID dell'utente") @PathVariable UUID id) {
        log.debug("DELETE /api/users/{}", id);
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
