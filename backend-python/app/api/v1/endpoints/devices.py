"""
Device endpoints
"""
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from typing import List
from uuid import UUID
import secrets

from app.db.session import get_db
from app.models.device import Device, DeviceCredentials
from app.models.user import TbUser, Authority
from app.schemas.device import (
    DeviceCreate,
    DeviceUpdate,
    DeviceResponse,
    DeviceCredentialsResponse,
    DeviceCredentialsUpdate,
)
from app.core.security import get_current_user, require_tenant_admin
from app.core.logging import get_logger

logger = get_logger(__name__)
router = APIRouter()


@router.post("", response_model=DeviceResponse)
async def create_device(
    device_data: DeviceCreate,
    db: AsyncSession = Depends(get_db),
    current_user: TbUser = Depends(require_tenant_admin),
):
    """Create a new device"""
    device = Device(**device_data.model_dump(exclude={"device_profile_id"}))
    device.tenant_id = current_user.tenant_id
    device.device_profile_id = UUID(device_data.device_profile_id)

    if device_data.customer_id:
        device.customer_id = UUID(device_data.customer_id)

    device.update_search_text()

    db.add(device)
    await db.flush()

    # Create default access token credentials
    credentials = DeviceCredentials(
        device_id=device.id,
        credentials_type="ACCESS_TOKEN",
        credentials_id=secrets.token_urlsafe(20),
    )
    db.add(credentials)

    await db.commit()
    await db.refresh(device)

    logger.info(f"Device {device.name} created by {current_user.email}")

    return DeviceResponse.model_validate(device)


@router.get("/{device_id}", response_model=DeviceResponse)
async def get_device(
    device_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: TbUser = Depends(get_current_user),
):
    """Get device by ID"""
    result = await db.execute(select(Device).where(Device.id == device_id))
    device = result.scalar_one_or_none()

    if not device:
        raise HTTPException(status_code=404, detail="Device not found")

    # Check tenant access
    if current_user.authority != Authority.SYS_ADMIN and device.tenant_id != current_user.tenant_id:
        raise HTTPException(status_code=403, detail="Access denied")

    # Check customer access
    if current_user.authority == Authority.CUSTOMER_USER:
        if not device.customer_id or device.customer_id != current_user.customer_id:
            raise HTTPException(status_code=403, detail="Access denied")

    return DeviceResponse.model_validate(device)


@router.put("/{device_id}", response_model=DeviceResponse)
async def update_device(
    device_id: UUID,
    device_data: DeviceUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: TbUser = Depends(require_tenant_admin),
):
    """Update device"""
    result = await db.execute(select(Device).where(Device.id == device_id))
    device = result.scalar_one_or_none()

    if not device:
        raise HTTPException(status_code=404, detail="Device not found")

    # Check tenant access
    if current_user.authority != Authority.SYS_ADMIN and device.tenant_id != current_user.tenant_id:
        raise HTTPException(status_code=403, detail="Access denied")

    # Update fields
    for field, value in device_data.model_dump(exclude_unset=True).items():
        if field == "device_profile_id" and value:
            setattr(device, field, UUID(value))
        elif field == "customer_id" and value:
            setattr(device, field, UUID(value))
        else:
            setattr(device, field, value)

    device.update_search_text()

    await db.commit()
    await db.refresh(device)

    logger.info(f"Device {device.name} updated by {current_user.email}")

    return DeviceResponse.model_validate(device)


@router.delete("/{device_id}")
async def delete_device(
    device_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: TbUser = Depends(require_tenant_admin),
):
    """Delete device"""
    result = await db.execute(select(Device).where(Device.id == device_id))
    device = result.scalar_one_or_none()

    if not device:
        raise HTTPException(status_code=404, detail="Device not found")

    # Check tenant access
    if current_user.authority != Authority.SYS_ADMIN and device.tenant_id != current_user.tenant_id:
        raise HTTPException(status_code=403, detail="Access denied")

    await db.delete(device)
    await db.commit()

    logger.info(f"Device {device.name} deleted by {current_user.email}")

    return {"message": "Device deleted successfully"}


@router.get("", response_model=List[DeviceResponse])
async def list_devices(
    db: AsyncSession = Depends(get_db),
    current_user: TbUser = Depends(get_current_user),
    customer_id: str = None,
    device_type: str = None,
    limit: int = 100,
    offset: int = 0,
):
    """List devices"""
    query = select(Device).where(Device.tenant_id == current_user.tenant_id)

    # Filter by customer for customer users
    if current_user.authority == Authority.CUSTOMER_USER:
        query = query.where(Device.customer_id == current_user.customer_id)
    elif customer_id:
        query = query.where(Device.customer_id == UUID(customer_id))

    if device_type:
        query = query.where(Device.type == device_type)

    query = query.order_by(Device.created_time.desc()).limit(limit).offset(offset)

    result = await db.execute(query)
    devices = result.scalars().all()

    return [DeviceResponse.model_validate(d) for d in devices]


@router.get("/{device_id}/credentials", response_model=DeviceCredentialsResponse)
async def get_device_credentials(
    device_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: TbUser = Depends(require_tenant_admin),
):
    """Get device credentials"""
    result = await db.execute(
        select(DeviceCredentials).where(DeviceCredentials.device_id == device_id)
    )
    credentials = result.scalar_one_or_none()

    if not credentials:
        raise HTTPException(status_code=404, detail="Device credentials not found")

    # Check tenant access for the device
    result = await db.execute(select(Device).where(Device.id == device_id))
    device = result.scalar_one_or_none()

    if not device or (current_user.authority != Authority.SYS_ADMIN and device.tenant_id != current_user.tenant_id):
        raise HTTPException(status_code=403, detail="Access denied")

    return DeviceCredentialsResponse.model_validate(credentials)


@router.post("/{device_id}/credentials", response_model=DeviceCredentialsResponse)
async def update_device_credentials(
    device_id: UUID,
    credentials_data: DeviceCredentialsUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: TbUser = Depends(require_tenant_admin),
):
    """Update device credentials"""
    result = await db.execute(
        select(DeviceCredentials).where(DeviceCredentials.device_id == device_id)
    )
    credentials = result.scalar_one_or_none()

    if not credentials:
        raise HTTPException(status_code=404, detail="Device credentials not found")

    # Check tenant access
    result = await db.execute(select(Device).where(Device.id == device_id))
    device = result.scalar_one_or_none()

    if not device or (current_user.authority != Authority.SYS_ADMIN and device.tenant_id != current_user.tenant_id):
        raise HTTPException(status_code=403, detail="Access denied")

    # Update credentials
    for field, value in credentials_data.model_dump(exclude_unset=True).items():
        setattr(credentials, field, value)

    await db.commit()
    await db.refresh(credentials)

    logger.info(f"Device {device.name} credentials updated by {current_user.email}")

    return DeviceCredentialsResponse.model_validate(credentials)
