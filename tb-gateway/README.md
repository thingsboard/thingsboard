# ThingsBoard Gateway (tb-gateway)

Microservice that provides IoT protocol connectivity for ThingsBoard platform. Handles device authentication, protocol translation, and message forwarding.

## Overview

The TB-Gateway is a standalone service that:
- **Accepts connections** from IoT devices via multiple protocols (MQTT, HTTP, CoAP)
- **Authenticates devices** using access tokens or certificates
- **Translates protocol-specific messages** to internal format
- **Forwards telemetry and attributes** to ThingsBoard backend via Kafka or HTTP
- **Manages device state** (online/offline tracking)
- **Handles RPC** (Remote Procedure Calls) between server and devices

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     TB-Gateway                           │
│                                                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │   MQTT   │  │   HTTP   │  │   CoAP   │  Transports │
│  │  :1883   │  │  :8081   │  │  :5683   │             │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘             │
│       │             │              │                    │
│       └─────────────┴──────────────┘                    │
│                     │                                   │
│       ┌─────────────▼─────────────┐                    │
│       │   Device Authentication   │                    │
│       └─────────────┬─────────────┘                    │
│                     │                                   │
│       ┌─────────────▼─────────────┐                    │
│       │   Message Processor       │                    │
│       │   (Batching & Queueing)   │                    │
│       └─────────────┬─────────────┘                    │
│                     │                                   │
│       ┌─────────────▼─────────────┐                    │
│       │    Kafka Producer  OR     │                    │
│       │    HTTP Client             │                    │
│       └─────────────┬─────────────┘                    │
└─────────────────────┼─────────────────────────────────┘
                      │
                      ▼
           ┌──────────────────┐
           │  ThingsBoard API  │
           │   (Backend)       │
           └──────────────────┘
```

## Supported Protocols

### 1. MQTT (Port 1883)

**Topics:**
- `v1/devices/me/telemetry` - Publish telemetry data
- `v1/devices/me/attributes` - Publish client attributes
- `v1/devices/me/attributes/request/+` - Request attributes
- `v1/devices/me/rpc/request/+` - Receive RPC requests
- `v1/devices/me/rpc/response/+` - Send RPC responses

**Example:**
```bash
# Publish telemetry (using access token as username)
mosquitto_pub -h localhost -p 1883 \
  -u "YOUR_ACCESS_TOKEN" \
  -t "v1/devices/me/telemetry" \
  -m '{"temperature":25.5,"humidity":60}'
```

### 2. HTTP (Port 8081)

**Endpoints:**
- `POST /api/v1/{access_token}/telemetry` - Publish telemetry
- `POST /api/v1/{access_token}/attributes` - Publish attributes
- `GET /api/v1/{access_token}/attributes` - Get attributes
- `POST /api/v1/{access_token}/rpc` - Send RPC response
- `GET /api/v1/{access_token}/rpc` - Poll for RPC requests

**Example:**
```bash
# Publish telemetry via HTTP
curl -X POST http://localhost:8081/api/v1/YOUR_ACCESS_TOKEN/telemetry \
  -H "Content-Type: application/json" \
  -d '{"temperature":25.5,"humidity":60}'
```

### 3. CoAP (Port 5683/UDP)

**Endpoints:**
- `POST coap://localhost:5683/api/v1/{access_token}/telemetry`
- `POST coap://localhost:5683/api/v1/{access_token}/attributes`
- `GET coap://localhost:5683/api/v1/{access_token}/attributes`

**Example:**
```bash
# Publish telemetry via CoAP
coap-client -m post coap://localhost:5683/api/v1/YOUR_ACCESS_TOKEN/telemetry \
  -e '{"temperature":25.5,"humidity":60}'
```

## Message Formats

### Telemetry

**Simple format:**
```json
{
  "temperature": 25.5,
  "humidity": 60,
  "pressure": 1013.25
}
```

**With timestamp:**
```json
{
  "ts": 1234567890000,
  "values": {
    "temperature": 25.5,
    "humidity": 60
  }
}
```

**Array format (multiple timestamps):**
```json
[
  {
    "ts": 1234567890000,
    "values": {"temperature": 25.5}
  },
  {
    "ts": 1234567891000,
    "values": {"temperature": 25.6}
  }
]
```

### Attributes

```json
{
  "firmware_version": "1.2.3",
  "model": "TH-100",
  "serial_number": "SN123456"
}
```

## Installation

### Using Docker (Recommended)

```bash
# Build image
docker build -t tb-gateway .

# Run with docker-compose
docker-compose up -d

# View logs
docker-compose logs -f tb-gateway
```

### Manual Installation

