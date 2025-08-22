package com.authmat.application.users.mapper;

import com.authmat.application.authorization.entity.Permission;
import com.authmat.application.authorization.entity.Role;
import com.authmat.application.users.User;
import com.authmat.application.users.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @Mapping(target = "permissions", source = "roles", qualifiedByName = "rolesToPermissionNames")
    UserDto entityToDto(User user);

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
