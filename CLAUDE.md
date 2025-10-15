# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ThingsBoard is an open-source IoT platform for data collection, processing, visualization, and device management. It's a Spring Boot-based Java application with an Angular frontend, supporting multiple transport protocols (MQTT, HTTP, CoAP, LWM2M, SNMP) and designed for microservices deployment.

**Version**: 4.3.0-SNAPSHOT
**Java Version**: 17
**Spring Boot Version**: 3.4.8
**Main Class**: `org.thingsboard.server.ThingsboardServerApplication`

## Build and Development Commands

### Maven (Backend)

```bash
# Full build with tests
mvn clean install

# Build without tests (faster)
mvn clean install -DskipTests

# Build specific module
mvn clean install -pl application -am

# Build with parallel execution (2 threads)
mvn -T2 clean install -DskipTests

# Run tests for a specific module
mvn test -pl application

# Format license headers
mvn license:format

# Build proto files
./build_proto.sh
```

### Angular (Frontend - ui-ngx)

```bash
cd ui-ngx

# Install dependencies
npm install

# Start dev server (opens on http://0.0.0.0:4200)
npm start

# Production build
npm run build:prod

# Lint code
npm run lint

# Generate TypeScript types
npm run build:types

# Generate icon metadata
npm run build:icon-metadata
```

### Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ClassName

# Run tests in a specific module
mvn test -pl dao

# Integration tests (located in msa/black-box-tests)
cd msa/black-box-tests
mvn test
```

### Docker

```bash
# Build Docker images
./build.sh

