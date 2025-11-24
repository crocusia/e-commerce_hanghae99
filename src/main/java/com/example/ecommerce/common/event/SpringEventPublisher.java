package com.example.ecommerce.common.event;

import com.example.ecommerce.common.exception.ErrorCode;
import com.example.ecommerce.common.exception.EventPublishException;
import com.example.ecommerce.common.outbox.domain.Outbox;
import com.example.ecommerce.common.outbox.repository.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SpringEventPublisher implements MessagePublisher {
    private final ApplicationEventPublisher eventPublisher;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void publish(DomainEvent event) {
        Outbox outboxEvent = convertToOutbox(event);
        outboxRepository.save(outboxEvent);
        eventPublisher.publishEvent(event);
    }

    private Outbox convertToOutbox(DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            return Outbox.create(
                event.getEventId(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getEventType(),
                payload
            );
        } catch (JsonProcessingException e) {
            throw new EventPublishException(ErrorCode.EVENT_SERIALIZATION_FAILED, e);
        }
    }

}
