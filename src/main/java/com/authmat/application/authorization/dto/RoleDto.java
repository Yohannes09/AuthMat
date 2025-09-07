package com.authmat.application.authorization.dto;

import java.time.LocalDateTime;

public record RoleDto(
        Long id, String name, String description, LocalDateTime createdAt, LocalDateTime updatedAt) {
}
