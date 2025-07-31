package com.authmat.application.authorization.persistence;

import com.authmat.application.authorization.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
}
