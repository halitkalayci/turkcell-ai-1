package com.ecommerce.order.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for outbox pattern.
 * Per docs/events/outbox-pattern.md
 */
@Repository
public interface OutboxRepository extends JpaRepository<OutboxEntity, UUID> {

    /**
     * Find NEW events for polling publisher.
     * Ordered by creation time to maintain event ordering.
     * Batch size controlled by caller (use Pageable if needed).
     * 
     * Per docs/events/outbox-pattern.md:
     * - Query: SELECT * FROM outbox WHERE status = 'NEW' ORDER BY created_at ASC LIMIT 10
     * - Transaction: Separate from business write
     */
    @Query("SELECT o FROM OutboxEntity o WHERE o.status = 'NEW' ORDER BY o.createdAt ASC")
    List<OutboxEntity> findNewEventsForPublishing();
}
