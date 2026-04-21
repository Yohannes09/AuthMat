INSERT INTO permissions (name, description, external_id, created_at, updated_at)
VALUES
    ('document:read', 'Read own documents', DEFAULT, NOW(), NOW()),
    ('document:write', 'Upload and edit own documents', DEFAULT, NOW(), NOW()),
    ('document:delete', 'Delete own documents', DEFAULT, NOW(), NOW())
--    ('document:share', 'Share documents with other users', NOW()),
--    ('account:read', 'Read own account profile', NOW()),
--    ('account:manage', 'Modify own account settings', NOW()),
--    ('user:read', 'Read any user profile', NOW()),
--    ('user:manage', 'Ban, suspend, or modify any user', NOW()),
--    ('role:read', 'View roles', NOW()),
--    ('role:manage', 'Create and modify roles', NOW()),
--    ('role:assign', 'Assign roles to users', NOW()),
--    ('permission:read', 'View permissions', NOW()),
--    ('permission:manage', 'Create and modify permissions', NOW()),
--    ('actuator:view', 'View Spring Actuator endpoints', NOW()),
--    ('system:config', 'Modify system configuration', NOW()),
--    ('api:docs:view', 'View API documentation', NOW())
ON CONFLICT (name) DO NOTHING;