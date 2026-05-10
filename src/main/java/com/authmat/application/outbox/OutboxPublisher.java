package com.authmat.application.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class OutboxPublisher {
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }


    @Scheduled(fixedRate = 60000)
    public void publishPendingEvents() {
        List<OutboxEvent> batch = outboxRepository.fetchPendingEvents();
        if(batch.isEmpty()) { return; }

        List<CompletableFuture<Void>> futures = batch.stream()
                .map(event -> kafkaTemplate
                                .send(event.getTopic(), event.getIdempotencyKey(), event.getPayload())
                                .thenRun(() -> event.setStatus(OutboxStatus.PUBLISHED))
                                .exceptionally(e -> {
                                    event.setStatus(OutboxStatus.FAILED);
                                    event.setLastError(e.getMessage());
                                    event.incrementRetryCount();
                                    log.debug("Failed to publish event: {}",
                                            event.getAggregateType(), e);
                                    return null;
                                }))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        outboxRepository.saveAll(batch);

        long published = batch.stream()
                .filter(event -> event.getStatus() == OutboxStatus.PUBLISHED)
                .count();

        log.info("Published events {}/{}", published, batch.size());
    }

}