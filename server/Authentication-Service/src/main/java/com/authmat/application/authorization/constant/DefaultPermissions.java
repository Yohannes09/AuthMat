package com.authmat.application.authorization.constant;

import com.authmat.application.config.GeneralApplicationConfig;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum DefaultPermissions {
    USER_ACCESS_RESOURCES("AUTH_USER_ACCESS", "Basic permission for regular users."),
    ROLE_FULL_ACCESS("AUTH_FULL_ACCESS", "Read, Update, Delete, and Create roles."),
    ROLE_CREATE("AUTH_ROLE_CREATE", "Create a new role."),
    ROLE_DELETE("AUTH_ROLE_DELETE", "Delete an existing role."),
    ROLE_UPDATE("AUTH_ROLE_UPDATE", "Update an existing role."),
    ROLE_READ("AUTH_ROLE_READ", "Read an existing role.");

    private final String name;
    private final String description;

    public String getFormattedName(){
        return name.replace("_",":").toLowerCase();
    }

}
