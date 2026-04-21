CREATE SEQUENCE IF NOT EXISTS role_id_sequence
    START WITH 3456
    INCREMENT BY 63;

CREATE SEQUENCE IF NOT EXISTS permission_id_sequence
    START WITH 6456
    INCREMENT BY 39;

CREATE TABLE IF NOT EXISTS roles (
    id                      BIGINT          PRIMARY KEY DEFAULT nextval('role_id_sequence'),
    external_id             UUID            NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    name                    VARCHAR(50)     NOT NULL UNIQUE,
    description             VARCHAR(255),
    created_at              TIMESTAMP       NOT NULL,
    updated_at              TIMESTAMP       NOT NULL,
    is_system_role          BOOLEAN         DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS permissions (
    id                      BIGINT          PRIMARY KEY DEFAULT nextval('permission_id_sequence'),
    external_id             UUID            NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    name                    VARCHAR(50)     NOT NULL UNIQUE,
    description             VARCHAR(255),
    created_at              TIMESTAMP       NOT NULL,
    updated_at              TIMESTAMP       NOT NULL
);

CREATE TABLE IF NOT EXISTS role_permissions(
    role_id BIGINT REFERENCES roles(id),
    permission_id BIGINT REFERENCES permissions(id),
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS user_roles(
    user_id BIGINT REFERENCES users(id),
    role_id BIGINT REFERENCES roles(id),
    PRIMARY KEY(user_id, role_id)
);