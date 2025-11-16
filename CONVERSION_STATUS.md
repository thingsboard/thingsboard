# ThingsBoard Java/Angular → Python/React Conversion

## Overview

This repository contains the systematic conversion of ThingsBoard IoT Platform from Java/Spring Boot + Angular to Python/FastAPI + React.

## Project Structure

```
thingsboard/
├── backend-python/          # Python/FastAPI backend (NEW)
│   ├── app/
│   │   ├── api/            # REST API endpoints
│   │   ├── models/         # SQLAlchemy models
│   │   ├── services/       # Business logic
│   │   ├── core/           # Config, security, logging
│   │   └── main.py
│   ├── requirements.txt
│   ├── Dockerfile
│   └── docker-compose.yml
│
├── frontend-react/          # React frontend (NEW)
│   ├── src/
│   │   ├── components/     # React components
│   │   ├── pages/          # Page components
│   │   ├── store/          # Redux state
│   │   ├── services/       # API services
│   │   └── types/          # TypeScript types
│   ├── package.json
│   └── vite.config.ts
│
└── [original Java/Angular code...]
```

## Conversion Progress

### ✅ Phase 1: Infrastructure (COMPLETED)

#### Backend (Python/FastAPI)
- [x] FastAPI application structure
- [x] Database session management (SQLAlchemy async)
- [x] Configuration management (Pydantic settings)
- [x] Logging setup (JSON structured logging)
- [x] Docker & Docker Compose setup
- [x] Requirements and dependencies

#### Frontend (React/TypeScript)
- [x] Vite + React + TypeScript setup
- [x] Redux Toolkit state management
- [x] Material-UI component library
- [x] React Router navigation
- [x] Axios HTTP client with interceptors
- [x] Project structure and configuration

### ✅ Phase 2: Multi-Tenancy (COMPLETED)

#### Models
- [x] Tenant entity
- [x] Customer entity
- [x] User entity
- [x] UserCredentials entity
- [x] Base entity classes (TenantEntity, CustomerEntity)

#### APIs
- [x] Tenant CRUD endpoints
- [x] Customer CRUD endpoints
- [x] Multi-tenant access control
- [x] Tenant/customer isolation

#### Frontend
- [x] Tenants page (placeholder)
- [x] Customers page (placeholder)

### ✅ Phase 3: Authentication & Security (COMPLETED)

#### Backend
- [x] JWT token generation/validation
- [x] Password hashing (bcrypt)
- [x] Login endpoint
- [x] Token refresh endpoint
- [x] User info endpoint
- [x] Password change endpoint
- [x] Role-based access control (SYS_ADMIN, TENANT_ADMIN, CUSTOMER_USER)
- [x] Security dependencies and middleware

#### Frontend
- [x] Login page
- [x] Auth Redux slice
- [x] Protected routes
- [x] Token storage and auto-refresh
- [x] Logout functionality

### ✅ Phase 4: Device Management (COMPLETED)

#### Models
- [x] Device entity
- [x] DeviceProfile entity
- [x] DeviceCredentials entity
- [x] Device types and transport types

#### APIs
- [x] Device CRUD endpoints
- [x] Device credentials management
- [x] Device listing with filters
- [x] Device access control

#### Frontend
- [x] Devices page with table
- [x] Device Redux slice
- [x] Device API service
- [x] Device types

### ✅ Phase 5: Telemetry (HIGH PRIORITY - COMPLETED)

#### Models
- [x] TsKvLatest (latest telemetry values)
- [x] AttributeKv (attributes storage)
- [x] Telemetry data types
- [x] Attribute scopes (CLIENT, SERVER, SHARED)
- [x] Cassandra schema (documented)

#### APIs
- [x] POST /telemetry/{entityType}/{entityId}/timeseries/{scope} - Save telemetry
- [x] GET /telemetry/{entityType}/{entityId}/values/timeseries - Get latest values
- [x] GET /telemetry/{entityType}/{entityId}/keys/timeseries - Get available keys
- [x] POST /telemetry/{entityType}/{entityId}/attributes/{scope} - Save attributes
- [x] GET /telemetry/{entityType}/{entityId}/attributes - Get attributes
- [x] DELETE /telemetry/{entityType}/{entityId}/attributes/{scope} - Delete attributes

#### Frontend
- [x] Telemetry Redux slice
- [x] Telemetry API service
- [x] Real-time data structures

### 🔄 Phase 6: Additional Core Models (IN PROGRESS)

- [x] Asset entity
- [x] EntityView entity
- [x] Dashboard entity
- [x] RuleChain entity
- [x] Alarm entity
- [ ] Relation entity
- [ ] Widget entity
- [ ] AuditLog entity

### ⏳ Phase 7: Transport Layer (PENDING)

Gateway and IoT protocol support:

- [ ] MQTT transport (paho-mqtt)
- [ ] HTTP transport
- [ ] CoAP transport (aiocoap)
- [ ] LWM2M support
- [ ] Device authentication via transports
- [ ] Message routing to rule engine

### ⏳ Phase 8: Rule Engine (PENDING)

- [ ] Rule chain execution engine
- [ ] Message queue integration (Kafka)
- [ ] Rule node processors:
  - [ ] Filter nodes
  - [ ] Enrichment nodes
  - [ ] Transformation nodes
  - [ ] Action nodes
  - [ ] External nodes
