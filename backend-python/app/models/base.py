"""
Base models for all entities
"""
from datetime import datetime
from typing import Optional
from sqlalchemy import Column, String, BigInteger, DateTime, text
from sqlalchemy.dialects.postgresql import UUID
from app.db.session import Base
import uuid


class UUIDMixin:
    """Mixin for UUID primary key"""
    id = Column(
        UUID(as_uuid=True),
        primary_key=True,
        default=uuid.uuid4,
        server_default=text("gen_random_uuid()"),
    )


class TimestampMixin:
    """Mixin for created/updated timestamps"""
    created_time = Column(
        BigInteger,
        nullable=False,
        default=lambda: int(datetime.utcnow().timestamp() * 1000),
    )


class BaseEntity(Base, UUIDMixin, TimestampMixin):
    """Base entity class for all domain models"""
    __abstract__ = True

    def to_dict(self):
        """Convert model to dictionary"""
        return {c.name: getattr(self, c.name) for c in self.__table__.columns}


class TenantEntity(BaseEntity):
    """Base class for tenant-scoped entities"""
    __abstract__ = True

    tenant_id = Column(
        UUID(as_uuid=True),
        nullable=False,
        index=True,
    )


class CustomerEntity(TenantEntity):
    """Base class for customer-scoped entities"""
    __abstract__ = True

    customer_id = Column(
        UUID(as_uuid=True),
        nullable=True,
        index=True,
    )


class NamedEntity(BaseEntity):
    """Base class for entities with name and search text"""
    __abstract__ = True

    name = Column(String(255), nullable=False)
    search_text = Column(String(255), nullable=True)

    def update_search_text(self):
        """Update search text for full-text search"""
        self.search_text = self.name.lower() if self.name else None
