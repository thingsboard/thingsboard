"""
Tenant entity model
"""
from sqlalchemy import Column, String, Boolean, JSON, Index
from app.models.base import NamedEntity


class Tenant(NamedEntity):
    """
    Tenant entity - top-level isolation boundary
    Each tenant has its own users, devices, assets, dashboards, etc.
    """
    __tablename__ = "tenant"

    # Tenant title/name (inherited from NamedEntity)
    # name = Column(String(255), nullable=False)

    # Region (for multi-region deployments)
    region = Column(String(255), nullable=True)

    # Tenant profile ID (for resource limits, rate limits, etc.)
    tenant_profile_id = Column(String(255), nullable=True)

    # Email for notifications
    email = Column(String(255), nullable=True)

    # Phone number
    phone = Column(String(255), nullable=True)

    # Address
    address = Column(String(1024), nullable=True)
    address2 = Column(String(1024), nullable=True)
    city = Column(String(255), nullable=True)
    state = Column(String(255), nullable=True)
    zip = Column(String(255), nullable=True)
    country = Column(String(255), nullable=True)

    # Additional info (JSON)
    additional_info = Column(JSON, nullable=True)

    # Isolated tenant B2B (for complete data isolation)
    isolated_tb_core = Column(Boolean, default=False, nullable=False)
    isolated_tb_rule_engine = Column(Boolean, default=False, nullable=False)

    __table_args__ = (
        Index('idx_tenant_region', 'region'),
        Index('idx_tenant_search_text', 'search_text'),
    )

    def __repr__(self):
        return f"<Tenant(id={self.id}, name={self.name}, region={self.region})>"
