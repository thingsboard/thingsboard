"""
Device authentication service
"""
import aiohttp
import asyncio
from typing import Optional, Dict
from datetime import datetime, timedelta
from app.core.config import settings
from app.core.logging_config import get_logger

logger = get_logger(__name__)


class DeviceAuthService:
    """
    Handles device authentication and credential validation
    Caches validated devices to reduce backend API calls
    """

    def __init__(self):
        self.cache: Dict[str, Dict] = {}  # token -> device info
        self.cache_ttl = 3600  # 1 hour
        self.session: Optional[aiohttp.ClientSession] = None

    async def start(self):
        """Initialize the service"""
        self.session = aiohttp.ClientSession(
            headers={"Authorization": f"Bearer {settings.GATEWAY_ACCESS_TOKEN}"}
        )
        logger.info("Device authentication service started")

    async def stop(self):
        """Cleanup resources"""
        if self.session:
            await self.session.close()
        logger.info("Device authentication service stopped")

    async def authenticate_device(self, credentials_id: str, credentials_type: str = "ACCESS_TOKEN") -> Optional[Dict]:
        """
        Authenticate device using credentials

        Args:
            credentials_id: Access token or credentials identifier
            credentials_type: Type of credentials (ACCESS_TOKEN, X509, etc.)

        Returns:
            Device info dict if authenticated, None otherwise
        """
        # Check cache first
        cache_key = f"{credentials_type}:{credentials_id}"
        cached = self._get_from_cache(cache_key)
        if cached:
            logger.debug(f"Device authenticated from cache: {cached.get('device_id')}")
            return cached

        # Query backend API
        try:
            device_info = await self._authenticate_with_backend(credentials_id, credentials_type)
            if device_info:
                self._add_to_cache(cache_key, device_info)
                logger.info(f"Device authenticated: {device_info.get('device_id')}")
                return device_info
            else:
                logger.warning(f"Authentication failed for credentials: {credentials_id}")
                return None
        except Exception as e:
            logger.error(f"Error authenticating device: {e}")
            return None

    async def _authenticate_with_backend(self, credentials_id: str, credentials_type: str) -> Optional[Dict]:
        """
        Call backend API to validate device credentials

        In production, this would call:
        GET /api/device/credentials/{credentials_id}
        """
        if not self.session:
            raise RuntimeError("DeviceAuthService not started")

        try:
            # For now, we'll use a simplified approach
            # In production, implement proper API call to backend
            api_url = f"{settings.get_api_url()}/device/credentials/{credentials_id}"

            async with self.session.get(api_url, timeout=aiohttp.ClientTimeout(total=5)) as response:
                if response.status == 200:
                    data = await response.json()
                    return {
                        "device_id": data.get("device_id"),
                        "device_name": data.get("device_name"),
                        "tenant_id": data.get("tenant_id"),
                        "customer_id": data.get("customer_id"),
                        "device_type": data.get("device_type"),
                        "device_profile_id": data.get("device_profile_id"),
                    }
                elif response.status == 404:
                    return None
                else:
                    logger.error(f"Backend API error: {response.status}")
                    return None
        except asyncio.TimeoutError:
            logger.error("Backend API timeout")
            return None
        except Exception as e:
            logger.error(f"Error calling backend API: {e}")
            return None

    def _get_from_cache(self, cache_key: str) -> Optional[Dict]:
        """Get device info from cache if not expired"""
        if cache_key in self.cache:
            entry = self.cache[cache_key]
            if datetime.utcnow() < entry["expires_at"]:
                return entry["data"]
            else:
                # Expired, remove from cache
                del self.cache[cache_key]
        return None

    def _add_to_cache(self, cache_key: str, device_info: Dict):
        """Add device info to cache with TTL"""
        self.cache[cache_key] = {
            "data": device_info,
            "expires_at": datetime.utcnow() + timedelta(seconds=self.cache_ttl)
        }

    def invalidate_cache(self, credentials_id: str, credentials_type: str = "ACCESS_TOKEN"):
        """Invalidate cached device info"""
        cache_key = f"{credentials_type}:{credentials_id}"
        if cache_key in self.cache:
            del self.cache[cache_key]
            logger.debug(f"Cache invalidated for: {cache_key}")

    async def get_device_by_id(self, device_id: str) -> Optional[Dict]:
        """Get device info by device ID"""
        # Check if device is in cache (by ID)
        for entry in self.cache.values():
            if entry["data"].get("device_id") == device_id:
                if datetime.utcnow() < entry["expires_at"]:
                    return entry["data"]

        # Query backend
        if not self.session:
            raise RuntimeError("DeviceAuthService not started")

        try:
            api_url = f"{settings.get_api_url()}/devices/{device_id}"
            async with self.session.get(api_url, timeout=aiohttp.ClientTimeout(total=5)) as response:
                if response.status == 200:
                    return await response.json()
                else:
                    return None
        except Exception as e:
            logger.error(f"Error fetching device {device_id}: {e}")
            return None


# Global instance
device_auth_service = DeviceAuthService()
