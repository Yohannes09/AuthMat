package com.authmat.application.authorization.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.EnumSet;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum DefaultPermission {
    DOCUMENT_READ("document:read",          "Read own documents"),
    DOCUMENT_WRITE("document:write",        "Upload and edit own documents"),
    DOCUMENT_DELETE("document:delete",      "Delete own documents"),
    DOCUMENT_SHARE("document:share",        "Share documents with other users"),

    ACCOUNT_READ("account:read",            "Read own account profile"),
    ACCOUNT_MANAGE("account:manage",        "Modify own account settings"),

    // Admin-level user management
    USER_READ("user:read",                  "Read any user profile"),
    USER_MANAGE("user:manage",              "Ban, suspend, or modify any user"),

    ROLE_READ("role:read",                  "View roles"),
    ROLE_MANAGE("role:manage",              "Create and modify roles"),
    ROLE_ASSIGN("role:assign",              "Assign roles to users"),
    PERMISSION_READ("permission:read",      "View permissions"),
    PERMISSION_MANAGE("permission:manage",  "Create and modify permissions"),

    ACTUATOR_VIEW("actuator:view",          "View Spring Actuator endpoints"),
    SYSTEM_CONFIG("system:config",          "Modify system configuration"),
    API_DOCS_VIEW("api:docs:view",          "View API documentation");

    private final String name;
    private final String description;

    public String getNameFormattedName(){
        return name.replace("_",":").toLowerCase();
    }

    public static Set<DefaultPermission> getAll(){
        return EnumSet.allOf(DefaultPermission.class);
    }
}
