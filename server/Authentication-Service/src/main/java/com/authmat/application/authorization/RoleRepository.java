package com.authmat.application.authorization;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleIgnoreCase(String role);

    boolean existsByRole(String role);
}
