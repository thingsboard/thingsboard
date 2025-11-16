"""
Customer schemas
"""
from pydantic import BaseModel, EmailStr
from typing import Optional, Dict, Any, List


class CustomerBase(BaseModel):
    """Base customer schema"""
    title: str
    email: Optional[EmailStr] = None
    phone: Optional[str] = None
    address: Optional[str] = None
    address2: Optional[str] = None
    city: Optional[str] = None
    state: Optional[str] = None
    zip: Optional[str] = None
    country: Optional[str] = None
    additional_info: Optional[Dict[str, Any]] = None
    is_public: bool = False


class CustomerCreate(CustomerBase):
    """Schema for creating a customer"""
    pass


class CustomerUpdate(BaseModel):
    """Schema for updating a customer"""
    title: Optional[str] = None
    email: Optional[EmailStr] = None
    phone: Optional[str] = None
    address: Optional[str] = None
    address2: Optional[str] = None
    city: Optional[str] = None
    state: Optional[str] = None
    zip: Optional[str] = None
    country: Optional[str] = None
    additional_info: Optional[Dict[str, Any]] = None
    is_public: Optional[bool] = None


class CustomerResponse(CustomerBase):
    """Schema for customer response"""
    id: str
    tenant_id: str
    name: str
    created_time: int

    class Config:
        from_attributes = True


class CustomerListResponse(BaseModel):
    """Schema for paginated customer list response"""
    data: List[CustomerResponse]
    totalElements: int
    totalPages: int
