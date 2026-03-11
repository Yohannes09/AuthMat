package com.authmat.application.users.model;

import com.authmat.application.authorization.dto.RoleDto;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

// todo: de-lombok safely, use record instead.
@NoArgsConstructor
@Getter
public class UserDto {
    public UserDto(Long id, String username){
        this.id = id;
        this.username = username;
    }

    // TODO: Should i keep internal Id???
    private Long id;
    private String externalId;
    private String username;
    private String email;
    private Set<RoleDto> roles;
    private LocalDateTime updatedAt;
}
