package it.intesigroup.interview.usermanagement.repository;

import it.intesigroup.interview.usermanagement.entity.UserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// §2.1 – Repository per le operazioni CRUD su UserEntity (§1.1 Definizione degli Utenti).
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    // §1.1 – "Non sono consentiti più utenti con la stessa mail": verifica unicità prima del salvataggio.
    boolean existsByEmailIgnoreCase(String email);

    // @EntityGraph evita il problema N+1: carica roles in un'unica query JOIN invece di query separate per ogni utente.
    @Override
    @EntityGraph(attributePaths = "roles")
    List<UserEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "roles")
    Optional<UserEntity> findById(UUID id);
}