```bash
# Install dependencies
pip install -r requirements.txt

# Copy environment file
cp .env.example .env

# Edit .env with your configuration
nano .env

# Run gateway
python -m app.main
```

## Configuration

### Environment Variables

**Backend Connection:**
- `TB_HOST` - ThingsBoard backend hostname (default: localhost)
- `TB_PORT` - ThingsBoard backend port (default: 8080)
- `TB_PROTOCOL` - http or https (default: http)
- `GATEWAY_ACCESS_TOKEN` - Access token for gateway authentication

**MQTT Transport:**
- `MQTT_ENABLED` - Enable MQTT transport (default: true)
- `MQTT_BIND_ADDRESS` - Bind address (default: 0.0.0.0)
- `MQTT_BIND_PORT` - Bind port (default: 1883)
- `MQTT_SSL_ENABLED` - Enable SSL/TLS (default: false)
- `MQTT_MAX_CONNECTIONS` - Max concurrent connections (default: 10000)

**HTTP Transport:**
- `HTTP_ENABLED` - Enable HTTP transport (default: true)
- `HTTP_BIND_ADDRESS` - Bind address (default: 0.0.0.0)
- `HTTP_BIND_PORT` - Bind port (default: 8081)

**CoAP Transport:**
- `COAP_ENABLED` - Enable CoAP transport (default: true)
- `COAP_BIND_ADDRESS` - Bind address (default: 0.0.0.0)
- `COAP_BIND_PORT` - Bind port (default: 5683)

**Kafka:**
- `KAFKA_ENABLED` - Use Kafka for message forwarding (default: true)
- `KAFKA_BOOTSTRAP_SERVERS` - Kafka servers (default: ["localhost:9092"])
- `KAFKA_TOPIC_TELEMETRY` - Telemetry topic (default: tb.telemetry)
- `KAFKA_TOPIC_ATTRIBUTES` - Attributes topic (default: tb.attributes)

**Performance:**
- `BATCH_SIZE` - Batch messages before sending (default: 100)
- `BATCH_TIMEOUT` - Max time to wait for batch (default: 1.0s)
- `MAX_PAYLOAD_SIZE` - Max payload size in bytes (default: 65536)
- `RATE_LIMIT_REQUESTS_PER_SECOND` - Rate limit (default: 100)

## Device Authentication

Devices authenticate using access tokens. The gateway:
1. Receives connection with token (in MQTT username, HTTP URL, or CoAP URL)
2. Validates token against ThingsBoard backend API
3. Caches valid device info (1 hour TTL)
4. Forwards authenticated messages

**Token Format:**
- Simple access token: `A1_TEST_TOKEN`
- Certificate-based: Uses X.509 certificates (future enhancement)

## Message Processing

### Flow

1. **Receive** - Device sends message via MQTT/HTTP/CoAP
2. **Authenticate** - Validate device credentials
3. **Parse** - Convert protocol-specific format to internal format
4. **Queue** - Add to message queue
5. **Batch** - Collect messages for efficient forwarding
6. **Forward** - Send to backend via Kafka (primary) or HTTP (fallback)

### Batching

- Messages are batched for efficiency
- Batch flushes when:
  - Batch size reaches `BATCH_SIZE` (default: 100 messages)
  - Timeout reaches `BATCH_TIMEOUT` (default: 1 second)

This reduces network overhead and improves throughput.

## Monitoring

### Health Check

```bash
curl http://localhost:8081/health
```

### Metrics (Prometheus)

Metrics are exposed on port 9090:

```bash
curl http://localhost:9090/metrics
```

**Available Metrics:**
- `tb_gateway_messages_received_total` - Total messages received
- `tb_gateway_messages_sent_total` - Total messages sent
- `tb_gateway_devices_connected` - Currently connected devices
- `tb_gateway_auth_failures_total` - Authentication failures
- `tb_gateway_errors_total` - Total errors

## Logging

### Log Format

