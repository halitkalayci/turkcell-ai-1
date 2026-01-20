package com.ecommerce.inventory.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity for idempotency pattern (inbox).
 * Per docs/events/idempotency.md
 * 
 * Stores processed event IDs to prevent duplicate processing.
 */
@Entity
@Table(name = "inbox")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InboxEntity {

    @Id
    private UUID eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
