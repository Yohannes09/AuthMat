package com.authmat.application.authorization.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.EnumSet;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum DefaultRole {
    BASIC(
            "ROLE_BASIC",
            "",
            Set.of(DefaultPermission.BASIC_USER)),

    ELEVATED(
            "ROLE_ELEVATED",
            "",
            Set.of(DefaultPermission.BASIC_USER, DefaultPermission.ACTUATOR_VIEW)),

    ADMIN(
            "ROLE_ADMIN",
            "",
            Set.of()),

    SUPER_ADMIN(
            "ROLE_SUPER_ADMIN",
            "Highest authority.",
            Set.of());


    private final String name;
    private final String description;
    private final Set<DefaultPermission> permissions;

    public static Set<DefaultRole> getAll(){
        return EnumSet.allOf(DefaultRole.class);
    }

}
