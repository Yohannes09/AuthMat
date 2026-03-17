package com.authmat.application.outbox;

import com.authmat.events.NewUserEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class OutboxUserEventPublisher implements UserEventPublisher{
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxUserEventPublisher(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }


    @Override
    public void userCreatedEvent(NewUserEvent userEvent){

        try {
            OutboxEvent outbox = new OutboxEvent(
                    userEvent.aggregateType(),
                    userEvent.userId(),
                    userEvent.eventType(),
                    objectMapper.writeValueAsString(userEvent)
            );

            outboxRepository.save(outbox);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void userDeletedEvent() {

    }
}
