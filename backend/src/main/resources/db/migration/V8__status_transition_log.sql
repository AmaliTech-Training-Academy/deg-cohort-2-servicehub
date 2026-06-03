CREATE TABLE IF NOT EXISTS status_transition_log (
    id             BIGSERIAL    PRIMARY KEY,
    request_id     BIGINT       NOT NULL REFERENCES service_requests(id) ON DELETE CASCADE,
    from_status    VARCHAR(50)  NOT NULL,
    to_status      VARCHAR(50)  NOT NULL,
    changed_by_id  BIGINT       NOT NULL REFERENCES users(id),
    comment        VARCHAR(500),
    changed_at     TIMESTAMP    NOT NULL
);

CREATE INDEX idx_stl_request ON status_transition_log(request_id);
