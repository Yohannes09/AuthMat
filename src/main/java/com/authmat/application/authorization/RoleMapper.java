package com.authmat.application.authorization;

import com.authmat.application.authorization.dto.RoleDto;
import com.authmat.application.authorization.entity.Role;

public interface RoleMapper {
    RoleDto entityToDto(Role role);
}
