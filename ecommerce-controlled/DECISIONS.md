## DECISIONS.md - (ADR - Architecture Decision Record)
Last updated: 2026-01-19

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