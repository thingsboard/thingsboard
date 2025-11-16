"""
Tenant endpoints
"""
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from typing import List
from uuid import UUID

from app.db.session import get_db
from app.models.tenant import Tenant
from app.models.user import TbUser
from app.schemas.tenant import TenantCreate, TenantUpdate, TenantResponse
from app.core.security import get_current_user, require_sys_admin
from app.core.logging import get_logger

logger = get_logger(__name__)
router = APIRouter()


@router.post("", response_model=TenantResponse, dependencies=[Depends(require_sys_admin)])
async def create_tenant(
    tenant_data: TenantCreate,
    db: AsyncSession = Depends(get_db),
    current_user: TbUser = Depends(get_current_user),
):
    """Create a new tenant (sys admin only)"""
    tenant = Tenant(**tenant_data.model_dump())
    tenant.update_search_text()

    db.add(tenant)
    await db.commit()
    await db.refresh(tenant)

    logger.info(f"Tenant {tenant.name} created by {current_user.email}")

    return TenantResponse.model_validate(tenant)


@router.get("/{tenant_id}", response_model=TenantResponse)
async def get_tenant(
    tenant_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: TbUser = Depends(require_sys_admin),
):
    """Get tenant by ID"""
    result = await db.execute(select(Tenant).where(Tenant.id == tenant_id))
    tenant = result.scalar_one_or_none()

    if not tenant:
        raise HTTPException(status_code=404, detail="Tenant not found")

    return TenantResponse.model_validate(tenant)


@router.put("/{tenant_id}", response_model=TenantResponse)
async def update_tenant(
    tenant_id: UUID,
    tenant_data: TenantUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: TbUser = Depends(require_sys_admin),
):
    """Update tenant"""
    result = await db.execute(select(Tenant).where(Tenant.id == tenant_id))
    tenant = result.scalar_one_or_none()

    if not tenant:
        raise HTTPException(status_code=404, detail="Tenant not found")

    # Update fields
    for field, value in tenant_data.model_dump(exclude_unset=True).items():
        setattr(tenant, field, value)

    tenant.update_search_text()

    await db.commit()
    await db.refresh(tenant)

    logger.info(f"Tenant {tenant.name} updated by {current_user.email}")

    return TenantResponse.model_validate(tenant)


@router.delete("/{tenant_id}")
async def delete_tenant(
    tenant_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: TbUser = Depends(require_sys_admin),
):
    """Delete tenant"""
    result = await db.execute(select(Tenant).where(Tenant.id == tenant_id))
    tenant = result.scalar_one_or_none()

    if not tenant:
        raise HTTPException(status_code=404, detail="Tenant not found")

    await db.delete(tenant)
    await db.commit()

    logger.info(f"Tenant {tenant.name} deleted by {current_user.email}")

    return {"message": "Tenant deleted successfully"}


@router.get("", response_model=List[TenantResponse])
async def list_tenants(
    db: AsyncSession = Depends(get_db),
    current_user: TbUser = Depends(require_sys_admin),
    limit: int = 100,
    offset: int = 0,
):
    """List all tenants (paginated)"""
    result = await db.execute(
        select(Tenant).order_by(Tenant.created_time.desc()).limit(limit).offset(offset)
    )
    tenants = result.scalars().all()

    return [TenantResponse.model_validate(t) for t in tenants]
