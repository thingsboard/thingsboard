"""
Device schemas
"""
from pydantic import BaseModel
from typing import Optional, Dict, Any


class DeviceBase(BaseModel):
    """Base device schema"""
    name: str
    type: str
    label: Optional[str] = None
    additional_info: Optional[Dict[str, Any]] = None


class DeviceCreate(DeviceBase):
    """Schema for creating a device"""
    device_profile_id: str
    customer_id: Optional[str] = None


class DeviceUpdate(BaseModel):
    """Schema for updating a device"""
    name: Optional[str] = None
    type: Optional[str] = None
    label: Optional[str] = None
    device_profile_id: Optional[str] = None
    customer_id: Optional[str] = None
    additional_info: Optional[Dict[str, Any]] = None


class DeviceResponse(DeviceBase):
    """Schema for device response"""
    id: str
    tenant_id: str
    customer_id: Optional[str] = None
    device_profile_id: str
    created_time: int

    class Config:
        from_attributes = True


class DeviceCredentialsResponse(BaseModel):
    """Schema for device credentials response"""
    id: str
    device_id: str
    credentials_type: str
    credentials_id: Optional[str] = None

    class Config:
        from_attributes = True


class DeviceCredentialsUpdate(BaseModel):
    """Schema for updating device credentials"""
    credentials_type: str
    credentials_id: Optional[str] = None
    credentials_value: Optional[str] = None
