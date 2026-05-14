package it.intesigroup.interview.usermanagement.service;

import it.intesigroup.interview.usermanagement.dto.CreateUserRequest;
import it.intesigroup.interview.usermanagement.dto.UpdateUserRequest;
import it.intesigroup.interview.usermanagement.dto.UserResponse;
import it.intesigroup.interview.usermanagement.entity.UserEntity;
import it.intesigroup.interview.usermanagement.event.UserCreatedEvent;
import it.intesigroup.interview.usermanagement.exception.DuplicateEmailException;
import it.intesigroup.interview.usermanagement.exception.UserNotFoundException;
import it.intesigroup.interview.usermanagement.mapper.UserMapper;
import it.intesigroup.interview.usermanagement.repository.UserRepository;
import it.intesigroup.interview.usermanagement.security.CurrentUserProvider;
import it.intesigroup.interview.usermanagement.security.ResponseVisibility;
import it.intesigroup.interview.usermanagement.security.ResponseVisibilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

// §2.1 – Logica di business per i 5 requisiti funzionali base.
// §2.2.3 – Applica ResponseVisibility a ogni risposta in base al ruolo del chiamante.
// §2.2.4 – Pubblica UserCreatedEvent dopo ogni creazione utente andata a buon fine.
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final ResponseVisibilityService responseVisibilityService;
    private final CurrentUserProvider currentUserProvider;
    private final ApplicationEventPublisher eventPublisher;

    public UserService(
            UserRepository userRepository,
            UserMapper userMapper,
            ResponseVisibilityService responseVisibilityService,
            CurrentUserProvider currentUserProvider,
            ApplicationEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.responseVisibilityService = responseVisibilityService;
        this.currentUserProvider = currentUserProvider;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        log.info("Fetching all users, requestedBy={}", currentUserProvider.username());
        ResponseVisibility visibility = responseVisibilityService.currentVisibility();
        List<UserResponse> users = userRepository.findAll().stream()
                .map(user -> userMapper.toResponse(user, visibility))
                .toList();
        log.debug("Returning {} user(s)", users.size());
        return users;
    }

    @Transactional(readOnly = true)
    public UserResponse findById(UUID id) {
        log.info("Fetching user id={}, requestedBy={}", id, currentUserProvider.username());
        UserEntity user = findUser(id);
        return userMapper.toResponse(user, responseVisibilityService.currentVisibility());
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        log.info("Creating user email={}, requestedBy={}", request.email(), currentUserProvider.username());
        // §1.1 – "Non sono consentiti più utenti con la stessa mail": controllo applicativo pre-insert
        String normalizedEmail = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            log.warn("Duplicate email detected: {}", normalizedEmail);
            throw new DuplicateEmailException(request.email());
        }

        String currentUsername = currentUserProvider.username();
        UserEntity user = new UserEntity(
                request.username(),
                normalizedEmail,
                request.taxCode(),
                request.name(),
                request.surname()
        );
        user.replaceRoles(request.roles());
        user.setCreatedBy(currentUsername);
        user.setUpdatedBy(currentUsername);

        UserEntity saved = userRepository.save(user);
        log.info("User created id={}, email={}, createdBy={}", saved.getId(), saved.getEmail(), currentUsername);
        eventPublisher.publishEvent(new UserCreatedEvent(saved.getId(), saved.getEmail(), currentUsername));
        return userMapper.toResponse(saved, responseVisibilityService.currentVisibility());
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request) {
        log.info("Updating user id={}, requestedBy={}", id, currentUserProvider.username());
        UserEntity user = findUser(id);
        user.setUsername(request.username());
        user.setTaxCode(request.taxCode());
        user.setName(request.name());
        user.setSurname(request.surname());
        user.replaceRoles(request.roles());
        user.setUpdatedBy(currentUserProvider.username());
        log.debug("User updated id={}", id);
        return userMapper.toResponse(user, responseVisibilityService.currentVisibility());
    }

    @Transactional
    public void delete(UUID id) {
        log.info("Deleting user id={}, requestedBy={}", id, currentUserProvider.username());
        if (!userRepository.existsById(id)) {
            log.warn("User not found for deletion id={}", id);
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
        log.info("User deleted id={}", id);
    }

    private UserEntity findUser(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
