"""
User entity model
"""
from sqlalchemy import Column, String, Boolean, Enum as SQLEnum, JSON, Index, ForeignKey
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import relationship
from enum import Enum
from app.models.base import BaseEntity, TenantEntity


class Authority(str, Enum):
    """User authority levels"""
    SYS_ADMIN = "SYS_ADMIN"  # System administrator
    TENANT_ADMIN = "TENANT_ADMIN"  # Tenant administrator
    CUSTOMER_USER = "CUSTOMER_USER"  # Customer user


class TbUser(BaseEntity, TenantEntity):
    """
    User entity
    Supports multi-tenancy with different authority levels
    """
    __tablename__ = "tb_user"

    # Email (username)
    email = Column(String(255), unique=True, nullable=False, index=True)

    # Authority/Role
    authority = Column(
        SQLEnum(Authority, name="authority_enum"),
        nullable=False,
        default=Authority.CUSTOMER_USER,
    )

    # Customer ID (for customer users)
    customer_id = Column(
        UUID(as_uuid=True),
        ForeignKey("customer.id", ondelete="CASCADE"),
        nullable=True,
        index=True,
    )

    # First and last name
    first_name = Column(String(255), nullable=True)
    last_name = Column(String(255), nullable=True)

    # Additional info (JSON)
    additional_info = Column(JSON, nullable=True)

    __table_args__ = (
        Index('idx_user_email', 'email'),
        Index('idx_user_tenant_id', 'tenant_id'),
        Index('idx_user_customer_id', 'customer_id'),
        Index('idx_user_authority', 'authority'),
    )

    def __repr__(self):
        return f"<TbUser(id={self.id}, email={self.email}, authority={self.authority})>"


class UserCredentials(BaseEntity):
    """
    User credentials - separate table for security
    Stores password hash and security settings
    """
    __tablename__ = "user_credentials"

    # User ID (one-to-one with TbUser)
    user_id = Column(
        UUID(as_uuid=True),
        ForeignKey("tb_user.id", ondelete="CASCADE"),
        nullable=False,
        unique=True,
        index=True,
    )

    # Password hash
    password = Column(String(255), nullable=False)

    # Activation token (for email verification)
    activate_token = Column(String(255), nullable=True, unique=True)

    # Reset token (for password reset)
    reset_token = Column(String(255), nullable=True, unique=True)

    # Enabled flag
    enabled = Column(Boolean, default=True, nullable=False)

    # Additional info
    additional_info = Column(JSON, nullable=True)

    # Relationship to user
    user = relationship("TbUser", backref="credentials", uselist=False)

    __table_args__ = (
        Index('idx_user_credentials_user_id', 'user_id'),
        Index('idx_user_credentials_activate_token', 'activate_token'),
        Index('idx_user_credentials_reset_token', 'reset_token'),
    )

    def __repr__(self):
        return f"<UserCredentials(user_id={self.user_id}, enabled={self.enabled})>"
