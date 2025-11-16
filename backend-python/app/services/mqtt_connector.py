"""
MQTT Connector Service
Connects to MQTT brokers and forwards data to ThingsBoard
"""

import asyncio
import json
import logging
from typing import Dict, List, Optional, Any
from dataclasses import dataclass
import paho.mqtt.client as mqtt
from jsonpath_ng import parse as jsonpath_parse

logger = logging.getLogger(__name__)


@dataclass
class MqttConnectorConfig:
    """MQTT connector configuration"""
    broker: str
    port: int
    client_id: str
    username: Optional[str] = None
    password: Optional[str] = None
    qos: int = 1
    clean_session: bool = True
    topic_filters: List[Dict[str, Any]] = None


@dataclass
class DataConverter:
    """Data converter for parsing MQTT messages"""
    type: str  # json, bytes, custom
    device_name_expression: str
    device_type_expression: str
    timeout: int = 60000
    attributes: List[Dict[str, Any]] = None
    timeseries: List[Dict[str, Any]] = None


class MqttDataConverter:
    """Converts MQTT messages to telemetry and attributes"""

    def __init__(self, converter_config: DataConverter):
        self.config = converter_config

    def convert(self, topic: str, payload: bytes) -> Dict[str, Any]:
        """
        Convert MQTT message to device data

        Args:
            topic: MQTT topic
            payload: MQTT payload

        Returns:
            Dictionary with device_name, device_type, attributes, telemetry
        """
        try:
            # Parse payload based on converter type
            if self.config.type == "json":
                data = json.loads(payload.decode('utf-8'))
            elif self.config.type == "bytes":
                data = {"payload": payload.hex()}
            else:
                data = {"raw": payload.decode('utf-8')}

            # Extract device name and type
            topic_parts = topic.split('/')
            device_name = self._evaluate_expression(
                self.config.device_name_expression,
                data,
                topic_parts
            )
            device_type = self._evaluate_expression(
                self.config.device_type_expression,
                data,
                topic_parts
            )

            # Extract attributes
            attributes = {}
            if self.config.attributes:
                for attr in self.config.attributes:
                    key = attr.get('key')
                    value_expr = attr.get('value')
                    value = self._evaluate_expression(value_expr, data, topic_parts)
                    attributes[key] = self._convert_value(value, attr.get('type', 'string'))

            # Extract timeseries
            timeseries = {}
            if self.config.timeseries:
                for ts in self.config.timeseries:
                    key = ts.get('key')
                    value_expr = ts.get('value')
                    value = self._evaluate_expression(value_expr, data, topic_parts)
                    timeseries[key] = self._convert_value(value, ts.get('type', 'double'))

            return {
                'device_name': device_name,
                'device_type': device_type,
                'attributes': attributes,
                'timeseries': timeseries
            }

        except Exception as e:
            logger.error(f"Error converting MQTT message: {e}")
            return None

    def _evaluate_expression(self, expression: str, data: Dict, topic_parts: List[str]) -> Any:
        """
        Evaluate expression to extract value

        Supports:
        - ${key} for JSON keys
        - ${topic[0]} for topic parts
        - Literal values
        """
        if not expression:
            return None

        # Handle topic references: ${topic[1]}
        if '${topic[' in expression:
            import re
            matches = re.findall(r'\${topic\[(\d+)\]}', expression)
            result = expression
            for match in matches:
                index = int(match)
                if index < len(topic_parts):
                    result = result.replace(f'${{topic[{match}]}}', topic_parts[index])
            return result

        # Handle JSON path: ${key} or ${nested.key}
        if expression.startswith('${') and expression.endswith('}'):
            json_path = expression[2:-1]

            # Simple key access
            if '.' not in json_path:
                return data.get(json_path)

            # Nested key access
            try:
                jsonpath_expr = jsonpath_parse(json_path)
                matches = jsonpath_expr.find(data)
                if matches:
                    return matches[0].value
            except Exception as e:
                logger.warning(f"Error parsing JSONPath {json_path}: {e}")
                return None

        # Literal value
        return expression

    def _convert_value(self, value: Any, value_type: str) -> Any:
        """Convert value to specified type"""
        if value is None:
            return None

        try:
            if value_type == 'double':
                return float(value)
            elif value_type == 'long':
                return int(value)
            elif value_type == 'boolean':
                if isinstance(value, str):
                    return value.lower() in ('true', '1', 'yes')
                return bool(value)
            else:  # string
                return str(value)
        except (ValueError, TypeError):
            logger.warning(f"Could not convert {value} to {value_type}")
            return value


