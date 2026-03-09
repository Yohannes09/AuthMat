package com.authmat.application.authorization.dto;

import java.time.Instant;

public record RoleDto(
        Long id, String name, String description, Instant createdAt, Instant updatedAt) {

    public static RoleDto of(Long id, String name){
        return new RoleDto(id, name, "Description not provided", Instant.now(), Instant.now());
    }
}
