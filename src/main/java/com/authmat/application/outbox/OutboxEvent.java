package com.authmat.application.outbox;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/// why no JPA indexes??
/// A composite partial index on (status, created_at) WHERE status = 'PENDING' means:
/// - Postgres only indexes PENDING rows — once published, rows fall out of the index
/// - The index stays tiny over time as most rows will be PUBLISHED
/// - Both the filter and sort are served by a single index scan
///
/// A regular JPA @Index has no concept of a WHERE clause — it indexes every row regardless of status.
/// So as your outbox grows over time with thousands of PUBLISHED rows, the index bloats with entries
/// you'll never query against.

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id
    private UUID id;

    // The entity: User, Order, etc.
    @Column(nullable = false, updatable = false, length = 255)
    private String aggregateType;

    // Entity's ID: userId, orderId, etc.
    @Column(nullable = false, updatable = false)
    private UUID aggregateId;

    @Column(nullable = false, updatable = false, length = 255)
    private String eventType;

    @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant publishedAt;

    private int retryCount;

    // Unbounded string w/o performance gain, however, useful for
    // Storing stack traces which can be large
    @Column(columnDefinition = "TEXT")
    private String lastError;

    public OutboxEvent(
            String aggregateType,
            UUID aggregateId, String eventType,
            String payload
    ) {
        this.id = UUID.randomUUID();
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.retryCount = 0;
        this.lastError = null;
    }

    // Avoid a no args constructor from being created in by other classes
    // forcing other classes to use parameterized constructor
    protected OutboxEvent(){}

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public void setStatus(OutboxStatus status) {
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public String getLastError() {
        return lastError;
    }
}
