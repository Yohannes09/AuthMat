package com.authmat.application.authorization.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.EnumSet;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum DefaultRole {
    USER(
            "ROLE_USER",
            "Standard authenticated user. Can manage their own account and documents.",
            Set.of(
                    DefaultPermission.ACCOUNT_READ,
                    DefaultPermission.ACCOUNT_MANAGE,
                    DefaultPermission.DOCUMENT_READ,
                    DefaultPermission.DOCUMENT_WRITE,
                    DefaultPermission.DOCUMENT_DELETE,
                    DefaultPermission.DOCUMENT_SHARE
            )
    ),

    SUPPORT(
            "ROLE_SUPPORT",
            "Internal support staff. Read-only visibility into user accounts and documents for triage purposes.",
            Set.of(
                    DefaultPermission.ACCOUNT_READ,
                    DefaultPermission.USER_READ,
                    DefaultPermission.DOCUMENT_READ,
                    DefaultPermission.ACTUATOR_VIEW
            )
    ),

    ADMIN(
            "ROLE_ADMIN",
            "Operational administrator. Can manage users, assign roles, and view system health.",
            Set.of(
                    DefaultPermission.ACCOUNT_READ,
                    DefaultPermission.ACCOUNT_MANAGE,
                    DefaultPermission.DOCUMENT_READ,
                    DefaultPermission.DOCUMENT_WRITE,
                    DefaultPermission.DOCUMENT_DELETE,
                    DefaultPermission.DOCUMENT_SHARE,
                    DefaultPermission.USER_READ,
                    DefaultPermission.USER_MANAGE,
                    DefaultPermission.ROLE_READ,
                    DefaultPermission.ROLE_ASSIGN,
                    DefaultPermission.PERMISSION_READ,
                    DefaultPermission.ACTUATOR_VIEW,
                    DefaultPermission.API_DOCS_VIEW
            )
    ),

    SUPER_ADMIN(
            "ROLE_SUPER_ADMIN",
            "Highest privilege human operator. Includes all admin rights plus RBAC structure modification and system configuration.",
            Set.of(
                    DefaultPermission.ACCOUNT_READ,
                    DefaultPermission.ACCOUNT_MANAGE,
                    DefaultPermission.DOCUMENT_READ,
                    DefaultPermission.DOCUMENT_WRITE,
                    DefaultPermission.DOCUMENT_DELETE,
                    DefaultPermission.DOCUMENT_SHARE,
                    DefaultPermission.USER_READ,
                    DefaultPermission.USER_MANAGE,
                    DefaultPermission.ROLE_READ,
                    DefaultPermission.ROLE_ASSIGN,
                    DefaultPermission.ROLE_MANAGE,
                    DefaultPermission.PERMISSION_READ,
                    DefaultPermission.PERMISSION_MANAGE,
                    DefaultPermission.ACTUATOR_VIEW,
                    DefaultPermission.API_DOCS_VIEW,
                    DefaultPermission.SYSTEM_CONFIG
            )
    ),

    SERVICE(
            "ROLE_SERVICE",
            "Internal service account role for machine-to-machine communication between AuthMat and downstream services.",
            Set.of(
                    DefaultPermission.USER_READ,
                    DefaultPermission.ROLE_READ,
                    DefaultPermission.PERMISSION_READ
            )
    );


    private final String name;
    private final String description;
    private final Set<DefaultPermission> permissions;

    public static Set<DefaultRole> getAll(){
        return EnumSet.allOf(DefaultRole.class);
    }

}
