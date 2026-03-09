package com.authmat.application.authorization.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
public enum DefaultRole {
    USER(
            "BASIC",
            "Baseline access.",
            Set.of(DefaultPermission.BASIC_USER)),

    ELEVATED(
            "ELEVATED",
            "Developer type access to view system performance.",
            Set.of(
                    DefaultPermission.BASIC_USER,
                    DefaultPermission.ACTUATOR_VIEW,
                    DefaultPermission.API_DOCS_VIEW)),

    ADMIN(
            "ADMIN",
            "Developer + Basic + Sensitive operations.",
            Set.of(
                    DefaultPermission.ROLE_MANAGE,
                    DefaultPermission.ROLE_ASSIGN,
                    DefaultPermission.ACCOUNT_MANAGE,
                    DefaultPermission.PERMISSION_MANAGE)),

    SUPER_ADMIN(
            "SUPER_ADMIN",
            "Highest authority.",
            Set.of(
                    DefaultPermission.ROLE_MANAGE,
                    DefaultPermission.ROLE_ASSIGN,
                    DefaultPermission.ACCOUNT_MANAGE,
                    DefaultPermission.PERMISSION_MANAGE,
                    DefaultPermission.SYSTEM_CONFIG));


    private final String name;
    private final String description;
    private final Set<DefaultPermission> permissions;

    public static Set<DefaultRole> getAll(){
        return EnumSet.allOf(DefaultRole.class);
    }

    public Set<String> getAuthorities(){
        Set<String> authorities = this.permissions.stream()
                .map(DefaultPermission::getName)
                .collect(Collectors.toSet());
        authorities.add(this.name);
        return authorities;
    }

    public static List<String> getSystemRoles(){
        return Stream.of(ELEVATED, ADMIN, SUPER_ADMIN).map(DefaultRole::getName).toList();
    }
}
