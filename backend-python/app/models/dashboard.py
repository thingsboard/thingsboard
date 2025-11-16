"""
Dashboard entity model
"""
from sqlalchemy import Column, String, Text, JSON, Boolean, Index
from sqlalchemy.dialects.postgresql import UUID
from app.models.base import NamedEntity, TenantEntity


class Dashboard(NamedEntity, TenantEntity):
    """
    Dashboard entity
    Contains widgets, layouts, and visualization configuration
    """
    __tablename__ = "dashboard"

    # Title
    title = Column(String(255), nullable=False)

    # Image (thumbnail)
    image = Column(Text, nullable=True)

    # Mobile order/hide flag
    mobile_hide = Column(Boolean, default=False)
    mobile_order = Column(Integer, nullable=True)

    # Configuration (JSON) - widgets, layouts, settings
    configuration = Column(JSON, nullable=True)

    # Assigned customers (JSON array of customer IDs)
    assigned_customers = Column(JSON, nullable=True)

    # Additional info
    additional_info = Column(JSON, nullable=True)

    # External ID
    external_id = Column(UUID(as_uuid=True), nullable=True, index=True)

    __table_args__ = (
        Index('idx_dashboard_tenant_id', 'tenant_id'),
        Index('idx_dashboard_search_text', 'search_text'),
    )

    def __repr__(self):
        return f"<Dashboard(id={self.id}, title={self.title})>"


# Import Integer for mobile_order
from sqlalchemy import Integer
