package com.authmat.authentication.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum DefaultRoles {
    GUEST("GUEST", "Lowest Permissions"),
    USER("USER", "Basic permissions"),
    SERVICE("SERVICE", "Inter-service communication permitted."),
    ADMIN("ADMIN", "Elevated Privileges. "),
    SUPER_ADMIN("SUPER ADMIN", "All Privileges. "),
    DEV("DEV", "Access internal documentation. ");

    private final String role;
    private final String description;

    public static Set<DefaultRoles> getAll(){
        return Arrays.stream(DefaultRoles.values()).collect(Collectors.toSet());
    }

}
