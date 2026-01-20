# Order Service - Business Rules

## 1. Overview

- **Purpose**: Define order domain rules for mini e-commerce system
- **Scope**: Order lifecycle, validation, state transitions, error handling
- **Version**: 1.0
- **Last Updated**: 2026-01-20
- **Status**: APPROVED - Rules are now frozen under contract-first governance

---

## 2. Domain Model

### 2.1 Order Entity

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | UUID | Yes | Unique order identifier (auto-generated) |
| `orderNumber` | String | Yes | Human-readable order reference (auto-generated) |
| `customerId` | UUID | Yes | Customer who placed the order |
| `status` | OrderStatus | Yes | Current order state |
| `totalAmount` | BigDecimal | Yes | Total order amount (calculated) |
| `shippingAddress` | Address | Yes | Delivery address |
| `lineItems` | List\<LineItem\> | Yes | Ordered products (min: 1, max: 50) |
| `createdAt` | Instant | Yes | Order creation timestamp (auto-generated) |
| `updatedAt` | Instant | Yes | Last modification timestamp (auto-updated) |
| `confirmedAt` | Instant | No | Confirmation timestamp |
| `shippedAt` | Instant | No | Shipment timestamp |
| `deliveredAt` | Instant | No | Delivery timestamp |
| `cancelledAt` | Instant | No | Cancellation timestamp |
| `cancellationReason` | String | No | Reason for cancellation |

### 2.2 LineItem Value Object

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `productId` | UUID | Yes | Product identifier |
| `quantity` | Integer | Yes | Quantity ordered (min: 1, max: 999) |
| `unitPrice` | BigDecimal | Yes | Price per unit (scale: 2) |

### 2.3 Address Value Object

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `street` | String | Yes | Street address (max: 255 chars) |
| `city` | String | Yes | City name (max: 100 chars) |
| `postalCode` | String | Yes | Postal/ZIP code (max: 20 chars) |
| `country` | String | Yes | ISO 3166-1 alpha-2 country code |

### 2.4 Order States

| State | Description | Terminal |
|-------|-------------|----------|
| `PENDING` | Order created, awaiting confirmation | No |
| `CONFIRMED` | Order confirmed (payment validated), awaiting fulfillment | No |
| `SHIPPED` | Order dispatched to customer | No |
| `DELIVERED` | Order successfully delivered | Yes |
| `CANCELLED` | Order cancelled by customer or system | Yes |

### 2.5 State Transition Diagram

```
PENDING → CONFIRMED → SHIPPED → DELIVERED
   ↓           ↓          ↓
CANCELLED   CANCELLED  CANCELLED
```

**Allowed Transitions:**
- `PENDING` → `CONFIRMED` (order confirmation/payment)
- `PENDING` → `CANCELLED` (early cancellation)
- `CONFIRMED` → `SHIPPED` (fulfillment begins)
- `CONFIRMED` → `CANCELLED` (cancellation before shipment)
- `SHIPPED` → `DELIVERED` (successful delivery)
- `SHIPPED` → `CANCELLED` (rare: lost/returned package)

**Forbidden Transitions:**
- Any transition FROM `DELIVERED` (immutable final state)
- Any transition FROM `CANCELLED` (immutable final state)
- Any backward transitions (e.g., `SHIPPED` → `CONFIRMED`)
- Skipping states (e.g., `PENDING` → `DELIVERED`)

---

## 3. Business Rules

### 3.1 Create Order (POST /api/v1/orders)

**Preconditions:**
- Must have at least 1 line item
- Maximum 50 line items per order

**Business Rules:**
1. Each line item MUST contain valid `productId`, positive `quantity`, and positive `unitPrice`
2. `customerId` is REQUIRED and must be valid UUID
3. `shippingAddress` is REQUIRED with all fields populated
4. `totalAmount` MUST equal: `Σ(quantity × unitPrice)` for all line items
5. Initial status is ALWAYS `PENDING`
6. `createdAt` and `updatedAt` are auto-set to current timestamp
7. `orderNumber` is auto-generated using format: `ORD-{yyyyMMddHHmmss}-{random5digits}`
8. Line items MUST NOT contain duplicate `productId` values

