"""
Customer API endpoints
CRUD operations for customers
"""

from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func, or_
from uuid import uuid4

from app.core.database import get_db
from app.core.security import get_current_user
from app.models.user import User
from app.models.customer import Customer
from app.schemas.customer import (
    CustomerCreate,
    CustomerUpdate,
    CustomerResponse,
    CustomerListResponse,
)

router = APIRouter(prefix="/customers", tags=["customers"])


@router.get("", response_model=CustomerListResponse)
async def get_customers(
    page: int = Query(0, ge=0),
    page_size: int = Query(10, ge=1, le=100),
    search: Optional[str] = None,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Get all customers with pagination and filtering"""
    # Only tenant admins can manage customers
    if current_user.authority not in ["SYS_ADMIN", "TENANT_ADMIN"]:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only tenant administrators can access customers",
        )

    query = select(Customer).where(Customer.tenant_id == current_user.tenant_id)

    # Apply search filter
    if search:
        query = query.where(
            or_(
                Customer.title.ilike(f"%{search}%"),
                Customer.email.ilike(f"%{search}%"),
                Customer.country.ilike(f"%{search}%"),
                Customer.city.ilike(f"%{search}%"),
            )
        )

    # Get total count
    count_query = select(func.count()).select_from(query.subquery())
    total = await db.scalar(count_query)

    # Apply pagination and sorting
    query = query.order_by(Customer.created_time.desc()).offset(page * page_size).limit(page_size)

    result = await db.execute(query)
    customers = result.scalars().all()

    return {
        "data": customers,
        "totalElements": total or 0,
        "totalPages": ((total or 0) + page_size - 1) // page_size,
    }


@router.get("/{customer_id}", response_model=CustomerResponse)
async def get_customer(
    customer_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Get customer by ID"""
    query = select(Customer).where(
        Customer.id == customer_id,
        Customer.tenant_id == current_user.tenant_id,
    )

    result = await db.execute(query)
    customer = result.scalar_one_or_none()

    if not customer:
        raise HTTPException(status_code=404, detail="Customer not found")

    return customer


@router.post("", response_model=CustomerResponse, status_code=status.HTTP_201_CREATED)
async def create_customer(
    customer_in: CustomerCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Create a new customer"""
    # Only tenant admins can create customers
    if current_user.authority not in ["SYS_ADMIN", "TENANT_ADMIN"]:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only tenant administrators can create customers",
        )

    # Check if customer title already exists
    existing = await db.execute(
        select(Customer).where(
            Customer.title == customer_in.title,
            Customer.tenant_id == current_user.tenant_id,
        )
    )
    if existing.scalar_one_or_none():
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=f"Customer with title '{customer_in.title}' already exists",
        )

    # Create customer
    customer = Customer(
        id=str(uuid4()),
        tenant_id=current_user.tenant_id,
        title=customer_in.title,
        email=customer_in.email,
        phone=customer_in.phone,
        country=customer_in.country,
        state=customer_in.state,
        city=customer_in.city,
        address=customer_in.address,
        address2=customer_in.address2,
        zip=customer_in.zip,
        additional_info=customer_in.additional_info,
    )

    db.add(customer)
    await db.commit()
    await db.refresh(customer)

    return customer


@router.put("/{customer_id}", response_model=CustomerResponse)
async def update_customer(
    customer_id: str,
    customer_in: CustomerUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Update customer"""
    # Only tenant admins can update customers
    if current_user.authority not in ["SYS_ADMIN", "TENANT_ADMIN"]:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only tenant administrators can update customers",
        )

    result = await db.execute(
        select(Customer).where(
            Customer.id == customer_id,
            Customer.tenant_id == current_user.tenant_id,
        )
    )
    customer = result.scalar_one_or_none()

    if not customer:
        raise HTTPException(status_code=404, detail="Customer not found")

    # Update fields
    update_data = customer_in.dict(exclude_unset=True)
    for field, value in update_data.items():
        setattr(customer, field, value)

    await db.commit()
    await db.refresh(customer)

    return customer


@router.delete("/{customer_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_customer(
    customer_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Delete customer"""
    # Only tenant admins can delete customers
    if current_user.authority not in ["SYS_ADMIN", "TENANT_ADMIN"]:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only tenant administrators can delete customers",
        )

    result = await db.execute(
        select(Customer).where(
            Customer.id == customer_id,
            Customer.tenant_id == current_user.tenant_id,
        )
    )
    customer = result.scalar_one_or_none()

    if not customer:
        raise HTTPException(status_code=404, detail="Customer not found")

    # Check if customer has devices or assets
    # TODO: Add check for associated devices/assets/users

    await db.delete(customer)
    await db.commit()

    return None


@router.get("/{customer_id}/users", response_model=List[dict])
async def get_customer_users(
    customer_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Get all users for a customer"""
    # Verify customer exists and user has access
    result = await db.execute(
        select(Customer).where(
            Customer.id == customer_id,
            Customer.tenant_id == current_user.tenant_id,
        )
    )
    customer = result.scalar_one_or_none()

    if not customer:
        raise HTTPException(status_code=404, detail="Customer not found")

    # TODO: Query users for this customer
    # For now, return empty list
    return []


@router.get("/{customer_id}/devices", response_model=List[dict])
async def get_customer_devices(
    customer_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Get all devices assigned to a customer"""
    # Verify customer exists and user has access
    result = await db.execute(
        select(Customer).where(
            Customer.id == customer_id,
            Customer.tenant_id == current_user.tenant_id,
        )
    )
    customer = result.scalar_one_or_none()

    if not customer:
        raise HTTPException(status_code=404, detail="Customer not found")

    # TODO: Query devices for this customer
    # For now, return empty list
    return []
