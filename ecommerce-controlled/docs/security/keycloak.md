# Keycloak Integration

**Last Updated:** 2026-01-20  
**Status:** APPROVED - Ready for Human Execution  
**Realm:** `example`

---

## 1. Realm Configuration (HUMAN-EXECUTED)

### Realm: `example`

**Access:** Keycloak Admin Console → Create Realm

**Settings:**
- **Realm Name:** `example`
- **Display Name:** E-Commerce Example Realm
- **Enabled:** `true`
- **User Registration:** `false` (controlled registration only)
- **Email as Username:** `false`
- **Login with Email:** `true`
- **Edit Username:** `false`
- **Forgot Password:** `true`
- **Remember Me:** `true`
- **Verify Email:** `true`

**Token Lifetimes:**
- Access Token Lifespan: `15 minutes` (900s)
- Access Token Lifespan For Implicit Flow: `15 minutes`
- Client Login Timeout: `5 minutes`
- Refresh Token Max Reuse: `0`
- SSO Session Idle: `30 minutes`
- SSO Session Max: `10 hours`
- Offline Session Idle: `30 days`

---

## 2. Client Definitions (HUMAN-EXECUTED)

### 2.1 gateway-client (Public Gateway)

**Purpose:** API Gateway that validates tokens and routes requests

**Settings:**
- **Client ID:** `gateway-client`
- **Client Protocol:** `openid-connect`
- **Access Type:** `confidential`
- **Standard Flow Enabled:** `true`
- **Direct Access Grants Enabled:** `false`
- **Service Accounts Enabled:** `false`
- **Authorization Enabled:** `false`
- **Valid Redirect URIs:** 
  - `http://localhost:8080/*`
  - `https://gateway.ecommerce.example.com/*`
- **Web Origins:** `*` (or restrict to specific origins)
- **Root URL:** `http://localhost:8080`

