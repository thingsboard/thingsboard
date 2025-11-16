"""
Asset API endpoints
CRUD operations for assets
"""

from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func, or_
from uuid import uuid4

from app.core.database import get_db
from app.core.security import get_current_user
from app.models.user import User
from app.models.asset import Asset
from app.schemas.asset import (
    AssetCreate,
    AssetUpdate,
    AssetResponse,
    AssetListResponse,
)

router = APIRouter(prefix="/assets", tags=["assets"])


@router.get("", response_model=AssetListResponse)
async def get_assets(
    page: int = Query(0, ge=0),
    page_size: int = Query(10, ge=1, le=100),
    search: Optional[str] = None,
    asset_type: Optional[str] = None,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Get all assets with pagination and filtering"""
    query = select(Asset).where(Asset.tenant_id == current_user.tenant_id)

    # Apply filters
    if search:
        query = query.where(
            or_(
                Asset.name.ilike(f"%{search}%"),
                Asset.label.ilike(f"%{search}%"),
                Asset.type.ilike(f"%{search}%"),
            )
        )

    if asset_type:
        query = query.where(Asset.type == asset_type)

    # Filter by customer if user is customer user
    if current_user.authority == "CUSTOMER_USER" and current_user.customer_id:
        query = query.where(Asset.customer_id == current_user.customer_id)

    # Get total count
    count_query = select(func.count()).select_from(query.subquery())
    total = await db.scalar(count_query)

    # Apply pagination and sorting
    query = query.order_by(Asset.created_time.desc()).offset(page * page_size).limit(page_size)

    result = await db.execute(query)
    assets = result.scalars().all()

    return {
        "data": assets,
        "totalElements": total or 0,
        "totalPages": ((total or 0) + page_size - 1) // page_size,
    }


@router.get("/{asset_id}", response_model=AssetResponse)
async def get_asset(
    asset_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Get asset by ID"""
    query = select(Asset).where(
        Asset.id == asset_id,
        Asset.tenant_id == current_user.tenant_id,
    )

    # Customer user can only see their own assets
    if current_user.authority == "CUSTOMER_USER" and current_user.customer_id:
        query = query.where(Asset.customer_id == current_user.customer_id)

    result = await db.execute(query)
    asset = result.scalar_one_or_none()

    if not asset:
        raise HTTPException(status_code=404, detail="Asset not found")

    return asset


@router.post("", response_model=AssetResponse, status_code=status.HTTP_201_CREATED)
async def create_asset(
    asset_in: AssetCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Create a new asset"""
    # Check if asset name already exists
    existing = await db.execute(
        select(Asset).where(
            Asset.name == asset_in.name,
            Asset.tenant_id == current_user.tenant_id,
        )
    )
    if existing.scalar_one_or_none():
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=f"Asset with name '{asset_in.name}' already exists",
        )

    # Create asset
    asset = Asset(
        id=str(uuid4()),
        tenant_id=current_user.tenant_id,
        name=asset_in.name,
        type=asset_in.type or "default",
        label=asset_in.label,
        customer_id=asset_in.customer_id,
        asset_profile_id=asset_in.asset_profile_id,
        additional_info=asset_in.additional_info,
    )

    db.add(asset)
    await db.commit()
    await db.refresh(asset)

    return asset


@router.put("/{asset_id}", response_model=AssetResponse)
async def update_asset(
    asset_id: str,
    asset_in: AssetUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Update asset"""
    result = await db.execute(
        select(Asset).where(
            Asset.id == asset_id,
            Asset.tenant_id == current_user.tenant_id,
        )
    )
    asset = result.scalar_one_or_none()

    if not asset:
        raise HTTPException(status_code=404, detail="Asset not found")

    # Update fields
    update_data = asset_in.dict(exclude_unset=True)
    for field, value in update_data.items():
        setattr(asset, field, value)

    await db.commit()
    await db.refresh(asset)

    return asset


@router.delete("/{asset_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_asset(
    asset_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Delete asset"""
    result = await db.execute(
        select(Asset).where(
            Asset.id == asset_id,
            Asset.tenant_id == current_user.tenant_id,
        )
    )
    asset = result.scalar_one_or_none()

    if not asset:
        raise HTTPException(status_code=404, detail="Asset not found")

    await db.delete(asset)
    await db.commit()

    return None


@router.post("/{asset_id}/assign/{customer_id}", response_model=AssetResponse)
async def assign_asset_to_customer(
    asset_id: str,
    customer_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Assign asset to customer"""
    result = await db.execute(
        select(Asset).where(
            Asset.id == asset_id,
            Asset.tenant_id == current_user.tenant_id,
        )
    )
    asset = result.scalar_one_or_none()

    if not asset:
        raise HTTPException(status_code=404, detail="Asset not found")

    asset.customer_id = customer_id
    await db.commit()
    await db.refresh(asset)

    return asset


@router.delete("/{asset_id}/unassign", response_model=AssetResponse)
async def unassign_asset_from_customer(
    asset_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Unassign asset from customer"""
    result = await db.execute(
        select(Asset).where(
            Asset.id == asset_id,
            Asset.tenant_id == current_user.tenant_id,
        )
    )
    asset = result.scalar_one_or_none()

    if not asset:
        raise HTTPException(status_code=404, detail="Asset not found")

    asset.customer_id = None
    await db.commit()
    await db.refresh(asset)

    return asset
