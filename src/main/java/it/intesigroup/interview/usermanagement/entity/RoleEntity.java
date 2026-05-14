package it.intesigroup.interview.usermanagement.entity;

import it.intesigroup.interview.usermanagement.enums.ApplicationRoleType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Objects;

// §1.2 – Entità "Roles" del diagramma UML: name = RolesType (ApplicationRoleType).
// La unique constraint (user_id, role_type) garantisce che lo stesso ruolo non venga assegnato
// due volte allo stesso utente a livello DB, in aggiunta al controllo applicativo in replaceRoles.
@Entity
@Table(
        name = "roles",
        uniqueConstraints = @UniqueConstraint(name = "uk_roles_user_role_type", columnNames = {"user_id", "role_type"})
)
public class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false, length = 30)
    private ApplicationRoleType roleType;

    protected RoleEntity() {
        // JPA
    }

    public RoleEntity(UserEntity user, ApplicationRoleType roleType) {
        this.user = user;
        this.roleType = roleType;
    }

    public Long getId() {
        return id;
    }

    public UserEntity getUser() {
        return user;
    }

    public ApplicationRoleType getRoleType() {
        return roleType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RoleEntity that)) {
            return false;
        }
        return Objects.equals(user != null ? user.getId() : null, that.user != null ? that.user.getId() : null)
                && roleType == that.roleType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(user != null ? user.getId() : null, roleType);
    }
}
