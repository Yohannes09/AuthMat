package com.authmat.application.authorization.entity;

import com.authmat.application.authorization.constant.DefaultRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;


@Table(name = "roles")
@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "is_system_role", nullable = false)
    private boolean isSystemRole = false;

    @ManyToMany
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

    public Role(DefaultRole defaultRole){
        this.name = defaultRole.getName();
        this.description = defaultRole.getDescription();
    }

}
