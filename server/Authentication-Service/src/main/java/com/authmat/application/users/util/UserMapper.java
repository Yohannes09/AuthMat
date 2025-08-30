package com.authmat.application.users.util;

import com.authmat.application.authorization.entity.Permission;
import com.authmat.application.authorization.entity.Role;
import com.authmat.application.users.model.User;
import com.authmat.application.users.model.UserDto;
import com.authmat.application.users.model.UserPrincipal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    //UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @Mapping(target = "permissions", source = "roles", qualifiedByName = "rolesToPermissionNames")
    UserDto entityToDto(User user);

    @Mapping(target = "roles", source = "roles")
    UserPrincipal entityToPrincipal(User user);

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
