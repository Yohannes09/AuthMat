INSERT INTO roles (name, description, created_at)
VALUES
    ('ROLE_USER', 'Standard authenticated user. Can manage their own account and documents.', NOW()),
    ('ROLE_SUPPORT', 'Internal support staff. Read-only visibility into user accounts and documents for triage purposes.', NOW()),
    ('ROLE_ADMIN', 'Operational administrator. Can manage users, assign roles, and view system health.', NOW()),
    ('ROLE_SUPER_ADMIN', 'Highest privilege human operator. Includes all admin rights plus RBAC structure modification and system configuration.', NOW()),
    ('ROLE_SERVICE', 'Internal service account role for machine-to-machine communication between AuthMat and downstream services.', NOW()),
ON CONFLICT (name) DO NOTHING;