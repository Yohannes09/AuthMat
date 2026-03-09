package com.authmat.application.authorization.repository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PermissionCache {
    private final PermissionRepository permissionRepository;

    private static final String PERMISSION_KEY = "auth:permission:";
}
