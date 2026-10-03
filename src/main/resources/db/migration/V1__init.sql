CREATE TABLE role (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by TEXT NOT NULL,
    updated_by TEXT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    CONSTRAINT uk_role_name UNIQUE (name)
);

CREATE TABLE "user" (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by TEXT NOT NULL,
    updated_by TEXT NOT NULL,
    keycloak_id VARCHAR(36) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    CONSTRAINT uk_user_keycloak_id UNIQUE (keycloak_id),
    CONSTRAINT uk_user_email UNIQUE (email)
);

CREATE TABLE user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT pk_user_role PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES "user" (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES role (id) ON DELETE CASCADE
);

CREATE INDEX idx_user_role_role_id ON user_role (role_id);
