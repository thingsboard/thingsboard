"""
CoAP Transport Layer
Handles CoAP protocol communication with devices
"""
import asyncio
import orjson
from typing import Dict, Optional
from datetime import datetime
import aiocoap
import aiocoap.resource as resource
from aiocoap import Context, Message, CHANGED, CONTENT
from app.core.config import settings
from app.core.logging_config import get_logger
from app.core.device_auth import device_auth_service
from app.core.message_processor import message_processor
from app.models.message import TelemetryMessage, AttributesMessage

logger = get_logger(__name__)


class TelemetryResource(resource.Resource):
    """CoAP resource for telemetry"""

    async def render_post(self, request):
        """Handle POST request to /api/v1/{token}/telemetry"""
        try:
            # Extract access token from URI path
            path_parts = request.opt.uri_path
            if len(path_parts) < 4:
                return Message(code=aiocoap.numbers.codes.Code.BAD_REQUEST, payload=b"Invalid path")

            access_token = path_parts[2].decode('utf-8')

            # Authenticate device
            device_info = await device_auth_service.authenticate_device(access_token)
            if not device_info:
                return Message(code=aiocoap.numbers.codes.Code.UNAUTHORIZED, payload=b"Invalid token")

            device_id = device_info["device_id"]

            # Parse payload
            payload = request.payload
            data = orjson.loads(payload)

            # Process telemetry
            if "values" in data:
                telemetry = TelemetryMessage(
                    device_id=device_id,
                    ts=data.get("ts", int(datetime.utcnow().timestamp() * 1000)),
                    values=data["values"]
                )
            else:
                telemetry = TelemetryMessage(
                    device_id=device_id,
                    ts=int(datetime.utcnow().timestamp() * 1000),
                    values=data
                )

            await message_processor.process_telemetry(device_id, telemetry, "coap")

            logger.info(f"CoAP telemetry received from device {device_id}")

            return Message(code=CHANGED, payload=b'{"status":"success"}')

        except Exception as e:
            logger.error(f"Error processing CoAP telemetry: {e}")
            return Message(code=aiocoap.numbers.codes.Code.INTERNAL_SERVER_ERROR, payload=b"Error")


class AttributesResource(resource.Resource):
    """CoAP resource for attributes"""

    async def render_post(self, request):
        """Handle POST request to /api/v1/{token}/attributes"""
        try:
            # Extract access token
            path_parts = request.opt.uri_path
            if len(path_parts) < 4:
                return Message(code=aiocoap.numbers.codes.Code.BAD_REQUEST, payload=b"Invalid path")

            access_token = path_parts[2].decode('utf-8')

            # Authenticate device
            device_info = await device_auth_service.authenticate_device(access_token)
            if not device_info:
                return Message(code=aiocoap.numbers.codes.Code.UNAUTHORIZED, payload=b"Invalid token")

            device_id = device_info["device_id"]

            # Parse payload
            payload = request.payload
            attributes = orjson.loads(payload)

            # Process attributes
            attr_msg = AttributesMessage(
                device_id=device_id,
                attributes=attributes
            )

            await message_processor.process_attributes(device_id, attr_msg, "coap")

            logger.info(f"CoAP attributes received from device {device_id}")

            return Message(code=CHANGED, payload=b'{"status":"success"}')

        except Exception as e:
            logger.error(f"Error processing CoAP attributes: {e}")
            return Message(code=aiocoap.numbers.codes.Code.INTERNAL_SERVER_ERROR, payload=b"Error")

    async def render_get(self, request):
        """Handle GET request to fetch attributes"""
        try:
            # Extract access token
            path_parts = request.opt.uri_path
            if len(path_parts) < 4:
                return Message(code=aiocoap.numbers.codes.Code.BAD_REQUEST, payload=b"Invalid path")

            access_token = path_parts[2].decode('utf-8')

            # Authenticate device
            device_info = await device_auth_service.authenticate_device(access_token)
            if not device_info:
                return Message(code=aiocoap.numbers.codes.Code.UNAUTHORIZED, payload=b"Invalid token")

            device_id = device_info["device_id"]

            logger.info(f"CoAP attributes requested by device {device_id}")

            # Fetch attributes from backend (implementation needed)
            # For now, return empty
            result = {"client": {}, "shared": {}}

            return Message(code=CONTENT, payload=orjson.dumps(result))

        except Exception as e:
            logger.error(f"Error fetching CoAP attributes: {e}")
            return Message(code=aiocoap.numbers.codes.Code.INTERNAL_SERVER_ERROR, payload=b"Error")


class CoAPTransport:
    """
    CoAP Transport implementation

    CoAP endpoints:
    - POST coap://{gateway}:5683/api/v1/{access_token}/telemetry
    - POST coap://{gateway}:5683/api/v1/{access_token}/attributes
    - GET coap://{gateway}:5683/api/v1/{access_token}/attributes
    """

    def __init__(self):
        self.context: Optional[Context] = None
        self.root = resource.Site()
        self.running = False

    async def start(self):
        """Start CoAP transport"""
        if not settings.COAP_ENABLED:
            logger.info("CoAP transport disabled")
            return

        # Setup resources
        # Note: aiocoap uses a tree structure, so we need to handle dynamic paths
        # For simplicity, we'll use a catch-all approach

        # Create resource tree
        # /api/v1/{token}/telemetry
        # /api/v1/{token}/attributes

        # We need a way to handle dynamic tokens
        # One approach is to use a custom resource that handles all paths under /api/v1/
        self.root.add_resource(['api', 'v1', '*', 'telemetry'], TelemetryResource())
        self.root.add_resource(['api', 'v1', '*', 'attributes'], AttributesResource())

        # Bind to address and port
        self.context = await Context.create_server_context(
            self.root,
            bind=(settings.COAP_BIND_ADDRESS, settings.COAP_BIND_PORT)
        )

        logger.info(f"CoAP transport started on {settings.COAP_BIND_ADDRESS}:{settings.COAP_BIND_PORT}")
        self.running = True

    async def stop(self):
        """Stop CoAP transport"""
        self.running = False

        if self.context:
            await self.context.shutdown()

        logger.info("CoAP transport stopped")


# Global instance
coap_transport = CoAPTransport()