**Postconditions:**
- Order persisted with status `PENDING`
- HTTP 201 Created with order details in response body
- `Location` header contains order resource URI

**Example:**
```json
POST /api/v1/orders
{
  "customerId": "123e4567-e89b-12d3-a456-426614174000",
  "shippingAddress": {
    "street": "123 Main St",
    "city": "Istanbul",
    "postalCode": "34000",
    "country": "TR"
  },
  "lineItems": [
    {
      "productId": "789e4567-e89b-12d3-a456-426614174000",
      "quantity": 2,
      "unitPrice": 49.99
    }
  ],
  "totalAmount": 99.98
}
```

---

### 3.2 Get Order (GET /api/v1/orders/{id})

**Preconditions:**
- `orderId` must be valid UUID format

**Business Rules:**
1. Order MUST exist
2. Return full order details including all line items
3. Include all timestamp fields (even if null)

**Postconditions:**
- HTTP 200 OK with complete order representation
- HTTP 404 Not Found if order does not exist

**Example:**
```
GET /api/v1/orders/123e4567-e89b-12d3-a456-426614174000
```

---

### 3.3 List Orders (GET /api/v1/orders)

**Preconditions:**
- None (public endpoint)

**Business Rules:**
1. Support filtering by:
   - `customerId` (UUID) - return orders for specific customer
   - `status` (OrderStatus enum) - return orders in specific state
   - `createdAfter` (ISO-8601 date) - orders created after this date
   - `createdBefore` (ISO-8601 date) - orders created before this date
2. Support pagination:
   - `page` (integer, default: 0, min: 0)
   - `size` (integer, default: 20, min: 1, max: 100)
3. Default sorting: `createdAt DESC` (newest first)
4. All filters are optional and combinable

**Postconditions:**
- HTTP 200 OK with paginated results
- Returns empty array if no matches (NOT 404)
- Response includes pagination metadata: `totalElements`, `totalPages`, `currentPage`, `pageSize`

**Example:**
```
GET /api/v1/orders?customerId=123e4567-e89b-12d3-a456-426614174000&status=PENDING&page=0&size=10
```

---

### 3.4 Cancel Order (POST /api/v1/orders/{id}/cancel)

**Preconditions:**
- `orderId` must exist
- Order status MUST be `PENDING` or `CONFIRMED`

**Business Rules:**
1. CANNOT cancel if status is:
   - `SHIPPED` (already dispatched)
   - `DELIVERED` (already received)
   - `CANCELLED` (already cancelled)
2. When cancelled:
   - Status changes to `CANCELLED`
   - `cancelledAt` timestamp is set to current time
   - `updatedAt` is updated to current time
   - Optional `cancellationReason` is stored if provided
3. Cancellation is idempotent (calling cancel on already cancelled order returns 409)

**Postconditions:**
- HTTP 200 OK with updated order (status = CANCELLED)
- HTTP 404 Not Found if order does not exist
- HTTP 409 Conflict if cancellation not allowed (wrong state)

**Example:**
```json
POST /api/v1/orders/123e4567-e89b-12d3-a456-426614174000/cancel
{
  "reason": "Customer changed mind"
}
```

---

### 3.5 Update Order Status (PATCH /api/v1/orders/{id}/status)

**Preconditions:**
- `orderId` must exist
- New status must be valid OrderStatus enum value

**Business Rules:**
1. Transition MUST be allowed according to state diagram (see 2.5)
2. CANNOT skip states (e.g., `PENDING` → `DELIVERED`)
3. Status change triggers:
   - `updatedAt` always updated to current time
   - Specific timestamp fields set based on target status:
     - → `CONFIRMED`: set `confirmedAt`
     - → `SHIPPED`: set `shippedAt`
     - → `DELIVERED`: set `deliveredAt`
     - → `CANCELLED`: set `cancelledAt`
