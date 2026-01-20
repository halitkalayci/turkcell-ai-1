# Rate Limiting Strategy

**Last Updated:** 2026-01-20  
**Status:** APPROVED - Implementation Ready

---

## 1. Gateway-Level Limits

### 1.1 Strategy

**Gateway implements rate limiting as a cross-cutting concern:**
- Protects all downstream services from overload
- Prevents abuse and DDoS attacks
- Ensures fair resource allocation

### 1.2 Limit Types

#### Global Rate Limit
**Applied to all requests regardless of user:**

- **Threshold:** 10,000 requests per minute (global)
- **Purpose:** Protect infrastructure from total overload
- **Response:** 429 Too Many Requests

#### Per-IP Rate Limit
**Applied per client IP address:**

- **Threshold:** 100 requests per minute per IP
- **Purpose:** Prevent single source abuse
- **Response:** 429 with `Retry-After` header

#### Per-User Rate Limit (Authenticated)
**Applied per authenticated user (extracted from JWT sub claim):**

- **Threshold:** 60 requests per minute per user
- **Purpose:** Fair usage across users
- **Bypass:** Admin users exempt (optional)

#### Public Endpoint Rate Limit
**Applied to unauthenticated endpoints:**

- **Threshold:** 10 requests per minute per IP
- **Endpoints:** `/api/v1/identity/login`, `/api/v1/identity/register`
- **Purpose:** Prevent brute force attacks

### 1.3 Implementation Options

#### Option A: Spring Cloud Gateway Rate Limiter (Redis)

**Dependencies:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
```

**Configuration:**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/v1/orders/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 60  # tokens per second
                redis-rate-limiter.burstCapacity: 100  # max tokens
                redis-rate-limiter.requestedTokens: 1  # tokens per request
                key-resolver: "#{@userKeyResolver}"
  
  redis:
    host: localhost
    port: 6379
```

**Key Resolver Bean:**
```java
@Bean
public KeyResolver userKeyResolver() {
    return exchange -> {
        // Extract user ID from JWT or IP address
        String userId = extractUserId(exchange);
        return Mono.just(userId != null ? userId : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
    };
}
```

#### Option B: Bucket4j (In-Memory or Redis)

**For simple in-memory rate limiting (stateless):**
- Use Bucket4j library
- Cache per-user/per-IP buckets
- Suitable for single gateway instance

**Trade-off:** Does not work across multiple gateway instances without distributed cache.

### 1.4 Response Format

**429 Too Many Requests:**
```json
{
  "timestamp": "2026-01-20T14:30:00Z",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Please try again later.",
  "path": "/api/v1/orders"
}
```

**Headers:**
```
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1705760460
Retry-After: 30
```

---

## 2. Per-Service Limits

### 2.1 Why Service-Level Rate Limiting?

**Gateway rate limiting is NOT sufficient for:**
- Expensive operations (e.g., complex queries, reports)
- Background jobs consuming resources
- Direct service-to-service calls (if gateway bypassed)

**Services implement their own limits for:**
- Fine-grained control per endpoint
- Business-specific thresholds
- Resource protection

### 2.2 Example Limits (order-service)

| Endpoint | Rate Limit | Scope | Reason |
|----------|------------|-------|--------|
| `POST /api/v1/orders` | 10/minute | Per user | Prevent order spam |
| `GET /api/v1/orders` | 60/minute | Per user | Expensive query |
| `GET /api/v1/orders/{id}` | 120/minute | Per user | Cheap lookup |
| `DELETE /api/v1/orders/{id}` | 5/minute | Per user | Prevent abuse |

### 2.3 Implementation (Bucket4j in Service)

**Dependency:**
```xml
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.7.0</version>
</dependency>
```

**Rate Limiter Filter:**
```java
@Component
public class RateLimitFilter implements Filter {
    
    private final Map<String, Bucket> userBuckets = new ConcurrentHashMap<>();
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String userId = httpRequest.getHeader("X-User-Id");
        
        if (userId != null) {
            Bucket bucket = resolveUserBucket(userId);
            
            if (!bucket.tryConsume(1)) {
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.setStatus(429);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
                return;
            }
        }
        
        chain.doFilter(request, response);
    }
    
    private Bucket resolveUserBucket(String userId) {
        return userBuckets.computeIfAbsent(userId, key -> {
            Bandwidth limit = Bandwidth.simple(60, Duration.ofMinutes(1));
            return Bucket.builder().addLimit(limit).build();
        });
    }
}
```

---

## 3. DDoS Protection

### 3.1 Infrastructure-Level

**HUMAN configures (outside application):**

- **Cloud Provider DDoS Protection:**
  - AWS Shield
  - Azure DDoS Protection
  - Cloudflare

- **WAF (Web Application Firewall):**
  - AWS WAF
  - Azure Front Door
  - Cloudflare WAF

**Rules:**
- Block known malicious IPs
- Detect and block suspicious patterns
- Rate limit at CDN/edge level

### 3.2 Application-Level

**Gateway implements:**

1. **Connection Limits:**
   - Max concurrent connections per IP: 100
   - Max connections per user: 10

2. **Request Size Limits:**
   - Max request body size: 1 MB
   - Max header size: 16 KB

3. **Timeout Policies:**
   - Request timeout: 30 seconds
   - Idle connection timeout: 60 seconds

**Configuration:**
```yaml
server:
  max-http-header-size: 16KB
  connection-timeout: 60s

spring:
  servlet:
    multipart:
      max-file-size: 1MB
      max-request-size: 1MB
```

