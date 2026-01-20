# Authorization Model

**Last Updated:** 2026-01-20  
**Status:** APPROVED - Implementation Ready  
**Keycloak Realm:** `example`

---

## 1. Role Definitions

### 1.1 Realm Roles (Keycloak)

**Human creates these in Keycloak Admin Console:**

| Role | Description | Inherits | Use Case |
|------|-------------|----------|----------|
| `customer` | Standard user | - | Browse, create own orders |
| `admin` | System administrator | customer | All operations |
| `order-manager` | Order management | customer | Manage all orders |
| `inventory-manager` | Inventory management | - | Manage inventory |
| `service-account` | Service-to-service | - | Internal auth |

### 1.2 Client-Specific Roles (Fine-Grained)

#### order-service-client roles:

| Role | Action | Scope |
|------|--------|-------|
| `order:read` | Read orders | Own or all (depends on realm role) |
| `order:create` | Create orders | Own only |
| `order:update` | Update order status | Requires order-manager |
| `order:cancel` | Cancel orders | Own orders only |
| `order:cancel:any` | Cancel any order | Requires admin |

#### inventory-service-client roles:

| Role | Action | Scope |
|------|--------|-------|
| `inventory:read` | Read inventory | All users |
| `inventory:write` | Modify inventory | Requires inventory-manager |
| `inventory:reserve` | Reserve stock | All users |
| `inventory:release` | Release reservations | Order-service only |

---

## 2. Permission Matrix

### 2.1 Order Service Endpoints

| Endpoint | Method | customer | admin | order-manager | Notes |
|----------|--------|----------|-------|---------------|-------|
| `/api/v1/orders` | POST | ✅ | ✅ | ✅ | Create own order |
| `/api/v1/orders` | GET | ✅ (own) | ✅ (all) | ✅ (all) | List orders |
| `/api/v1/orders/{id}` | GET | ✅ (own) | ✅ (all) | ✅ (all) | Get order details |
| `/api/v1/orders/{id}` | DELETE | ✅ (own) | ✅ (all) | ❌ | Cancel order |
| `/api/v1/orders/{id}/status` | PATCH | ❌ | ✅ | ✅ | Update status |
| `/api/v1/orders/admin/**` | * | ❌ | ✅ | ✅ | Admin operations |

**Legend:**
- ✅ = Allowed
- ❌ = Forbidden (403)
- ✅ (own) = Allowed only for own resources
- ✅ (all) = Allowed for all resources

### 2.2 Inventory Service Endpoints

| Endpoint | Method | customer | admin | inventory-manager | Notes |
|----------|--------|----------|-------|-------------------|-------|
| `/api/v1/inventory` | GET | ✅ | ✅ | ✅ | List inventory |
| `/api/v1/inventory/{id}` | GET | ✅ | ✅ | ✅ | Get item details |
| `/api/v1/inventory` | POST | ❌ | ✅ | ✅ | Create inventory |
| `/api/v1/inventory/{id}` | PUT | ❌ | ✅ | ✅ | Update inventory |
| `/api/v1/inventory/{id}` | DELETE | ❌ | ✅ | ✅ | Delete inventory |
| `/api/v1/inventory/{id}/reserve` | POST | 🔒 | 🔒 | 🔒 | Service-only |
| `/api/v1/inventory/{id}/release` | POST | 🔒 | 🔒 | 🔒 | Service-only |

**Legend:**
- 🔒 = Service-to-service only (requires `service-account` role)

### 2.3 Identity Service Endpoints

| Endpoint | Method | Authenticated | Role Required | Notes |
|----------|--------|---------------|---------------|-------|
| `/api/v1/identity/login` | POST | ❌ Public | - | Returns JWT |
| `/api/v1/identity/register` | POST | ❌ Public | - | Creates user |
| `/api/v1/identity/refresh` | POST | ❌ Public | - | Refresh token |
| `/api/v1/identity/me` | GET | ✅ | customer | Get own profile |
| `/api/v1/identity/users` | GET | ✅ | admin | List all users |
| `/api/v1/identity/users/{id}` | GET | ✅ | admin | Get user details |

