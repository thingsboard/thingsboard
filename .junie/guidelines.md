### Purpose
Guidelines for Junie when working on this ThingsBoard repository. Follow these rules in addition to global Junie policies. Keep changes minimal, consistent, and always validated by tests.

### Repository Overview
- ThingsBoard: open-source IoT platform for data collection, processing, visualization, and device management.
- Monorepo managed with Maven (multi-module), Java 17, Spring Boot 3.4.x.

### Tech Stack
- Backend: Java 17 (Spring Boot), Maven
- Frontend: Angular (`ui-ngx`)
- Databases: PostgreSQL; Cassandra for telemetry
- Messaging: Kafka, RabbitMQ, Redis, MQTT
- Containerization/Orchestration: Docker, Kubernetes

### Key Modules (root-level folders)
- `application` — Main Spring Boot services, REST controllers, integration tests
- `common` — Shared utils, data models, scripting APIs
- `dao` — Data access layer and validators
- `rule-engine` — Rule engine implementation
- `msa` — Microservices-related components
- `transport` — MQTT/HTTP/CoAP and other transports
- `ui-ngx` — Angular frontend
- `monitoring` — Health checks and monitoring helpers

### Build & Run Basics
- Java: 17
- Maven wrapper: prefer `mvn` installed locally
- Common commands:
  - Full build without tests: `mvn -T4 -DskipTests clean install`
  - Full build with tests (takes 2 hours): `mvn clean verify`
  - Build/verify a specific module and its deps: `mvn -pl <module> -am clean install`
  - Run a single test class: `mvn -Dtest=FullClassName test`
  - Run a single test method: `mvn -Dtest=FullClassName#methodName test`
- Scripts in repo:
  - `build.sh` — project packaging helper
  - `build_proto.sh` — regenerate protobuf classes when needed. **Note: any changes of `*.proto` files require to rebuild all protobufs using this script.**
- See `TEST_FAST.md` for fast testing tips if applicable.

### Coding Guidelines
- Follow existing Java style and project conventions.
- Use Lombok where present (`@Data`, `@Builder`, `@Slf4j`, etc.).
- Keep changes localized; mirror surrounding idioms and formatting.
- Naming: CamelCase for classes/methods; meaningful names consistent with existing code.
- Logging: use `@Slf4j` logger; for temporary diagnostics use prefix `[DEBUG_LOG]` and remove before final submit unless explicitly requested to keep.
- Avoid introducing new dependencies unless approved by the issue scope.

### Testing Guidelines
- Frameworks: JUnit (Jupiter) and Mockito (see `pom.xml`, Surefire 3.5.x).
- Test placement: `src/test/java` under the same module/package as the code under test.
- **Important: NoSQL Tests and TestSuites**:
  - Some tests (especially in `application` module under `**/nosql/**`) are excluded from regular Maven execution because they require a Cassandra environment managed by a `TestSuite`.
  - These tests will fail if run individually (e.g., via `-Dtest=SpecificNoSqlTest`).
  - To run them, use the corresponding `TestSuite` class (e.g., `mvn -pl application -Dtest=TransportNoSqlTestSuite test`).
  - See `application/pom.xml` surefire configuration and `TEST_FAST.md` for more details.
- For bugs: create a failing reproduction test first; confirm it fails; then implement the fix; confirm it passes.
- Prefer targeted module tests over whole-repo when possible.
- Integration tests may require Docker services (PostgreSQL, Cassandra, Kafka, etc.). Only run/add them if the issue requires it.
- Do not weaken, skip, or disable existing tests. Do not add `@Disabled` without explicit approval in the issue context.

### Working With Modules
- `application` tests often cover REST/controllers and end-to-end flows.
  - **Seed Data & Resources**: Additional resource files, including demo data, dashboards, certificates, and upgrade scripts, are located under `application/src/main/data`.
- `dao` contains validators and persistence logic — add focused unit tests here for validation bugs.
  - **Database Schemas**: All database schema files (SQL for PostgreSQL/TimescaleDB and CQL for Cassandra) are located under `dao/src/main/resources/sql` and `dao/src/main/resources/cassandra` respectively.
- `rule-engine` changes may require covering both positive/negative rule paths.
- `transport` changes should include protocol-specific tests when feasible.

### Security & Compliance
- Follow existing patterns in `security.md` and related modules.
- Keep configs and secrets out of source. Do not hardcode credentials.

### Documentation
- Update `README.md`, module READMEs, or other `.md` files if behavior, setup, or user-facing flows change.
- Public APIs/classes: add Javadoc; keep it concise and aligned with existing style.

### Definition of Done (checklist)
- Code compiles for affected modules and their dependents.
- All existing and newly added tests are green.
- Reproduction test (for bugs) demonstrates the fix (fails before, passes after).
- No stray `[DEBUG_LOG]` left unless explicitly required.
- No API/behavior changes without updating docs and tests accordingly.

### Common Recipes
- Run tests for a path quickly:
  - Module: `mvn -pl application -am -Dtest=* test`
  - One test: `mvn -Dtest=org.thingsboard.server.controller.AssetControllerTest test`
  - One test method: `mvn -Dtest=org.thingsboard.server.controller.AssetControllerTest#testName test`
- Generate protobufs when proto changes: `./build_proto.sh`

### Communication & Process
- Apply Latest‑First Principle for interpreting issue updates.
- If any step of the user’s plan is unclear or contradicts repo reality, ask for clarification before making broad changes.
- Prefer minimal diffs; avoid broad refactors unless required by the issue.
