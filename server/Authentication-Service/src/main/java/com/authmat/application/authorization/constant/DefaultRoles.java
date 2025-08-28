package com.authmat.application.authorization.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.EnumSet;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum DefaultRoles {
    BASIC(Set.of(DefaultPermission.BASIC_USER), "", ""),
    ELEVATED(Set.of(DefaultPermission.BASIC_USER, DefaultPermission.ACTUATOR_VIEW), "", ""),
    ADMIN(Set.of(), "", ""),
    SUPER_ADMIN(Set.of(), "", "");

    private final Set<DefaultPermission> permissions;
    private final String name;
    private final String description;

    public static Set<DefaultRoles> getAll(){
        return EnumSet.allOf(DefaultRoles.class);
    }

}
