package com.ecommerce.inventory.infrastructure.persistence.repository;

import com.ecommerce.inventory.infrastructure.persistence.entity.InboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for inbox pattern (idempotency).
 * Per docs/events/idempotency.md
 */
@Repository
public interface InboxRepository extends JpaRepository<InboxEntity, UUID> {
    // PK lookup (eventId) is sufficient for duplicate detection
}