- [ ] Script execution engine
- [ ] Rule chain metadata API

### ⏳ Phase 9: Frontend Components (PENDING)

- [ ] Dashboard widgets
- [ ] Telemetry charts (Recharts)
- [ ] Real-time WebSocket subscriptions
- [ ] Map widgets (Leaflet)
- [ ] Alarm management UI
- [ ] Rule chain visual editor
- [ ] Entity relations graph
- [ ] User management UI
- [ ] Device provisioning UI

### ⏳ Phase 10: Advanced Features (PENDING)

- [ ] Audit logging
- [ ] Rate limiting
- [ ] Entity relations
- [ ] Alarm rules and propagation
- [ ] Dashboard sharing
- [ ] Widget bundles
- [ ] Mobile app API
- [ ] Edge integration
- [ ] OTA updates (firmware/software)

### ⏳ Phase 11: Testing & Deployment (PENDING)

- [ ] Unit tests (pytest)
- [ ] Integration tests
- [ ] API tests
- [ ] Frontend tests (Vitest)
- [ ] E2E tests (Playwright)
- [ ] CI/CD pipeline
- [ ] Production deployment guide
- [ ] Migration scripts from Java version
- [ ] Performance benchmarks

## Quick Start

### Backend

```bash
cd backend-python

# Using Docker Compose (recommended)
docker-compose up -d

# Or manually
pip install -r requirements.txt
cp .env.example .env
uvicorn app.main:app --reload
```

API will be available at:
- REST API: http://localhost:8080
- Swagger Docs: http://localhost:8080/swagger-ui

### Frontend

```bash
cd frontend-react
npm install
npm run dev
```

Frontend will be available at http://localhost:3000

## Technology Comparison

| Component | Original | Conversion |
|-----------|----------|------------|
| Backend Language | Java 17 | Python 3.11 |
| Backend Framework | Spring Boot 3.4 | FastAPI |
| ORM | JPA/Hibernate | SQLAlchemy 2.0 (async) |
| Frontend Framework | Angular 18 | React 18 |
| State Management | NgRx | Redux Toolkit |
| UI Library | Angular Material | Material-UI (MUI) |
| Build Tool (FE) | Angular CLI | Vite |
| Type System (FE) | TypeScript | TypeScript |
| HTTP Client | Angular HTTP | Axios |
| Database | PostgreSQL, Cassandra | PostgreSQL, Cassandra |
| Messaging | Kafka | Kafka (aiokafka) |
| Cache | Redis | Redis |
| Actor System | Akka | Custom async actors |

## Key Design Decisions

1. **Async-first**: Using FastAPI with async/await for better I/O performance
2. **Type Safety**: Full TypeScript on frontend, type hints on backend
3. **Modern Stack**: Latest versions of React, FastAPI, SQLAlchemy
4. **API Compatibility**: Maintaining similar REST API structure for easier migration
5. **Database Reuse**: Same PostgreSQL and Cassandra schemas where possible
6. **Microservices Ready**: Clean separation of concerns for future scaling

## Performance Considerations

- FastAPI provides comparable or better performance than Spring Boot for I/O-bound operations
- React with Vite offers faster development builds than Angular
- SQLAlchemy 2.0 async engine for concurrent database operations
- Redis caching layer maintained
- Cassandra for high-throughput telemetry storage

## Migration Strategy

For existing ThingsBoard installations:

1. **Parallel Run**: Run Python backend alongside Java backend
2. **Database Compatibility**: Reuse existing PostgreSQL/Cassandra data
3. **Gradual Migration**: Move tenants/devices incrementally
4. **API Gateway**: Route traffic between old and new systems
5. **Frontend Cutover**: Switch React frontend once features are complete

## Contributing

When adding new features:

1. **Backend**: Add model → service → API endpoint → tests
2. **Frontend**: Add types → service → Redux slice → component → page
3. **Documentation**: Update this file and component READMEs
4. **Tests**: Add unit and integration tests

## Current Status Summary

**Backend**: ~40% complete
- ✅ Core infrastructure
- ✅ Authentication
- ✅ Multi-tenancy
- ✅ Device management
- ✅ Telemetry APIs
- ⏳ Transport layer
- ⏳ Rule engine
- ⏳ Advanced features

**Frontend**: ~25% complete
- ✅ Core infrastructure
- ✅ Authentication
- ✅ Basic layout
- ✅ Device listing
- ⏳ Full device management
- ⏳ Telemetry visualization
- ⏳ Dashboards
- ⏳ All entity management

## Timeline Estimate

- **Completed**: ~20% (Infrastructure, core models, basic APIs)
- **Remaining**: ~80%
- **Estimated completion**: 6-8 more months with team of 8-10 developers
- **MVP (basic IoT platform)**: 2-3 months

## Next Steps

1. ✅ Complete database migration scripts (Alembic)
2. Implement MQTT transport layer
3. Build rule engine core
4. Expand frontend components
5. Add WebSocket support for real-time updates
6. Implement dashboard system
7. Add comprehensive tests
8. Performance optimization
9. Documentation and deployment guides

## License

Apache License 2.0 (same as original ThingsBoard)