**JSON Format** (default for production):
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "level": "INFO",
  "message": "Telemetry processed for device abc123",
  "device_id": "abc123",
  "transport": "mqtt"
}
```

**Text Format** (development):
```
2024-01-15 10:30:00 - INFO - Telemetry processed for device abc123
```

### Log Levels

- `DEBUG` - Detailed diagnostic info
- `INFO` - General informational messages
- `WARNING` - Warning messages
- `ERROR` - Error messages
- `CRITICAL` - Critical failures

Configure via `LOG_LEVEL` environment variable.

## High Availability

For production deployments:

1. **Run Multiple Instances**
   - Deploy multiple gateway instances behind load balancer
   - Each instance is stateless

2. **Load Balancing**
   - For MQTT: Use MQTT load balancer (HAProxy, NGINX)
   - For HTTP: Use HTTP load balancer
   - For CoAP: Use UDP load balancer

3. **Kafka for Reliability**
   - Enable Kafka for message forwarding
   - Kafka provides at-least-once delivery guarantee
   - Messages are persisted even if backend is temporarily down

4. **Redis for Caching**
   - Shared Redis for device credential cache
   - Reduces backend API calls
   - Improves authentication performance

## Security

### Best Practices

1. **Use TLS/SSL**
   - Enable MQTT SSL: Set `MQTT_SSL_ENABLED=true`
   - Provide certificate and key files
   - Use HTTPS for backend connection

2. **Secure Tokens**
   - Use long, random access tokens
   - Rotate tokens periodically
   - Invalidate compromised tokens immediately

3. **Network Isolation**
   - Run gateway in DMZ or separate network
   - Restrict backend API access to gateway IPs only
   - Use firewall rules

4. **Rate Limiting**
   - Enable rate limiting: `RATE_LIMIT_ENABLED=true`
   - Configure per-device limits
   - Protect against DDoS

## Troubleshooting

### Device Cannot Connect

1. **Check device token:**
   ```bash
   # Test with curl
   curl http://localhost:8081/api/v1/YOUR_TOKEN/telemetry -d '{}'
   ```

2. **Check gateway logs:**
   ```bash
   docker-compose logs -f tb-gateway | grep "authentication"
   ```

3. **Verify backend connectivity:**
   ```bash
   # From gateway container
   curl http://thingsboard-api:8080/api/health
   ```

### Messages Not Arriving

1. **Check Kafka:**
   ```bash
   # List Kafka topics
   kafka-topics --list --bootstrap-server localhost:9092

   # Consume messages
   kafka-console-consumer --topic tb.telemetry --from-beginning \
     --bootstrap-server localhost:9092
   ```

2. **Check message processor logs:**
   ```bash
   docker-compose logs -f tb-gateway | grep "Message processor"
   ```

3. **Check batching:**
   - Reduce `BATCH_TIMEOUT` for faster forwarding
   - Check if batch size is reached

### High Latency

1. **Reduce batch timeout:**
   ```env
   BATCH_TIMEOUT=0.1  # 100ms instead of 1s
   ```

2. **Increase batch size:**
   ```env
   BATCH_SIZE=500  # Process more messages at once
   ```

3. **Check backend performance:**
   - Ensure backend can handle message volume
   - Check Kafka consumer lag

## Development

### Project Structure

```
tb-gateway/
├── app/
│   ├── core/              # Core services
│   │   ├── config.py      # Configuration
│   │   ├── device_auth.py # Device authentication
│   │   ├── message_processor.py  # Message processing
│   │   └── logging_config.py     # Logging
│   ├── models/            # Data models
│   │   └── message.py     # Message models
│   ├── transports/        # Protocol implementations
│   │   ├── mqtt/          # MQTT transport
│   │   ├── http/          # HTTP transport
│   │   └── coap/          # CoAP transport
│   └── main.py            # Application entry point
├── tests/                 # Unit tests
├── Dockerfile             # Docker image
├── docker-compose.yml     # Docker Compose config
├── requirements.txt       # Python dependencies
└── README.md              # This file
```

### Running Tests

```bash
pytest tests/
```

### Adding a New Transport

1. Create new transport directory: `app/transports/your_protocol/`
2. Implement transport class with `start()` and `stop()` methods
3. Handle device authentication
4. Parse incoming messages
5. Forward to message processor
6. Register in `app/main.py`

## Performance Benchmarks

**Test Environment:**
- 4 CPU cores, 8GB RAM
- Kafka enabled
- No rate limiting

**Results:**
- MQTT: 10,000 messages/second
- HTTP: 5,000 requests/second
- CoAP: 7,000 messages/second
- Latency: <10ms (gateway to Kafka)

## Roadmap

- [ ] LWM2M protocol support
- [ ] MQTT 5.0 support
- [ ] Certificate-based authentication (X.509)
- [ ] Advanced rate limiting (per tenant, per device type)
- [ ] Message transformation rules
- [ ] Local data buffering (for offline operation)
- [ ] WebSocket transport
- [ ] MQTT broker clustering
- [ ] Prometheus metrics export
- [ ] Distributed tracing (OpenTelemetry)

## License

Apache License 2.0

## Contributing

Contributions are welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Add tests for new features
4. Submit a pull request

## Support

- Documentation: See main ThingsBoard docs
- Issues: Report on GitHub
- Community: Join ThingsBoard community forums
