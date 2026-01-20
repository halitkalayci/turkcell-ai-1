# Gateway Security

**Last Updated:** 2026-01-20  
**Status:** APPROVED - Implementation Ready  
**Keycloak Realm:** `example`

---

## 1. JWT Validation

### 1.1 Gateway Responsibility

The gateway-server acts as the **single entry point** for all API requests and MUST:

1. **Validate JWT tokens** on every incoming request (except public endpoints)
2. **Extract user context** (userId, roles, email) from validated tokens
3. **Forward security context** to downstream services
4. **Reject invalid tokens** with appropriate HTTP status codes

### 1.2 Validation Rules

| Rule | Action | HTTP Status |
|------|--------|-------------|
| Token missing | Reject | 401 Unauthorized |
| Token expired | Reject | 401 Unauthorized |
| Invalid signature | Reject | 401 Unauthorized |
| Invalid issuer | Reject | 401 Unauthorized |
| Insufficient roles | Reject | 403 Forbidden |
| Token valid | Forward | - |

### 1.3 Token Extraction

**Expected Header:**
```
Authorization: Bearer <JWT_TOKEN>
```

**Gateway extracts:**
- `sub` → User ID
- `realm_access.roles` → User roles
- `email` → User email
- `preferred_username` → Username

---

## 2. Token Forwarding

### 2.1 Downstream Service Headers

After successful validation, gateway MUST forward:

```
Authorization: Bearer <ORIGINAL_JWT>
X-User-Id: <extracted_from_sub>
X-User-Email: <extracted_from_email>
X-User-Roles: <comma_separated_roles>
X-Forwarded-For: <client_ip>
X-Request-Id: <generated_uuid>
```

### 2.2 Header Sanitization

**Gateway MUST remove these headers from client requests:**
- `X-User-Id`
- `X-User-Email`
- `X-User-Roles`
- Any custom security headers

**Reason:** Prevent header injection attacks where clients forge identity headers.

### 2.3 Service Trust Boundary

**Downstream services MAY trust gateway-injected headers:**
- Services do NOT re-validate JWT (performance optimization)
- Services read user context from `X-User-*` headers
- Services assume gateway has already validated authentication

**Alternative (Defense-in-Depth):**
- Services MAY re-validate JWT if paranoid
- Requires each service to have Keycloak jwk-set-uri configured

---

## 3. CORS Configuration

### 3.1 Development Settings

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowed-origins:
              - "http://localhost:3000"
              - "http://localhost:3001"
            allowed-methods:
              - GET
              - POST
              - PUT
              - DELETE
              - PATCH
              - OPTIONS
            allowed-headers:
              - "*"
            allow-credentials: true
            max-age: 3600
```

### 3.2 Production Settings (HUMAN CONFIGURES)

**MUST restrict origins:**
```yaml
allowed-origins:
  - "https://app.ecommerce.example.com"
  - "https://admin.ecommerce.example.com"
```

**MUST restrict headers:**
```yaml
allowed-headers:
  - "Authorization"
  - "Content-Type"
  - "Accept"
  - "X-Request-Id"
```

---

## 4. Security Filters

### 4.1 Filter Chain Order

1. **CorsFilter** - Handle preflight OPTIONS requests
2. **AuthenticationFilter** - Validate JWT token
3. **RoleEnrichmentFilter** - Extract and forward user context
4. **RequestIdFilter** - Generate X-Request-Id for tracing
5. **RateLimitFilter** - Apply rate limits (see rate-limits.md)
6. **RouteFilter** - Forward to downstream service

### 4.2 Authentication Filter Logic

**Pseudocode:**
```
IF request.path IN publicEndpoints THEN
  allow()
ELSE
  token = extract_token(request.headers["Authorization"])
  
  IF token IS NULL THEN
    return 401 "Missing Authorization header"
  END
  
  IF NOT validate_jwt_signature(token) THEN
    return 401 "Invalid token signature"
  END
  
  IF token.expired() THEN
    return 401 "Token expired"
  END
  
  IF token.issuer != EXPECTED_ISSUER THEN
    return 401 "Invalid issuer"
  END
  
  userContext = extract_claims(token)
  enrich_request_headers(userContext)
  allow()
