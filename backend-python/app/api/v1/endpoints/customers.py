"""
Customer endpoints
"""
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from typing import List
from uuid import UUID

from app.db.session import get_db
from app.models.customer import Customer
from app.models.user import TbUser, Authority
from app.schemas.customer import CustomerCreate, CustomerUpdate, CustomerResponse
from app.core.security import get_current_user, require_tenant_admin
from app.core.logging import get_logger

logger = get_logger(__name__)
router = APIRouter()


@router.post("", response_model=CustomerResponse)
async def create_customer(
    customer_data: CustomerCreate,
    db: AsyncSession = Depends(get_db),
    current_user: TbUser = Depends(require_tenant_admin),
):
    """Create a new customer"""
    customer = Customer(**customer_data.model_dump())
    customer.tenant_id = current_user.tenant_id
    customer.name = customer_data.title  # Set name same as title
    customer.update_search_text()

    db.add(customer)
    await db.commit()
    await db.refresh(customer)

    logger.info(f"Customer {customer.title} created by {current_user.email}")

    return CustomerResponse.model_validate(customer)


@router.get("/{customer_id}", response_model=CustomerResponse)
async def get_customer(
    customer_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: TbUser = Depends(get_current_user),
):
    """Get customer by ID"""
    result = await db.execute(select(Customer).where(Customer.id == customer_id))
    customer = result.scalar_one_or_none()

    if not customer:
        raise HTTPException(status_code=404, detail="Customer not found")

    # Check tenant access
    if current_user.authority != Authority.SYS_ADMIN and customer.tenant_id != current_user.tenant_id:
        raise HTTPException(status_code=403, detail="Access denied")

    return CustomerResponse.model_validate(customer)


@router.put("/{customer_id}", response_model=CustomerResponse)
async def update_customer(
    customer_id: UUID,
    customer_data: CustomerUpdate,
    db: AsyncSession = Depends(get_db),
    current_user: TbUser = Depends(require_tenant_admin),
):
    """Update customer"""
    result = await db.execute(select(Customer).where(Customer.id == customer_id))
    customer = result.scalar_one_or_none()

    if not customer:
        raise HTTPException(status_code=404, detail="Customer not found")

    # Check tenant access
    if current_user.authority != Authority.SYS_ADMIN and customer.tenant_id != current_user.tenant_id:
        raise HTTPException(status_code=403, detail="Access denied")

    # Update fields
    for field, value in customer_data.model_dump(exclude_unset=True).items():
        if field == "title":
            customer.name = value  # Update name when title changes
        setattr(customer, field, value)

    customer.update_search_text()

    await db.commit()
    await db.refresh(customer)

    logger.info(f"Customer {customer.title} updated by {current_user.email}")

    return CustomerResponse.model_validate(customer)


@router.delete("/{customer_id}")
async def delete_customer(
    customer_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: TbUser = Depends(require_tenant_admin),
):
    """Delete customer"""
    result = await db.execute(select(Customer).where(Customer.id == customer_id))
    customer = result.scalar_one_or_none()

    if not customer:
        raise HTTPException(status_code=404, detail="Customer not found")

    # Check tenant access
    if current_user.authority != Authority.SYS_ADMIN and customer.tenant_id != current_user.tenant_id:
        raise HTTPException(status_code=403, detail="Access denied")

    await db.delete(customer)
    await db.commit()

    logger.info(f"Customer {customer.title} deleted by {current_user.email}")

    return {"message": "Customer deleted successfully"}


@router.get("", response_model=List[CustomerResponse])
async def list_customers(
    db: AsyncSession = Depends(get_db),
    current_user: TbUser = Depends(require_tenant_admin),
    limit: int = 100,
    offset: int = 0,
):
    """List customers for current tenant"""
    query = select(Customer).where(Customer.tenant_id == current_user.tenant_id)
    query = query.order_by(Customer.created_time.desc()).limit(limit).offset(offset)

    result = await db.execute(query)
    customers = result.scalars().all()

    return [CustomerResponse.model_validate(c) for c in customers]
