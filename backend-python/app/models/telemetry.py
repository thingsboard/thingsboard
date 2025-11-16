"""
Telemetry data models
Note: Actual time-series data is typically stored in Cassandra or TimescaleDB
These models are for metadata and latest values
"""
from sqlalchemy import Column, String, BigInteger, Text, Index, UniqueConstraint
from sqlalchemy.dialects.postgresql import UUID, JSONB
from app.models.base import Base
from typing import Dict, Any, Optional
from dataclasses import dataclass
from enum import Enum


class DataType(str, Enum):
    """Telemetry data types"""
    STRING = "STRING"
    LONG = "LONG"
    DOUBLE = "DOUBLE"
    BOOLEAN = "BOOLEAN"
    JSON = "JSON"


class AttributeScope(str, Enum):
    """Attribute scopes"""
    CLIENT_SCOPE = "CLIENT_SCOPE"  # From device
    SERVER_SCOPE = "SERVER_SCOPE"  # From server/rule engine
    SHARED_SCOPE = "SHARED_SCOPE"  # Shared between device and server


@dataclass
class TsKvEntry:
    """
    Time-series key-value entry
    This is a dataclass for in-memory representation
    Actual storage will be in Cassandra/TimescaleDB
    """
    entity_id: str
    key: str
    ts: int  # milliseconds since epoch
    value: Any
    data_type: DataType


@dataclass
class AttributeKvEntry:
    """
    Attribute key-value entry
    """
    entity_id: str
    attribute_type: AttributeScope
    attribute_key: str
    last_update_ts: int
    value: Any
    data_type: DataType


class AttributeKv(Base):
    """
    Latest attribute values - stored in PostgreSQL for fast lookup
    """
    __tablename__ = "attribute_kv"

    # Entity ID
    entity_id = Column(UUID(as_uuid=True), nullable=False, primary_key=True)

    # Attribute type/scope
    attribute_type = Column(String(255), nullable=False, primary_key=True)

    # Attribute key
    attribute_key = Column(String(255), nullable=False, primary_key=True)

    # Last update timestamp
    last_update_ts = Column(BigInteger, nullable=False)

    # Value (stored as JSON for flexibility)
    bool_v = Column(Boolean, nullable=True)
    str_v = Column(Text, nullable=True)
    long_v = Column(BigInteger, nullable=True)
    dbl_v = Column(Float, nullable=True)
    json_v = Column(JSONB, nullable=True)

    __table_args__ = (
        Index('idx_attribute_kv_entity_id', 'entity_id'),
        Index('idx_attribute_kv_entity_type_key', 'entity_id', 'attribute_type', 'attribute_key'),
    )

    def __repr__(self):
        return f"<AttributeKv(entity_id={self.entity_id}, key={self.attribute_key})>"


class TsKvLatest(Base):
    """
    Latest telemetry values - stored in PostgreSQL for fast lookup
    """
    __tablename__ = "ts_kv_latest"

    # Entity ID
    entity_id = Column(UUID(as_uuid=True), nullable=False, primary_key=True)

    # Key
    key = Column(String(255), nullable=False, primary_key=True)

    # Timestamp
    ts = Column(BigInteger, nullable=False)

    # Value (polymorphic storage)
    bool_v = Column(Boolean, nullable=True)
    str_v = Column(Text, nullable=True)
    long_v = Column(BigInteger, nullable=True)
    dbl_v = Column(Float, nullable=True)
    json_v = Column(JSONB, nullable=True)

    __table_args__ = (
        Index('idx_ts_kv_latest_entity_id', 'entity_id'),
        Index('idx_ts_kv_latest_key', 'key'),
    )

    def __repr__(self):
        return f"<TsKvLatest(entity_id={self.entity_id}, key={self.key}, ts={self.ts})>"


# Import missing types
from sqlalchemy import Boolean, Float


# Cassandra table definitions (for reference - will be created by Cassandra driver)
"""
CREATE TABLE IF NOT EXISTS thingsboard.ts_kv_cf (
    entity_id uuid,
    key text,
    partition bigint,
    ts bigint,
    bool_v boolean,
    str_v text,
    long_v bigint,
    dbl_v double,
    json_v text,
    PRIMARY KEY ((entity_id, key, partition), ts)
) WITH CLUSTERING ORDER BY (ts DESC);

CREATE TABLE IF NOT EXISTS thingsboard.ts_kv_partitions_cf (
    entity_id uuid,
    key text,
    partition bigint,
    PRIMARY KEY ((entity_id, key), partition)
) WITH CLUSTERING ORDER BY (partition DESC);
"""