**Roles:** None (gateway validates tokens, doesn't need roles)

**Client Secret:** Generate via Credentials tab (HUMAN copies to gateway config)

---

### 2.2 order-service-client (Backend Service)

**Purpose:** Order Service internal authentication

**Settings:**
- **Client ID:** `order-service-client`
- **Client Protocol:** `openid-connect`
- **Access Type:** `confidential`
- **Standard Flow Enabled:** `false`
- **Direct Access Grants Enabled:** `false`
- **Service Accounts Enabled:** `true` (for service-to-service auth)
- **Authorization Enabled:** `false`

**Service Account Roles:**
- Realm Role: `service-account`
- Custom Role: `order-service`

**Client Secret:** Generate via Credentials tab

---

### 2.3 inventory-service-client (Backend Service)

**Purpose:** Inventory Service internal authentication

**Settings:**
- **Client ID:** `inventory-service-client`
- **Client Protocol:** `openid-connect`
- **Access Type:** `confidential`
- **Standard Flow Enabled:** `false`
- **Direct Access Grants Enabled:** `false`
- **Service Accounts Enabled:** `true`
- **Authorization Enabled:** `false`

**Service Account Roles:**
- Realm Role: `service-account`
- Custom Role: `inventory-service`

**Client Secret:** Generate via Credentials tab

---

### 2.4 identity-service-client (Identity Service)

**Purpose:** User authentication and token management

**Settings:**
- **Client ID:** `identity-service-client`
- **Client Protocol:** `openid-connect`
- **Access Type:** `confidential`
- **Standard Flow Enabled:** `true`
- **Direct Access Grants Enabled:** `true` (for password grant)
- **Service Accounts Enabled:** `true`
- **Authorization Enabled:** `false`

**Client Secret:** Generate via Credentials tab

---

### 2.5 web-app-client (Frontend Application)

**Purpose:** Future web/mobile frontend applications

**Settings:**
- **Client ID:** `web-app-client`
- **Client Protocol:** `openid-connect`
- **Access Type:** `public` (SPA/Mobile - no secret)
- **Standard Flow Enabled:** `true`
- **Direct Access Grants Enabled:** `false`
- **Implicit Flow Enabled:** `false`
- **Valid Redirect URIs:**
  - `http://localhost:3000/*`
  - `https://app.ecommerce.example.com/*`
- **Web Origins:** `+` (all valid redirect URIs)

**PKCE:** Required (S256)

---

## 3. Role Mapping Strategy

### 3.1 Realm Roles

**Human creates these roles in Keycloak:**

| Role Name | Description | Composite |
|-----------|-------------|-----------|
| `service-account` | Internal service authentication | No |
| `customer` | Standard customer role | No |
| `admin` | Administrative access | Yes (includes customer) |
| `order-manager` | Manage all orders | No |
| `inventory-manager` | Manage inventory | No |

### 3.2 Client Roles

**order-service-client roles:**
- `order.read` - Read orders
- `order.create` - Create orders
- `order.update` - Update order status
- `order.cancel` - Cancel orders

**inventory-service-client roles:**
- `inventory.read` - Read inventory
- `inventory.write` - Modify inventory
- `inventory.reserve` - Reserve stock

### 3.3 Default Role Assignment

**Human configures in Keycloak:**
- New users → `customer` role (default)
- Service accounts → `service-account` role
- Admin users → `admin` role (manual assignment)

---

## 4. Token Configuration

### 4.1 JWT Claims (Included by Default)

**Standard Claims:**
- `sub` - Subject (user ID)
- `iat` - Issued at
- `exp` - Expiration
- `iss` - Issuer (Keycloak URL)
- `aud` - Audience (client ID)
- `typ` - Token type (Bearer)

**Custom Claims (to add via Mappers):**
- `email` - User email
- `preferred_username` - Username
- `realm_access.roles` - Realm roles array
- `resource_access.{client}.roles` - Client-specific roles

### 4.2 Token Mappers (HUMAN-EXECUTED)

**Add these mappers to clients:**

1. **User ID Mapper**
   - Name: `user-id`
   - Mapper Type: `User Property`
   - Property: `id`
   - Token Claim Name: `userId`
   - Claim JSON Type: `String`
   - Add to ID token: `true`
   - Add to access token: `true`

2. **Email Mapper**
   - Name: `email`
   - Mapper Type: `User Property`
   - Property: `email`
   - Token Claim Name: `email`
   - Add to ID token: `true`
   - Add to access token: `true`

3. **Realm Roles Mapper**
   - Name: `realm-roles`
   - Mapper Type: `User Realm Role`
   - Token Claim Name: `realm_access.roles`
   - Multivalued: `true`
   - Add to ID token: `true`
   - Add to access token: `true`

---

## 5. Integration Checklist (HUMAN EXECUTION)

### Phase 1: Keycloak Setup
- [ ] Install Keycloak (Docker or Standalone)
- [ ] Access Admin Console (default: http://localhost:8180)
- [ ] Create realm: `example`
- [ ] Configure realm settings (Section 1)
- [ ] Set token lifetimes (Section 1)

### Phase 2: Client Creation
- [ ] Create `gateway-client` (Section 2.1)
- [ ] Generate and save gateway-client secret
- [ ] Create `order-service-client` (Section 2.2)
- [ ] Generate and save order-service-client secret
- [ ] Create `inventory-service-client` (Section 2.3)
- [ ] Generate and save inventory-service-client secret
- [ ] Create `identity-service-client` (Section 2.4)
- [ ] Generate and save identity-service-client secret
- [ ] Create `web-app-client` (Section 2.5)

### Phase 3: Roles & Permissions
- [ ] Create realm roles (Section 3.1)
- [ ] Create client roles for order-service-client (Section 3.2)
- [ ] Create client roles for inventory-service-client (Section 3.2)
- [ ] Configure default roles (Section 3.3)

### Phase 4: Token Configuration
- [ ] Add token mappers (Section 4.2)
- [ ] Verify JWT structure via Keycloak token inspector
- [ ] Test token with jwt.io

### Phase 5: Test Users (HUMAN CREATES)
- [ ] Create test user: `testuser@example.com` / password / role: `customer`
- [ ] Create admin user: `admin@example.com` / password / role: `admin`
- [ ] Verify email (if enabled)
- [ ] Test login via Keycloak login page

### Phase 6: Service Configuration
- [ ] Copy client secrets to each service's application.yml
- [ ] Configure Keycloak URLs in services
- [ ] Restart services
- [ ] Verify token validation

---

## 6. Local Development Keycloak (Docker)

**HUMAN EXECUTES:**

```bash
docker run -d \
  --name keycloak-example \
  -p 8180:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:23.0.0 \
  start-dev
```

**Access:**
- Admin Console: http://localhost:8180
- Username: `admin`
- Password: `admin`

**Realm Export (after manual setup):**
```bash
# Export realm configuration (HUMAN EXECUTES after setup)
docker exec -it keycloak-example \
  /opt/keycloak/bin/kc.sh export \
  --dir /tmp/export \
  --realm example
```

---

## 7. Spring Boot Configuration (AI GUIDANCE ONLY)

**AI will generate this structure in each service's `application.yml`:**

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/example
          jwk-set-uri: http://localhost:8180/realms/example/protocol/openid-connect/certs
```

**HUMAN MUST PROVIDE:**
- Actual client secrets (not committed to git)
- Production Keycloak URLs
- Environment-specific overrides

---

## 8. Security Constraints

### 8.1 Secret Management
- **NEVER** commit client secrets to git
- Use environment variables or secret managers
- Rotate secrets quarterly
- Use different secrets per environment (dev/staging/prod)

### 8.2 Token Validation Rules
- Services MUST validate JWT signature
- Services MUST validate token expiration
- Services MUST validate issuer
- Services MUST validate audience (if applicable)
- Services MUST check required roles/scopes

### 8.3 HTTPS Requirements
- Production Keycloak MUST use HTTPS
- Token transmission MUST be over HTTPS
- Redirect URIs MUST use HTTPS (except localhost)

---

## 9. Troubleshooting

### Token Validation Fails
1. Check issuer-uri matches Keycloak realm
2. Verify jwk-set-uri is reachable
3. Check token expiration
4. Verify client is enabled in Keycloak
5. Check service logs for signature validation errors

### Service-to-Service Auth Fails
1. Verify service account is enabled for client
2. Check client secret is correct
3. Verify service account has required roles
4. Check token endpoint URL

### Login Fails
1. Verify user exists and is enabled
2. Check password is correct
3. Verify email is verified (if required)
4. Check client redirect URIs
5. Verify client is enabled

---

**END OF KEYCLOAK.MD**
