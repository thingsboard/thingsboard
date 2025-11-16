"""
Gateway Schemas
"""

from typing import Optional, List, Any, Dict
from pydantic import BaseModel, Field
from datetime import datetime


class ConnectorConfig(BaseModel):
    """Connector configuration"""

    name: str
    type: str  # mqtt, modbus, opcua, ble, request, can, bacnet, odbc, rest
    enabled: bool = True
    configuration: Dict[str, Any] = Field(default_factory=dict)


class GatewayBase(BaseModel):
    """Base gateway schema"""

    name: str = Field(..., min_length=1, max_length=255)
    label: Optional[str] = Field(None, max_length=255)
    type: str = Field(default="Default Gateway", max_length=100)
    gateway_profile_id: Optional[str] = None
    customer_id: Optional[str] = None
    active: bool = True


class GatewayCreate(GatewayBase):
    """Gateway creation schema"""

    pass


class GatewayUpdate(BaseModel):
    """Gateway update schema"""

    name: Optional[str] = Field(None, min_length=1, max_length=255)
    label: Optional[str] = Field(None, max_length=255)
    type: Optional[str] = Field(None, max_length=100)
    gateway_profile_id: Optional[str] = None
    customer_id: Optional[str] = None
    active: Optional[bool] = None


class GatewayResponse(GatewayBase):
    """Gateway response schema"""

    id: str
    tenant_id: str
    connected: bool = False
    last_activity_time: Optional[int] = None
    created_time: int
    additional_info: Optional[Dict[str, Any]] = None

    # Computed fields
    gateway_profile_name: Optional[str] = None
    customer_title: Optional[str] = None

    class Config:
        from_attributes = True


class GatewayListResponse(BaseModel):
    """Gateway list response with pagination"""

    data: List[GatewayResponse]
    totalElements: int
    totalPages: int
