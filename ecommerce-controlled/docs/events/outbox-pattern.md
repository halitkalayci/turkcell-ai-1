# Transactional Outbox Pattern

## Purpose
Guarantee at-least-once delivery of events to Kafka by storing events in the same database transaction as the business operation, then publishing asynchronously via polling.

## Outbox Table Schema (order-service)

**Table:** `outbox`

| Column | Type | Constraints | Purpose |
|--------|------|-------------|---------|
| id | UUID | PK | Unique event ID (also used as eventId in payload) |
| aggregate_type | VARCHAR(255) | NOT NULL | "Order" |
| aggregate_id | UUID | NOT NULL | orderId |
| event_type | VARCHAR(255) | NOT NULL | "OrderCreated" |
| payload | TEXT | NOT NULL | JSON event payload |
| status | VARCHAR(50) | NOT NULL | NEW/SENT/FAILED |
| created_at | TIMESTAMP | NOT NULL | Event creation time |
| sent_at | TIMESTAMP | NULL | When published to Kafka |
| error_message | TEXT | NULL | Last error if FAILED |

**Indexes:**
- `idx_outbox_status_created` on (status, created_at) — for polling NEW events
- `idx_outbox_aggregate` on (aggregate_id) — for debugging

## Status Model

- **NEW:** Event created, not yet published
- **SENT:** Successfully published to Kafka (received ack)
- **FAILED:** Failed after max retries (requires manual intervention)

## Polling Strategy

- **Scheduler:** @Scheduled(fixedDelay = 5000) — every 5 seconds
- **Batch Size:** 10 events per poll
- **Query:** `SELECT * FROM outbox WHERE status = 'NEW' ORDER BY created_at ASC LIMIT 10`
- **Transaction Boundary:** Poll in **separate transaction** from business write (critical!)
- **Publish Semantics:** At-least-once (Kafka may see duplicates; consumers MUST be idempotent)
- **Concurrency:** Single scheduler instance per service instance (no distributed locking for MVP)

## Status Transitions

- NEW → SENT (on successful Kafka ack)
- NEW → FAILED (after 5 retries with exponential backoff)
- FAILED → NEW (manual retry via admin tool — out of scope for MVP)

## Retry Logic (Publisher)

- Max attempts: 5
- Backoff: Exponential (100ms, 200ms, 400ms, 800ms, 1600ms)
- After 5 failures: Mark as FAILED and log error
- NO infinite retries (prevents poison pill scenarios)

## Critical Rules

1. Outbox write MUST be in same transaction as business write
2. Outbox publisher MUST run in separate transaction
3. Never delete from outbox (keep for audit; or add cleanup job after 30 days)
4. Payload MUST be complete (consumers cannot query order-service for details)
