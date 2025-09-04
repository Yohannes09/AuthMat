package com.authmat.application.util;

import com.authmat.application.authorization.entity.Permission;
import com.authmat.application.authorization.entity.Role;
import com.authmat.application.users.model.User;
import com.authmat.application.users.model.UserDto;
import com.authmat.application.authentication.models.UserPrincipal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    @Mapping(target = "permissions", source = "roles", qualifiedByName = "rolesToPermissionNames")
    UserDto entityToDto(User user);

    @Mapping(target = "roles", source = "roles")
    UserPrincipal entityToPrincipal(User user);

    UserPrincipal dtoToPrincipal(UserDto userDto);

    @Named("rolesToPermissionNames")
    default Set<String> mapRolesToPermissions(Set<Role> roles){
        return roles
                .stream()
                .flatMap(role -> role
                        .getPermissions()
                        .stream()
                        .map(Permission::getName))
                .collect(Collectors.toSet());
    }

}
