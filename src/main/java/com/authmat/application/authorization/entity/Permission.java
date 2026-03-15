package com.authmat.application.authorization.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Table(name = "permissions")
@Entity
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

    @Column(nullable = false)
    @Size(min = 4, max = 50)
    private String name;

    private String description;

    @Column(name = "created_at", updatable = false, nullable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private Instant updatedAt;

    protected Permission() {}

    public Permission(String name, String description){
        this.name = name;
        this.description = description;
    }


    public Long getId() {
        return this.id;
    }

    public @Size(min = 4, max = 50) String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }
}
