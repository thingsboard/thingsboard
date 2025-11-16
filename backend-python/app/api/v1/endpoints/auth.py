"""
Authentication endpoints
"""
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from datetime import timedelta

from app.db.session import get_db
from app.models.user import TbUser, UserCredentials
from app.schemas.auth import (
    LoginRequest,
    TokenResponse,
    RefreshTokenRequest,
    UserInfo,
    ChangePasswordRequest,
)
from app.core.security import (
    verify_password,
    get_password_hash,
    create_access_token,
    create_refresh_token,
    decode_token,
    get_current_user,
)
from app.core.config import settings
from app.core.logging import get_logger

logger = get_logger(__name__)
router = APIRouter()


@router.post("/login", response_model=TokenResponse)
async def login(
    login_data: LoginRequest,
    db: AsyncSession = Depends(get_db),
):
    """
    Login endpoint
    Returns JWT access token and refresh token
    """
    # Find user by email
    result = await db.execute(
        select(TbUser).where(TbUser.email == login_data.username)
    )
    user = result.scalar_one_or_none()

    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect username or password",
        )

    # Check credentials
    result = await db.execute(
        select(UserCredentials).where(UserCredentials.user_id == user.id)
    )
    credentials = result.scalar_one_or_none()

    if not credentials or not credentials.enabled:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User account is disabled",
        )

    if not verify_password(login_data.password, credentials.password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect username or password",
        )

    # Create tokens
    access_token = create_access_token(
        data={"sub": str(user.id), "email": user.email, "authority": user.authority.value}
    )
    refresh_token = create_refresh_token(
        data={"sub": str(user.id)}
    )

    logger.info(f"User {user.email} logged in successfully")

    return TokenResponse(
        token=access_token,
        refresh_token=refresh_token,
    )


@router.post("/token/refresh", response_model=TokenResponse)
async def refresh_token(
    refresh_data: RefreshTokenRequest,
    db: AsyncSession = Depends(get_db),
):
    """
    Refresh access token using refresh token
    """
    payload = decode_token(refresh_data.refresh_token)

    if payload.get("type") != "refresh":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid refresh token",
        )

    user_id = payload.get("sub")
    if not user_id:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid refresh token",
        )

    # Get user
    result = await db.execute(select(TbUser).where(TbUser.id == user_id))
    user = result.scalar_one_or_none()

    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User not found",
        )

    # Create new tokens
    access_token = create_access_token(
        data={"sub": str(user.id), "email": user.email, "authority": user.authority.value}
    )
    new_refresh_token = create_refresh_token(
        data={"sub": str(user.id)}
    )

    return TokenResponse(
        token=access_token,
        refresh_token=new_refresh_token,
    )


@router.get("/user", response_model=UserInfo)
async def get_current_user_info(
    current_user: TbUser = Depends(get_current_user),
):
    """
    Get current authenticated user information
    """
    return UserInfo(
        id=str(current_user.id),
        email=current_user.email,
        first_name=current_user.first_name,
        last_name=current_user.last_name,
        authority=current_user.authority,
        tenant_id=str(current_user.tenant_id) if current_user.tenant_id else None,
        customer_id=str(current_user.customer_id) if current_user.customer_id else None,
    )


@router.post("/changePassword")
async def change_password(
    password_data: ChangePasswordRequest,
    current_user: TbUser = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """
    Change current user's password
    """
    # Get user credentials
    result = await db.execute(
        select(UserCredentials).where(UserCredentials.user_id == current_user.id)
    )
    credentials = result.scalar_one_or_none()

    if not credentials:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User credentials not found",
        )

    # Verify current password
    if not verify_password(password_data.current_password, credentials.password):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Current password is incorrect",
        )

    # Update password
    credentials.password = get_password_hash(password_data.new_password)
    await db.commit()

    logger.info(f"User {current_user.email} changed password")

    return {"message": "Password changed successfully"}


@router.post("/logout")
async def logout(
    current_user: TbUser = Depends(get_current_user),
):
    """
    Logout endpoint (client should discard tokens)
    """
    logger.info(f"User {current_user.email} logged out")
    return {"message": "Logged out successfully"}