### 3.3 Suspicious Pattern Detection

**Monitor and alert on:**
- Spike in 429 responses
- High volume from single IP
- Repeated failed authentication (401)
- Rapid sequential requests to same endpoint
- Unusual geographic patterns

**Action:** Temporarily ban IP for 1 hour (human reviews)

---

## 4. Client Quotas

### 4.1 Client Application Quotas

**For frontend applications (future):**

| Client Type | Daily Quota | Rate Limit | Priority |
|-------------|-------------|------------|----------|
| Web App | 100,000 req/day | 200/min | Normal |
| Mobile App | 50,000 req/day | 100/min | Normal |
| Admin Dashboard | Unlimited | 500/min | High |
| Third-Party API | 10,000 req/day | 50/min | Low |

**Tracked by:** Client ID (from OAuth2 client)

### 4.2 Implementation

**Gateway reads client ID from JWT `aud` claim:**

```java
@Bean
public KeyResolver clientKeyResolver() {
    return exchange -> {
        // Extract client ID from JWT
        String clientId = extractClientId(exchange);
        return Mono.justOrEmpty(clientId);
    };
}
```

**Redis stores daily counter:**
```
Key: rate_limit:client:{clientId}:daily
Value: request count
TTL: 24 hours
```

### 4.3 Quota Exceeded Response

**HTTP 429:**
```json
{
  "error": "quota_exceeded",
  "message": "Daily quota exceeded for client",
  "quota": 10000,
  "used": 10000,
  "reset_at": "2026-01-21T00:00:00Z"
}
```

---

## 5. Bypass Rules

### 5.1 Whitelisted IPs

**For internal tools, monitoring, health checks:**

```yaml
rate-limit:
  whitelist:
    ips:
      - 127.0.0.1
      - 10.0.0.0/8  # Internal network
      - 192.168.1.100  # Admin workstation
```

### 5.2 Whitelisted Users

**Admin users exempt from rate limits:**

```java
if (user.hasRole("admin")) {
    // Skip rate limiting
    chain.doFilter(request, response);
    return;
}
```

### 5.3 Health Check Endpoints

**Always exempt from rate limiting:**
- `/actuator/health`
- `/actuator/info`
- `/actuator/prometheus`

---

## 6. Configuration Management

### 6.1 Environment-Specific Limits

**Development:**
```yaml
rate-limit:
  enabled: false  # Disable for local testing
```

**Staging:**
```yaml
rate-limit:
  per-user: 200  # Higher for load testing
```

**Production:**
```yaml
rate-limit:
  per-user: 60
  per-ip: 100
  global: 10000
```

### 6.2 Dynamic Configuration (Future)

**HUMAN can adjust limits without redeployment:**
- Store limits in Redis/database
- Gateway reads limits on startup
- Admin API to update limits
- Changes take effect within 1 minute

---

## 7. Monitoring & Metrics

### 7.1 Metrics to Track

**Prometheus metrics:**
- `http_requests_rate_limited_total` (counter)
- `http_requests_per_user` (histogram)
- `http_requests_per_ip` (histogram)
- `rate_limit_quota_remaining` (gauge)

### 7.2 Dashboards

**Grafana panels:**
1. Rate limit hit rate (% of requests rate limited)
2. Top users by request volume
3. Top IPs by request volume
4. Rate limit errors over time
5. Per-endpoint rate limit status

### 7.3 Alerts

**Critical:**
- Rate limit hit rate > 10% for 5 minutes
- Single IP exceeds 1000 requests in 1 minute
- Failed authentication rate > 100/minute

**Warning:**
- Rate limit hit rate > 5%
- User approaching quota (80% used)

---

## 8. Testing Strategy

### 8.1 Load Testing

**Simulate rate limit scenarios:**

```bash
# Test per-user rate limit
for i in {1..100}; do
  curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/orders &
done
wait

# Verify 429 response received
```

### 8.2 Integration Tests

**Test rate limiting:**
```java
@Test
void shouldRateLimitUser() {
    // Make 60 requests (within limit)
    for (int i = 0; i < 60; i++) {
        Response response = given()
            .header("Authorization", "Bearer " + userToken)
            .get("/api/v1/orders");
        assertThat(response.statusCode()).isEqualTo(200);
    }
    
    // 61st request should be rate limited
    Response response = given()
        .header("Authorization", "Bearer " + userToken)
        .get("/api/v1/orders");
    assertThat(response.statusCode()).isEqualTo(429);
}
```

---

## 9. Client Guidance

### 9.1 Best Practices for Clients

**Clients SHOULD:**
- Respect `Retry-After` header
- Implement exponential backoff
- Cache responses when possible
- Batch requests instead of individual calls
- Monitor quota usage

**Example (JavaScript):**
```javascript
async function fetchOrders() {
  try {
    const response = await fetch('/api/v1/orders');
    
    if (response.status === 429) {
      const retryAfter = response.headers.get('Retry-After');
      console.warn(`Rate limited. Retry after ${retryAfter} seconds`);
      await sleep(retryAfter * 1000);
      return fetchOrders(); // Retry
    }
    
    return response.json();
  } catch (error) {
    console.error('Request failed:', error);
  }
}
```

### 9.2 Quota Monitoring (Client-Side)

**Clients can track their usage:**
- Read `X-RateLimit-*` headers
- Display quota usage in UI
- Warn user when approaching limit

---

**END OF RATE-LIMITS.MD**
