"""
Asset schemas
"""
from pydantic import BaseModel
from typing import Optional, Dict, Any, List


class AssetBase(BaseModel):
    """Base asset schema"""
    name: str
    type: str
    label: Optional[str] = None
    additional_info: Optional[Dict[str, Any]] = None


class AssetCreate(AssetBase):
    """Schema for creating an asset"""
    asset_profile_id: Optional[str] = None
    customer_id: Optional[str] = None


class AssetUpdate(BaseModel):
    """Schema for updating an asset"""
    name: Optional[str] = None
    type: Optional[str] = None
    label: Optional[str] = None
    asset_profile_id: Optional[str] = None
    customer_id: Optional[str] = None
    additional_info: Optional[Dict[str, Any]] = None


class AssetResponse(AssetBase):
    """Schema for asset response"""
    id: str
    tenant_id: str
    customer_id: Optional[str] = None
    asset_profile_id: Optional[str] = None
    created_time: int

    class Config:
        from_attributes = True


class AssetListResponse(BaseModel):
    """Schema for paginated asset list response"""
    data: List[AssetResponse]
    totalElements: int
    totalPages: int
