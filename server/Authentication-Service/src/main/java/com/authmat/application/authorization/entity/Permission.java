package com.authmat.application.authorization.entity;

import com.authmat.application.authorization.constant.DefaultPermission;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Permission {
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "permission_id_sequence"
    )
    @SequenceGenerator(
            name = "permission_id_sequence",
            sequenceName = "permission_id_sequence",
            initialValue = 6456,
            allocationSize = 39
    )
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;


    private String description;

    @Column(name = "created_at", updatable = false, nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Permission(DefaultPermission defaultPermission){
        this.name = defaultPermission.getName();
        this.description = defaultPermission.getDescription();
    }
}
