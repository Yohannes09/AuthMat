package com.authmat.application.user;

import com.authmat.application.authentication.models.UserDetailsImpl;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    //@Mapping(target = "permissions", source = "roles", qualifiedByName = "rolesToPermissionNames")
    UserDto entityToDto(User user);

    UserDetailsImpl dtoToUserDetails(UserDto userDto);
}
