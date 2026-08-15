package com.nextgen.bank.auth.repository;

import com.nextgen.bank.auth.domain.User;
import com.nextgen.bank.common.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByRole(UserRole role);

    List<User> findByRole(UserRole role);

    List<User> findByRoleIn(List<UserRole> roles);
}
