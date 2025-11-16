"""
Alarm entity model
"""
from sqlalchemy import Column, String, Text, JSON, Boolean, BigInteger, Index, Enum as SQLEnum
from sqlalchemy.dialects.postgresql import UUID
from enum import Enum
from app.models.base import BaseEntity, TenantEntity


class AlarmSeverity(str, Enum):
    """Alarm severity levels"""
    CRITICAL = "CRITICAL"
    MAJOR = "MAJOR"
    MINOR = "MINOR"
    WARNING = "WARNING"
    INDETERMINATE = "INDETERMINATE"


class AlarmStatus(str, Enum):
    """Alarm status"""
    ACTIVE_UNACK = "ACTIVE_UNACK"  # Active and unacknowledged
    ACTIVE_ACK = "ACTIVE_ACK"  # Active and acknowledged
    CLEARED_UNACK = "CLEARED_UNACK"  # Cleared but unacknowledged
    CLEARED_ACK = "CLEARED_ACK"  # Cleared and acknowledged


class Alarm(BaseEntity, TenantEntity):
    """
    Alarm entity
    Represents an alarm condition triggered by rule engine
    """
    __tablename__ = "alarm"

    # Originator (device/asset) type and ID
    originator_type = Column(String(255), nullable=False)
    originator_id = Column(UUID(as_uuid=True), nullable=False, index=True)

    # Customer ID (if alarm is assigned to customer)
    customer_id = Column(UUID(as_uuid=True), nullable=True, index=True)

    # Alarm type
    type = Column(String(255), nullable=False, index=True)

    # Severity
    severity = Column(
        SQLEnum(AlarmSeverity, name="alarm_severity_enum"),
        nullable=False,
        default=AlarmSeverity.INDETERMINATE,
    )

    # Status
    status = Column(
        SQLEnum(AlarmStatus, name="alarm_status_enum"),
        nullable=False,
        default=AlarmStatus.ACTIVE_UNACK,
    )

    # Start time
    start_ts = Column(BigInteger, nullable=False, index=True)

    # End time (when cleared)
    end_ts = Column(BigInteger, nullable=True)

    # Acknowledge time
    ack_ts = Column(BigInteger, nullable=True)

    # Clear time
    clear_ts = Column(BigInteger, nullable=True)

    # Details (JSON)
    details = Column(JSON, nullable=True)

    # Propagate flag (for alarm propagation in entity hierarchies)
    propagate = Column(Boolean, default=False, nullable=False)

    # Propagate to owner
    propagate_to_owner = Column(Boolean, default=False, nullable=False)

    # Propagate to tenant
    propagate_to_tenant = Column(Boolean, default=False, nullable=False)

    # Propagate relation types (JSON array)
    propagate_relation_types = Column(JSON, nullable=True)

    __table_args__ = (
        Index('idx_alarm_tenant_id', 'tenant_id'),
        Index('idx_alarm_originator_id', 'originator_id'),
        Index('idx_alarm_originator_type', 'originator_type'),
        Index('idx_alarm_type', 'type'),
        Index('idx_alarm_status', 'status'),
        Index('idx_alarm_severity', 'severity'),
        Index('idx_alarm_start_ts', 'start_ts'),
    )

    def __repr__(self):
        return f"<Alarm(id={self.id}, type={self.type}, severity={self.severity}, status={self.status})>"
