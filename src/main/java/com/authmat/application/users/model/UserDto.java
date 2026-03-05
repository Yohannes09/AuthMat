package com.authmat.application.users.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

// todo: de-lombok safely
@Builder
@NoArgsConstructor
@Getter
public class UserDto {
    public UserDto(Long id){
        this.id = id;
    }

    private Long id;
    private String username;
    private String email;
    private Set<String> permissions;
    private LocalDateTime updatedAt;
}
