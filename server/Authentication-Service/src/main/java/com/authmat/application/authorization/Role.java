package com.authmat.application.authorization;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;


@Table(name = "roles")
//@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
@Getter
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
            initialValue = 3456
    )
    private Long id;

    @Column(nullable = false)
    private String role;

    private String description;

    @Column(name = "created_at", updatable = false, nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "is_system_role", nullable = false)
    private boolean isSystemRole = false;

    public Role(DefaultRoles defaultRole){
        this.role = defaultRole.getRole();
        this.description = defaultRole.getDescription();
    }

}
