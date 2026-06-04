DROP INDEX IF EXISTS idx_stl_request;
DROP TABLE IF EXISTS status_transition_log;

CREATE TABLE IF NOT EXISTS comments (
    id                BIGSERIAL     PRIMARY KEY,
    request_id        BIGINT        NOT NULL REFERENCES service_requests(id) ON DELETE CASCADE,
    author_id         BIGINT        NOT NULL REFERENCES users(id),
    body              VARCHAR(1000) NOT NULL,
    system_generated  BOOLEAN       NOT NULL DEFAULT false,
    created_at        TIMESTAMP     NOT NULL
);

CREATE INDEX idx_comments_request ON comments(request_id);
