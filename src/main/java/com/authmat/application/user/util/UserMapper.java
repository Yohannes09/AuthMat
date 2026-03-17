package com.authmat.application.user.util;

import com.authmat.application.authentication.models.UserDetailsImpl;
import com.authmat.application.user.entity.User;
import com.authmat.application.user.dto.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    //@Mapping(target = "permissions", source = "roles", qualifiedByName = "rolesToPermissionNames")
    UserDto entityToDto(User user);

    UserDetailsImpl dtoToUserDetails(UserDto userDto);
}
