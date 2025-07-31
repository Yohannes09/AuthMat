package com.authmat.application.authorization.persistence;

import com.authmat.application.authorization.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    List<Role> findAllByNameIgnoreCase(List<String> roleNames);

    Optional<Role> findByRoleIgnoreCase(String role);

    boolean existsByRole(String role);
}
