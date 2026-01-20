## DECISIONS.md - (ADR - Architecture Decision Record)
Last updated: 2026-01-20T14:00

This document contains HUMAN-APPROVED technical decisions.
AI assistants MUST follow these decisions and MUST NOT ask unless information is missing.

---

## D001 - Build Tool

**Decision:** Maven

**Why:** Team standard

**Alternatives considered:** Gradle (Rejected for MVP consistency)

---


## D002 - Java Version

**Decision:** Java 21

**Why:** LTS + modern language features + Spring Boot 3

---

## D003 - Database

**Decision:** H2 in-memory

**Why:** Zero-ops local demo

**Non-goal:** Production DB decisions are out of scope for this project.

---

## D004 - Order Service Business Rules

**Decision:** Formal business rules approved for order-service

**Date:** 2026-01-20

**Status:** APPROVED & FROZEN - Now under contract-first governance

**What:** Defined complete order domain model including:
- 5 order states (PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)
- State transition rules and forbidden transitions
- Business rules for all use-cases (Create, Get, List, Cancel, Update Status)
- Validation rules (technical + business)
- Error handling strategy (400, 404, 409, 500)
- Calculation formulas and domain invariants

**Why:** 
- Order-service was intentionally created with EMPTY implementation
- Design-first approach allows reviewing business logic before coding
- Establishes clear contract for future payment/inventory integrations

**Documentation:** `docs/rules/order-service-rules.md`

**Next Steps:**
1. ✅ Business rules documented
2. Generate OpenAPI spec based on rules
3. Implement domain model (entities, enums, value objects)
4. Implement application layer (use-cases)
5. Implement infrastructure layer (repositories, persistence)

**Note:** This was an EXPLORATION phase where inventing rules was ALLOWED. 
Now that rules are approved, they are frozen and require formal review to change.

---

## D005 - Order to Inventory Stock Check Integration

**Decision:** Synchronous stock validation using OpenFeign before order creation

**Date:** 2026-01-20

**Status:** APPROVED & IMPLEMENTED

**What:** 
- Order creation MUST synchronously check stock availability from inventory-service
- Orders with insufficient stock MUST NOT be created
- Communication mode: SYNC HTTP via OpenFeign (per AGENTS.md §7.1)
- Endpoint: `GET /api/v1/inventory/product/{productId}` (from inventory-service contract)

**Implementation Details:**
- Dependency: `spring-cloud-starter-openfeign:4.1.0`
- Timeouts: 5s connect, 10s read
- Retry policy: 3 retries with exponential backoff (100ms-1s)
- Stock validation: `inventory.quantity >= lineItem.quantity`

**Error Handling:**
- 404 Not Found → ProductNotFoundException → 400 Bad Request (product misconfigured)
- Insufficient stock → InsufficientStockException → 400 Bad Request
- 5xx/timeout after retries → InventoryServiceUnavailableException → 503 Service Unavailable

**Architecture:**
- Port: `StockCheckPort` (application layer)
- Adapter: `StockCheckAdapter` (infrastructure layer)
- Client: `InventoryServiceClient` (Feign interface)

**Why:**
- Per AGENTS.md §7.1: "Blocking calls MUST be SYNC (Feign)"
- Stock check is a validation/pre-check that blocks order creation flow
- Cannot proceed with order creation without knowing stock availability

**Alternatives Considered:**
- Async (Kafka) - Rejected: This is a blocking validation, not a state change notification
- Bulk check endpoint - Deferred: Current N-call approach acceptable for MVP

**Trade-offs:**
- Pro: Simple, clear contract, follows existing inventory-service API
- Pro: Strong consistency - order never created if stock insufficient
- Con: N HTTP calls for N products in order (acceptable for MVP scale)
- Con: Order creation latency increases with number of products

**Future Considerations:**
- Add bulk stock check endpoint if performance becomes issue
- Stock reservation after order creation (async via Kafka) - out of scope for this task

---

## D006 - Event-Driven Architecture: Transactional Outbox Pattern