---

## 3. Scope Strategy

### 3.1 OAuth2 Scopes (Future Enhancement)

**Reserved for future use if granular control needed:**

| Scope | Description | Required For |
|-------|-------------|--------------|
| `orders.read` | Read orders | GET /orders |
| `orders.write` | Create/update orders | POST/PUT /orders |
| `inventory.read` | Read inventory | GET /inventory |
| `inventory.write` | Modify inventory | POST/PUT /inventory |
| `profile` | Access user profile | GET /identity/me |

**Current Implementation:**
- Role-based authorization (RBAC) is sufficient
- Scopes OPTIONAL for MVP
- Human decides if scopes needed for mobile/web apps

### 3.2 Scope vs Role Decision

**Use ROLES when:**
- Permission tied to user identity
- Long-lived access
- Internal system users

**Use SCOPES when:**
- Permission tied to client application
- Delegated access (user consents)
- Third-party integrations

**Current project: ROLES only**

---

## 4. Service-Level Authorization

### 4.1 Resource Ownership Check

**Services MUST implement ownership validation:**

```java
// Pseudocode for order-service
@GetMapping("/api/v1/orders/{orderId}")
public OrderDto getOrder(@PathVariable UUID orderId, @RequestHeader("X-User-Id") UUID userId) {
    Order order = orderRepository.findById(orderId);
    
    // Check ownership or admin
    if (!order.getUserId().equals(userId) && !hasRole("admin")) {
        throw new ForbiddenException("Cannot access order");
    }
    
    return toDto(order);
}
```

**Rule:** Services MUST NOT rely solely on gateway authorization for ownership checks.

### 4.2 Role Extraction from Headers

**Gateway forwards roles as:**
```
X-User-Roles: customer,order-manager
```

**Service parses:**
```java
String rolesHeader = request.getHeader("X-User-Roles");
List<String> roles = Arrays.asList(rolesHeader.split(","));
boolean isAdmin = roles.contains("admin");
```

### 4.3 Service-to-Service Authorization

**For internal service calls (e.g., order-service → inventory-service):**

1. **Option A (Current MVP): Trusted Headers**
   - order-service includes `X-Service-Name: order-service` header
   - inventory-service trusts this header
   - **Risk:** Header injection if gateway compromised

2. **Option B (Future): Client Credentials Flow**
   - order-service obtains token from Keycloak using client credentials
   - Sends `Authorization: Bearer <SERVICE_TOKEN>` to inventory-service
   - inventory-service validates token with `service-account` role
   - **Benefit:** Cryptographic proof of service identity

**Current Decision:** Option A (documented in service-to-service-auth.md)

---

## 5. Enforcement Strategy

### 5.1 Gateway-Level (Coarse-Grained)

**Gateway enforces:**
- Authentication (token presence & validity)
- Public endpoint bypass
- Optional: Route-level role checks (e.g., `/admin/**` requires `admin`)

