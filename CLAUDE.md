# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Full build (skip tests)
MAVEN_OPTS="-Xmx1024m" NODE_OPTIONS="--max_old_space_size=4096" \
  mvn clean install -T6 -DskipTests

# Full build, skip packaging artifacts (faster for dev/test cycles)
mvn clean install -T6 -DskipTests -Dpkg.skip=true

# Build a specific module and its dependencies
mvn clean install -pl application --also-make -DskipTests

# Run tests for a specific module
mvn test -pl dao
mvn test -pl application -Dtest='org.thingsboard.server.controller.**'

# Run a single test class
mvn test -pl application -Dtest=SomeTestClass

# License header check/format
mvn license:check
mvn license:format
```

## Test Environment Variables

```bash
export MAVEN_OPTS="-Xmx1024m"
export NODE_OPTIONS="--max_old_space_size=4096"
export SUREFIRE_JAVA_OPTS="-Xmx1200m -Xss256k -XX:+ExitOnOutOfMemoryError"
```

See `TEST_FAST.md` for parallel test execution patterns and `pkg.skip.*` flag reference.

## Module Architecture

ThingsBoard is a multi-module Maven project (Java 17, Spring Boot 3.x) organized as:

| Module | Purpose |
|--------|---------|
| `common/` | Shared libraries: `data` (domain model), `dao-api`, `transport`, `actor`, `cache`, `message`, `queue`, `proto`, `util`, `cluster-api`, `discovery-api`, `edge-api`, `edqs`, `version-control`, `script`, `stats` |
| `dao/` | Data Access Object layer — JPA (PostgreSQL) and Cassandra implementations |
| `rule-engine/` | Rule engine API and built-in rule node components |
| `transport/` | IoT protocol adapters: `mqtt`, `http`, `coap`, `lwm2m`, `snmp` |
| `application/` | Main Spring Boot application — REST API controllers, actors, services, install/upgrade logic. Entry point: `ThingsboardServerApplication` |
| `edqs/` | Entity Data Query Service — high-performance entity/attribute query microservice |
| `netty-mqtt/` | Netty-based MQTT codec library |
| `ui-ngx/` | Angular frontend (built via frontend-maven-plugin) |
| `tools/` | Utility tools (Cassandra, etc.) |
| `rest-client/` | Java REST client library |
| `monitoring/` | Monitoring/metrics module |
| `msa/` | Microservice assembly: `tb-node`, `tb`, `transport/*`, `js-executor`, `web-ui`, `edqs`, `vc-executor`, docker images |
| `packaging/java/` | Assembly filters and install scripts for .deb/.rpm/.zip packaging |

## Packaging Flags

The root `pom.xml` defines four skip flags for packaging artifacts:

- `-Dpkg.skip=true` — skip all packaging at once (bootjar + deb + rpm + zip)
- `-Dpkg.skip.bootjar=true` — skip fat boot jar (`*-boot.jar`); also prevents .deb build
- `-Dpkg.skip.deb=true` — skip jdeb .deb build and Maven reactor attachment
- `-Dpkg.skip.rpm=true` — skip RPM build (safe; no downstream dependency)
- `-Dpkg.skip.zip=true` — skip Windows ZIP (safe; no downstream dependency)

MSA docker modules copy .deb files directly from upstream `target/` directories (not `.m2`).

## Key Architectural Patterns

- **Actor model**: `common/actor` + application actors for device sessions, rules, tenants
- **Transport abstraction**: Each protocol in `transport/` implements `SessionMsgListener` / `TransportService` interfaces from `common/transport`
- **Queue abstraction**: `common/queue` provides Kafka/in-memory message bus used between microservices
- **DAO layer**: `common/dao-api` defines interfaces; `dao/` provides SQL (JPA) and NoSQL (Cassandra) implementations; tests in `dao/` use Testcontainers
- **Rule engine**: Rule chains composed of rule nodes (components in `rule-engine-components`); executed via actor messages

## Testcontainers Docker Compatibility

If tests fail due to Docker API version mismatch, add to Docker daemon config and restart:
```json
{ "min-api-version": "1.32" }
```
On Mac, edit via Docker Desktop UI. If testcontainers can't find Docker:
```bash
rm ~/.testcontainers.properties
```
