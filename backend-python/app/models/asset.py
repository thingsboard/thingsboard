"""
Asset entity model
"""
from sqlalchemy import Column, String, Text, JSON, Index
from sqlalchemy.dialects.postgresql import UUID
from app.models.base import NamedEntity, CustomerEntity


class Asset(NamedEntity, CustomerEntity):
    """
    Asset entity
    Represents a logical grouping or hierarchy of devices
    """
    __tablename__ = "asset"

    # Asset type (e.g., "building", "floor", "room")
    type = Column(String(255), nullable=False, index=True)

    # Label for UI display
    label = Column(String(255), nullable=True)

    # Asset profile ID
    asset_profile_id = Column(UUID(as_uuid=True), nullable=True, index=True)

    # Additional info (JSON)
    additional_info = Column(JSON, nullable=True)

    # External ID
    external_id = Column(UUID(as_uuid=True), nullable=True, index=True)

    __table_args__ = (
        Index('idx_asset_tenant_id', 'tenant_id'),
        Index('idx_asset_customer_id', 'customer_id'),
        Index('idx_asset_type', 'type'),
        Index('idx_asset_search_text', 'search_text'),
    )

    def __repr__(self):
        return f"<Asset(id={self.id}, name={self.name}, type={self.type})>"
