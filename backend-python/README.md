# ThingsBoard IoT Platform - Python Backend

Python/FastAPI conversion of the ThingsBoard IoT platform core backend.

## Tech Stack

- **Framework**: FastAPI (async, high-performance)
- **Database**: PostgreSQL (main), Cassandra (timeseries), Redis (cache)
- **Messaging**: Apache Kafka
- **Authentication**: JWT tokens
- **ORM**: SQLAlchemy 2.0 (async)

## Project Structure

```
backend-python/
├── app/
│   ├── api/            # API endpoints
│   │   └── v1/
│   │       └── endpoints/
│   ├── core/           # Core functionality (config, security, logging)
│   ├── models/         # Database models
│   ├── schemas/        # Pydantic schemas
│   ├── services/       # Business logic services
│   ├── db/             # Database session management
│   ├── transport/      # IoT protocol handlers (MQTT, CoAP, etc.)
│   └── main.py         # FastAPI application
├── tests/              # Tests
├── alembic/            # Database migrations
├── requirements.txt    # Python dependencies
├── Dockerfile
└── docker-compose.yml
```

## Quick Start

### Using Docker Compose (Recommended)

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f thingsboard-api

# Stop services
docker-compose down
```

The API will be available at:
- REST API: http://localhost:8080
- API Docs: http://localhost:8080/swagger-ui
- MQTT: tcp://localhost:1883
- CoAP: udp://localhost:5683

### Manual Setup

1. Install dependencies:
```bash
pip install -r requirements.txt
```

2. Set up environment:
```bash
cp .env.example .env
# Edit .env with your configuration
```

3. Start PostgreSQL, Redis, and Kafka

4. Run database migrations:
```bash
alembic upgrade head
```

5. Start the server:
```bash
uvicorn app.main:app --reload
```

## API Endpoints

### Authentication
- `POST /api/auth/login` - Login
- `POST /api/auth/token/refresh` - Refresh token
- `GET /api/auth/user` - Get current user

### Tenants
- `POST /api/tenants` - Create tenant
- `GET /api/tenants/{id}` - Get tenant
- `PUT /api/tenants/{id}` - Update tenant
- `DELETE /api/tenants/{id}` - Delete tenant

### Customers
- `POST /api/customers` - Create customer
- `GET /api/customers/{id}` - Get customer
- `PUT /api/customers/{id}` - Update customer
- `DELETE /api/customers/{id}` - Delete customer

### Devices
- `POST /api/devices` - Create device
- `GET /api/devices/{id}` - Get device
- `PUT /api/devices/{id}` - Update device
- `DELETE /api/devices/{id}` - Delete device
- `GET /api/devices/{id}/credentials` - Get device credentials

### Telemetry (HIGH PRIORITY)
- `POST /api/telemetry/{entityType}/{entityId}/timeseries/{scope}` - Save telemetry
- `GET /api/telemetry/{entityType}/{entityId}/values/timeseries` - Get latest telemetry
- `GET /api/telemetry/{entityType}/{entityId}/keys/timeseries` - Get telemetry keys
- `POST /api/telemetry/{entityType}/{entityId}/attributes/{scope}` - Save attributes
- `GET /api/telemetry/{entityType}/{entityId}/attributes` - Get attributes

## Development

### Running Tests
```bash
pytest
```

### Code Formatting
```bash
black app/
isort app/
```

### Type Checking
```bash
mypy app/
```

## Migration Status

### ✅ Completed
- Phase 1: Backend infrastructure setup
- Core models: Tenant, Customer, User, Device, DeviceProfile
- Authentication & JWT system
- Multi-tenancy support
- Device management APIs
- Telemetry APIs (basic)

### 🔄 In Progress
- Transport layer (MQTT, CoAP, HTTP)
- Rule engine core
- Cassandra integration for timeseries

### ⏳ Pending
- Gateway support
- Advanced rule engine nodes
- Alarms system
- Dashboards & widgets
- Entity relations
- Audit logs
- Edge support

## Configuration

Key configuration options in `.env`:

- `ENVIRONMENT` - development/production
- `SECRET_KEY` - JWT signing key
- `POSTGRES_*` - PostgreSQL connection
- `REDIS_*` - Redis connection
- `KAFKA_*` - Kafka configuration
- `MQTT_ENABLED` - Enable MQTT transport
- `COAP_ENABLED` - Enable CoAP transport

## License

Apache License 2.0
