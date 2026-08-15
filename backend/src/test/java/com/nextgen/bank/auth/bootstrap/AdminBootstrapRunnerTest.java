package com.nextgen.bank.auth.bootstrap;

import com.nextgen.bank.auth.domain.User;
import com.nextgen.bank.auth.event.UserRegisteredEvent;
import com.nextgen.bank.auth.repository.UserRepository;
import com.nextgen.bank.common.enums.UserRole;
import com.nextgen.bank.common.event.OutboxEventWriter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapRunnerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OutboxEventWriter outboxEventWriter;

    @Mock
    private EntityManager entityManager;

    private AdminBootstrapProperties properties;
    private AdminBootstrapRunner runner;

    @BeforeEach
    void setUp() {
        properties = new AdminBootstrapProperties();
        runner = new AdminBootstrapRunner(properties, userRepository, passwordEncoder, outboxEventWriter, entityManager);

        Query mockQuery = mock(Query.class);
        lenient().when(entityManager.createNativeQuery(anyString())).thenReturn(mockQuery);
        lenient().when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        lenient().when(mockQuery.getSingleResult()).thenReturn(null);
    }

    @Test
    @DisplayName("Should do nothing when bootstrap is disabled")
    void testBootstrapDisabled_DoesNothing() {
        properties.setEnabled(false);

        runner.run(new DefaultApplicationArguments());

        verify(entityManager, never()).createNativeQuery(anyString());
        verify(userRepository, never()).existsByRole(any());
        verify(userRepository, never()).save(any());
        verify(outboxEventWriter, never()).write(any());
    }

    @Test
    @DisplayName("Should acquire advisory lock and create exactly one ADMIN when enabled and no admin exists")
    void testBootstrapEnabled_NoAdmin_CreatesAdmin() {
        properties.setEnabled(true);
        properties.setUsername("sysadmin");
        properties.setEmail("admin@nextgenbank.com");
        properties.setPassword("SuperAdminPass2026!");

        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);
        when(userRepository.existsByUsername("sysadmin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@nextgenbank.com")).thenReturn(false);
        when(passwordEncoder.encode("SuperAdminPass2026!")).thenReturn("bcrypt_hash_admin_123");

        User savedUser = new User("sysadmin", "admin@nextgenbank.com", "bcrypt_hash_admin_123", UserRole.ADMIN);
        savedUser.setUserId(UUID.randomUUID());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        runner.run(new DefaultApplicationArguments());

        // Verify advisory lock was requested with the constant key
        verify(entityManager).createNativeQuery("SELECT pg_advisory_xact_lock(:lockKey)");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User created = userCaptor.getValue();

        assertThat(created.getUsername()).isEqualTo("sysadmin");
        assertThat(created.getEmail()).isEqualTo("admin@nextgenbank.com");
        assertThat(created.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(created.getPasswordHash()).isEqualTo("bcrypt_hash_admin_123");

        verify(outboxEventWriter).write(any(UserRegisteredEvent.class));
    }

    @Test
    @DisplayName("Should skip creation and leave existing admin unchanged when ADMIN already exists")
    void testBootstrapEnabled_AdminExists_Skips() {
        properties.setEnabled(true);
        properties.setUsername("sysadmin");
        properties.setEmail("admin@nextgenbank.com");
        properties.setPassword("SuperAdminPass2026!");

        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(true);

        runner.run(new DefaultApplicationArguments());

        verify(entityManager).createNativeQuery("SELECT pg_advisory_xact_lock(:lockKey)");
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
        verify(outboxEventWriter, never()).write(any());
    }

    @Test
    @DisplayName("Should fail startup when bootstrap is enabled but username is missing")
    void testValidation_MissingUsername_Fails() {
        properties.setEnabled(true);
        properties.setUsername("");
        properties.setEmail("admin@nextgenbank.com");
        properties.setPassword("SuperAdminPass2026!");

        assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("username");

        verify(entityManager, never()).createNativeQuery(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should fail startup when bootstrap is enabled but email is invalid")
    void testValidation_InvalidEmail_Fails() {
        properties.setEnabled(true);
        properties.setUsername("sysadmin");
        properties.setEmail("invalid-email-format");
        properties.setPassword("SuperAdminPass2026!");

        assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("email");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should fail startup when bootstrap is enabled but password is missing")
    void testValidation_MissingPassword_Fails() {
        properties.setEnabled(true);
        properties.setUsername("sysadmin");
        properties.setEmail("admin@nextgenbank.com");
        properties.setPassword("");

        assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("password");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should fail startup when bootstrap admin password violates BR-AUTH-001 complexity policy")
    void testValidation_WeakPassword_Fails() {
        properties.setEnabled(true);
        properties.setUsername("sysadmin");
        properties.setEmail("admin@nextgenbank.com");
        properties.setPassword("weakpass"); // Missing uppercase, digit, special char, < 8

        assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BR-AUTH-001");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Repeated bootstrap executions should remain idempotent")
    void testRepeatedBootstrap_IsIdempotent() {
        properties.setEnabled(true);
        properties.setUsername("sysadmin");
        properties.setEmail("admin@nextgenbank.com");
        properties.setPassword("SuperAdminPass2026!");

        // First run: no admin -> creates admin
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false).thenReturn(true);
        when(userRepository.existsByUsername("sysadmin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@nextgenbank.com")).thenReturn(false);
        when(passwordEncoder.encode("SuperAdminPass2026!")).thenReturn("hash_admin");

        User savedUser = new User("sysadmin", "admin@nextgenbank.com", "hash_admin", UserRole.ADMIN);
        savedUser.setUserId(UUID.randomUUID());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        runner.run(new DefaultApplicationArguments());

        // Second run: admin now exists -> skips
        runner.run(new DefaultApplicationArguments());

        verify(userRepository, times(1)).save(any());
        verify(outboxEventWriter, times(1)).write(any());
    }

    @Test
    @DisplayName("Should handle concurrent application startup gracefully without throwing error")
    void testConcurrentBootstrap_HandlesDuplicateKeyGracefully() {
        properties.setEnabled(true);
        properties.setUsername("sysadmin");
        properties.setEmail("admin@nextgenbank.com");
        properties.setPassword("SuperAdminPass2026!");

        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);
        when(userRepository.existsByUsername("sysadmin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@nextgenbank.com")).thenReturn(false);
        when(passwordEncoder.encode("SuperAdminPass2026!")).thenReturn("hash_admin");

        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        // Must not throw an unhandled exception
        runner.run(new DefaultApplicationArguments());

        verify(outboxEventWriter, never()).write(any());
    }
}
