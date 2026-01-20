package com.ecommerce.order.application.service;

import com.ecommerce.order.domain.event.OrderCreatedEvent;
import com.ecommerce.order.infrastructure.persistence.OutboxEntity;
import com.ecommerce.order.infrastructure.persistence.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for writing events to outbox table.
 * Per docs/events/outbox-pattern.md
 * 
 * MUST be called within the same transaction as business logic.
 */
@Service
@Slf4j
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Write OrderCreated event to outbox.
     * MUST be called within same transaction as order creation.
     * 
     * @param event OrderCreated domain event
     */
    @Transactional
    public void writeOrderCreatedEvent(OrderCreatedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            
            OutboxEntity outboxEntry = OutboxEntity.builder()
                .id(event.getEventId())
                .aggregateType("Order")
                .aggregateId(event.getOrderId())
                .eventType(event.getEventType())
                .payload(payload)
                .status(OutboxEntity.OutboxStatus.NEW)
                .createdAt(Instant.now())
                .build();
            
            outboxRepository.save(outboxEntry);
            
            log.info("OrderCreated event written to outbox: eventId={}, orderId={}", 
                event.getEventId(), event.getOrderId());
                
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event to JSON", e);
            throw new RuntimeException("Failed to write event to outbox", e);
        }
    }
}
