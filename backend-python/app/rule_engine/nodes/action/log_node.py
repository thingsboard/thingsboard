"""
Log Node
Logs message data to console/file
Equivalent to: org.thingsboard.rule.engine.action.TbLogNode
"""
import orjson
from app.rule_engine.core.rule_node_base import TbActionNode
from app.rule_engine.models.rule_engine_msg import TbMsg, TbContext
from app.rule_engine.core.rule_engine_executor import register_node


@register_node("org.thingsboard.rule.engine.action.TbLogNode")
class TbLogNode(TbActionNode):
    """
    Log message to console

    Configuration:
    {
        "jsScript": "return 'Temperature: ' + msg.temperature + '°C';",
        "toStdout": true
    }
    """

    async def init(self):
        """Initialize node"""
        self.js_script = self.config.get("jsScript", "return JSON.stringify(msg);")
        self.to_stdout = self.config.get("toStdout", True)
        self.log_info("Initialized log node")

    async def execute(self, ctx: TbContext, msg: TbMsg):
        """
        Log message

        Args:
            ctx: Execution context
            msg: Message to log
        """
        try:
            # Parse message data
            msg_data = orjson.loads(msg.data)

            # Build log message
            log_message = self._build_log_message(msg_data, msg.metadata)

            # Log the message
            if self.to_stdout:
                self.log_info(f"MESSAGE LOG: {log_message}")

            self.log_debug(msg, f"Logged message: {log_message[:100]}")

        except Exception as e:
            self.log_error(msg, f"Failed to log message: {e}")
            raise

    def _build_log_message(self, msg_data: dict, metadata: dict) -> str:
        """
        Build log message

        Args:
            msg_data: Message data
            metadata: Message metadata

        Returns:
            Log message string
        """
        try:
            # Simple formatting
            parts = []

            # Add device info
            if "deviceName" in metadata:
                parts.append(f"Device: {metadata['deviceName']}")

            # Add telemetry data
            for key, value in msg_data.items():
                parts.append(f"{key}={value}")

            return " | ".join(parts) if parts else orjson.dumps(msg_data).decode('utf-8')

        except Exception as e:
            return f"Error formatting log: {e}"

    async def destroy(self):
        """Cleanup"""
        pass