**Decision:** Implement transactional outbox pattern for guaranteed event delivery

**Date:** 2026-01-20

**Status:** APPROVED & IMPLEMENTED

**What:**
- Order-service writes OrderCreated events to outbox table in same transaction as order creation
- Polling publisher reads NEW events from outbox and publishes to Kafka every 5 seconds
- Inventory-service consumes OrderCreated events and decrements stock with idempotency
- Event catalog, Kafka topics, outbox schema, and inbox schema fully documented

**Implementation Details:**

**Producer Side (order-service):**
- Outbox table with NEW/SENT/FAILED status lifecycle
- OutboxService writes events within business transaction
- OutboxPublisher polls every 5s, batch size 10, separate transaction
- Retry: 5 attempts with exponential backoff (100ms-1600ms)
- Failed events marked as FAILED after max retries (manual intervention required)
- Topic: order.events, Key: orderId
- Kafka broker: 127.0.0.1:29023

**Consumer Side (inventory-service):**
- Inbox table stores processed eventIds (PK constraint for duplicate detection)
- DecrementStockUseCase inserts into inbox + decrements stock in single transaction
- Duplicate events detected via DataIntegrityViolationException, processing skipped
- Consumer group: inventory-service-order-events
- Retry: 5 attempts with exponential backoff (1s-16s)
- DLQ: order.events.dlq (after 5 failures)
- Internal endpoint: POST /internal/stock/decrement-by-product (for testing)

**Architecture:**
- Communication matrix documented: docs/architecture/communication-matrix.md
- Event catalog: docs/events/event-catalog.md (OrderCreated v1)
- Kafka topics: docs/events/kafka-topics.md (order.events + DLQ)
- Outbox pattern: docs/events/outbox-pattern.md
- Idempotency: docs/events/idempotency.md
- Dependencies: spring-cloud-stream + spring-cloud-stream-binder-kafka

**Why:**
- Per AGENTS.md §7.2: "State changes MUST be ASYNC (Kafka)"
- Per AGENTS.md §7.8: "Producer MUST implement Transactional Outbox Pattern"
- Per AGENTS.md §7.8: "Consumer MUST implement Idempotency using inbox table"
- Guarantees at-least-once delivery without distributed transactions
- Decouples order creation from inventory update (eventual consistency)

**Reliability Strategy:**

**Retry:**
- Producer: 5 attempts, exponential backoff, then FAILED status
- Consumer: 5 attempts via Spring Kafka, exponential backoff, then DLQ

**DLQ:**
- Topic: order.events.dlq
- Triggered after 5 consumer retry failures
- Manual review/replay required (out of scope for MVP)

**Idempotency:**
- Key: eventId (NOT orderId - supports multiple events per order)
- Inbox PK constraint prevents duplicate processing
- Atomic: inbox insert + business logic in same transaction

**Trade-offs:**
- ✅ Guaranteed event delivery (outbox persists before Kafka)
- ✅ No distributed transactions (simpler, more reliable)
- ✅ Consumer idempotency prevents duplicate stock decrements
- ✅ DLQ prevents poison pill scenarios
- ❌ 5-second polling delay (eventual consistency acceptable for stock updates)
- ❌ Outbox/inbox tables grow (cleanup strategy needed for production)
- ❌ Manual intervention required for FAILED producer events

**Files Created:**
- order-service: OutboxEntity, OutboxRepository, OutboxService, OutboxPublisher, OrderCreatedEvent, schema.sql
- inventory-service: InboxEntity, InboxRepository, OrderCreatedEvent, OrderCreatedEventConsumer, DecrementStockUseCase, InternalStockController, schema.sql
- Configuration: application.yml updates for both services

**Testing:**
1. Create order → outbox entry created with status=NEW
2. Wait 5s → OutboxPublisher publishes to Kafka, status=SENT
3. Inventory consumer receives event → inbox insert + stock decrement
4. Duplicate event → inbox PK violation → skip processing

---