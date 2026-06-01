CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL    PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    full_name   VARCHAR(255) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(50)  NOT NULL,
    department  VARCHAR(255),
    created_at  TIMESTAMP
);

CREATE TABLE IF NOT EXISTS departments (
    id            BIGSERIAL    PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    category      VARCHAR(50)  NOT NULL UNIQUE,
    contact_email VARCHAR(255),
    is_active     BOOLEAN      NOT NULL DEFAULT true
);

CREATE TABLE IF NOT EXISTS sla_policies (
    id                    BIGSERIAL   PRIMARY KEY,
    priority              VARCHAR(50) NOT NULL UNIQUE,
    response_time_hours   INTEGER     NOT NULL,
    resolution_time_hours INTEGER     NOT NULL
);

CREATE TABLE IF NOT EXISTS service_requests (
    id             BIGSERIAL    PRIMARY KEY,
    title          VARCHAR(255) NOT NULL,
    description    TEXT,
    category       VARCHAR(50)  NOT NULL,
    priority       VARCHAR(50)  NOT NULL,
    status         VARCHAR(50)  NOT NULL,
    department_id  BIGINT       REFERENCES departments(id),
    assigned_to_id BIGINT       REFERENCES users(id),
    requester_id   BIGINT       NOT NULL REFERENCES users(id),
    sla_deadline   TIMESTAMP,
    created_at     TIMESTAMP,
    updated_at     TIMESTAMP,
    resolved_at    TIMESTAMP
);
