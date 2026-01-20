# Service-to-Service Authentication

**Last Updated:** 2026-01-20  
**Status:** APPROVED - Implementation Ready  
**Keycloak Realm:** `example`

---

## 1. Authentication Strategy

### 1.1 Current Approach (MVP)

**Trusted Service Headers**

- Gateway validates user JWT tokens
- Gateway forwards user context to downstream services via headers
- Services trust gateway-injected headers for authentication
- For service-to-service calls (no user context), services use special headers

**Rationale:**
- Simplest implementation
- Low latency (no extra token validation)
- Suitable for trusted internal network

**Trade-off:**
- Requires absolute trust in gateway
- Risk if gateway compromised or bypassed

### 1.2 Future Enhancement (Defense-in-Depth)

**Client Credentials Flow**

- Each service obtains its own JWT from Keycloak using client credentials
- Service-to-service calls include service JWT
- Receiving service validates service JWT

**Benefits:**
- Cryptographic proof of service identity
- Works even if gateway bypassed
- Stronger security posture

**Cost:**
- Additional Keycloak round-trips
- Slightly higher latency
- Token caching complexity

**Decision:** Human decides when to migrate from MVP to Client Credentials.

---

## 2. Client Credentials Flow (Recommended Pattern)

### 2.1 OAuth2 Client Credentials Grant

**Used when:**
- Service A needs to call Service B
- No user context exists
- Background jobs, async event processing

**Flow:**
```
1. Service A → Keycloak: POST /token (client_credentials grant)
   - client_id: order-service-client
   - client_secret: <SECRET>
   - grant_type: client_credentials

2. Keycloak → Service A: JWT with service account token
   - sub: service-account-order-service-client
   - realm_access.roles: ["service-account"]

3. Service A → Service B: HTTP request with Bearer token
   - Authorization: Bearer <SERVICE_JWT>

4. Service B: Validates JWT, checks service-account role, processes request
```

### 2.2 Keycloak Configuration (HUMAN-EXECUTED)

**Already configured in keycloak.md:**
- `order-service-client`: Service Accounts Enabled = `true`
- `inventory-service-client`: Service Accounts Enabled = `true`
- Both have `service-account` realm role assigned

**No additional setup needed.**

### 2.3 Token Caching Strategy

**Services MUST cache service tokens:**

- Fetch token once on startup or when expired
- Cache until expiration (typically 15 minutes)
- Refresh 1 minute before expiration
- Retry on token fetch failure (exponential backoff)

**Do NOT:**
- Fetch new token for every request (performance)
- Cache beyond expiration (security)
- Share tokens across services (each service has own client)

---

## 3. Token Validation

### 3.1 Receiving Service Validation

**When Service B receives a call from Service A:**

1. **Extract token** from `Authorization: Bearer <TOKEN>` header
2. **Validate JWT signature** using Keycloak JWK keys
3. **Check expiration** (exp claim)
4. **Verify issuer** matches Keycloak realm
5. **Check role** contains `service-account`
6. **Proceed** with request processing

### 3.2 Spring Security Configuration

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/example
          jwk-set-uri: http://localhost:8180/realms/example/protocol/openid-connect/certs