4. Terminal states (`DELIVERED`, `CANCELLED`) are immutable

**Postconditions:**
- HTTP 200 OK with updated order
- HTTP 400 Bad Request if invalid status value
- HTTP 404 Not Found if order does not exist
- HTTP 409 Conflict if transition violates state machine rules

**Example:**
```json
PATCH /api/v1/orders/123e4567-e89b-12d3-a456-426614174000/status
{
  "status": "CONFIRMED"
}
```

---

## 4. Validation Rules

### 4.1 Input Validation (Technical)

| Field | Constraint | Violation Response |
|-------|------------|-------------------|
| `orderId` | Valid UUID v4 | 400 Bad Request |
| `customerId` | Valid UUID v4, not null | 400 Bad Request |
| `productId` | Valid UUID v4, not null | 400 Bad Request |
| `orderNumber` | Max 50 chars | 400 Bad Request |
| `quantity` | Integer, min: 1, max: 999 | 400 Bad Request |
| `unitPrice` | BigDecimal, min: 0.01, scale: 2 | 400 Bad Request |
| `totalAmount` | BigDecimal, min: 0.01, scale: 2 | 400 Bad Request |
| `shippingAddress.street` | Not blank, max 255 chars | 400 Bad Request |
| `shippingAddress.city` | Not blank, max 100 chars | 400 Bad Request |
| `shippingAddress.postalCode` | Not blank, max 20 chars | 400 Bad Request |
| `shippingAddress.country` | ISO 3166-1 alpha-2 code | 400 Bad Request |
| `status` | Must be valid OrderStatus enum | 400 Bad Request |
| `lineItems` | Min size: 1, max size: 50 | 400 Bad Request |
| Timestamps | ISO-8601 format | 400 Bad Request |
| `cancellationReason` | Max 500 chars | 400 Bad Request |

### 4.2 Business Validation

| Rule | Check | When | Violation Response |
|------|-------|------|-------------------|
| **Total calculation** | `totalAmount == Σ(qty × price)` | Create order | 400 Bad Request |
| **State transition** | Transition in allowed list | Update status | 409 Conflict |
| **Cancellation eligibility** | Status in [PENDING, CONFIRMED] | Cancel order | 409 Conflict |
| **Line items not empty** | Size >= 1 | Create order | 400 Bad Request |
| **No duplicate products** | Unique productIds in lineItems | Create order | 400 Bad Request |
| **Future date prevention** | `createdAt <= now()` | Create order | 400 Bad Request |
| **Terminal state immutability** | No changes if DELIVERED/CANCELLED | Any update | 409 Conflict |

### 4.3 Domain Invariants

These conditions MUST ALWAYS be true:

1. **Final states are immutable**: Once `DELIVERED` or `CANCELLED`, no field can change
2. **Timestamps are monotonic**: `createdAt <= updatedAt`
3. **Total consistency**: `totalAmount` always equals `Σ(lineItems[i].quantity × lineItems[i].unitPrice)`
4. **State machine compliance**: `status` can only transition via allowed paths
5. **Non-negative money**: All monetary values >= 0.01
6. **Positive quantities**: All quantities >= 1

---

## 5. Error Handling

### 5.1 HTTP 400 (Bad Request)

**Triggers:**
- Invalid UUID format
- Negative or zero quantity/price
- Missing required fields (customerId, shippingAddress)
- Empty line items array
- Total amount calculation mismatch
- Invalid enum value for status
- Malformed date format
- Quantity exceeds maximum (999)
- Too many line items (>50)
- Duplicate productIds in line items
- String length violations

