package it.intesigroup.interview.usermanagement.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

// §2.2.3 – Determina quale sottoinsieme di campi può vedere il chiamante,
// leggendo il ruolo Keycloak (ADMIN/OPERATOR/USER) dalle authority del SecurityContext.
@Service
public class ResponseVisibilityService {

    public ResponseVisibility currentVisibility() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return ResponseVisibility.user();
        }

        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        if (authorities.contains("ADMIN")) {
            return ResponseVisibility.admin();
        }
        if (authorities.contains("OPERATOR")) {
            return ResponseVisibility.operator();
        }
        // Fallback a USER (visibilità minima): nasconde tax_code e roles
        return ResponseVisibility.user();
    }
}
