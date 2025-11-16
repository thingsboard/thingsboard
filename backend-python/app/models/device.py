"""
Device entity model
"""
from sqlalchemy import Column, String, Text, JSON, Index, ForeignKey
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import relationship
from app.models.base import NamedEntity, CustomerEntity


class Device(NamedEntity, CustomerEntity):
    """
    Device entity
    Represents a physical or virtual device in the IoT platform
    """
    __tablename__ = "device"

    # Device type (e.g., "thermometer", "humidity_sensor")
    type = Column(String(255), nullable=False, index=True)

    # Label for UI display
    label = Column(String(255), nullable=True)

    # Device profile ID
    device_profile_id = Column(
        UUID(as_uuid=True),
        ForeignKey("device_profile.id"),
        nullable=False,
        index=True,
    )

    # Device data (JSON) - configuration, firmware version, etc.
    device_data = Column(JSON, nullable=True)

    # Additional info (JSON)
    additional_info = Column(JSON, nullable=True)

    # Firmware ID
    firmware_id = Column(UUID(as_uuid=True), nullable=True)

    # Software ID
    software_id = Column(UUID(as_uuid=True), nullable=True)

    # External ID (for integration with external systems)
    external_id = Column(UUID(as_uuid=True), nullable=True, index=True)

    # Relationship to device profile
    device_profile = relationship("DeviceProfile", backref="devices")

    __table_args__ = (
        Index('idx_device_tenant_id', 'tenant_id'),
        Index('idx_device_customer_id', 'customer_id'),
        Index('idx_device_type', 'type'),
        Index('idx_device_device_profile_id', 'device_profile_id'),
        Index('idx_device_search_text', 'search_text'),
        Index('idx_device_external_id', 'external_id'),
    )

    def __repr__(self):
        return f"<Device(id={self.id}, name={self.name}, type={self.type})>"


class DeviceCredentials(BaseEntity):
    """
    Device credentials - for device authentication
    """
    __tablename__ = "device_credentials"

    # Device ID
    device_id = Column(
        UUID(as_uuid=True),
        ForeignKey("device.id", ondelete="CASCADE"),
        nullable=False,
        unique=True,
        index=True,
    )

    # Credentials type (ACCESS_TOKEN, X509_CERTIFICATE, MQTT_BASIC, LWM2M_CREDENTIALS)
    credentials_type = Column(String(255), nullable=False)

    # Credentials ID (e.g., access token)
    credentials_id = Column(String(255), nullable=True, unique=True, index=True)

    # Credentials value (encrypted)
    credentials_value = Column(Text, nullable=True)

    # Relationship to device
    device = relationship("Device", backref="credentials", uselist=False)

    __table_args__ = (
        Index('idx_device_credentials_device_id', 'device_id'),
        Index('idx_device_credentials_credentials_id', 'credentials_id'),
    )

    def __repr__(self):
        return f"<DeviceCredentials(device_id={self.device_id}, type={self.credentials_type})>"


# Import BaseEntity for DeviceCredentials
from app.models.base import BaseEntity
