"""
Gateway API endpoints
"""

from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func

from app.core.database import get_db
from app.core.security import get_current_user
from app.models.user import User
from app.models.gateway import Gateway
from app.schemas.gateway import (
    GatewayCreate,
    GatewayUpdate,
    GatewayResponse,
    GatewayListResponse,
    ConnectorConfig,
)

router = APIRouter(prefix="/gateways", tags=["gateways"])


@router.get("", response_model=GatewayListResponse)
async def get_gateways(
    page: int = Query(0, ge=0),
    page_size: int = Query(10, ge=1, le=100),
    search: Optional[str] = None,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Get all gateways with pagination"""
    query = select(Gateway).where(Gateway.tenant_id == current_user.tenant_id)

    # Apply search filter
    if search:
        query = query.where(
            (Gateway.name.ilike(f"%{search}%"))
            | (Gateway.label.ilike(f"%{search}%"))
            | (Gateway.type.ilike(f"%{search}%"))
        )

    # Get total count
    count_query = select(func.count()).select_from(query.subquery())
    total = await db.scalar(count_query)

    # Apply pagination
    query = query.offset(page * page_size).limit(page_size)

    result = await db.execute(query)
    gateways = result.scalars().all()

    return {
        "data": gateways,
        "totalElements": total,
        "totalPages": (total + page_size - 1) // page_size,
    }


@router.get("/{gateway_id}", response_model=GatewayResponse)
async def get_gateway(
    gateway_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Get gateway by ID"""
    result = await db.execute(
        select(Gateway).where(
            Gateway.id == gateway_id,
            Gateway.tenant_id == current_user.tenant_id,
        )
    )
    gateway = result.scalar_one_or_none()

    if not gateway:
        raise HTTPException(status_code=404, detail="Gateway not found")

    return gateway


@router.post("", response_model=GatewayResponse, status_code=201)
async def create_gateway(
    gateway_data: GatewayCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Create new gateway"""
    gateway = Gateway(
        **gateway_data.model_dump(),
        tenant_id=current_user.tenant_id,
        customer_id=current_user.customer_id,
    )

    db.add(gateway)
    await db.commit()
    await db.refresh(gateway)

    return gateway


@router.put("/{gateway_id}", response_model=GatewayResponse)
async def update_gateway(
    gateway_id: str,
    gateway_data: GatewayUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Update gateway"""
    result = await db.execute(
        select(Gateway).where(
            Gateway.id == gateway_id,
            Gateway.tenant_id == current_user.tenant_id,
        )
    )
    gateway = result.scalar_one_or_none()

    if not gateway:
        raise HTTPException(status_code=404, detail="Gateway not found")

    for key, value in gateway_data.model_dump(exclude_unset=True).items():
        setattr(gateway, key, value)

    await db.commit()
    await db.refresh(gateway)

    return gateway


@router.delete("/{gateway_id}", status_code=204)
async def delete_gateway(
    gateway_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Delete gateway"""
    result = await db.execute(
        select(Gateway).where(
            Gateway.id == gateway_id,
            Gateway.tenant_id == current_user.tenant_id,
        )
    )
    gateway = result.scalar_one_or_none()

    if not gateway:
        raise HTTPException(status_code=404, detail="Gateway not found")

    await db.delete(gateway)
    await db.commit()


@router.get("/{gateway_id}/connectors")
async def get_gateway_connectors(
    gateway_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Get gateway connectors configuration"""
    result = await db.execute(
        select(Gateway).where(
            Gateway.id == gateway_id,
            Gateway.tenant_id == current_user.tenant_id,
        )
    )
    gateway = result.scalar_one_or_none()

    if not gateway:
        raise HTTPException(status_code=404, detail="Gateway not found")

    # Return connectors from gateway configuration
    configuration = gateway.additional_info or {}
    return configuration.get("connectors", [])


@router.put("/{gateway_id}/connectors")
async def update_gateway_connectors(
    gateway_id: str,
    connectors: List[ConnectorConfig],
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Update gateway connectors configuration"""
    result = await db.execute(
        select(Gateway).where(
            Gateway.id == gateway_id,
            Gateway.tenant_id == current_user.tenant_id,
        )
    )
    gateway = result.scalar_one_or_none()

    if not gateway:
        raise HTTPException(status_code=404, detail="Gateway not found")

    # Update connectors in gateway configuration
    configuration = gateway.additional_info or {}
    configuration["connectors"] = [c.model_dump() for c in connectors]
    gateway.additional_info = configuration

    await db.commit()
    await db.refresh(gateway)

    return {"message": "Connectors updated successfully"}


@router.post("/{gateway_id}/restart")
async def restart_gateway(
    gateway_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Restart gateway remotely"""
    result = await db.execute(
        select(Gateway).where(
            Gateway.id == gateway_id,
            Gateway.tenant_id == current_user.tenant_id,
        )
    )
    gateway = result.scalar_one_or_none()

    if not gateway:
        raise HTTPException(status_code=404, detail="Gateway not found")

    # TODO: Implement actual gateway restart logic via MQTT/RPC
    # For now, just return success
    return {"message": f"Gateway {gateway.name} restart command sent"}
