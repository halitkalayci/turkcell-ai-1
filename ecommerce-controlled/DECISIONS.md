## DECISIONS.md - (ADR - Architecture Decision Record)
Last updated: 2026-01-19T10:00

This document contains HUMAN-APPROVED technical decisions.
AI assistants MUST follow these decisions and MUST NOT ask unless information is missing.

---

## D001 - Build Tool

**Decision:** Maven

**Why:** Team standard

**Alternatives considered:** Gradle (Rejected for MVP consistency)

---


## D002 - Java Version

**Decision:** Java 21

**Why:** LTS + modern language features + Spring Boot 3

---

## D003 - Database

**Decision:** H2 in-memory

**Why:** Zero-ops local demo

**Non-goal:** Production DB decisions are out of scope for this project.

---

## D004 - Order Service Business Rules

**Decision:** Formal business rules approved for order-service

**Date:** 2026-01-20

**Status:** APPROVED & FROZEN - Now under contract-first governance

**What:** Defined complete order domain model including:
- 5 order states (PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)
- State transition rules and forbidden transitions
- Business rules for all use-cases (Create, Get, List, Cancel, Update Status)
- Validation rules (technical + business)
- Error handling strategy (400, 404, 409, 500)
- Calculation formulas and domain invariants

**Why:** 
- Order-service was intentionally created with EMPTY implementation
- Design-first approach allows reviewing business logic before coding
- Establishes clear contract for future payment/inventory integrations

**Documentation:** `docs/rules/order-service-rules.md`

**Next Steps:**
1. ✅ Business rules documented
2. Generate OpenAPI spec based on rules
3. Implement domain model (entities, enums, value objects)
4. Implement application layer (use-cases)
5. Implement infrastructure layer (repositories, persistence)

**Note:** This was an EXPLORATION phase where inventing rules was ALLOWED. 
Now that rules are approved, they are frozen and require formal review to change.

---