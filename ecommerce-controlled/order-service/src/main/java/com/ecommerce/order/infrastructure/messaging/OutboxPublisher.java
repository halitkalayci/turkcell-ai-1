package com.ecommerce.order.infrastructure.messaging;

import com.ecommerce.order.infrastructure.persistence.OutboxEntity;
import com.ecommerce.order.infrastructure.persistence.OutboxRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Polling publisher for outbox pattern.
 * Per docs/events/outbox-pattern.md
 * 
 * Runs in SEPARATE transaction from business write (critical!).
 * Publishes NEW events to Kafka with at-least-once semantics.
 */
@Component
@Slf4j
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final StreamBridge streamBridge;
    
    // Retry configuration per outbox-pattern.md
    private static final int MAX_RETRIES = 5;
    private static final long[] BACKOFF_MS = {100, 200, 400, 800, 1600};

    public OutboxPublisher(OutboxRepository outboxRepository, StreamBridge streamBridge) {
        this.outboxRepository = outboxRepository;
        this.streamBridge = streamBridge;
    }

    /**
     * Poll and publish NEW outbox events.
     * Scheduled every 5 seconds per outbox-pattern.md.
     * Batch size: 10 events.
     */
    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {
        List<OutboxEntity> newEvents = outboxRepository.findNewEventsForPublishing();
        
        if (newEvents.isEmpty()) {
            return;
        }
        
        log.info("Found {} NEW events to publish", newEvents.size());
        
        // Limit to batch size of 10 per outbox-pattern.md
        List<OutboxEntity> batch = newEvents.stream().limit(10).toList();
        
        for (OutboxEntity event : batch) {
            publishEventWithRetry(event);
        }
    }

    /**
     * Publish single event with retry logic.
     * Max 5 attempts with exponential backoff.
     */
    private void publishEventWithRetry(OutboxEntity event) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                // Publish to Kafka using Spring Cloud Stream
                // Topic: order.events, Key: aggregateId (orderId)
                boolean sent = streamBridge.send(
                    "orderEvents-out-0",
                    org.springframework.messaging.support.MessageBuilder
                        .withPayload(event.getPayload())
                        .setHeader("messageKey", event.getAggregateId().toString())
                        .build()
                );
                
                if (sent) {
                    markEventAsSent(event);
                    log.info("Event published successfully: eventId={}, orderId={}", 
                        event.getId(), event.getAggregateId());
                    return;
                }
                
            } catch (Exception e) {
                log.warn("Failed to publish event (attempt {}/{}): eventId={}, error={}", 
                    attempt, MAX_RETRIES, event.getId(), e.getMessage());
                
                if (attempt < MAX_RETRIES) {
                    // Exponential backoff
                    try {
                        Thread.sleep(BACKOFF_MS[attempt - 1]);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    // Max retries exceeded - mark as FAILED
                    markEventAsFailed(event, e.getMessage());
                    log.error("Event marked as FAILED after {} retries: eventId={}", 
                        MAX_RETRIES, event.getId());
                }
            }
        }
    }

    /**
     * Mark event as SENT in separate transaction.
     */
    @Transactional
    protected void markEventAsSent(OutboxEntity event) {
        event.markAsSent();
        outboxRepository.save(event);
    }

    /**
     * Mark event as FAILED in separate transaction.
     */
    @Transactional
    protected void markEventAsFailed(OutboxEntity event, String errorMessage) {
        event.markAsFailed(errorMessage);
        outboxRepository.save(event);
    }
}