# Build specific services
./build.sh msa/web-ui,msa/tb-node
```

## High-Level Architecture

### Modular Structure

ThingsBoard follows a **multi-module Maven architecture** with clear separation of concerns:

#### Core Modules

- **application**: Main Spring Boot application entry point, REST controllers, service implementations, and actors. Contains the monolith deployment mode.
- **common**: Shared infrastructure organized into sub-modules:
  - `data`: Core domain models, DTOs, and data structures
  - `dao-api`: Data Access Object interfaces
  - `transport`: Base transport abstractions
  - `transport/*`: Protocol-specific implementations (mqtt, http, coap, lwm2m, snmp)
  - `message`: Internal messaging protocol definitions
  - `queue`: Message queue abstractions (Kafka, RabbitMQ, AWS SQS, Google Pub/Sub)
  - `actor`: Actor-based concurrency framework
  - `cache`: Caching abstractions (Redis, Caffeine)
  - `cluster-api`: Cluster coordination interfaces
  - `edge-api`: Edge computing functionality
  - `proto`: Protobuf definitions
  - `script`: JavaScript/rule evaluation (Nashorn, remote JS executor)
  - `stats`: Statistics and metrics
  - `util`: Common utilities
  - `version-control`: Git-based version control for entities

- **dao**: Database access layer implementations supporting:
  - PostgreSQL (primary)
  - Cassandra (time-series data)
  - TimescaleDB (time-series alternative)
  - SQL migration scripts in `src/main/resources`

- **rule-engine**: Event processing and rule chain execution
  - `rule-engine-api`: Core rule engine interfaces
  - `rule-engine-components`: Rule node implementations (transformation, enrichment, action, etc.)

- **transport**: Protocol-specific transport servers (standalone microservice modes)
  - Each can run as separate microservice or embedded in monolith

- **ui-ngx**: Angular 18 frontend application
  - Material Design components
  - Real-time dashboards with websockets
  - Widget system for data visualization
  - SCADA support

- **msa**: Microservices architecture components
  - `tb-node`: Core microservice
  - `web-ui`: UI microservice
  - `js-executor`: JavaScript execution microservice
  - `black-box-tests`: Integration test suite
  - Transport microservices (mqtt, http, coap, lwm2m, snmp)

#### Supporting Modules

- **rest-client**: Java REST API client library
- **netty-mqtt**: Custom MQTT protocol implementation
- **tools**: Development utilities and helper scripts
- **monitoring**: Monitoring and observability

### Architectural Patterns

1. **Actor Model**: Concurrent processing using custom actor framework in `common/actor`
2. **Event-Driven**: Rule engine processes events through configurable rule chains
3. **Multi-Tenancy**: Built-in tenant and customer hierarchy
4. **Message Queue Integration**: Kafka/RabbitMQ for scalability and reliability
5. **gRPC Communication**: Inter-service communication in microservices mode
6. **Caching Strategy**: Redis for distributed cache, Caffeine for local cache

### Key Abstractions

- **TbActorSystem**: Custom actor-based concurrency model for handling high throughput
- **TbRuleEngine**: Rule chain execution engine processing telemetry and events
- **TransportService**: Protocol-agnostic device communication layer
- **DeviceActorSystem**: Actor hierarchy for device state management
- **QueueService**: Message queue abstraction supporting multiple providers

### Data Flow

1. Device connects via transport layer (MQTT/HTTP/CoAP/LWM2M/SNMP)
2. Transport validates device credentials and publishes messages to queue
3. Core service consumes messages and forwards to rule engine
4. Rule engine executes rule chains (transformation, validation, enrichment)
5. Processed data stored in DAO layer (PostgreSQL/Cassandra)
6. WebSocket updates push real-time data to UI
7. REST API serves dashboards and entity management

### Component Annotations

ThingsBoard uses custom component markers for microservice deployment:
- `@TbCoreComponent`: Core service components
- `@TbRuleEngineComponent`: Rule engine components
- `@TbTransportComponent`: Transport layer components

When adding new services, ensure they are properly annotated for correct deployment in microservices mode.

## Database Considerations

- **SQL migrations**: Located in `dao/src/main/resources/sql/schema-*.sql`
- **Cassandra migrations**: Located in `dao/src/main/resources/*.cql`
- **TimescaleDB**: Optional alternative to Cassandra for time-series
- **Version upgrades**: Check `dao/src/main/java/org/thingsboard/server/dao/sql/install` for schema update scripts

## Testing Strategy

- **Unit tests**: Co-located with source in `src/test/java`
- **Integration tests**: Use Testcontainers for database/queue dependencies
- **Black-box tests**: Selenium-based UI tests in `msa/black-box-tests`
- **Test profiles**: PostgreSQL (default), Cassandra (optional)
- Tests excluded from main build: `**/nosql/*Test.java` (Cassandra tests)

## Important Development Notes

### Kafka Client Version Synchronization

When updating `kafka.version` in root `pom.xml`, synchronize the `NetworkReceive` class in the application module. This custom implementation addresses [KAFKA-4090](https://issues.apache.org/jira/browse/KAFKA-4090).

### Frontend Development

- Hot reload available via `npm start` in `ui-ngx/`
- Backend proxy configured in `angular.json` (typically localhost:8080)
- Widget development requires building with `build:prod` to see changes in backend

### Protobuf Compilation

If modifying `.proto` files, run `./build_proto.sh` to regenerate Java classes.

### License Headers

All source files must have Apache 2.0 license headers. Run `mvn license:format` to add/update headers automatically.

### Pull Request Requirements

When creating PRs:
- Add unit/integration tests for backend changes
- Update `RestClient.java` if adding new REST endpoints
- Create Python REST client issue if new REST API added
- Document new YML properties with descriptions
- Ensure backward compatibility or provide upgrade scripts
- Check dependency tree for conflicts when adding dependencies
- Follow the PR template checklist in `.github/pull_request_template.md`

## Configuration

Main configuration file: `application/src/main/resources/thingsboard.yml`

Key configuration areas:
- Database connection (PostgreSQL/Cassandra)
- Queue provider (Kafka/RabbitMQ/AWS SQS/Google Pub/Sub)
- Cache configuration (Redis/Caffeine)
- Transport protocols enablement
- Security and JWT settings
- Cluster coordination (Zookeeper)

## Documentation Generation

A documentation generation system is available in `tools/gen_docs.sh`:

```bash
# Generate architecture and module documentation
./tools/gen_docs.sh
```

This uses prompts in `docs/prompts/` to generate Markdown documentation automatically.

## Debugging Tips

- Main application class for debugging: `ThingsboardServerApplication`
- Actor system logging: Enable debug for `org.thingsboard.server.actors`
- Rule engine debugging: Enable debug for `org.thingsboard.server.service.ruleengine`
- Transport debugging: Enable debug for specific transport package (e.g., `org.thingsboard.server.transport.mqtt`)
- Database query logging: Enable debug for `org.thingsboard.server.dao`

## Code Ownership

Check `CODEOWNERS` file for module ownership when making changes to specific areas.
