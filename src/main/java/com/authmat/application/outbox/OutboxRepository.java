package com.authmat.application.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
    @Query(value = """
            SELECT * FROM outbox_events events
            WHERE events.status = 'PENDING'
            ORDER BY events.created_at ASC
            LIMIT 100
            FOR UPDATE SKIP LOCKED;
            """,
            nativeQuery = true
    )
    List<OutboxEvent> fetchPendingEvents();
}
