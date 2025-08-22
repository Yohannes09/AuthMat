package com.authmat.application.users;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMapper {

    public static UserDto entityToDto(User user){
//        Set<String> permissions = user.getPermissions().stream()
//                .map(Permission::getName)
//                .collect(Collectors.toSet());

        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                //.permissions(permissions)
//                .accountNonExpired(user.isAccountNonExpired())
//                .accountNonLocked(user.isAccountNonLocked())
//                .credentialsNonExpired(user.isCredentialsNonExpired())
//                .enabled(user.isEnabled())
                .build();
    }

    public static UserPrincipal dtoToPrincipal(UserDto user){
        return UserPrincipal.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
//                .password(user.getPassword())
//                //.roles(user.getRoles())
//                .accountNonExpired(user.isAccountNonExpired())
//                .accountNonLocked(user.isAccountNonLocked())
//                .credentialsNonExpired(user.isCredentialsNonExpired())
//                .enabled(user.isEnabled())
                .build();
    }


    public static User principalToEntity(UserPrincipal user){
        return User.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .password(user.getPassword())
                //.roles(user.getRoles())
                .accountNonExpired(user.isAccountNonExpired())
                .accountNonLocked(user.isAccountNonLocked())
                .credentialsNonExpired(user.isCredentialsNonExpired())
                .enabled(user.isEnabled())
                .build();
    }
}