package it.intesigroup.interview.usermanagement.event;

import java.util.UUID;

// §2.2.4 – Payload dell'evento asincrono pubblicato dopo la creazione di un utente.
// Trasportato dal bus interno di Spring (ApplicationEventPublisher), senza broker esterno.
public record UserCreatedEvent(UUID userId, String email, String createdBy) {
}
