package com.authmat.application.user;

import com.authmat.application.user.model.User;
import com.authmat.application.user.model.UserDetailsImpl;
import com.authmat.application.user.model.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    UserDto entityToDto(User user);
    UserDetailsImpl entityToUserDetails(User user);
}
