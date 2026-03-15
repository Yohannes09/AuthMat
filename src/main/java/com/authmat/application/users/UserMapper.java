package com.authmat.application.users;

import com.authmat.application.authentication.models.UserPrincipal;
import com.authmat.application.users.model.User;
import com.authmat.application.users.model.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    //@Mapping(target = "permissions", source = "roles", qualifiedByName = "rolesToPermissionNames")
    UserDto entityToDto(User user);

    UserPrincipal dtoToPrincipal(UserDto userDto);
}