```

**Automatic:**
- Spring Security validates JWT signature
- Checks expiration
- Verifies issuer
- Extracts roles from `realm_access.roles`

### 3.3 Role Check

**Service MUST verify caller has `service-account` role:**

```java
@PreAuthorize("hasRole('service-account')")
@PostMapping("/api/v1/inventory/{id}/reserve")
public ReservationDto reserve(@PathVariable UUID id, @RequestBody ReserveRequest request) {
    // Only services can call this endpoint
    return inventoryService.reserve(id, request.getQuantity());
}
```

---

## 4. Failure Handling

### 4.1 Token Fetch Failure

**If service cannot obtain token from Keycloak:**

| Attempt | Action | Backoff |
|---------|--------|---------|
| 1 | Retry | 1 second |
| 2 | Retry | 2 seconds |
| 3 | Retry | 4 seconds |
| 4+ | Log error, return 503 | - |

**Service MUST NOT:**
- Proceed without token
- Use expired token
- Use hardcoded fallback credentials

**Circuit Breaker:** Consider using Resilience4j to prevent cascade failures.

### 4.2 Token Validation Failure

**If Service B cannot validate token from Service A:**

| Error | HTTP Status | Response |
|-------|-------------|----------|
| Token missing | 401 | "Missing Authorization header" |
| Invalid signature | 401 | "Invalid token" |
| Token expired | 401 | "Token expired" |
| Wrong issuer | 401 | "Invalid issuer" |
| Missing role | 403 | "Insufficient permissions" |
| Keycloak unreachable | 503 | "Authentication service unavailable" |

### 4.3 Degraded Mode

**If Keycloak is down:**

**Option A (Strict):**
- Reject all service-to-service calls
- Return 503
- Wait for Keycloak recovery

**Option B (Resilient):**
- Use cached JWK keys (if still valid)
- Continue validating tokens with cached keys
- Alert operations team

**Current Decision:** Option B (Spring Security caches JWK keys for 10 minutes)

---

## 5. Implementation Patterns

### 5.1 RestTemplate with Token

**Service A calling Service B (Java):**

```java
@Service
public class InventoryClient {
    
    private final RestTemplate restTemplate;
    private final ServiceTokenProvider tokenProvider;
    
    public ReservationDto reserve(UUID inventoryId, int quantity) {
        String token = tokenProvider.getToken(); // Cached service token
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        ReserveRequest request = new ReserveRequest(quantity);
        HttpEntity<ReserveRequest> entity = new HttpEntity<>(request, headers);
        
        ResponseEntity<ReservationDto> response = restTemplate.exchange(
            "http://inventory-service/api/v1/inventory/{id}/reserve",
            HttpMethod.POST,
            entity,
            ReservationDto.class,
            inventoryId
        );
        
        return response.getBody();
    }
}
```

### 5.2 OpenFeign with Token

**Feign client with interceptor:**

```java
@FeignClient(name = "inventory-service", configuration = ServiceAuthConfiguration.class)
public interface InventoryServiceClient {
    
    @PostMapping("/api/v1/inventory/{id}/reserve")
    ReservationDto reserve(@PathVariable("id") UUID id, @RequestBody ReserveRequest request);
}

@Configuration
public class ServiceAuthConfiguration {
    
    @Bean
    public RequestInterceptor serviceTokenInterceptor(ServiceTokenProvider tokenProvider) {
        return requestTemplate -> {
            String token = tokenProvider.getToken();
            requestTemplate.header("Authorization", "Bearer " + token);
        };
    }
}
```

### 5.3 Service Token Provider

**Manages token fetching and caching:**

```java
@Service
public class ServiceTokenProvider {
    
    private final RestTemplate keycloakRestTemplate;
    private final ServiceTokenProperties properties;
    
    private String cachedToken;
    private Instant expiresAt;
    
    public synchronized String getToken() {
        if (cachedToken == null || isExpired()) {
            fetchToken();
        }
        return cachedToken;
    }
    
    private boolean isExpired() {
        return expiresAt == null || Instant.now().isAfter(expiresAt.minusSeconds(60)); // Refresh 1 min early
    }
    
    private void fetchToken() {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", properties.getClientId());
        body.add("client_secret", properties.getClientSecret());
        body.add("grant_type", "client_credentials");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        
        TokenResponse response = keycloakRestTemplate.postForObject(
            properties.getTokenEndpoint(),
            request,
            TokenResponse.class
        );
        
        this.cachedToken = response.getAccessToken();
        this.expiresAt = Instant.now().plusSeconds(response.getExpiresIn());
    }
}
```

---

## 6. Configuration Management

### 6.1 Application Properties

**order-service (example):**

```yaml
keycloak:
  token-endpoint: http://localhost:8180/realms/example/protocol/openid-connect/token
  client-id: order-service-client
  client-secret: ${ORDER_SERVICE_CLIENT_SECRET}  # From environment variable

