CREATE TABLE outbox_events(
    id                  UUID            NOT NULL PRIMARY KEY,
    aggregate_type      VARCHAR(255)    NOT NULL,
    aggregate_id        UUID            NOT NULL,
    event_type          VARCHAR(255)    NOT NULL,
    payload             JSONB           NOT NULL,
    status              VARCHAR(50)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL,
    published_at        TIMESTAMP,
    retry_count         INTEGER         NOT NULL DEFAULT 0
);

CREATE INDEX idx_outbox_pending ON outbox_events (status, created_at) WHERE status = 'PENDING';