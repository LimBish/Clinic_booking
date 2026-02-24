package com.clinic.repository;

import com.clinic.model.User;
import com.clinic.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRole(Role role);

    long countByRole(Role role);

    long countByRoleAndEnabledTrue(Role role);

    long countByRoleAndCreatedAtBetween(Role role, LocalDateTime from, LocalDateTime to);

    List<User> findByRoleAndCreatedAtBetween(Role role, LocalDateTime from, LocalDateTime to);

}
