package com.authmat.application.authorization.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.EnumSet;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum DefaultPermission {
    BASIC_USER("basic:user", ""),
    ACTUATOR_VIEW("actuator:view", ""),
    ACCOUNT_MANAGE("account:manage", ""),
    ROLE_ASSIGN("role:assign", ""),
    PERMISSION_MANAGE("permission:manage", ""),
    ROLE_MANAGE("role:manage", ""),
    API_DOCS_VIEW("api-documentation:view", ""),
    SYSTEM_CONFIG("system:config", "");

    private final String name;
    private final String description;

    public String getNameFormattedName(){
        return name.replace("_",":").toLowerCase();
    }

    public static Set<DefaultPermission> getAll(){
        return EnumSet.allOf(DefaultPermission.class);
    }

}
