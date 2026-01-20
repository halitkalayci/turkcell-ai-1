# Kafka Topics

## order.events
- **Purpose:** All order lifecycle events
- **Producers:** order-service
- **Consumers:** inventory-service, payment-service (future)
- **Retention:** 7 days (demo; production TBD)
- **Partitions:** 3 (demo)
- **Replication:** 1 (demo; production requires 3)
- **Key:** orderId (UUID as string)

**Event Types on this topic:**
- OrderCreated (v1)

## order.events.dlq
- **Purpose:** Dead Letter Queue for failed order events
- **Producers:** inventory-service consumer (on fatal failure)
- **Consumers:** Manual review / replay tooling (out of scope for MVP)
- **Retention:** 30 days
- **Partitions:** 1
- **Replication:** 1

**When events go to DLQ:**
- After 5 consumer retry attempts
- Consumer encounters non-retryable exception
- Deserialization failures
