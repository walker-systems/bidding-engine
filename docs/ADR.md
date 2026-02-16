# Architecture Decision Records (ADR)

## ADR-001: Use of Reactive Redis over RDBMS

### Context
We expect burst traffic (10k+ TPS) during the final seconds of an auction ("sniping"). Traditional RDBMS (Postgres) blocking I/O would require massive connection pools and vertical scaling.

### Decision
We will use **Spring Data Redis Reactive** as the primary source of truth for active auctions.

### Consequences
* **Pros:** Ultra-low latency (<5ms). Native support for Atomic operations (Lua).
* **Cons:** Dataset must fit in RAM.
* **Mitigation:** We enable Redis AOF (Append Only File) for durability.

---

## ADR-002: Java 25 & Records

### Context
Domain objects in bidding systems are data-heavy and behavior-light.

### Decision
We use **Java 25 Records** for all domain models.

### Consequences
* **Pros:** Immutability by default (thread-safe). Compact constructors for validation.
* **Cons:** Cannot use JPA (Hibernate) lazy loading (not an issue since we use Redis).
