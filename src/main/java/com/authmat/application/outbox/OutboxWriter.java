package com.authmat.application.outbox;

import com.authmat.application.exception.InternalException;
import com.authmat.events.NewUserEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OutboxWriter {
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxWriter(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    public void userCreatedEvent(NewUserEvent userEvent){
        try {
            OutboxEvent outbox = new OutboxEvent(
                    userEvent.aggregateType(),
                    userEvent.userId(),
                    userEvent.eventType(),
                    "authmat.users.v1",
                    objectMapper.writeValueAsString(userEvent));

            outboxRepository.save(outbox);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate outbox event suppressed for id: {}:{}:{}",
                    userEvent.aggregateType(), userEvent.userId(), userEvent.eventType());
        }catch (JsonProcessingException e) {
            log.debug("Error parsing NewUserEvent into JSON", e);
            throw new InternalException("Failed to save NewUserEvent. Could not parse into JSON");
        }
    }

    public void userDeletedEvent() {

    }

}
