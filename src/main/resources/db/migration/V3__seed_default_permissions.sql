INSERT INTO permissions (name, description, external_id, created_at, updated_at)
VALUES
    ('document:read', 'Read own documents', DEFAULT, NOW(), NOW()),
    ('document:write', 'Upload and edit own documents', DEFAULT, NOW(), NOW()),
    ('document:delete', 'Delete own documents', DEFAULT, NOW(), NOW()),
    ('document:share', 'Share documents with other users', DEFAULT, NOW(), NOW()),
    ('account:read', 'Read own account profile', DEFAULT, NOW(), NOW()),
    ('account:manage', 'Modify own account settings', DEFAULT, NOW(), NOW()),
    ('user:read', 'Read any user profile', DEFAULT, NOW(), NOW()),
    ('user:manage', 'Ban, suspend, or modify any user', DEFAULT, NOW(), NOW()),
    ('role:read', 'View roles', DEFAULT, NOW(), NOW()),
    ('role:manage', 'Create and modify roles', DEFAULT, NOW(), NOW()),
    ('role:assign', 'Assign roles to users', DEFAULT, NOW(), NOW()),
    ('permission:read', 'View permissions', DEFAULT, NOW(), NOW()),
    ('permission:manage', 'Create and modify permissions', DEFAULT, NOW(), NOW()),
    ('actuator:view', 'View Spring Actuator endpoints', DEFAULT, NOW(), NOW()),
    ('system:config', 'Modify system configuration', DEFAULT, NOW(), NOW()),
    ('api:docs:view', 'View API documentation', DEFAULT, NOW(), NOW())
ON CONFLICT (name) DO NOTHING;