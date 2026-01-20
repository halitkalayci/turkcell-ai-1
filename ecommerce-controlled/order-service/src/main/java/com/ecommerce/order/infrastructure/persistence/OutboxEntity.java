package com.ecommerce.order.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity for transactional outbox pattern.
 * Per docs/events/outbox-pattern.md
 * 
 * Stores domain events for guaranteed at-least-once delivery to Kafka.
 */
@Entity
@Table(name = "outbox", indexes = {
    @Index(name = "idx_outbox_status_created", columnList = "status, created_at"),
    @Index(name = "idx_outbox_aggregate", columnList = "aggregate_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEntity {

    @Id
    private UUID id;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Outbox event status lifecycle
     */
    public enum OutboxStatus {
        /** Event created, waiting to be published */
        NEW,
        /** Successfully published to Kafka */
        SENT,
        /** Failed after max retries, requires manual intervention */
        FAILED
    }

    /**
     * Mark event as successfully sent
     */
    public void markAsSent() {
        this.status = OutboxStatus.SENT;
        this.sentAt = Instant.now();
        this.errorMessage = null;
    }

    /**
     * Mark event as failed with error message
     */
    public void markAsFailed(String errorMessage) {
        this.status = OutboxStatus.FAILED;
        this.errorMessage = errorMessage;
    }
}
