package com.ecommerce.order.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Domain event representing order creation.
 * Per docs/events/event-catalog.md - OrderCreated (v1)
 * 
 * Published to Kafka topic: order.events
 * Key: orderId (for ordering guarantees per order)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreatedEvent {

    /**
     * Unique event ID (also used as outbox ID)
     * Used by consumers for idempotency
     */
    private UUID eventId;

    /**
     * Event type identifier
     */
    private String eventType = "OrderCreated";

    /**
     * Event schema version
     */
    private String version = "1";

    /**
     * Event creation timestamp (ISO-8601)
     */
    private OffsetDateTime timestamp;

    /**
     * Order aggregate ID (also Kafka key)
     */
    private UUID orderId;

    /**
     * Customer who created the order
     */
    private UUID customerId;

    /**
     * Line items in the order
     */
    private List<LineItem> lineItems;

    /**
     * Line item representation for event payload
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LineItem {
        private UUID productId;
        private Integer quantity;
    }
}
