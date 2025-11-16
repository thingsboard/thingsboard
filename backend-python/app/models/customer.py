"""
Customer entity model
"""
from sqlalchemy import Column, String, Boolean, JSON, Index, ForeignKey
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import relationship
from app.models.base import NamedEntity, TenantEntity


class Customer(NamedEntity, TenantEntity):
    """
    Customer entity - sub-tenant for multi-customer scenarios
    A tenant can have multiple customers
    Each customer can have its own users, devices, assets, dashboards
    """
    __tablename__ = "customer"

    # Customer title/name (inherited from NamedEntity)
    # tenant_id (inherited from TenantEntity)

    # Title
    title = Column(String(255), nullable=False)

    # Email
    email = Column(String(255), nullable=True)

    # Phone
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

    # Public flag (for public dashboards, etc.)
    is_public = Column(Boolean, default=False, nullable=False)

    __table_args__ = (
        Index('idx_customer_tenant_id', 'tenant_id'),
        Index('idx_customer_search_text', 'search_text'),
        Index('idx_customer_tenant_title', 'tenant_id', 'title'),
    )

    def __repr__(self):
        return f"<Customer(id={self.id}, title={self.title}, tenant_id={self.tenant_id})>"
