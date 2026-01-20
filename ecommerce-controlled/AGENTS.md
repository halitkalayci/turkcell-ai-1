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

