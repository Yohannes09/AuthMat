package com.authmat.application.util;

import com.authmat.application.authorization.dto.RoleDto;
import com.authmat.application.authorization.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {PermissionMapper.class})
public interface RoleMapper {
    RoleDto entityToDto(Role role);
}
