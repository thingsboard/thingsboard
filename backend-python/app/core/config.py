"""
Application configuration management
"""
from typing import List, Optional
from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import PostgresDsn, validator


class Settings(BaseSettings):
    """Application settings"""

    # Environment
    ENVIRONMENT: str = "development"
    DEBUG: bool = True

    # API Settings
    API_V1_PREFIX: str = "/api/v1"
    PROJECT_NAME: str = "ThingsBoard IoT Platform"
    VERSION: str = "4.0.0-PYTHON"

    # Security
    SECRET_KEY: str = "change-this-secret-key-in-production"
    JWT_ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60
    REFRESH_TOKEN_EXPIRE_DAYS: int = 30

    # CORS
    CORS_ORIGINS: List[str] = ["http://localhost:4200", "http://localhost:3000"]

    # Database - PostgreSQL (main database)
    POSTGRES_SERVER: str = "localhost"
    POSTGRES_PORT: int = 5432
    POSTGRES_USER: str = "postgres"
    POSTGRES_PASSWORD: str = "postgres"
    POSTGRES_DB: str = "thingsboard"
    DATABASE_URL: Optional[str] = None

    @validator("DATABASE_URL", pre=True)
    def assemble_db_connection(cls, v: Optional[str], values: dict) -> str:
        if isinstance(v, str):
            return v
        return f"postgresql+asyncpg://{values.get('POSTGRES_USER')}:{values.get('POSTGRES_PASSWORD')}@{values.get('POSTGRES_SERVER')}:{values.get('POSTGRES_PORT')}/{values.get('POSTGRES_DB')}"

    # Cassandra (timeseries database)
    CASSANDRA_ENABLED: bool = False
    CASSANDRA_HOSTS: List[str] = ["localhost"]
    CASSANDRA_PORT: int = 9042
    CASSANDRA_KEYSPACE: str = "thingsboard"

    # Redis (cache and distributed locks)
    REDIS_HOST: str = "localhost"
    REDIS_PORT: int = 6379
    REDIS_DB: int = 0
    REDIS_PASSWORD: Optional[str] = None

    # Kafka (messaging)
    KAFKA_ENABLED: bool = True
    KAFKA_BOOTSTRAP_SERVERS: List[str] = ["localhost:9092"]
    KAFKA_TOPIC_PREFIX: str = "tb"

    # MQTT Transport
    MQTT_ENABLED: bool = True
    MQTT_BIND_ADDRESS: str = "0.0.0.0"
    MQTT_BIND_PORT: int = 1883
    MQTT_SSL_ENABLED: bool = False

    # HTTP Transport
    HTTP_TRANSPORT_ENABLED: bool = True
    HTTP_BIND_PORT: int = 8081

    # CoAP Transport
    COAP_ENABLED: bool = True
    COAP_BIND_ADDRESS: str = "0.0.0.0"
    COAP_BIND_PORT: int = 5683

    # LWM2M Transport
    LWM2M_ENABLED: bool = False
    LWM2M_BIND_ADDRESS: str = "0.0.0.0"
    LWM2M_BIND_PORT: int = 5685

    # Telemetry Settings
    TELEMETRY_MAX_STRING_VALUE_LENGTH: int = 10000
    TELEMETRY_TTL_DAYS: int = 0  # 0 = infinite

    # Queue Settings
    QUEUE_TYPE: str = "kafka"  # kafka, rabbitmq, in-memory
    QUEUE_PARTITIONS: int = 10

    # Actor System
    ACTOR_SYSTEM_THROUGHPUT: int = 5
    ACTOR_MAX_ACTORS: int = 1000

    # Rate Limits
    RATE_LIMIT_ENABLED: bool = True
    DEFAULT_TENANT_DEVICE_LIMIT: int = 0  # 0 = unlimited
    DEFAULT_TENANT_ASSET_LIMIT: int = 0

    # Logging
    LOG_LEVEL: str = "INFO"
    LOG_FORMAT: str = "json"

    model_config = SettingsConfigDict(
        env_file=".env",
        case_sensitive=True,
        extra="allow"
    )


settings = Settings()