**Example:**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: admin-orders
          uri: lb://order-service
          predicates:
            - Path=/api/v1/orders/admin/**
          filters:
            - RequireRole=admin
```

### 5.2 Service-Level (Fine-Grained)

**Services enforce:**
- Resource ownership
- Action-level permissions (create vs update vs delete)
- Business rule authorization (e.g., "can only cancel pending orders")

**Recommended:** Use Spring Security `@PreAuthorize` annotations

```java
@PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOwner(#orderId, authentication)")
@DeleteMapping("/api/v1/orders/{orderId}")
public void cancelOrder(@PathVariable UUID orderId) {
    orderService.cancel(orderId);
}
```

### 5.3 Domain-Level (Business Rules)

**Domain layer enforces:**
- State-based authorization (e.g., "can only cancel PENDING orders")
- Aggregate invariants

```java
// In Order aggregate
public void cancel(User user) {
    if (this.status == OrderStatus.SHIPPED) {
        throw new IllegalStateException("Cannot cancel shipped order");
    }
    
    if (!this.userId.equals(user.getId()) && !user.isAdmin()) {
        throw new ForbiddenException("Not authorized");
    }
    
    this.status = OrderStatus.CANCELLED;
}
```

---

## 6. Authorization Failure Responses

### 6.1 HTTP Status Codes

| Scenario | Status | When to Use |
|----------|--------|-------------|
| Not authenticated | 401 | Token missing/invalid |
| Authenticated but forbidden | 403 | Valid token, insufficient permissions |
| Resource not found | 404 | Instead of 403 to avoid info leak |

### 6.2 Response Format

**403 Forbidden:**
```json
{
  "timestamp": "2026-01-20T14:30:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Insufficient permissions",
  "path": "/api/v1/orders/admin/123"
}
```

**Security Note:** Do NOT expose details like:
- "You need admin role" (info leak)
- "Order belongs to user X" (info leak)

**Instead:** Generic "Insufficient permissions" or 404 if resource hidden.

---

## 7. Testing Authorization

### 7.1 Required Test Scenarios

**For each protected endpoint:**

1. **No token** → 401
2. **Valid customer token, own resource** → 200/201
3. **Valid customer token, other's resource** → 403 or 404
4. **Valid admin token, any resource** → 200/201
5. **Valid token, wrong role** → 403
6. **Expired token** → 401

### 7.2 Test Data Setup

**Human creates test users in Keycloak:**

| Username | Password | Roles | Purpose |
|----------|----------|-------|---------|
| `customer1@example.com` | `test123` | `customer` | Standard user |
| `customer2@example.com` | `test123` | `customer` | Other user |
| `admin@example.com` | `admin123` | `admin` | Admin operations |
| `ordermgr@example.com` | `test123` | `order-manager` | Order management |
| `invmgr@example.com` | `test123` | `inventory-manager` | Inventory mgmt |

### 7.3 Integration Test Example

```java
@Test
void customerCannotAccessOthersOrder() {
    // Given: customer1 creates order
    UUID orderId = createOrder(customer1Token);
    
    // When: customer2 tries to access
    Response response = given()
        .header("Authorization", "Bearer " + customer2Token)
        .get("/api/v1/orders/" + orderId);
    
    // Then: 403 or 404
    assertThat(response.statusCode()).isIn(403, 404);
}
```

---

## 8. Role Management (HUMAN-EXECUTED)

### 8.1 Assigning Roles to Users

**Via Keycloak Admin Console:**

1. Navigate to: Users → Select User → Role Mappings
2. Assign Realm Roles: `customer`, `admin`, etc.
3. Assign Client Roles (if using): `order:read`, etc.
4. Save

### 8.2 Default Role Assignment

**Keycloak Configuration:**
- Realm Settings → Roles → Default Roles
- Add `customer` to default roles
- New users automatically get `customer` role

### 8.3 Programmatic Role Assignment (Future)

**If identity-service needs to assign roles:**
- Use Keycloak Admin REST API
- Requires admin client credentials
- **HUMAN decides if needed**

---

## 9. Audit & Compliance

### 9.1 Authorization Audit Logging

**Services MUST log:**
- Authorization failures (403)
- Admin actions (with userId)
- Role changes (if implemented)

**Log format:**
```json
{
  "timestamp": "2026-01-20T14:30:00Z",
  "level": "WARN",
  "event": "AUTHORIZATION_FAILURE",
  "userId": "a1b2c3d4-...",
  "resource": "/api/v1/orders/123",
  "action": "GET",
  "reason": "INSUFFICIENT_PERMISSIONS",
  "userRoles": ["customer"]
}
```

### 9.2 Compliance Requirements

**If regulatory compliance needed (GDPR, PCI-DSS):**
- Implement detailed audit trails
- Log access to sensitive resources
- Retain logs per compliance period
- **HUMAN defines retention policy**

---

**END OF AUTHORIZATION.MD**
