package com.authmat.application.authorization.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum DefaultRoles {
    USER(
            "USER",
            "Basic permissions",
            Set.of(DefaultPermissions.USER_ACCESS_RESOURCES)
    ),
    SERVICE(
            "SERVICE",
            "Inter-service communication permitted.",
            Set.of()
    ),
    ADMIN(
            "ADMIN",
            "Elevated Privileges. ",
            Set.of(DefaultPermissions.ROLE_CREATE, DefaultPermissions.ROLE_READ, DefaultPermissions.ROLE_UPDATE)
    ),
    SUPER_ADMIN(
            "SUPER ADMIN",
            "All Privileges. ",
            Set.of(DefaultPermissions.ROLE_FULL_ACCESS)
    ),
    DEV(
            "DEV",
            "Access internal documentation. ",
            Set.of(DefaultPermissions.USER_ACCESS_RESOURCES)
    );

    private final String name;
    private final String description;
    private final Set<DefaultPermissions> defaultPermissions;

    public static Set<DefaultRoles> getAll(){
        return Arrays.stream(DefaultRoles.values()).collect(Collectors.toSet());
    }

}
