"""
Database models package
"""
from app.models.base import Base, BaseEntity, TenantEntity, CustomerEntity, NamedEntity
from app.models.tenant import Tenant
from app.models.customer import Customer
from app.models.user import TbUser, UserCredentials, Authority
from app.models.device_profile import DeviceProfile, DeviceProfileType, DeviceTransportType
from app.models.device import Device, DeviceCredentials
from app.models.asset import Asset
from app.models.entity_view import EntityView
from app.models.dashboard import Dashboard
from app.models.rule_chain import RuleChain, RuleChainType
from app.models.alarm import Alarm, AlarmSeverity, AlarmStatus

__all__ = [
    # Base classes
    "Base",
    "BaseEntity",
    "TenantEntity",
    "CustomerEntity",
    "NamedEntity",
    # Tenant & Customer
    "Tenant",
    "Customer",
    # User & Auth
    "TbUser",
    "UserCredentials",
    "Authority",
    # Device
    "DeviceProfile",
    "DeviceProfileType",
    "DeviceTransportType",
    "Device",
    "DeviceCredentials",
    # Asset
    "Asset",
    # Entity View
    "EntityView",
    # Dashboard
    "Dashboard",
    # Rule Chain
    "RuleChain",
    "RuleChainType",
    # Alarm
    "Alarm",
    "AlarmSeverity",
    "AlarmStatus",
]
