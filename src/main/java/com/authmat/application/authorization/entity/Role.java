package com.authmat.application.authorization.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;


@Table(name = "roles")
@Entity
public class Role {
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "role_id_sequence"
    )
    @SequenceGenerator(
            name = "role_id_sequence",
            sequenceName = "role_id_sequence",
            initialValue = 3456,
            allocationSize = 63
    )
    private Long id;

    @Column(nullable = false)
    @Size(min = 4, max = 25)
    private String name;

    private String description;

    @Column(name = "created_at", updatable = false, nullable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private Instant updatedAt;

    @Column(name = "is_system_role", nullable = false)
    private boolean isSystemRole = false;

    @ManyToMany
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

    protected Role(){}

    public Role(String name, String description){
        this.name = name;
        this.description = description;
    }

    public Role(String name, String description, boolean isSystemRole){
        this.name = name;
        this.description = description;
        this.isSystemRole = isSystemRole;
    }


    public Set<Permission> getPermissions() {
        return permissions;
    }

    public boolean isSystemRole() {
        return isSystemRole;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public Long getId() {
        return id;
    }
}