END
```

### 4.3 Role-Based Routing (Optional Enhancement)

**If needed, gateway CAN enforce role checks:**

```yaml
routes:
  - id: admin-orders
    uri: lb://order-service
    predicates:
      - Path=/api/v1/orders/admin/**
    filters:
      - RequireRole=admin
```

**Filter logic:**
```
IF user.roles NOT CONTAINS required_role THEN
  return 403 "Insufficient permissions"
END
```

---

## 5. Public Endpoints

### 5.1 Endpoints Exempt from Authentication

**MUST NOT require JWT:**

| Endpoint | Service | Reason |
|----------|---------|--------|
| `GET /actuator/health` | All services | Health checks |
| `GET /actuator/info` | All services | Service info |
| `POST /api/v1/identity/login` | identity-service | User login |
| `POST /api/v1/identity/register` | identity-service | User registration |
| `POST /api/v1/identity/refresh` | identity-service | Token refresh |
| `GET /swagger-ui/**` | All services | API docs (dev only) |
| `GET /v3/api-docs/**` | All services | OpenAPI spec (dev only) |

### 5.2 Configuration

```yaml
security:
  public-endpoints:
    - /actuator/health
    - /actuator/info
    - /api/v1/identity/login
    - /api/v1/identity/register
    - /api/v1/identity/refresh
    - /swagger-ui/**
    - /v3/api-docs/**
```

**Gateway filter checks this list before authentication.**

---

## 6. Error Responses

### 6.1 Standard Security Error Format

```json
{
  "timestamp": "2026-01-20T14:30:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired token",
  "path": "/api/v1/orders"
}
```

### 6.2 Error Scenarios

| Scenario | Status | Message |
|----------|--------|---------|
| No token | 401 | "Missing Authorization header" |
| Invalid token | 401 | "Invalid or expired token" |
| Expired token | 401 | "Token expired" |
| Wrong issuer | 401 | "Invalid token issuer" |
| Missing role | 403 | "Insufficient permissions" |
| Rate limit exceeded | 429 | "Too many requests" |

### 6.3 Security Headers in Responses

**Gateway MUST add:**
```
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Strict-Transport-Security: max-age=31536000; includeSubDomains
```

---

## 7. Integration with Keycloak

### 7.1 Spring Boot Configuration

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/example
          jwk-set-uri: http://localhost:8180/realms/example/protocol/openid-connect/certs

keycloak:
  realm: example
  auth-server-url: http://localhost:8180
  resource: gateway-client
  credentials:
    secret: ${GATEWAY_CLIENT_SECRET}  # HUMAN provides via env var
```

### 7.2 JWK Set Caching

**Gateway MUST cache JWK keys:**
- Cache duration: 10 minutes (default Spring Security behavior)
- Retry on failure: 3 attempts with exponential backoff
- Fallback: Return 503 if Keycloak unreachable

---

## 8. Testing Strategy

### 8.1 Unit Tests

**Required tests:**
- Token missing → 401
- Token expired → 401
- Invalid signature → 401
- Valid token → forwards with headers
- Public endpoint → no authentication

### 8.2 Integration Tests

**Required scenarios:**
1. **Valid user token** → Access allowed
2. **Admin token** → Access admin endpoints
3. **Customer token** → Denied admin endpoints (403)
4. **Expired token** → 401
5. **Public endpoint** → No token required

### 8.3 Test Token Generation (Dev Only)

**HUMAN can use Keycloak token endpoint:**

```bash
curl -X POST http://localhost:8180/realms/example/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=identity-service-client" \
  -d "client_secret=<SECRET>" \
  -d "grant_type=password" \
  -d "username=testuser@example.com" \
  -d "password=password"
```

**Response contains:**
```json
{
  "access_token": "<JWT>",
  "expires_in": 900,
  "refresh_token": "<REFRESH>",
  "token_type": "Bearer"
}
```

---

## 9. Security Constraints

### 9.1 MUST
- Validate ALL tokens (except public endpoints)
- Reject expired tokens
- Sanitize client headers
- Use HTTPS in production
- Log all authentication failures

### 9.2 MUST NOT
- Trust client-provided identity headers
- Log sensitive data (tokens, secrets)
- Return detailed error messages to clients (security)
- Cache tokens (only JWK keys)

### 9.3 SHOULD
- Implement rate limiting (see rate-limits.md)
- Use correlation IDs (X-Request-Id)
- Monitor authentication failures
- Alert on unusual patterns

---

**END OF GATEWAY-SECURITY.MD**
