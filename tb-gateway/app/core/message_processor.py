"""
Message processor - forwards messages to backend via Kafka or HTTP
"""
import asyncio
import aiohttp
from typing import Optional, List
from datetime import datetime
from app.core.config import settings
from app.core.logging_config import get_logger
from app.models.message import GatewayMessage, MessageType, TelemetryMessage, AttributesMessage

logger = get_logger(__name__)


class MessageProcessor:
    """
    Processes and forwards messages from devices to the backend
    Supports both Kafka (for high throughput) and HTTP (fallback)
    """

    def __init__(self):
        self.kafka_producer: Optional[object] = None
        self.http_session: Optional[aiohttp.ClientSession] = None
        self.message_queue: asyncio.Queue = asyncio.Queue(maxsize=10000)
        self.batch: List[GatewayMessage] = []
        self.last_batch_time = datetime.utcnow()
        self.running = False

    async def start(self):
        """Start the message processor"""
        if settings.KAFKA_ENABLED:
            await self._start_kafka()

        self.http_session = aiohttp.ClientSession()
        self.running = True

        # Start background processor
        asyncio.create_task(self._process_messages())

        logger.info("Message processor started")

    async def stop(self):
        """Stop the message processor"""
        self.running = False

        if self.kafka_producer:
            await self.kafka_producer.stop()

        if self.http_session:
            await self.http_session.close()

        # Flush remaining messages
        await self._flush_batch()

        logger.info("Message processor stopped")

    async def _start_kafka(self):
        """Initialize Kafka producer"""
        try:
            from aiokafka import AIOKafkaProducer
            self.kafka_producer = AIOKafkaProducer(
                bootstrap_servers=settings.KAFKA_BOOTSTRAP_SERVERS,
                value_serializer=lambda v: v.encode('utf-8') if isinstance(v, str) else v,
            )
            await self.kafka_producer.start()
            logger.info("Kafka producer started")
        except Exception as e:
            logger.error(f"Failed to start Kafka producer: {e}")
            self.kafka_producer = None

    async def process_telemetry(self, device_id: str, telemetry: TelemetryMessage, transport: str):
        """Process telemetry message from device"""
        message = GatewayMessage(
            message_type=MessageType.TELEMETRY,
            device_id=device_id,
            transport=transport,
            payload={
                "ts": telemetry.ts,
                "values": telemetry.values
            },
            timestamp=int(datetime.utcnow().timestamp() * 1000)
        )

        await self.message_queue.put(message)
        logger.debug(f"Telemetry queued for device {device_id}")

    async def process_attributes(self, device_id: str, attributes: AttributesMessage, transport: str):
        """Process attributes message from device"""
        message = GatewayMessage(
            message_type=MessageType.ATTRIBUTES,
            device_id=device_id,
            transport=transport,
            payload={
                "attributes": attributes.attributes
            },
            timestamp=int(datetime.utcnow().timestamp() * 1000)
        )

        await self.message_queue.put(message)
        logger.debug(f"Attributes queued for device {device_id}")

    async def _process_messages(self):
        """Background task to process message queue"""
        while self.running:
            try:
                # Get message from queue with timeout
                try:
                    message = await asyncio.wait_for(
                        self.message_queue.get(),
                        timeout=settings.BATCH_TIMEOUT
                    )
                    self.batch.append(message)
                except asyncio.TimeoutError:
                    pass

                # Check if batch should be flushed
                should_flush = (
                    len(self.batch) >= settings.BATCH_SIZE or
                    (datetime.utcnow() - self.last_batch_time).total_seconds() >= settings.BATCH_TIMEOUT
                )

                if should_flush and self.batch:
                    await self._flush_batch()

            except Exception as e:
                logger.error(f"Error processing messages: {e}")
                await asyncio.sleep(1)

    async def _flush_batch(self):
        """Flush batch of messages to backend"""
        if not self.batch:
            return

        batch = self.batch.copy()
        self.batch = []
        self.last_batch_time = datetime.utcnow()

        logger.debug(f"Flushing batch of {len(batch)} messages")

        # Try Kafka first
        if self.kafka_producer:
            success = await self._send_via_kafka(batch)
            if success:
                return

        # Fallback to HTTP
        await self._send_via_http(batch)

    async def _send_via_kafka(self, messages: List[GatewayMessage]) -> bool:
        """Send messages via Kafka"""
        if not self.kafka_producer:
            return False

        try:
            import orjson

            for message in messages:
                # Determine topic based on message type
                if message.message_type == MessageType.TELEMETRY:
                    topic = settings.KAFKA_TOPIC_TELEMETRY
                elif message.message_type == MessageType.ATTRIBUTES:
                    topic = settings.KAFKA_TOPIC_ATTRIBUTES
                else:
                    topic = settings.KAFKA_TOPIC_EVENTS

                # Serialize message
                value = orjson.dumps(message.model_dump())

                # Send to Kafka
                await self.kafka_producer.send_and_wait(
                    topic,
                    value=value,
                    key=message.device_id.encode('utf-8') if message.device_id else None
                )

            logger.debug(f"Sent {len(messages)} messages via Kafka")
            return True
        except Exception as e:
            logger.error(f"Error sending via Kafka: {e}")
            return False

    async def _send_via_http(self, messages: List[GatewayMessage]):
        """Send messages via HTTP API (fallback)"""
        if not self.http_session:
            logger.error("HTTP session not available")
            return

        try:
            # Group messages by device and type
            grouped = {}
            for message in messages:
                key = (message.device_id, message.message_type)
                if key not in grouped:
                    grouped[key] = []
                grouped[key].append(message)

            # Send grouped messages
            for (device_id, msg_type), msgs in grouped.items():
                if msg_type == MessageType.TELEMETRY:
                    await self._send_telemetry_http(device_id, msgs)
                elif msg_type == MessageType.ATTRIBUTES:
                    await self._send_attributes_http(device_id, msgs)

            logger.debug(f"Sent {len(messages)} messages via HTTP")
        except Exception as e:
            logger.error(f"Error sending via HTTP: {e}")

    async def _send_telemetry_http(self, device_id: str, messages: List[GatewayMessage]):
        """Send telemetry messages via HTTP"""
        # Combine telemetry data
        telemetry_data = {}
        for msg in messages:
            payload = msg.payload
            ts = payload.get("ts", msg.timestamp)
            values = payload.get("values", {})

            for key, value in values.items():
                if key not in telemetry_data:
                    telemetry_data[key] = []
                telemetry_data[key].append({"ts": ts, "value": value})

        # Send to backend
        url = f"{settings.get_api_url()}/telemetry/DEVICE/{device_id}/timeseries/GATEWAY"
        try:
            async with self.http_session.post(
                url,
                json=telemetry_data,
                headers={"Authorization": f"Bearer {settings.GATEWAY_ACCESS_TOKEN}"},
                timeout=aiohttp.ClientTimeout(total=10)
            ) as response:
                if response.status != 200:
                    logger.error(f"HTTP telemetry error: {response.status}")
        except Exception as e:
            logger.error(f"Error sending telemetry HTTP: {e}")

    async def _send_attributes_http(self, device_id: str, messages: List[GatewayMessage]):
        """Send attributes messages via HTTP"""
        # Combine attributes
        attributes = {}
        for msg in messages:
            payload = msg.payload
            attrs = payload.get("attributes", {})
            attributes.update(attrs)

        # Send to backend
        url = f"{settings.get_api_url()}/telemetry/DEVICE/{device_id}/attributes/CLIENT_SCOPE"
        try:
            async with self.http_session.post(
                url,
                json=attributes,
                headers={"Authorization": f"Bearer {settings.GATEWAY_ACCESS_TOKEN}"},
                timeout=aiohttp.ClientTimeout(total=10)
            ) as response:
                if response.status != 200:
                    logger.error(f"HTTP attributes error: {response.status}")
        except Exception as e:
            logger.error(f"Error sending attributes HTTP: {e}")


# Global instance
message_processor = MessageProcessor()
