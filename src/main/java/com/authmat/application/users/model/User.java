package com.authmat.application.users.model;

import com.authmat.application.authorization.entity.Role;
import com.authmat.application.constant.ValidationConstants;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Table(name = "users")
@Entity
@Getter
@NoArgsConstructor
public class User{
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "user_id_sequence"
    )
    @SequenceGenerator(
            name = "user_id_sequence",
            sequenceName = "user_id_sequence",
            initialValue = 11_957_103,
            allocationSize = 9
    )
    private Long id;

    @Column(name = "external_id", unique = true)
    private UUID externalId;

    @Column(unique = true, nullable = false)
    private String username;

    private String hashedPassword;

    @Email(message = ValidationConstants.EMAIL_VALIDATION_MESSAGE)
    @Column(unique = true)
    private String email;

    @ManyToMany(fetch = FetchType.LAZY)
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

    // Suggested improvement for concurrency. Prevents another thread from overriding.
    @Version
    private Long version;

    @Column(name = "provider")
    private String provider;

    @Column(name = "provider_id")
    private String providerId;

    @PrePersist
    private void generateExternalId(){
        if(this.externalId == null) this.externalId = UUID.randomUUID();
    }


    public User(
            String username,
            String hashedPassword,
            String email,
            String provider,
            String providerId,
            Collection<Role> roles) {
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.email = email;
        this.provider = provider;
        this.providerId = providerId;
        this.roles.addAll(roles);
    }

    public User(
            String username,
            String hashedPassword,
            String email,
            String provider,
            String providerId) {
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.email = email;
        this.provider = provider;
        this.providerId = providerId;
    }

    public void lockAccount() { this.accountNonLocked = false; }
    public void unlockAccount() { this.accountNonLocked = true; }
    public void disableAccount() { this.enabled = false; }
    public void enableAccount() { this.enabled = true; }
    public void updatePassword(String newHashedPassword) { this.hashedPassword = newHashedPassword; }
    public void updateUsername(String newUsername) { this.username = newUsername; }
    public void updateEmail(String newEmail){ this.email = email;}
    public void addRole(Role role){this.roles.add(role);}
    public void addRoles(Collection<Role> roles){this.roles.addAll(roles);}
}
