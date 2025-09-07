package com.authmat.application.util;

import com.authmat.application.authorization.dto.PermissionDto;
import com.authmat.application.authorization.entity.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PermissionMapper {
    PermissionDto entityToDto(Permission permission);
}
