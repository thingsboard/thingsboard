"""
Device Profile entity model
"""
from sqlalchemy import Column, String, Boolean, JSON, Text, Index, Enum as SQLEnum
from enum import Enum
from app.models.base import NamedEntity, TenantEntity


class DeviceProfileType(str, Enum):
    """Device profile type"""
    DEFAULT = "DEFAULT"


class DeviceTransportType(str, Enum):
    """Device transport protocol type"""
    DEFAULT = "DEFAULT"
    MQTT = "MQTT"
    COAP = "COAP"
    LWM2M = "LWM2M"
    SNMP = "SNMP"


class DeviceProfile(NamedEntity, TenantEntity):
    """
    Device Profile entity
    Defines device capabilities, data processing rules, and transport configuration
    """
    __tablename__ = "device_profile"

    # Profile type
    type = Column(
        SQLEnum(DeviceProfileType, name="device_profile_type_enum"),
        nullable=False,
        default=DeviceProfileType.DEFAULT,
    )

    # Transport type
    transport_type = Column(
        SQLEnum(DeviceTransportType, name="device_transport_type_enum"),
        nullable=False,
        default=DeviceTransportType.DEFAULT,
    )

    # Description
    description = Column(Text, nullable=True)

    # Default flag
    is_default = Column(Boolean, default=False, nullable=False)

    # Default rule chain ID
    default_rule_chain_id = Column(String(255), nullable=True)

    # Default dashboard ID
    default_dashboard_id = Column(String(255), nullable=True)

    # Default queue name
    default_queue_name = Column(String(255), nullable=True)

    # Profile data (JSON) - contains device attributes, telemetry keys, etc.
    profile_data = Column(JSON, nullable=True)

    # Provisioning type and data
    provision_type = Column(String(255), nullable=True)
    provision_device_key = Column(String(255), nullable=True)

    # Transport configuration (JSON)
    transport_configuration = Column(JSON, nullable=True)

    # Firmware/Software OTA configuration
    firmware_id = Column(String(255), nullable=True)
    software_id = Column(String(255), nullable=True)

    # Default edge rule chain ID
    default_edge_rule_chain_id = Column(String(255), nullable=True)

    __table_args__ = (
        Index('idx_device_profile_tenant_id', 'tenant_id'),
        Index('idx_device_profile_is_default', 'is_default'),
        Index('idx_device_profile_transport_type', 'transport_type'),
    )

    def __repr__(self):
        return f"<DeviceProfile(id={self.id}, name={self.name}, type={self.type})>"
