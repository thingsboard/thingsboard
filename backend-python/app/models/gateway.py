"""
Gateway Model
"""

from sqlalchemy import Column, String, Boolean, Integer, JSON, ForeignKey
from sqlalchemy.orm import relationship
from app.models.base import TenantEntity


class Gateway(TenantEntity):
    """Gateway model for IoT gateway devices"""

    __tablename__ = "gateways"

    name = Column(String(255), nullable=False)
    label = Column(String(255))
    type = Column(String(100), default="Default Gateway")  # IoT, Modbus, OPC-UA, MQTT, BLE
    gateway_profile_id = Column(String(36), ForeignKey("gateway_profiles.id"))
    customer_id = Column(String(36), ForeignKey("customers.id"))
    active = Column(Boolean, default=True)
    connected = Column(Boolean, default=False)
    last_activity_time = Column(Integer)  # Unix timestamp in milliseconds

    # Relationships
    gateway_profile = relationship("GatewayProfile", back_populates="gateways")
    customer = relationship("Customer", back_populates="gateways")

    def __repr__(self):
        return f"<Gateway {self.name}>"
