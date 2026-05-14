package it.intesigroup.interview.usermanagement.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

// §2.2.1 – "È gradito tracciare applicativamente l'utente loggato che effettua le operazioni."
// Legge il principal dal SecurityContext (valorizzato con preferred_username dal JWT Keycloak)
// e lo espone al service per popolare i campi createdBy/updatedBy e per i log di audit.
@Component
public class CurrentUserProvider {

    public String username() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "anonymous";
        }
        return authentication.getName();
    }
}
