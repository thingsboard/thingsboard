"""
HTTP Transport Layer
Handles HTTP/REST API communication with devices
"""
from fastapi import FastAPI, HTTPException, Header, Request, Response
from fastapi.responses import JSONResponse
from typing import Dict, Any, Optional
from datetime import datetime
import orjson
from app.core.config import settings
from app.core.logging_config import get_logger
from app.core.device_auth import device_auth_service
from app.core.message_processor import message_processor
from app.models.message import TelemetryMessage, AttributesMessage

logger = get_logger(__name__)


class HTTPTransport:
    """
    HTTP Transport implementation

    API Endpoints:
    - POST /api/v1/{access_token}/telemetry - Publish telemetry
    - POST /api/v1/{access_token}/attributes - Publish attributes
    - GET /api/v1/{access_token}/attributes - Get client/shared attributes
    - POST /api/v1/{access_token}/rpc - Send RPC response
    - GET /api/v1/{access_token}/rpc - Poll for RPC requests
    """

    def __init__(self):
        self.app = FastAPI(
            title="ThingsBoard Gateway - HTTP Transport",
            description="HTTP API for device connectivity",
            version="1.0.0"
        )
        self.setup_routes()

    def setup_routes(self):
        """Setup HTTP routes"""

        @self.app.post("/api/v1/{access_token}/telemetry")
        async def post_telemetry(
            access_token: str,
            request: Request
        ):
            """
            Publish telemetry data

            Request body can be:
            1. Simple format: {"temperature": 25, "humidity": 60}
            2. With timestamp: {"ts": 1234567890000, "values": {"temperature": 25}}
            3. Array format: [{"ts": 123, "values": {...}}, ...]
            """
            try:
                # Authenticate device
                device_info = await device_auth_service.authenticate_device(access_token)
                if not device_info:
                    raise HTTPException(status_code=401, detail="Invalid access token")

                device_id = device_info["device_id"]

                # Parse request body
                body = await request.json()

                # Handle array format
                if isinstance(body, list):
                    for entry in body:
                        await self._process_telemetry_entry(device_id, entry)
                else:
                    await self._process_telemetry_entry(device_id, body)

                logger.info(f"HTTP telemetry received from device {device_id}")

                return {"status": "success"}

            except HTTPException:
                raise
            except Exception as e:
                logger.error(f"Error processing HTTP telemetry: {e}")
                raise HTTPException(status_code=500, detail="Internal server error")

        @self.app.post("/api/v1/{access_token}/attributes")
        async def post_attributes(
            access_token: str,
            request: Request
        ):
            """
            Publish client-side attributes

            Request body: {"attribute1": "value1", "attribute2": "value2"}
            """
            try:
                # Authenticate device
                device_info = await device_auth_service.authenticate_device(access_token)
                if not device_info:
                    raise HTTPException(status_code=401, detail="Invalid access token")

                device_id = device_info["device_id"]

                # Parse attributes
                attributes = await request.json()

                # Process attributes
                attr_msg = AttributesMessage(
                    device_id=device_id,
                    attributes=attributes
                )

                await message_processor.process_attributes(device_id, attr_msg, "http")

                logger.info(f"HTTP attributes received from device {device_id}")

                return {"status": "success"}

            except HTTPException:
                raise
            except Exception as e:
                logger.error(f"Error processing HTTP attributes: {e}")
                raise HTTPException(status_code=500, detail="Internal server error")

        @self.app.get("/api/v1/{access_token}/attributes")
        async def get_attributes(
            access_token: str,
            clientKeys: Optional[str] = None,
            sharedKeys: Optional[str] = None
        ):
            """
            Get attributes from server

            Query params:
            - clientKeys: Comma-separated list of client attribute keys
            - sharedKeys: Comma-separated list of shared attribute keys

            Returns: {"client": {...}, "shared": {...}}
            """
            try:
                # Authenticate device
                device_info = await device_auth_service.authenticate_device(access_token)
                if not device_info:
                    raise HTTPException(status_code=401, detail="Invalid access token")

                device_id = device_info["device_id"]

                # Fetch attributes from backend (implementation needed)
                # For now, return empty
                result = {
                    "client": {},
                    "shared": {}
                }

                logger.info(f"HTTP attributes requested by device {device_id}")

                return result

            except HTTPException:
                raise
            except Exception as e:
                logger.error(f"Error fetching attributes: {e}")
                raise HTTPException(status_code=500, detail="Internal server error")

        @self.app.post("/api/v1/{access_token}/rpc")
        async def post_rpc_response(
            access_token: str,
            request: Request
        ):
            """
            Send RPC response to server

            Request body: {"id": 123, "response": {...}}
            """
            try:
                # Authenticate device
                device_info = await device_auth_service.authenticate_device(access_token)
                if not device_info:
                    raise HTTPException(status_code=401, detail="Invalid access token")

                device_id = device_info["device_id"]

                # Parse RPC response
                data = await request.json()
                request_id = data.get("id")
                response_data = data.get("response")

                logger.info(f"HTTP RPC response from device {device_id}, request {request_id}")

                # Forward to backend (implementation depends on RPC architecture)

                return {"status": "success"}

            except HTTPException:
                raise
            except Exception as e:
                logger.error(f"Error processing RPC response: {e}")
                raise HTTPException(status_code=500, detail="Internal server error")

        @self.app.get("/api/v1/{access_token}/rpc")
        async def get_rpc_requests(
            access_token: str,
            timeout: int = 30000  # milliseconds
        ):
            """
            Long-poll for RPC requests from server

            Returns: {"id": 123, "method": "setValue", "params": {...}}
            or empty if no requests
            """
            try:
                # Authenticate device
                device_info = await device_auth_service.authenticate_device(access_token)
                if not device_info:
                    raise HTTPException(status_code=401, detail="Invalid access token")

                device_id = device_info["device_id"]

                # Check for pending RPC requests (implementation needed)
                # For now, return empty
                logger.info(f"HTTP RPC poll from device {device_id}")

                return {}

            except HTTPException:
                raise
            except Exception as e:
                logger.error(f"Error polling RPC: {e}")
                raise HTTPException(status_code=500, detail="Internal server error")

        @self.app.get("/health")
        async def health_check():
            """Health check endpoint"""
            return {"status": "healthy", "transport": "http"}

    async def _process_telemetry_entry(self, device_id: str, data: Dict[str, Any]):
        """Process a single telemetry entry"""
        # Support both formats
        if "values" in data:
            # Format with explicit timestamp
            telemetry = TelemetryMessage(
                device_id=device_id,
                ts=data.get("ts", int(datetime.utcnow().timestamp() * 1000)),
                values=data["values"]
            )
        else:
            # Simple format - all keys are telemetry values
            telemetry = TelemetryMessage(
                device_id=device_id,
                ts=int(datetime.utcnow().timestamp() * 1000),
                values=data
            )

        await message_processor.process_telemetry(device_id, telemetry, "http")

    async def start(self):
        """Start HTTP transport"""
        if not settings.HTTP_ENABLED:
            logger.info("HTTP transport disabled")
            return

        logger.info(f"HTTP transport configured on port {settings.HTTP_BIND_PORT}")

    async def stop(self):
        """Stop HTTP transport"""
        logger.info("HTTP transport stopped")


# Global instance
http_transport = HTTPTransport()