# Or using Spring Security OAuth2 client:
spring:
  security:
    oauth2:
      client:
        registration:
          keycloak:
            client-id: order-service-client
            client-secret: ${ORDER_SERVICE_CLIENT_SECRET}
            authorization-grant-type: client_credentials
        provider:
          keycloak:
            token-uri: http://localhost:8180/realms/example/protocol/openid-connect/token
```

### 6.2 Secret Management

**MUST:**
- Store client secrets as environment variables
- NEVER commit secrets to git
- Use different secrets per environment (dev/prod)
- Rotate secrets quarterly

**Recommended:**
- Use Kubernetes Secrets
- Use AWS Secrets Manager / Azure Key Vault
- Use HashiCorp Vault

---

## 7. Mixed Context Scenarios

### 7.1 User Request → Service-to-Service Call

**Scenario:**
1. User calls order-service to create order
2. order-service calls inventory-service to reserve stock

**Two options:**

#### Option A: Forward User Token
```
User → Gateway (user JWT) → Order Service (user JWT) → Inventory Service (user JWT)
```

**Pros:**
- Preserves user context for audit logs
- Inventory knows which user triggered reservation

**Cons:**
- Tight coupling to user token lifetime
- Inventory needs to understand user roles

#### Option B: Service Token (Recommended)
```
User → Gateway (user JWT) → Order Service (user JWT + service JWT) → Inventory Service (service JWT)
```

**Pros:**
- Clean separation: user context vs service context
- Inventory service-only endpoints secured properly

**Cons:**
- Loses user context (mitigate: pass userId in request body)

**Current Decision:** Option B - Service uses its own client credentials for downstream calls.

### 7.2 Async Event Processing

**Scenario:** Kafka consumer processes OrderCreated event

**No user token available:**
- Consumer uses service client credentials
- Calls inventory-service with service token
- Passes userId from event payload (not from token)

---

## 8. Testing Strategy

### 8.1 Unit Tests

**Mock token provider:**
```java
@MockBean
private ServiceTokenProvider tokenProvider;

@Test
void shouldReserveInventory() {
    when(tokenProvider.getToken()).thenReturn("mock-service-token");
    // Test service-to-service call
}
```

### 8.2 Integration Tests

**Test with real Keycloak (Testcontainers):**

1. Start Keycloak container
2. Create test realm + clients
3. Fetch real service token
4. Call service endpoints
5. Verify 401/403 responses

### 8.3 Manual Testing

**HUMAN can fetch service token:**

```bash
curl -X POST http://localhost:8180/realms/example/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=order-service-client" \
  -d "client_secret=<SECRET>" \
  -d "grant_type=client_credentials"
```

**Use token in API call:**
```bash
curl -X POST http://localhost:8082/api/v1/inventory/123/reserve \
  -H "Authorization: Bearer <SERVICE_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"quantity": 5}'
```

---

## 9. Security Considerations

### 9.1 Token Security

**MUST:**
- Transmit tokens over HTTPS only (production)
- Never log tokens
- Clear tokens from memory after use
- Use short expiration (15 minutes)

**MUST NOT:**
- Store tokens in databases
- Send tokens in query parameters
- Share tokens between services
- Use tokens beyond expiration

### 9.2 Client Secret Rotation

**Process (HUMAN-EXECUTED):**

1. Generate new client secret in Keycloak
2. Keycloak supports multiple secrets temporarily
3. Update service configuration with new secret
4. Deploy service
5. Remove old secret from Keycloak after deployment

**Frequency:** Quarterly or after suspected compromise

---

## 10. Monitoring & Alerting

### 10.1 Metrics to Track

- Service token fetch failures
- Service token validation failures
- Token cache hit rate
- Average token fetch latency
- Keycloak health check failures

### 10.2 Alerts

**Critical:**
- Keycloak unreachable for > 2 minutes
- Service token fetch failure rate > 10%

**Warning:**
- Token validation failure rate increasing
- Token cache miss rate > 20%

---

**END OF SERVICE-TO-SERVICE-AUTH.MD**
