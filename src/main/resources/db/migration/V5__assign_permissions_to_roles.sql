INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_USER'
  AND p.name IN ('document:read', 'document:write', 'document:delete', 'document:share', 'account:read', 'account:manage')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_SUPPORT'
  AND p.name IN ('account:read', 'document:read', 'actuator:view', 'user:read')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_ADMIN'
    AND p.name IN (
        'account:read',
        'account:manage',
        'document:read',
        'document:write',
        'document:delete',
        'document:share',
        'user:read',
        'user:manage',
        'role:read',
        'role:assign',
        'role:manage',
        'permission:read',
        'permission:manage',
        'actuator:view',
        'api:docs:view',
        'system:config'
    )
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_SUPER_ADMIN'
    AND p.name IN (
        'account:read',
        'account:manage',
        'document:read',
        'document:write',
        'document:delete',
        'document:share',
        'user:read',
        'user:manage',
        'role:read',
        'role:assign',
        'permission:read',
        'actuator:view',
        'api:docs:view'
    )
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_SERVICE'
  AND p.name IN ('role:read', 'permission:read', 'user:read')
ON CONFLICT DO NOTHING;