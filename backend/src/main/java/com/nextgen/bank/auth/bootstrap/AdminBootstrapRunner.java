package com.nextgen.bank.auth.bootstrap;

import com.nextgen.bank.auth.domain.User;
import com.nextgen.bank.auth.event.UserRegisteredEvent;
import com.nextgen.bank.auth.repository.UserRepository;
import com.nextgen.bank.common.enums.UserRole;
import com.nextgen.bank.common.event.OutboxEventWriter;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Component
@Order(10)
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    /**
     * Unique 64-bit identifier for the PostgreSQL transaction-scoped advisory lock.
     * Derived from ASCII hex "NEXTGENA" (0x4E45585447454E41L = 5640118320492850753L).
     */
    public static final long ADMIN_BOOTSTRAP_LOCK_KEY = 5640118320492850753L;

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final AdminBootstrapProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OutboxEventWriter outboxEventWriter;
    private final EntityManager entityManager;

    public AdminBootstrapRunner(
            AdminBootstrapProperties properties,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            OutboxEventWriter outboxEventWriter,
            EntityManager entityManager
    ) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.outboxEventWriter = outboxEventWriter;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            log.info("Admin bootstrap is disabled. Skipping admin account initialization.");
            return;
        }

        validateConfiguration();

        // 1. Acquire cluster-wide PostgreSQL transaction-scoped advisory lock before checking for existing ADMIN.
        // If another instance is currently bootstrapping, this blocks until that transaction completes.
        acquirePostgresAdvisoryLock();

        // 2. Re-check if ADMIN exists AFTER acquiring the cluster-wide lock
        if (userRepository.existsByRole(UserRole.ADMIN)) {
            log.info("Admin user already exists in the system. Skipping bootstrap initialization.");
            return;
        }

        String username = properties.getUsername().trim();
        String email = properties.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsername(username) || userRepository.existsByEmail(email)) {
            log.warn("Bootstrap user '{}' or email '{}' already exists with a different role. Skipping bootstrap.", username, email);
            return;
        }

        try {
            String encodedPassword = passwordEncoder.encode(properties.getPassword());
            User adminUser = new User(
                    username,
                    email,
                    encodedPassword,
                    UserRole.ADMIN
            );

            User savedAdmin = userRepository.save(adminUser);
            outboxEventWriter.write(new UserRegisteredEvent(
                    savedAdmin.getUserId(),
                    savedAdmin.getEmail(),
                    savedAdmin.getRole()
            ));

            log.info("Bootstrap ADMIN account successfully created for username: {}", username);
        } catch (DataIntegrityViolationException e) {
            // Defense-in-depth handling for concurrent race conditions
            log.info("Admin account was already created by another concurrent instance: {}", e.getMessage());
        }
    }

    private void acquirePostgresAdvisoryLock() {
        try {
            entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(:lockKey)")
                    .setParameter("lockKey", ADMIN_BOOTSTRAP_LOCK_KEY)
                    .getSingleResult();
            log.debug("Acquired PostgreSQL advisory lock for admin bootstrap (key={})", ADMIN_BOOTSTRAP_LOCK_KEY);
        } catch (Exception e) {
            // In non-PostgreSQL environments (e.g. unit test mocks or H2), log warning and proceed safely
            log.warn("Could not acquire PostgreSQL advisory lock (environment may not be PostgreSQL): {}", e.getMessage());
        }
    }

    private void validateConfiguration() {
        if (properties.getUsername() == null || properties.getUsername().trim().isBlank()) {
            throw new IllegalStateException("Admin bootstrap is enabled but 'app.bootstrap.admin.username' is missing or blank.");
        }
        if (properties.getUsername().trim().length() < 3 || properties.getUsername().trim().length() > 50) {
            throw new IllegalStateException("Bootstrap admin username must be between 3 and 50 characters.");
        }
        if (properties.getEmail() == null || properties.getEmail().trim().isBlank() || !EMAIL_PATTERN.matcher(properties.getEmail().trim()).matches()) {
            throw new IllegalStateException("Admin bootstrap is enabled but 'app.bootstrap.admin.email' is invalid or missing.");
        }
        if (properties.getPassword() == null || properties.getPassword().isBlank()) {
            throw new IllegalStateException("Admin bootstrap is enabled but 'app.bootstrap.admin.password' is missing or blank.");
        }
        if (!PASSWORD_PATTERN.matcher(properties.getPassword()).matches()) {
            throw new IllegalStateException("Bootstrap admin password does not satisfy BR-AUTH-001 complexity policy (minimum 8 characters, at least 1 uppercase, 1 lowercase, 1 digit, 1 special character @$!%*?&).");
        }
    }
}
