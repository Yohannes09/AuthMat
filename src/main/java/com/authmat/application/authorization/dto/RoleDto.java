package com.authmat.application.authorization.dto;

import java.time.Instant;
import java.util.Collection;

public record RoleDto(
        Long id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt,
        Collection<PermissionDto> permissions
) {

    // TODO: this method might be useless
    public static RoleDto of(Long id, String name, Collection<PermissionDto> permissions){
        return new RoleDto(id, name, "Description not provided", Instant.now(), Instant.now(), permissions);
    }
}
