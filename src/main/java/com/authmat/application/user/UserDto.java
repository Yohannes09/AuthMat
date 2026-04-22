package com.authmat.application.user;

import com.authmat.application.authorization.dto.RoleDto;

import java.time.Instant;
import java.util.Collection;

// TODO: Should i keep internal Id???
public record UserDto(
        Long id,
        String externalId,
        String username,
        String email,
        Collection<RoleDto> roles,
        Instant updatedAt
) {
    public static UserDto of(Long id, String username) {
        return new UserDto(id, username, null, null, null, null);
    }

}
