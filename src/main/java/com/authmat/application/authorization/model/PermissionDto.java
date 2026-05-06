package com.authmat.application.authorization.model;

import java.time.Instant;

public record PermissionDto(
        Long id, String name, String description, Instant createdAt, Instant updatedAt) {
}
