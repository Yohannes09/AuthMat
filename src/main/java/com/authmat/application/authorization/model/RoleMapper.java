package com.authmat.application.authorization.model;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {PermissionMapper.class})
public interface RoleMapper {
    RoleDto entityToDto(Role role);
}
