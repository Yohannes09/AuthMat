CREATE TABLE IF NOT EXISTS outbox_events(
    id                  UUID            NOT NULL PRIMARY KEY,
    aggregate_type      VARCHAR(255)    NOT NULL,
    aggregate_id        VARCHAR(255)    NOT NULL,
    event_type          VARCHAR(255)    NOT NULL,
    payload             JSONB           NOT NULL,
    status              VARCHAR(50)     NOT NULL,
    created_at          TIMESTAMP       NOT NULL,
    published_at        TIMESTAMP,
    retry_count         INTEGER         NOT NULL DEFAULT 0,
    last_error          TEXT
);

CREATE INDEX IF NOT EXISTS idx_outbox_pending ON outbox_events (status, created_at) WHERE status = 'PENDING';