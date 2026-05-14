package it.intesigroup.interview.usermanagement.entity;

import it.intesigroup.interview.usermanagement.enums.ApplicationRoleType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

// §1.1 + §1.2 – Entità "Users" del diagramma UML con i campi: username, email, name, surname, tax_code.
// §1.3 – Persistita su H2 in-memory (configurabile con MySQL/Oracle/Postgres tramite application.yml).
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String username;

    // §1.1 – "L'email è un campo non modificabile": updatable=false lo impone anche a livello JPA.
    @Column(nullable = false, unique = true, length = 255, updatable = false)
    private String email;

    @Column(name = "tax_code", nullable = false, length = 16)
    private String taxCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String surname;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<RoleEntity> roles = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", length = 100, updatable = false)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    protected UserEntity() {
        // JPA
    }

    public UserEntity(String username, String email, String taxCode, String name, String surname) {
        this.id = UUID.randomUUID();
        this.username = username;
        this.email = normalizeEmail(email);
        this.taxCode = taxCode;
        this.name = name;
        this.surname = surname;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Diff incrementale invece di clear()+addAll(): evita che Hibernate esegua INSERT prima di DELETE
    // sulla stessa coppia (user_id, role_type), che violerebbe l'unique constraint uk_roles_user_role_type.
    public void replaceRoles(Set<ApplicationRoleType> roleTypes) {
        this.roles.removeIf(role -> !roleTypes.contains(role.getRoleType()));
        Set<ApplicationRoleType> existing = getRoleTypes();
        roleTypes.stream()
                .filter(rt -> !existing.contains(rt))
                .forEach(rt -> this.roles.add(new RoleEntity(this, rt)));
    }

    public Set<ApplicationRoleType> getRoleTypes() {
        Set<ApplicationRoleType> roleTypes = new HashSet<>();
        this.roles.forEach(role -> roleTypes.add(role.getRoleType()));
        return roleTypes;
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public Set<RoleEntity> getRoles() {
        return roles;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserEntity that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
