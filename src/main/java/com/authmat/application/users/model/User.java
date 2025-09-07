package com.authmat.application.users.model;

import com.authmat.application.authorization.entity.Role;
import com.authmat.application.constant.ValidationConstants;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_user_username", columnList = "username"),
                @Index(name = "idx_user_email", columnList = "email")
        }
)
@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User{
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "user_id_sequence")
    @SequenceGenerator(
            name = "user_id_sequence",
            sequenceName = "user_id_sequence",
            initialValue = 11_957_103,
            allocationSize = 9)
    private Long id;

    @Pattern(regexp = ValidationConstants.USERNAME_PATTERN)
    @Column(unique = true, nullable = false)
    private String username;

    @Pattern(regexp = ValidationConstants.PASSWORD_PATTERN)
    private String password;

    @Email(message = ValidationConstants.EMAIL_VALIDATION_MESSAGE)
    @Column(unique = true)
    private String email;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "account_non_expired", nullable = false)
    private boolean accountNonExpired = true;

    @Column(name = "account_non_locked", nullable = false)
    private boolean accountNonLocked = true;

    @Column(name = "credentials_non_expired", nullable = false)
    private boolean credentialsNonExpired = true;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    // Suggested improvement for concurrency. Prevents another thread from
    // overriding.
    @Version
    private Long version;

    //GitHub, Google, Local
    @Column(name = "provider")
    private String provider;

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "external_id", unique = true)
    private String externalId;
}
