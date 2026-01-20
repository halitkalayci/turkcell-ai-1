package com.ecommerce.inventory.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Event DTO for OrderCreated event consumed from Kafka.
 * MUST match order-service event payload.
 * Per docs/events/event-catalog.md - OrderCreated (v1)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreatedEvent {

    private UUID eventId;
    private String eventType;
    private String version;
    private OffsetDateTime timestamp;
    private UUID orderId;
    private UUID customerId;
    private List<LineItem> lineItems;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LineItem {
        private UUID productId;
        private Integer quantity;
    }
}
