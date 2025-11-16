"""
MQTT Transport Layer
Handles MQTT protocol communication with devices
"""
import asyncio
import orjson
from typing import Dict, Set, Optional
from datetime import datetime
import paho.mqtt.client as mqtt
from app.core.config import settings
from app.core.logging_config import get_logger
from app.core.device_auth import device_auth_service
from app.core.message_processor import message_processor
from app.models.message import TelemetryMessage, AttributesMessage, DeviceState, DeviceStateMessage

logger = get_logger(__name__)


class MQTTTransport:
    """
    MQTT Transport implementation

    Topic structure:
    - v1/devices/me/telemetry - Publish telemetry
    - v1/devices/me/attributes - Publish attributes
    - v1/devices/me/attributes/request/+ - Request attributes
    - v1/devices/me/attributes/response/+ - Receive attributes
    - v1/devices/me/rpc/request/+ - Receive RPC requests
    - v1/devices/me/rpc/response/+ - Send RPC responses
    """

    def __init__(self):
        self.client: Optional[mqtt.Client] = None
        self.connected_devices: Dict[str, Dict] = {}  # client_id -> device info
        self.device_topics: Dict[str, Set[str]] = {}  # client_id -> set of subscribed topics
        self.running = False

    async def start(self):
        """Start MQTT transport"""
        if not settings.MQTT_ENABLED:
            logger.info("MQTT transport disabled")
            return

        # Create MQTT client (broker mode)
        self.client = mqtt.Client(client_id="tb-gateway-mqtt", protocol=mqtt.MQTTv311)

        # Set callbacks
        self.client.on_connect = self._on_connect
        self.client.on_disconnect = self._on_disconnect
        self.client.on_message = self._on_message
        self.client.on_subscribe = self._on_subscribe

        # Authentication callback
        self.client.on_connect = self._on_connect_auth

        # Configure TLS if enabled
        if settings.MQTT_SSL_ENABLED and settings.MQTT_SSL_CERT:
            self.client.tls_set(
                certfile=settings.MQTT_SSL_CERT,
                keyfile=settings.MQTT_SSL_KEY
            )

        # Connect to ourselves as broker (actually we need to run as broker)
        # In production, use a proper MQTT broker library like HBMQTT/amqtt
        # For now, this is a client-based implementation

        logger.info(f"MQTT transport started on {settings.MQTT_BIND_ADDRESS}:{settings.MQTT_BIND_PORT}")
        self.running = True

    async def stop(self):
        """Stop MQTT transport"""
        self.running = False

        # Disconnect all devices
        for device_id in list(self.connected_devices.keys()):
            await self._device_disconnected(device_id)

        if self.client:
            self.client.disconnect()

        logger.info("MQTT transport stopped")

    def _on_connect_auth(self, client, userdata, flags, rc):
        """Handle client authentication"""
        # In MQTT, authentication happens via username/password
        # The username is typically the device access token
        # This callback would validate the token
        pass

    def _on_connect(self, client, userdata, flags, rc):
        """Handle client connection"""
        if rc == 0:
            logger.info("MQTT client connected")
        else:
            logger.error(f"MQTT connection failed: {rc}")

    def _on_disconnect(self, client, userdata, rc):
        """Handle client disconnection"""
        logger.info(f"MQTT client disconnected: {rc}")

    def _on_subscribe(self, client, userdata, mid, granted_qos):
        """Handle subscription"""
        logger.debug(f"Subscribed: {mid}, QoS: {granted_qos}")

    def _on_message(self, client, userdata, msg):
        """Handle incoming MQTT message"""
        asyncio.create_task(self._handle_message(msg))

    async def _handle_message(self, msg):
        """Process MQTT message asynchronously"""
        try:
            topic = msg.topic
            payload = msg.payload

            logger.debug(f"MQTT message on topic: {topic}")

            # Extract device token from topic or connection
            # In real implementation, this would be from the MQTT connection username
            device_token = self._extract_device_token(topic)

            if not device_token:
                logger.warning("No device token found in message")
                return

            # Authenticate device
            device_info = await device_auth_service.authenticate_device(device_token)
            if not device_info:
                logger.warning(f"Authentication failed for token: {device_token}")
                return

            device_id = device_info["device_id"]

            # Route message based on topic
            if topic.endswith("/telemetry"):
                await self._handle_telemetry(device_id, payload)
            elif topic.endswith("/attributes"):
                await self._handle_attributes(device_id, payload)
            elif "/rpc/response/" in topic:
                await self._handle_rpc_response(device_id, topic, payload)
            else:
                logger.warning(f"Unknown topic: {topic}")

        except Exception as e:
            logger.error(f"Error handling MQTT message: {e}")

    def _extract_device_token(self, topic: str) -> Optional[str]:
        """Extract device access token from topic or connection"""
        # In real implementation, this would come from the MQTT connection
        # For now, we'll use a placeholder
        # Format could be: v1/devices/{token}/telemetry
        parts = topic.split("/")
        if len(parts) >= 3 and parts[1] == "devices":
            return parts[2]
        return None

    async def _handle_telemetry(self, device_id: str, payload: bytes):
        """Handle telemetry data"""
        try:
            # Parse JSON payload
            data = orjson.loads(payload)

            # Support both formats:
            # 1. {"temperature": 25, "humidity": 60}
            # 2. {"ts": 123456, "values": {"temperature": 25}}

            if "values" in data:
                # Format 2
                telemetry = TelemetryMessage(
                    device_id=device_id,
                    ts=data.get("ts", int(datetime.utcnow().timestamp() * 1000)),
                    values=data["values"]
                )
            else:
                # Format 1 - all keys are telemetry values
                telemetry = TelemetryMessage(
                    device_id=device_id,
                    ts=int(datetime.utcnow().timestamp() * 1000),
                    values=data
                )

            # Process telemetry
            await message_processor.process_telemetry(device_id, telemetry, "mqtt")

            logger.info(f"Telemetry processed for device {device_id}")

        except Exception as e:
            logger.error(f"Error processing telemetry: {e}")

    async def _handle_attributes(self, device_id: str, payload: bytes):
        """Handle attribute updates"""
        try:
            # Parse JSON payload
            data = orjson.loads(payload)

            attributes = AttributesMessage(
                device_id=device_id,
                attributes=data
            )

            # Process attributes
            await message_processor.process_attributes(device_id, attributes, "mqtt")

            logger.info(f"Attributes processed for device {device_id}")

        except Exception as e:
            logger.error(f"Error processing attributes: {e}")

    async def _handle_rpc_response(self, device_id: str, topic: str, payload: bytes):
        """Handle RPC response from device"""
        try:
            # Extract request ID from topic: v1/devices/me/rpc/response/123
            request_id = int(topic.split("/")[-1])

            # Parse response
            response_data = orjson.loads(payload)

            logger.info(f"RPC response from device {device_id}, request {request_id}")

            # Forward to backend (implementation depends on RPC architecture)
            # For now, just log
            logger.debug(f"RPC response: {response_data}")

        except Exception as e:
            logger.error(f"Error handling RPC response: {e}")

    async def _device_connected(self, device_id: str, device_info: Dict):
        """Handle device connection event"""
        self.connected_devices[device_id] = {
            **device_info,
            "connected_at": datetime.utcnow(),
            "last_activity": datetime.utcnow()
        }

        # Send device state message
        state_msg = DeviceStateMessage(
            device_id=device_id,
            state=DeviceState.ONLINE,
            timestamp=int(datetime.utcnow().timestamp() * 1000),
            transport="mqtt"
        )

        logger.info(f"Device connected: {device_id}")

    async def _device_disconnected(self, device_id: str):
        """Handle device disconnection event"""
        if device_id in self.connected_devices:
            del self.connected_devices[device_id]

        # Send device state message
        state_msg = DeviceStateMessage(
            device_id=device_id,
            state=DeviceState.OFFLINE,
            timestamp=int(datetime.utcnow().timestamp() * 1000),
            transport="mqtt"
        )

        logger.info(f"Device disconnected: {device_id}")

    async def send_rpc_request(self, device_id: str, request_id: int, method: str, params: Dict):
        """Send RPC request to device"""
        if not self.client:
            logger.error("MQTT client not initialized")
            return

        topic = f"v1/devices/{device_id}/rpc/request/{request_id}"
        payload = orjson.dumps({
            "method": method,
            "params": params
        })

        self.client.publish(topic, payload, qos=1)
        logger.info(f"RPC request sent to device {device_id}: {method}")

    async def send_attributes(self, device_id: str, attributes: Dict):
        """Send server-side attributes to device"""
        if not self.client:
            logger.error("MQTT client not initialized")
            return

        topic = f"v1/devices/{device_id}/attributes"
        payload = orjson.dumps(attributes)

        self.client.publish(topic, payload, qos=1)
        logger.info(f"Attributes sent to device {device_id}")


# Global instance
mqtt_transport = MQTTTransport()
