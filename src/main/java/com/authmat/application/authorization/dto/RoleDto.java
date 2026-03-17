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

}
