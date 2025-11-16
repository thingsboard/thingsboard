"""
Message models for gateway
"""
from typing import Dict, Any, Optional, List
from pydantic import BaseModel
from enum import Enum
from datetime import datetime


class MessageType(str, Enum):
    """Message type enum"""
    TELEMETRY = "telemetry"
    ATTRIBUTES = "attributes"
    RPC_REQUEST = "rpc_request"
    RPC_RESPONSE = "rpc_response"
    DEVICE_STATE = "device_state"
    CONNECT = "connect"
    DISCONNECT = "disconnect"


class DeviceState(str, Enum):
    """Device connection state"""
    ONLINE = "online"
    OFFLINE = "offline"


class TelemetryMessage(BaseModel):
    """Telemetry message from device"""
    device_id: Optional[str] = None  # If known
    device_token: Optional[str] = None  # Access token for auth
    ts: int  # Timestamp in milliseconds
    values: Dict[str, Any]  # key-value pairs of telemetry data

    class Config:
        json_schema_extra = {
            "example": {
                "device_token": "A1_TEST_TOKEN",
                "ts": 1234567890000,
                "values": {
                    "temperature": 25.5,
                    "humidity": 60,
                    "status": "ok"
                }
            }
        }


class AttributesMessage(BaseModel):
    """Attributes message from device"""
    device_id: Optional[str] = None
    device_token: Optional[str] = None
    attributes: Dict[str, Any]

    class Config:
        json_schema_extra = {
            "example": {
                "device_token": "A1_TEST_TOKEN",
                "attributes": {
                    "firmware_version": "1.2.3",
                    "model": "TH-100"
                }
            }
        }


class DeviceStateMessage(BaseModel):
    """Device connection state change message"""
    device_id: str
    state: DeviceState
    timestamp: int
    transport: str  # mqtt, http, coap


class RPCRequest(BaseModel):
    """RPC request from server to device"""
    device_id: str
    request_id: int
    method: str
    params: Dict[str, Any]
    timeout: int = 5000  # milliseconds


class RPCResponse(BaseModel):
    """RPC response from device to server"""
    device_id: str
    request_id: int
    response: Any
    error: Optional[str] = None


class GatewayMessage(BaseModel):
    """Internal gateway message envelope"""
    message_type: MessageType
    device_id: Optional[str] = None
    device_token: Optional[str] = None
    transport: str  # mqtt, http, coap
    payload: Dict[str, Any]
    timestamp: int = int(datetime.utcnow().timestamp() * 1000)
    gateway_id: str = "gateway-001"
