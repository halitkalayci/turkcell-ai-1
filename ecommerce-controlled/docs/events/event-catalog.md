# Event Catalog

## OrderCreated (v1)

**Purpose:** Notify other services when an order is created

**Producer:** order-service  
**Consumers:** inventory-service

**Payload Schema:**
```json
{
  "eventId": "uuid (unique per event)",
  "eventType": "OrderCreated",
  "version": "1",
  "timestamp": "ISO-8601 OffsetDateTime",
  "orderId": "uuid",
  "customerId": "uuid",
  "lineItems": [
    {
      "productId": "uuid",
      "quantity": "integer (positive)"
    }
  ]
}
```

**Key Strategy:** orderId (for ordering guarantees per order)

**Idempotency:** Consumers MUST use eventId to detect duplicates

**Event Type:** Domain Event (fact)

**Versioning:** Breaking changes require new version (e.g., OrderCreated-v2)
