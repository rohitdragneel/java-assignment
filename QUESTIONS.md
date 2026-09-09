# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
Yes, I would definitely refactor this to be consistent. Right now, we have three different persistence styles in the same project:
1. Store uses the Panache Active Record pattern (Store extends PanacheEntity, calling static methods like Store.listAll() and instance methods like store.persist()).
2. Product uses standard JPA with EntityManager / injected ProductRepository in ProductResource.
3. Warehouse uses Hexagonal Architecture (Ports & Adapters) with the Repository pattern (WarehouseRepository implementing the WarehouseStore port and PanacheRepository<DbWarehouse>), separating the domain model (Warehouse) from the DB entity (DbWarehouse).

If I were maintaining this, I would standardize the whole codebase on the Repository pattern (option 3 or PanacheRepository):
- Why:
  - Active Record tightly couples database concerns directly into domain classes, making unit testing harder because you need an active Hibernate/Panache session even for simple business logic tests.
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
Both approaches have clear trade-offs:

1. Contract-First (OpenAPI code generation - like Warehouse):
   - Pros:
     * Serves as a single source of truth and an explicit contract before writing code.
     * Frontend and backend teams can work in parallel by mocking the contract.
     * Prevents accidental breaking changes to external consumers.
     * Client SDKs and documentation can be automatically generated.
   - Cons:
     * Tooling friction: build-time plugins, generated ZIPs/folders, and potential compilation quirks.
     * Less flexibility: you often have to write boilerplate mapping layers between generated API DTOs and internal domain models.

2. Code-First (Direct implementation - like Product and Store):
   - Pros:
     * Faster initial development speed and simpler build pipeline (no generator plugins).
     * Full control over Java code, annotations, validation constraints, and refactoring tools.
     * You can still generate the OpenAPI spec automatically using Quarkus SmallRye OpenAPI annotations.
   - Cons:
     * Higher risk of accidental breaking changes if an engineer edits a field without consumer alignment.
     * Frontend/consumer teams must wait for backend code or manually written documentation.

My Choice:
For external, public, or multi-team APIs, I prefer Contract-First (OpenAPI-first) because the contract stability and parallel frontend development outweigh the tooling overhead.
However, for internal microservices owned by a single agile team, I lean towards Code-First with automated OpenAPI doc generation (Quarkus SmallRye OpenAPI). It gives you the speed and cleanliness of pure Java while still publishing an accurate Swagger/OpenAPI spec for consumers.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
To get the highest return on investment under time constraints, I follow a pragmatic testing pyramid:

1. What I would prioritize:
   - High Priority (70%): Pure Domain Unit Tests .
     * Why: They run in milliseconds, don't require Docker or starting Quarkus, and thoroughly test every single business validation rule (stock vs. capacity, max warehouses per location, the 3 fulfillment constraints). This is where our core business risk lives.
   - Medium Priority (25%): Targeted Integration  Tests .
     * Focus on database persistence, custom JPQL queries in repositories, and transaction boundary behaviors (specifically verifying that StoreService commits to the database before the legacy gateway is called).
   - Low Priority (5%): End-to-End / Happy-Path  Tests.
     * Just enough to verify HTTP routing, JSON serialization, and that the container boots cleanly.

2. How to ensure test effectiveness over time:
   - CI Quality Gates: Enforce automated test runs on every pull request and track branch coverage 
   - Test Behavior, Not Implementation: Write tests against the public use case and repository ports rather than private methods, so code can be refactored without breaking test suites.
   - Fast Feedback Loop: Keep the fast unit test suite completely decoupled from heavy external containers so developers actually run them locally before pushing code.
```