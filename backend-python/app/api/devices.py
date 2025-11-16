"""
Device API endpoints
CRUD operations for IoT devices
"""

from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func, or_
from uuid import uuid4

from app.core.database import get_db
from app.core.security import get_current_user
from app.models.user import User
from app.models.device import Device, DeviceCredentials
from app.schemas.device import (
    DeviceCreate,
    DeviceUpdate,
    DeviceResponse,
    DeviceListResponse,
   DeviceCredentialsResponse,
)

router = APIRouter(prefix="/devices", tags=["devices"])


@router.get("", response_model=DeviceListResponse)
async def get_devices(
    page: int = Query(0, ge=0),
    page_size: int = Query(10, ge=1, le=100),
    search: Optional[str] = None,
    device_type: Optional[str] = None,
    active: Optional[bool] = None,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Get all devices with pagination and filtering"""
    query = select(Device).where(Device.tenant_id == current_user.tenant_id)

    # Apply filters
    if search:
        query = query.where(
            or_(
                Device.name.ilike(f"%{search}%"),
                Device.label.ilike(f"%{search}%"),
                Device.type.ilike(f"%{search}%"),
            )
        )

    if device_type:
        query = query.where(Device.type == device_type)

    if active is not None:
        query = query.where(Device.active == active)

    # Filter by customer if user is customer user
    if current_user.authority == "CUSTOMER_USER" and current_user.customer_id:
        query = query.where(Device.customer_id == current_user.customer_id)

    # Get total count
    count_query = select(func.count()).select_from(query.subquery())
    total = await db.scalar(count_query)

    # Apply pagination and sorting
    query = query.order_by(Device.created_time.desc()).offset(page * page_size).limit(page_size)

    result = await db.execute(query)
    devices = result.scalars().all()

    return {
        "data": devices,
        "totalElements": total or 0,
        "totalPages": ((total or 0) + page_size - 1) // page_size,
    }


@router.get("/{device_id}", response_model=DeviceResponse)
async def get_device(
    device_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Get device by ID"""
    query = select(Device).where(
        Device.id == device_id,
        Device.tenant_id == current_user.tenant_id,
    )

    # Customer user can only see their own devices
    if current_user.authority == "CUSTOMER_USER" and current_user.customer_id:
        query = query.where(Device.customer_id == current_user.customer_id)

    result = await db.execute(query)
    device = result.scalar_one_or_none()

    if not device:
        raise HTTPException(status_code=404, detail="Device not found")

    return device


@router.post("", response_model=DeviceResponse, status_code=status.HTTP_201_CREATED)
async def create_device(
    device_in: DeviceCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Create a new device"""
    # Check if device name already exists
    existing = await db.execute(
        select(Device).where(
            Device.name == device_in.name,
            Device.tenant_id == current_user.tenant_id,
        )
    )
    if existing.scalar_one_or_none():
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=f"Device with name '{device_in.name}' already exists",
        )

    # Create device
    device = Device(
        id=str(uuid4()),
        tenant_id=current_user.tenant_id,
        name=device_in.name,
        type=device_in.type or "default",
        label=device_in.label,
        customer_id=device_in.customer_id,
        device_profile_id=device_in.device_profile_id,
        additional_info=device_in.additional_info,
        active=device_in.active if device_in.active is not None else True,
    )

    db.add(device)

    # Create device credentials (access token)
    if device_in.credentials_type:
        credentials = DeviceCredentials(
            id=str(uuid4()),
            device_id=device.id,
            credentials_type=device_in.credentials_type or "ACCESS_TOKEN",
            credentials_id=device_in.credentials_id or str(uuid4()),
            credentials_value=device_in.credentials_value,
        )
        db.add(credentials)

    await db.commit()
    await db.refresh(device)

    return device


@router.put("/{device_id}", response_model=DeviceResponse)
async def update_device(
    device_id: str,
    device_in: DeviceUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Update device"""
    result = await db.execute(
        select(Device).where(
            Device.id == device_id,
            Device.tenant_id == current_user.tenant_id,
        )
    )
    device = result.scalar_one_or_none()

    if not device:
        raise HTTPException(status_code=404, detail="Device not found")

    # Update fields
    update_data = device_in.dict(exclude_unset=True)
    for field, value in update_data.items():
        setattr(device, field, value)

    await db.commit()
    await db.refresh(device)

    return device


@router.delete("/{device_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_device(
    device_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Delete device"""
    result = await db.execute(
        select(Device).where(
            Device.id == device_id,
            Device.tenant_id == current_user.tenant_id,
        )
    )
    device = result.scalar_one_or_none()

    if not device:
        raise HTTPException(status_code=404, detail="Device not found")

    await db.delete(device)
    await db.commit()

    return None


@router.post("/{device_id}/assign/{customer_id}", response_model=DeviceResponse)
async def assign_device_to_customer(
    device_id: str,
    customer_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Assign device to customer"""
    result = await db.execute(
        select(Device).where(
            Device.id == device_id,
            Device.tenant_id == current_user.tenant_id,
        )
    )
    device = result.scalar_one_or_none()

    if not device:
        raise HTTPException(status_code=404, detail="Device not found")

    device.customer_id = customer_id
    await db.commit()
    await db.refresh(device)

    return device


@router.delete("/{device_id}/unassign", response_model=DeviceResponse)
async def unassign_device_from_customer(
    device_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Unassign device from customer"""
    result = await db.execute(
        select(Device).where(
            Device.id == device_id,
            Device.tenant_id == current_user.tenant_id,
        )
    )
    device = result.scalar_one_or_none()

    if not device:
        raise HTTPException(status_code=404, detail="Device not found")

    device.customer_id = None
    await db.commit()
    await db.refresh(device)

    return device


@router.get("/{device_id}/credentials", response_model=DeviceCredentialsResponse)
async def get_device_credentials(
    device_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Get device credentials"""
    # First check device exists and user has access
    device_result = await db.execute(
        select(Device).where(
            Device.id == device_id,
            Device.tenant_id == current_user.tenant_id,
        )
    )
    device = device_result.scalar_one_or_none()

    if not device:
        raise HTTPException(status_code=404, detail="Device not found")

    # Get credentials
    credentials_result = await db.execute(
        select(DeviceCredentials).where(DeviceCredentials.device_id == device_id)
    )
    credentials = credentials_result.scalar_one_or_none()

    if not credentials:
        raise HTTPException(status_code=404, detail="Device credentials not found")

    return credentials
