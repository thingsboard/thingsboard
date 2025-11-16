"""
Telemetry schemas
"""
from pydantic import BaseModel
from typing import Dict, Any, List, Optional, Union
from app.models.telemetry import AttributeScope


class TelemetryData(BaseModel):
    """Single telemetry data point"""
    ts: int  # timestamp in milliseconds
    values: Dict[str, Union[str, int, float, bool, dict]]


class TelemetryRequest(BaseModel):
    """Request to save telemetry data"""
    # Map of key -> list of {ts, value} pairs
    # OR simple {key: value} for current timestamp
    pass


class AttributeData(BaseModel):
    """Attribute data"""
    key: str
    value: Union[str, int, float, bool, dict]


class AttributeRequest(BaseModel):
    """Request to save attributes"""
    attributes: Dict[str, Union[str, int, float, bool, dict]]


class TelemetryResponse(BaseModel):
    """Telemetry response"""
    data: Dict[str, List[Dict[str, Any]]]


class AttributeResponse(BaseModel):
    """Attribute response"""
    client: Optional[Dict[str, Any]] = None
    server: Optional[Dict[str, Any]] = None
    shared: Optional[Dict[str, Any]] = None


class TimeseriesKeysResponse(BaseModel):
    """Response with available timeseries keys"""
    keys: List[str]


class LatestTimeseriesResponse(BaseModel):
    """Latest telemetry values response"""
    data: Dict[str, List[Dict[str, Any]]]


class TelemetrySubscription(BaseModel):
    """WebSocket subscription for telemetry updates"""
    entity_type: str
    entity_id: str
    keys: Optional[List[str]] = None  # None = all keys
    scope: Optional[AttributeScope] = None


class DeleteTelemetryRequest(BaseModel):
    """Request to delete telemetry data"""
    keys: List[str]
    deleteAllDataForKeys: bool = False
    startTs: Optional[int] = None
    endTs: Optional[int] = None
    rewriteLatestIfDeleted: bool = False
