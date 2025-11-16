"""
Entity View model
"""
from sqlalchemy import Column, String, BigInteger, JSON, Index
from sqlalchemy.dialects.postgresql import UUID
from app.models.base import NamedEntity, CustomerEntity


class EntityView(NamedEntity, CustomerEntity):
    """
    Entity View - a virtual entity that provides a filtered view of device/asset data
    """
    __tablename__ = "entity_view"

    # Entity type (DEVICE, ASSET)
    entity_type = Column(String(255), nullable=False)

    # Entity ID
    entity_id = Column(UUID(as_uuid=True), nullable=False, index=True)

    # Type
    type = Column(String(255), nullable=False)

    # Keys (JSON) - specifies which telemetry/attributes to expose
    keys = Column(JSON, nullable=True)

    # Start/End time for time-based filtering
    start_ts = Column(BigInteger, nullable=True)
    end_ts = Column(BigInteger, nullable=True)

    # Additional info
    additional_info = Column(JSON, nullable=True)

    __table_args__ = (
        Index('idx_entity_view_tenant_id', 'tenant_id'),
        Index('idx_entity_view_customer_id', 'customer_id'),
        Index('idx_entity_view_entity_id', 'entity_id'),
    )

    def __repr__(self):
        return f"<EntityView(id={self.id}, name={self.name}, entity_id={self.entity_id})>"