class MqttConnectorService:
    """MQTT Connector Service for IoT Gateway"""

    def __init__(self, config: MqttConnectorConfig):
        self.config = config
        self.client: Optional[mqtt.Client] = None
        self.connected = False
        self.converters: Dict[str, MqttDataConverter] = {}
        self._setup_converters()

    def _setup_converters(self):
        """Setup data converters for each topic filter"""
        if not self.config.topic_filters:
            return

        for topic_filter in self.config.topic_filters:
            topic = topic_filter.get('topicFilter')
            converter_config = topic_filter.get('converter', {})

            converter = DataConverter(
                type=converter_config.get('type', 'json'),
                device_name_expression=converter_config.get('deviceNameJsonExpression', ''),
                device_type_expression=converter_config.get('deviceTypeJsonExpression', ''),
                timeout=converter_config.get('timeout', 60000),
                attributes=converter_config.get('attributes', []),
                timeseries=converter_config.get('timeseries', [])
            )

            self.converters[topic] = MqttDataConverter(converter)

    def _on_connect(self, client, userdata, flags, rc):
        """Callback when connected to MQTT broker"""
        if rc == 0:
            logger.info(f"Connected to MQTT broker {self.config.broker}:{self.config.port}")
            self.connected = True

            # Subscribe to all topic filters
            for topic_filter in self.config.topic_filters or []:
                topic = topic_filter.get('topicFilter')
                client.subscribe(topic, qos=self.config.qos)
                logger.info(f"Subscribed to topic: {topic}")
        else:
            logger.error(f"Failed to connect to MQTT broker, return code {rc}")
            self.connected = False

    def _on_disconnect(self, client, userdata, rc):
        """Callback when disconnected from MQTT broker"""
        logger.warning(f"Disconnected from MQTT broker, return code {rc}")
        self.connected = False

    def _on_message(self, client, userdata, msg):
        """Callback when message received"""
        try:
            logger.debug(f"Received message on topic {msg.topic}: {msg.payload[:100]}")

            # Find matching converter
            converter = self._find_converter(msg.topic)
            if not converter:
                logger.warning(f"No converter found for topic {msg.topic}")
                return

            # Convert message
            device_data = converter.convert(msg.topic, msg.payload)
            if not device_data:
                logger.warning(f"Failed to convert message from topic {msg.topic}")
                return

            # Process device data
            asyncio.create_task(self._process_device_data(device_data))

        except Exception as e:
            logger.error(f"Error processing MQTT message: {e}")

    def _find_converter(self, topic: str) -> Optional[MqttDataConverter]:
        """Find converter for topic using MQTT wildcard matching"""
        for topic_filter, converter in self.converters.items():
            if self._topic_matches(topic, topic_filter):
                return converter
        return None

    def _topic_matches(self, topic: str, topic_filter: str) -> bool:
        """Check if topic matches filter with wildcards (+, #)"""
        topic_parts = topic.split('/')
        filter_parts = topic_filter.split('/')

        # Handle multi-level wildcard #
        if '#' in filter_parts:
            hash_index = filter_parts.index('#')
            if hash_index != len(filter_parts) - 1:
                return False  # # must be last
            filter_parts = filter_parts[:hash_index]
            topic_parts = topic_parts[:hash_index]

        if len(topic_parts) != len(filter_parts):
            return False

        for topic_part, filter_part in zip(topic_parts, filter_parts):
            if filter_part == '+':
                continue  # Single-level wildcard matches any value
            if topic_part != filter_part:
                return False

        return True

    async def _process_device_data(self, device_data: Dict[str, Any]):
        """
        Process device data - forward to rule engine

        In production, this would:
        1. Create/update device if not exists
        2. Save attributes to device
        3. Save telemetry to time-series database
        4. Forward to rule engine for processing
        """
        device_name = device_data.get('device_name')
        device_type = device_data.get('device_type')
        attributes = device_data.get('attributes', {})
        timeseries = device_data.get('timeseries', {})

        logger.info(f"Processing data for device {device_name} (type: {device_type})")
        logger.info(f"Attributes: {attributes}")
        logger.info(f"Telemetry: {timeseries}")

        # TODO: Forward to rule engine
        # TODO: Save to database
        # TODO: Send to WebSocket for real-time updates

    def connect(self):
        """Connect to MQTT broker"""
        try:
            # Create MQTT client
            self.client = mqtt.Client(
                client_id=self.config.client_id,
                clean_session=self.config.clean_session
            )

            # Set callbacks
            self.client.on_connect = self._on_connect
            self.client.on_disconnect = self._on_disconnect
            self.client.on_message = self._on_message

            # Set credentials if provided
            if self.config.username and self.config.password:
                self.client.username_pw_set(self.config.username, self.config.password)

            # Connect to broker
            logger.info(f"Connecting to MQTT broker {self.config.broker}:{self.config.port}")
            self.client.connect(self.config.broker, self.config.port, keepalive=60)

            # Start network loop
            self.client.loop_start()

            return True

        except Exception as e:
            logger.error(f"Error connecting to MQTT broker: {e}")
            return False

    def disconnect(self):
        """Disconnect from MQTT broker"""
        if self.client:
            self.client.loop_stop()
            self.client.disconnect()
            self.connected = False
            logger.info("Disconnected from MQTT broker")

    def publish(self, topic: str, payload: str, qos: int = None):
        """Publish message to MQTT broker"""
        if not self.connected:
            logger.error("Not connected to MQTT broker")
            return False

        try:
            qos = qos if qos is not None else self.config.qos
            self.client.publish(topic, payload, qos=qos)
            logger.debug(f"Published to {topic}: {payload}")
            return True
        except Exception as e:
            logger.error(f"Error publishing message: {e}")
            return False


# Example usage
if __name__ == "__main__":
    # Configure logging
    logging.basicConfig(level=logging.DEBUG)

    # Example configuration
    config = MqttConnectorConfig(
        broker="mqtt.eclipse.org",
        port=1883,
        client_id="tb-gateway-test",
        qos=1,
        topic_filters=[
            {
                "topicFilter": "sensors/+/data",
                "converter": {
                    "type": "json",
                    "deviceNameJsonExpression": "${topic[1]}",
                    "deviceTypeJsonExpression": "sensor",
                    "timeseries": [
                        {"key": "temperature", "type": "double", "value": "${temperature}"},
                        {"key": "humidity", "type": "double", "value": "${humidity}"}
                    ],
                    "attributes": [
                        {"key": "model", "type": "string", "value": "${model}"}
                    ]
                }
            }
        ]
    )

    # Create and start connector
    connector = MqttConnectorService(config)
    connector.connect()

    # Keep running
    try:
        while True:
            asyncio.sleep(1)
    except KeyboardInterrupt:
        connector.disconnect()
