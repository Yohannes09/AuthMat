package com.authmat.application.authorization.repository;

import com.authmat.application.authorization.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    @Query("""
            SELECT role FROM Role role
            WHERE role.name IN (:roleNames)
            """)
    List<Role> findAllByNameIgnoreCase(@Param("roleNames") List<String> roleNames);

    boolean existsByName(String name);

    Optional<Role> findByName(String name);
}
