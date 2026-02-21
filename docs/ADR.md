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

---

## ADR-003: Adoption of ReactiveRedisTemplate & JSON Serialization

### Context
I initially attempted to use `ReactiveCrudRepository` (the standard Spring Data interface). However, Spring Data Redis does not support reactive repositories for Redis (only blocking). This caused `InvalidDataAccessApiUsageException` at startup.

Additionally, standard JSON serializers (`Jackson2JsonRedisSerializer`) are deprecated in Spring Data Redis 4.0 in favor of `RedisSerializer` API.

### Decision
1.  **Manual Repositories (no Interface):** Manually implement the Repository pattern using `ReactiveRedisTemplate`. This provides fine-grained control over serialization and atomic operations (CAS).
2.  **Serialization:** We use `RedisSerializer.json()` instead of the deprecated classes.

### Consequences
* **Positive:** Full non-blocking I/O support. No compilation warnings.
* **Negative:** Must write all code for `save`, `find`, and `delete` methods (no auto-generated queries).
