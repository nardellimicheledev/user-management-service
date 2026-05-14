package it.intesigroup.interview.usermanagement.mapper;

import it.intesigroup.interview.usermanagement.dto.UserResponse;
import it.intesigroup.interview.usermanagement.entity.UserEntity;
import it.intesigroup.interview.usermanagement.enums.ApplicationRoleType;
import it.intesigroup.interview.usermanagement.security.ResponseVisibility;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class UserMapper {

    // §2.2.3 – Applica le regole di filtering: i campi null vengono omessi dal JSON
    // grazie a @JsonInclude(NON_NULL) su UserResponse, senza bisogno di DTO separati per ruolo.
    public UserResponse toResponse(UserEntity entity, ResponseVisibility visibility) {
        String taxCode = visibility.showTaxCode() ? entity.getTaxCode() : null;
        Set<ApplicationRoleType> roles = visibility.showRoles() ? entity.getRoleTypes() : null;
        return new UserResponse(
                entity.getId(),
                entity.getUsername(),
                entity.getEmail(),
                taxCode,
                entity.getName(),
                entity.getSurname(),
                roles
        );
    }
}