**Response Format:**
```json
{
  "timestamp": "2026-01-20T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "lineItems[0].quantity",
      "rejectedValue": "0",
      "message": "must be greater than or equal to 1"
    },
    {
      "field": "totalAmount",
      "rejectedValue": "50.00",
      "message": "does not match sum of line items (expected: 99.98)"
    }
  ]
}
```

---

### 5.2 HTTP 404 (Not Found)

**Triggers:**
- Order with given ID does not exist (GET, CANCEL, UPDATE)

**Response Format:**
```json
{
  "timestamp": "2026-01-20T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Order not found with id: 123e4567-e89b-12d3-a456-426614174000"
}
```

**Important:** List orders endpoint returns empty array with HTTP 200, NOT 404

---

### 5.3 HTTP 409 (Conflict)

**Triggers:**
- Attempting to cancel order not in PENDING/CONFIRMED status
- Attempting invalid state transition (e.g., SHIPPED → PENDING)
- Attempting to modify order in terminal state (DELIVERED/CANCELLED)
- Attempting to cancel already cancelled order
- Duplicate orderNumber (if using client-generated IDs)

**Response Format:**
```json
{
  "timestamp": "2026-01-20T10:30:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Cannot cancel order in SHIPPED status",
  "details": {
    "currentStatus": "SHIPPED",
    "allowedStatuses": ["PENDING", "CONFIRMED"]
  }
}
```

---

### 5.4 HTTP 500 (Internal Server Error)

**Triggers:**
- Database connection failure
- Unhandled runtime exception
- External service timeout (future: payment/inventory)
- Data corruption detected

**Response Format:**
```json
{
  "timestamp": "2026-01-20T10:30:00Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred",
  "traceId": "abc123-def456-ghi789"
}
```

**Security Note:** Never expose stack traces or internal details to clients in production

---

## 6. Calculations & Formulas

### 6.1 Total Amount Calculation

```
totalAmount = Σ(lineItems[i].quantity × lineItems[i].unitPrice)
             for i = 0 to lineItems.length - 1
```

**Example:**
- Item 1: 2 × $49.99 = $99.98
- Item 2: 1 × $25.00 = $25.00
- **Total: $124.98**

### 6.2 Order Number Generation

```
Format: ORD-{timestamp}-{random}
Where:
  - timestamp = yyyyMMddHHmmss (14 digits)
  - random = 5 random digits (00000-99999)

Example: ORD-20260120103045-12345
```

---

## 7. Future Considerations

These are OUT OF SCOPE for current implementation but documented for future planning:

### 7.1 Payment Integration
- Integration with payment-service for payment validation
- Status transition PENDING → CONFIRMED should be triggered by successful payment
- Handle payment failures and timeouts

### 7.2 Inventory Integration
- Integration with inventory-service for stock validation
- Reserve inventory on order creation
- Release inventory on cancellation
- Handle out-of-stock scenarios

### 7.3 Order Modification
- Allow quantity changes for PENDING orders
- Allow adding/removing line items before confirmation
- Recalculate totalAmount on modifications

### 7.4 Partial Operations
- Partial shipments (split orders)
- Partial cancellations (cancel specific items)
- Partial deliveries

### 7.5 Returns & Refunds
- Return initiated state
- Refund processing
- Return to inventory coordination

### 7.6 Customer Authentication
- Verify customerId against auth service
- Ensure customers can only access their own orders
- Admin roles for viewing all orders

---

## 8. Glossary

| Term | Definition |
|------|------------|
| **Order** | A customer request to purchase one or more products |
| **Line Item** | A single product entry within an order (product + quantity + price) |
| **Order Status** | Current state of an order in its lifecycle |
| **Terminal State** | A final state that cannot transition to any other state |
| **State Transition** | Movement from one order status to another |
| **Cancellation** | Customer or system-initiated termination of an order |
| **Fulfillment** | Process of preparing and shipping an order |
| **Total Amount** | Sum of all line item subtotals in an order |

---

**Document Status**: ✅ APPROVED & FROZEN  
**Governance**: Now under contract-first rules - changes require formal review
