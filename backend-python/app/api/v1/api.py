"""
API v1 router aggregation
"""
from fastapi import APIRouter
from app.api.v1.endpoints import (
    auth,
    tenants,
    customers,
    devices,
    telemetry,
    assets,
    dashboards,
    users,
    alarms,
    rule_chains,
)

api_router = APIRouter()

# Authentication
api_router.include_router(auth.router, prefix="/auth", tags=["auth"])

# Tenants
api_router.include_router(tenants.router, prefix="/tenants", tags=["tenants"])

# Customers
api_router.include_router(customers.router, prefix="/customers", tags=["customers"])

# Users
api_router.include_router(users.router, prefix="/users", tags=["users"])

# Devices
api_router.include_router(devices.router, prefix="/devices", tags=["devices"])

# Assets
api_router.include_router(assets.router, prefix="/assets", tags=["assets"])

# Telemetry
api_router.include_router(telemetry.router, prefix="/telemetry", tags=["telemetry"])

# Dashboards
api_router.include_router(dashboards.router, prefix="/dashboards", tags=["dashboards"])

# Alarms
api_router.include_router(alarms.router, prefix="/alarms", tags=["alarms"])

# Rule Chains
api_router.include_router(rule_chains.router, prefix="/ruleChains", tags=["rule-chains"])
