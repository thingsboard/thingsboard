"""
Authentication schemas
"""
from pydantic import BaseModel, EmailStr, Field
from typing import Optional
from app.models.user import Authority


class LoginRequest(BaseModel):
    """Login request"""
    username: EmailStr
    password: str = Field(..., min_length=4)


class TokenResponse(BaseModel):
    """Token response"""
    token: str
    refresh_token: str
    token_type: str = "Bearer"


class RefreshTokenRequest(BaseModel):
    """Refresh token request"""
    refresh_token: str


class UserInfo(BaseModel):
    """User information response"""
    id: str
    email: str
    first_name: Optional[str] = None
    last_name: Optional[str] = None
    authority: Authority
    tenant_id: Optional[str] = None
    customer_id: Optional[str] = None

    class Config:
        from_attributes = True


class ChangePasswordRequest(BaseModel):
    """Change password request"""
    current_password: str
    new_password: str = Field(..., min_length=8)


class ResetPasswordRequest(BaseModel):
    """Reset password request"""
    email: EmailStr


class ActivateUserRequest(BaseModel):
    """Activate user account request"""
    activate_token: str
    password: str = Field(..., min_length=8)
