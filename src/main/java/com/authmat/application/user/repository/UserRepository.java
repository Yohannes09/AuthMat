package com.authmat.application.user.repository;

import com.authmat.application.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    @Query("""
            SELECT user FROM User user
            WHERE LOWER(user.username) = LOWER(:usernameOrEmail)
            OR LOWER(user.email) = LOWER(:usernameOrEmail)
            """)
    Optional<User> findByUsernameOrEmail(@Param("usernameOrEmail") String usernameOrEmail);

    @Query("""
            SELECT CASE WHEN COUNT(user) > 0 THEN true ELSE false END
            FROM User user
            WHERE (LOWER(user.username) = LOWER(:username))
            OR LOWER(user.email) = LOWER(:email)
            """)
    boolean existsByUsernameOrEmail(
            @Param("username") String username,
            @Param("email") String email
    );

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    // TODO: externalId is of type UUID, maybe user's externalId should be of type String for flexibility
    Optional<User> findByExternalId(String externalId);
}
