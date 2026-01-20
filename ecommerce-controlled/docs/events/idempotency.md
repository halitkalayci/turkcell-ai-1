# Idempotency Strategy

## Purpose
Prevent duplicate event processing when Kafka delivers the same message multiple times (at-least-once semantics).

## Inbox Table Schema (inventory-service)

**Table:** `inbox`

| Column | Type | Constraints | Purpose |
|--------|------|-------------|---------|
| event_id | UUID | PK | Unique event ID (from OrderCreated.eventId) |
| event_type | VARCHAR(255) | NOT NULL | "OrderCreated" |
| processed_at | TIMESTAMP | NOT NULL | When event was processed |

**No indexes needed** (PK is sufficient for duplicate check)

## Processing Logic

```java
// Pseudo-code
@Transactional
public void processOrderCreated(OrderCreatedEvent event) {
    try {
        // 1. Try to insert into inbox (duplicate detection)
        inboxRepository.save(new Inbox(event.getEventId(), "OrderCreated", now()));
        
        // 2. If successful, perform business logic
        decrementStock(event.getLineItems());
        
    } catch (DataIntegrityViolationException e) {
        // Duplicate event detected, skip processing
        log.info("Event {} already processed, skipping", event.getEventId());
        return;
    }
}
```

## Key Points

- **Atomic:** Inbox insert + business logic in same transaction
- **Idempotency Key:** eventId (NOT orderId, since same order may generate multiple events)
- **Failure Handling:** If business logic fails, transaction rolls back (inbox NOT inserted), event will be retried
- **Retention:** Keep forever for audit (or cleanup after 30 days — out of scope for MVP)

## Trade-offs

**Pros:**
- Simple, reliable duplicate detection
- Works with any database that supports unique constraints
- Clear audit trail of processed events

**Cons:**
- Inbox table grows indefinitely (requires cleanup strategy for production)
- Extra DB write per event (acceptable overhead for correctness)
