# Communication Matrix

This document captures all cross-service interactions in the e-commerce system.

## Order → Inventory Interactions

### 1. Stock Availability Check (SYNC)
- **Mode:** SYNC HTTP (OpenFeign)
- **Blocking:** Yes
- **Reason:** Order creation must validate stock before proceeding
- **Endpoint:** `GET /api/v1/inventory/product/{productId}`
- **Timeout:** 5s connect, 10s read
- **Retry:** 3 attempts with exponential backoff (100ms-1s)
- **Failure Handling:** 
  - 404 → ProductNotFoundException → 400 Bad Request
  - Insufficient stock → InsufficientStockException → 400 Bad Request
  - 5xx/timeout → InventoryServiceUnavailableException → 503 Service Unavailable
- **Status:** IMPLEMENTED (D005)

### 2. Stock Decrement (ASYNC)
- **Mode:** ASYNC Kafka Event
- **Blocking:** No
- **Reason:** Order creation is a domain fact; inventory reacts to it asynchronously
- **Event:** OrderCreated (v1)
- **Topic:** order.events
- **Consumer:** inventory-service
- **Consumer Group:** inventory-service-order-events
- **Idempotency:** eventId stored in inbox table
- **Retry:** 5 attempts with exponential backoff (Spring Kafka default)
- **DLQ:** order.events.dlq (after 5 failures)
- **Failure Handling:** 
  - Transient errors → Retry up to 5 times
  - Non-retryable → Send to DLQ
  - Success → Mark event as processed in inbox
- **Status:** TO BE IMPLEMENTED (this task)

---

## Future Interactions (Out of Scope)

### Order → Payment (TBD)
- Payment authorization: SYNC (before order confirmation)
- Payment completed: ASYNC event

### Inventory → Order (TBD)
- Stock reserved: ASYNC event (for order confirmation workflow)
