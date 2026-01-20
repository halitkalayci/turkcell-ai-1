## Mini E-Commerce Microservices (Java 21 / Spring Boot)

This repository contains a mini e-commerce microservices ecosystem.

- Services: order-service, payment-service, inventory-service

- Tech: Java 21, Spring Boot 3.X, OpenAPI/Swagger

- Approach: Contract-First

If any rule below is violated, the output is WRONG.

---

## 1) HOW TO WORK (MANDATORY WORKFLOW)

### 1.1 Plan-first, then code

Before generating code, you MUST:

1. Confirm the relevant contract(s) exist and match the request. (OpenAPI)

2. Propose a FILE BREAKDOWN (what files will be created/changed + why)

3. Ask QUESTIONS for missing details instead of guessing.

4. Generate code ONE FILE AT A TIME (or in small batches of max 3 files).

### 1.2 No inventing

You MUST NOT invent:

- endpoints, request/response fields, error models

- event names/payloads

- DB schema/columns

- Business rules, status, money rules

If any detail is missing, ASK.


## 2) CONTRACT FIRST

### 2.1 OpenAPI is the source of truth

- API contracts live under: `/docs/openapi`

- Each service MUST have its own OpenAPI file:
  - `docs/openapi/order-service.yml`
  - `docs/openapi/inventory-service.yml`

- Swagger UI is generated from these contracts.

- Implementation MUST follow the contract; never the other way around.

### 2.2 Code Generation Policy

- We may use OpenAPI tooling ONLY if already present in the repository.

- You MUST NOT add new OpenAPI generator dependencies without explicit approval.

- If no generator is available, implement controllers/DTOs manually to match the spec.

### 2.3 Versioning

- API Changes MUST be versioned. (eg. `api/v1/orders`, `/api/v2/orders`)

- Version MUST be placed in path.

- Breaking changes require a new version; do not silently break clients.


## 3) ARCHITECTURE (PER SERVICE)

Each service MUST follow this layering:

`controller (web) -> application (use-cases) -> domain (business rules) -> infrastructure (persistence/clients/messaging)`

### 3.1 Controller (web)

- No business logic.

- Validates input, maps to application layer, return response DTOs.

- NEVER returns JPA entities.

### 3.2 Application (use-cases)

- Orchestrates use-cases, transaction, ports.

- MUST BE unit-testable.

### 3.3 Domain

- Contains business invariants and domain model.

- Avoid framework annotations in domain if possible.

### 3.4 Infrastructure

- JPA entities, repositories, external clients, messaging adapters.

- No business rules here.


## 4) DEPENDENCIES (VERY STRICT)

- You MUST NOT add new dependencies without explicit approval.

- Prefer Spring Boot starters already included.

- If a new dependency seems necessary, you MUST provide:
  
  1. Why it is required?

  2. Why standard Spring/Java is insufficient?

  3. Alternatives + trade-offs

## 5) CODING STANDARDS (QUALITY BAR)

### 5.1) Simplicity

- Prefer the simplest working solution.

- Avoid over-engineering, unnecessary patterns, extra layers.

### 5.2) Money & Time

- Money uses `BigDecimal`, never `double/float`

- Time uses `Instant` or `OffsetDateTime`

- Do not mix time types without reason.

### 5.3) IDs

- Use a single ID strategy across all services. (UUID)

- Never mix UUID and Long unless explicitly required by contract.

## 6) GATEWAY RULES (REPO-WIDE)

- Gateway contains NO business logic; only routing + cross-cutting concerns.

## 7) COMMUNICATION (SYNC/ASYNC) - STRICT RULES (NON-NEGOTIABLE)

This repo uses **two** communication modes only:

- **SYNC (blocking)**: HTTP via **OpenFeign**
- **ASYNC (non-blocking)**: Events via **Kafka** (through Spring Cloud Stream)

If a rule below is violated, the output is WRONG.

