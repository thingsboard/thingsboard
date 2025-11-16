"""
Telemetry endpoints - HIGH PRIORITY
Handles device telemetry data and attributes
"""
from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_
from typing import List, Dict, Any, Optional
from uuid import UUID
from datetime import datetime

from app.db.session import get_db
from app.models.device import Device
from app.models.telemetry import AttributeKv, TsKvLatest, AttributeScope
from app.models.user import TbUser, Authority
from app.schemas.telemetry import (
    AttributeRequest,
    AttributeResponse,
    TelemetryResponse,
    LatestTimeseriesResponse,
    TimeseriesKeysResponse,
)
from app.core.security import get_current_user
from app.core.logging import get_logger

logger = get_logger(__name__)
router = APIRouter()


# Helper function to check entity access
async def check_entity_access(entity_type: str, entity_id: UUID, current_user: TbUser, db: AsyncSession):
    """Check if user has access to entity"""
    if entity_type.upper() == "DEVICE":
        result = await db.execute(select(Device).where(Device.id == entity_id))
        entity = result.scalar_one_or_none()

        if not entity:
            raise HTTPException(status_code=404, detail="Entity not found")

        # Check tenant access
        if current_user.authority != Authority.SYS_ADMIN and entity.tenant_id != current_user.tenant_id:
            raise HTTPException(status_code=403, detail="Access denied")

        # Check customer access
        if current_user.authority == Authority.CUSTOMER_USER:
            if not entity.customer_id or entity.customer_id != current_user.customer_id:
                raise HTTPException(status_code=403, detail="Access denied")

        return entity
    else:
        # Support for other entity types (Asset, etc.) can be added here
        raise HTTPException(status_code=400, detail=f"Unsupported entity type: {entity_type}")


@router.post("/{entity_type}/{entity_id}/timeseries/{scope}")
async def save_entity_telemetry(
    entity_type: str,
    entity_id: UUID,
    scope: str,
    telemetry_data: Dict[str, Any],
    db: AsyncSession = Depends(get_db),
    current_user: TbUser = Depends(get_current_user),
):
    """
    Save telemetry data for an entity
    Body format: {
        "temperature": 25.5,
        "humidity": 60,
        "status": "online"
    }
    OR with timestamps:
    {
        "temperature": [{"ts": 1234567890000, "value": 25.5}]
    }
    """
    await check_entity_access(entity_type, entity_id, current_user, db)

    current_ts = int(datetime.utcnow().timestamp() * 1000)

    # Process telemetry data
    for key, value in telemetry_data.items():
        # Handle both simple values and time-series arrays
        if isinstance(value, list):
            # Time-series format: [{"ts": ..., "value": ...}]
            for entry in value:
                ts = entry.get("ts", current_ts)
                val = entry.get("value")
                await save_ts_value(db, entity_id, key, ts, val)
        else:
            # Simple format: {"key": value}
            await save_ts_value(db, entity_id, key, current_ts, value)

    await db.commit()

    logger.info(f"Telemetry saved for {entity_type} {entity_id}")

    return {"status": "ok"}


async def save_ts_value(db: AsyncSession, entity_id: UUID, key: str, ts: int, value: Any):
    """Save a single telemetry value"""
    # Update or insert latest value
    result = await db.execute(
        select(TsKvLatest).where(
            and_(TsKvLatest.entity_id == entity_id, TsKvLatest.key == key)
        )
    )
    latest = result.scalar_one_or_none()

    if latest:
        # Update if newer
        if ts >= latest.ts:
            latest.ts = ts
            _set_polymorphic_value(latest, value)
    else:
        # Create new
        latest = TsKvLatest(entity_id=entity_id, key=key, ts=ts)
        _set_polymorphic_value(latest, value)
        db.add(latest)

    # In production, also save to Cassandra/TimescaleDB for historical data
    # This would be handled by a separate service or background task


def _set_polymorphic_value(obj, value):
    """Set value on polymorphic storage object"""
    # Reset all value fields
    obj.bool_v = None
    obj.str_v = None
    obj.long_v = None
    obj.dbl_v = None
    obj.json_v = None

    # Set appropriate field based on type
    if isinstance(value, bool):
        obj.bool_v = value
    elif isinstance(value, int):
        obj.long_v = value
    elif isinstance(value, float):
        obj.dbl_v = value
    elif isinstance(value, str):
        obj.str_v = value
    elif isinstance(value, dict) or isinstance(value, list):
        obj.json_v = value


def _get_polymorphic_value(obj):
    """Get value from polymorphic storage object"""
    if obj.bool_v is not None:
        return obj.bool_v
    elif obj.long_v is not None:
        return obj.long_v
    elif obj.dbl_v is not None:
        return obj.dbl_v
    elif obj.str_v is not None:
        return obj.str_v
    elif obj.json_v is not None:
        return obj.json_v
    return None


