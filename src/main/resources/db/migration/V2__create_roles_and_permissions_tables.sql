CREATE SEQUENCE IF NOT EXISTS role_id_sequence
    START WITH 3456
    INCREMENT BY 63;

CREATE SEQUENCE IF NOT EXISTS permission_id_sequence
    START WITH 6456
    INCREMENT BY 39;

CREATE TABLE roles (
    id                      BIGINT          PRIMARY KEY DEFAULT nextval('role_id_sequence'),
    external_id             UUID            NOT NULL UNIQUE,
    name                    VARCHAR(50)     NOT NULL UNIQUE,
    description             VARCHAR(255),
    created_at              TIMESTAMP       NOT NULL,
    updated_at              TIMESTAMP       NOT NULL,
    is_system_role          BOOLEAN         DEFAULT FALSE;
);

CREATE TABLE permissions (
    id                      BIGINT          PRIMARY KEY DEFAULT nextval('permission_id_sequence'),
    external_id             UUID            NOT NULL UNIQUE,
    name                    VARCHAR(50)     NOT NULL UNIQUE,
    description             VARCHAR(255),
    created_at              TIMESTAMP       NOT NULL,
    updated_at              TIMESTAMP       NOT NULL,
);