---

### 7.1) Default rule: Blocking calls MUST be SYNC (Feign)

### 7.1.1 When a call is considered BLOCKING
A call is BLOCKING if the caller needs the result to continue the current request flow:

- validation / pre-check (e.g., "is stock available?")
- authorization / entitlement check
- "create order" that must ensure something before returning success
- any use-case that cannot be completed without the remote answer

### 7.1.2 Mandatory implementation rule for BLOCKING calls
- All BLOCKING inter-service calls MUST be **SYNC HTTP** using **OpenFeign**
- NO Kafka request/reply patterns
- NO polling patterns for blocking needs
- NO “event then wait” hacks

If a blocking dependency exists and no sync contract is defined, implementation is BLOCKED.

---

### 7.2) Async rule: State changes MUST be ASYNC (Kafka)

### 7.2.1 What MUST be async
Use Kafka events for:

- domain facts / state changes (OrderCreated, PaymentAuthorized, StockReserved, etc.)
- cross-service notifications
- eventual consistency workflows (Sagas)

### 7.2.2 Mandatory implementation rule for ASYNC
- All ASYNC communication MUST be Kafka events
- Producers publish facts; consumers react
- No business logic in consumers beyond applying the local reaction + idempotency

If async is used, events MUST be defined explicitly (name + payload + topic) in docs.

---

### 7.3) Decision matrix (must be documented per interaction)

For EACH cross-service interaction, AI MUST produce a decision entry:

- Interaction name (e.g., Order → Inventory: Check stock)
- Mode: SYNC (Feign) or ASYNC (Kafka)
- Blocking? (Yes/No)
- Reason (1 sentence)
- Failure handling (timeout/retry/DLQ)

If any interaction is not documented, STOP and ASK.

---

### 7.4) Contract requirements (hard blockers)

### 7.4.1 Sync (Feign) hard blockers
For every SYNC interaction, the following MUST exist BEFORE code:

- OpenAPI contract for the CALLEE endpoint
- Client interface mapping (Feign) MUST match OpenAPI
- Error model and status codes must match contract

Missing any of these = BLOCKER.

### 7.4.2 Async (Kafka) hard blockers
For every event, the following MUST exist BEFORE code:

- Event catalog entry (name, payload schema, version)
- Topic mapping (topic name, key, retention basics)
- Consumer group naming rule
- Idempotency strategy

Missing any of these = BLOCKER.

---

### 7.5) Reliability rules (mandatory)

### 7.5.1 SYNC reliability
- Timeouts MUST be set (no infinite waits)
- Retries are allowed ONLY for safe/idempotent operations (GET).
- For POST/side-effect calls: NO automatic retries unless an idempotency key is implemented and documented.

### 7.5.2 ASYNC reliability
- Consumers MUST be idempotent (keyed by eventId)
- DLQ strategy MUST exist conceptually (even if not implemented yet)
- Retry backoff MUST be documented
- Event versioning required for breaking payload changes

---

### 7.6) Forbidden patterns

- Using Kafka as a blocking dependency (request/reply)
- Cross-service DB reads
- “Temporary direct call” without contract
- Ad-hoc JSON payloads not defined in event catalog
- Silent contract drift (implementation leading contract)

---

### 7.7) Default tech choices (unless repo already differs)

- Sync HTTP client: **OpenFeign**
- Async broker: **Kafka**
- Event serialization: JSON (explicit schema in docs)
- Event key: aggregateId (e.g., orderId) unless explicitly stated otherwise

### 7.8) Default KAFKA Configurations

- Each producer MUST implement "Transactional Outbox Pattern" and MUST use a publisher to send events to kafka.

- Each consumer MUST implement "Idempotency" using table named `Inbox` 

- Kafka MUST try sending messages (retry) max. 5 times. If failed it MUST send them to DLQ.

- Kafka configurations MUST follow this adress: `127.0.0.1:29023`

---