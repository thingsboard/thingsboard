"""
TB-Gateway Configuration
"""
from typing import List, Optional
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Gateway configuration"""

    # Service Info
    SERVICE_NAME: str = "tb-gateway"
    SERVICE_ID: str = "gateway-001"
    ENVIRONMENT: str = "development"

    # Backend API Connection
    TB_HOST: str = "localhost"
    TB_PORT: int = 8080
    TB_PROTOCOL: str = "http"  # http or https
    TB_API_URL: Optional[str] = None

    def get_api_url(self) -> str:
        if self.TB_API_URL:
            return self.TB_API_URL
        return f"{self.TB_PROTOCOL}://{self.TB_HOST}:{self.TB_PORT}/api"

    # Gateway Authentication (to backend)
    GATEWAY_ACCESS_TOKEN: str = "YOUR_GATEWAY_ACCESS_TOKEN"

    # MQTT Transport
    MQTT_ENABLED: bool = True
    MQTT_BIND_ADDRESS: str = "0.0.0.0"
    MQTT_BIND_PORT: int = 1883
    MQTT_SSL_ENABLED: bool = False
    MQTT_SSL_CERT: Optional[str] = None
    MQTT_SSL_KEY: Optional[str] = None
    MQTT_MAX_CONNECTIONS: int = 10000
    MQTT_TIMEOUT: int = 300  # seconds

    # HTTP Transport
    HTTP_ENABLED: bool = True
    HTTP_BIND_ADDRESS: str = "0.0.0.0"
    HTTP_BIND_PORT: int = 8081

    # CoAP Transport
    COAP_ENABLED: bool = True
    COAP_BIND_ADDRESS: str = "0.0.0.0"
    COAP_BIND_PORT: int = 5683
    COAP_TIMEOUT: int = 60

    # Message Queue (Kafka)
    KAFKA_ENABLED: bool = True
    KAFKA_BOOTSTRAP_SERVERS: List[str] = ["localhost:9092"]
    KAFKA_TOPIC_TELEMETRY: str = "tb.telemetry"
    KAFKA_TOPIC_ATTRIBUTES: str = "tb.attributes"
    KAFKA_TOPIC_EVENTS: str = "tb.events"

    # Redis (for device session cache)
    REDIS_ENABLED: bool = True
    REDIS_HOST: str = "localhost"
    REDIS_PORT: int = 6379
    REDIS_DB: int = 1  # Different from main backend
    REDIS_PASSWORD: Optional[str] = None

    # Database (optional - for local device credential cache)
    DATABASE_ENABLED: bool = False
    DATABASE_URL: Optional[str] = None

    # Rate Limiting
    RATE_LIMIT_ENABLED: bool = True
    RATE_LIMIT_REQUESTS_PER_SECOND: int = 100
    RATE_LIMIT_BURST: int = 200

    # Telemetry Processing
    BATCH_SIZE: int = 100  # Batch telemetry messages
    BATCH_TIMEOUT: float = 1.0  # seconds
    MAX_PAYLOAD_SIZE: int = 65536  # 64KB

    # Device State
    DEVICE_ACTIVITY_TIMEOUT: int = 300  # Mark device offline after 5 min
    DEVICE_STATE_CHECK_INTERVAL: int = 60  # Check device states every minute

    # Logging
    LOG_LEVEL: str = "INFO"
    LOG_FORMAT: str = "json"

    # Metrics & Monitoring
    METRICS_ENABLED: bool = True
    METRICS_PORT: int = 9090

    model_config = SettingsConfigDict(
        env_file=".env",
        case_sensitive=True,
        extra="allow"
    )


settings = Settings()
