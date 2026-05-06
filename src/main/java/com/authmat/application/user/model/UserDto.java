package com.authmat.application.user.model;

import com.authmat.application.authorization.model.RoleDto;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

// TODO: Should i keep internal Id???
public record UserDto(
        Long id,
        String externalId,
        String username,
        String email,
        Collection<RoleDto> roles,
        Instant updatedAt) {

    public static UserDto sentinel(Long id, String username) {
        return new UserDto(
                id,
                "SENTINEL_USER",
                username,
                null,
                List.of(),
                null);
    }

}