@router.get("/{entity_type}/{entity_id}/values/timeseries")
async def get_latest_telemetry(
    entity_type: str,
    entity_id: UUID,
    keys: Optional[str] = Query(None, description="Comma-separated list of keys"),
    db: AsyncSession = Depends(get_db),
    current_user: TbUser = Depends(get_current_user),
) -> Dict[str, Any]:
    """Get latest telemetry values for an entity"""
    await check_entity_access(entity_type, entity_id, current_user, db)

    query = select(TsKvLatest).where(TsKvLatest.entity_id == entity_id)

    if keys:
        key_list = [k.strip() for k in keys.split(",")]
        query = query.where(TsKvLatest.key.in_(key_list))

    result = await db.execute(query)
    latest_values = result.scalars().all()

    # Format response
    data = {}
    for ts_kv in latest_values:
        data[ts_kv.key] = [
            {
                "ts": ts_kv.ts,
                "value": _get_polymorphic_value(ts_kv),
            }
        ]

    return {"data": data}


@router.get("/{entity_type}/{entity_id}/keys/timeseries", response_model=TimeseriesKeysResponse)
async def get_timeseries_keys(
    entity_type: str,
    entity_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: TbUser = Depends(get_current_user),
):
    """Get available telemetry keys for an entity"""
    await check_entity_access(entity_type, entity_id, current_user, db)

    result = await db.execute(
        select(TsKvLatest.key).where(TsKvLatest.entity_id == entity_id).distinct()
    )
    keys = [row[0] for row in result.all()]

    return TimeseriesKeysResponse(keys=keys)


@router.post("/{entity_type}/{entity_id}/attributes/{scope}")
async def save_entity_attributes(
    entity_type: str,
    entity_id: UUID,
    scope: str,
    attributes: Dict[str, Any],
    db: AsyncSession = Depends(get_db),
    current_user: TbUser = Depends(get_current_user),
):
    """
    Save entity attributes
    Scope can be: SERVER_SCOPE, SHARED_SCOPE, CLIENT_SCOPE
    """
    await check_entity_access(entity_type, entity_id, current_user, db)

    # Validate scope
    try:
        attribute_scope = AttributeScope(scope)
    except ValueError:
        raise HTTPException(status_code=400, detail=f"Invalid scope: {scope}")

    current_ts = int(datetime.utcnow().timestamp() * 1000)

    # Save each attribute
    for key, value in attributes.items():
        result = await db.execute(
            select(AttributeKv).where(
                and_(
                    AttributeKv.entity_id == entity_id,
                    AttributeKv.attribute_type == scope,
                    AttributeKv.attribute_key == key,
                )
            )
        )
        attr = result.scalar_one_or_none()

        if attr:
            # Update existing
            attr.last_update_ts = current_ts
            _set_polymorphic_value(attr, value)
        else:
            # Create new
            attr = AttributeKv(
                entity_id=entity_id,
                attribute_type=scope,
                attribute_key=key,
                last_update_ts=current_ts,
            )
            _set_polymorphic_value(attr, value)
            db.add(attr)

    await db.commit()

    logger.info(f"Attributes saved for {entity_type} {entity_id}, scope: {scope}")

    return {"status": "ok"}


@router.get("/{entity_type}/{entity_id}/attributes")
async def get_entity_attributes(
    entity_type: str,
    entity_id: UUID,
    keys: Optional[str] = Query(None, description="Comma-separated list of keys"),
    db: AsyncSession = Depends(get_db),
    current_user: TbUser = Depends(get_current_user),
) -> Dict[str, Any]:
    """Get entity attributes (all scopes)"""
    await check_entity_access(entity_type, entity_id, current_user, db)

    query = select(AttributeKv).where(AttributeKv.entity_id == entity_id)

    if keys:
        key_list = [k.strip() for k in keys.split(",")]
        query = query.where(AttributeKv.attribute_key.in_(key_list))

    result = await db.execute(query)
    attributes = result.scalars().all()

    # Group by scope
    response = {
        "client": {},
        "server": {},
        "shared": {},
    }

    for attr in attributes:
        scope_key = attr.attribute_type.replace("_SCOPE", "").lower()
        if scope_key in response:
            response[scope_key][attr.attribute_key] = {
                "lastUpdateTs": attr.last_update_ts,
                "value": _get_polymorphic_value(attr),
            }

    return response


@router.delete("/{entity_type}/{entity_id}/attributes/{scope}")
async def delete_entity_attributes(
    entity_type: str,
    entity_id: UUID,
    scope: str,
    keys: str = Query(..., description="Comma-separated list of keys to delete"),
    db: AsyncSession = Depends(get_db),
    current_user: TbUser = Depends(require_tenant_admin),
):
    """Delete entity attributes"""
    await check_entity_access(entity_type, entity_id, current_user, db)

    key_list = [k.strip() for k in keys.split(",")]

    result = await db.execute(
        select(AttributeKv).where(
            and_(
                AttributeKv.entity_id == entity_id,
                AttributeKv.attribute_type == scope,
                AttributeKv.attribute_key.in_(key_list),
            )
        )
    )
    attributes = result.scalars().all()

    for attr in attributes:
        await db.delete(attr)

    await db.commit()

    logger.info(f"Attributes deleted for {entity_type} {entity_id}")

    return {"status": "ok"}


# Import missing dependency
from app.core.security import require_tenant_admin
