package com.ecommerce.inventory.infrastructure.messaging.consumer;

import com.ecommerce.inventory.application.usecase.DecrementStockUseCase;
import com.ecommerce.inventory.infrastructure.messaging.event.OrderCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * Kafka consumer for OrderCreated events.
 * Per docs/architecture/communication-matrix.md
 * 
 * Consumer group: inventory-service-order-events
 * Topic: order.events
 * Idempotency: Handled by DecrementStockUseCase (inbox pattern)
 */
@Component
@Slf4j
public class OrderCreatedEventConsumer {

    private final DecrementStockUseCase decrementStockUseCase;
    private final ObjectMapper objectMapper;

    public OrderCreatedEventConsumer(DecrementStockUseCase decrementStockUseCase) {
        this.decrementStockUseCase = decrementStockUseCase;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Spring Cloud Stream functional consumer.
     * Bean name matches application.yml binding configuration.
     */
    @Bean
    public Consumer<Message<String>> orderCreatedConsumer() {
        return message -> {
            try {
                String payload = message.getPayload();
                log.info("Received OrderCreated event: {}", payload);
                
                OrderCreatedEvent event = objectMapper.readValue(payload, OrderCreatedEvent.class);
                
                // Process event with idempotency
                decrementStockUseCase.execute(event);
                
                log.info("OrderCreated event processed successfully: eventId={}, orderId={}", 
                    event.getEventId(), event.getOrderId());
                    
            } catch (Exception e) {
                log.error("Failed to process OrderCreated event", e);
                // Exception will trigger retry or DLQ per Kafka consumer config
                throw new RuntimeException("Event processing failed", e);
            }
        };
    }
}
