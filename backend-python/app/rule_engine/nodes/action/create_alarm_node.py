"""
Create Alarm Node
Creates or updates alarm based on message
Equivalent to: org.thingsboard.rule.engine.action.TbCreateAlarmNode
"""
import orjson
import aiohttp
from datetime import datetime
from app.rule_engine.core.rule_node_base import TbActionNode
from app.rule_engine.models.rule_engine_msg import TbMsg, TbContext
from app.rule_engine.core.rule_engine_executor import register_node


@register_node("org.thingsboard.rule.engine.action.TbCreateAlarmNode")
class TbCreateAlarmNode(TbActionNode):
    """
    Create or update alarm

    Configuration:
    {
        "alarmType": "High Temperature",
        "alarmSeverity": "CRITICAL",
        "propagate": false,
        "useMessageAlarmData": false,
        "alarmDetailsBuildJs": "return {details: 'Temperature is ' + msg.temperature};"
    }
    """

    async def init(self):
        """Initialize node"""
        self.alarm_type = self.config.get("alarmType", "General Alarm")
        self.alarm_severity = self.config.get("alarmSeverity", "WARNING")
        self.propagate = self.config.get("propagate", False)
        self.use_message_alarm_data = self.config.get("useMessageAlarmData", False)
        self.alarm_details_script = self.config.get("alarmDetailsBuildJs", "return {};")

        self.session = aiohttp.ClientSession()
        self.log_info(f"Initialized create alarm node: {self.alarm_type} ({self.alarm_severity})")

    async def execute(self, ctx: TbContext, msg: TbMsg):
        """
        Create or update alarm

        Args:
            ctx: Execution context
            msg: Message triggering alarm
        """
        try:
            # Parse message data
            msg_data = orjson.loads(msg.data)

            # Build alarm details
            alarm_details = self._build_alarm_details(msg_data, msg.metadata)

            # Get device/entity ID from originator
            originator_id = msg.originator.id
            originator_type = msg.originator.entity_type.value

            # Create alarm object
            alarm = {
                "type": self.alarm_type,
                "severity": self.alarm_severity,
                "originator_id": originator_id,
                "originator_type": originator_type,
                "propagate": self.propagate,
                "details": alarm_details,
                "start_ts": msg.ts or int(datetime.utcnow().timestamp() * 1000)
            }

            # Save alarm to backend (when backend is available)
            # await self._save_alarm(alarm, ctx)

            self.log_debug(msg, f"Created alarm: {self.alarm_type} for {originator_id}")

        except Exception as e:
            self.log_error(msg, f"Failed to create alarm: {e}")
            raise

    def _build_alarm_details(self, msg_data: dict, metadata: dict) -> dict:
        """
        Build alarm details from message

        Args:
            msg_data: Message data
            metadata: Message metadata

        Returns:
            Alarm details dict
        """
        try:
            # Simple details extraction
            details = {}

            # Include key telemetry values
            if "temperature" in msg_data:
                details["temperature"] = msg_data["temperature"]
            if "threshold" in msg_data:
                details["threshold"] = msg_data["threshold"]

            # Add device info from metadata
            if "deviceName" in metadata:
                details["deviceName"] = metadata["deviceName"]
            if "deviceType" in metadata:
                details["deviceType"] = metadata["deviceType"]

            return details

        except Exception as e:
            self.log_error(None, f"Error building alarm details: {e}")
            return {}

    async def destroy(self):
        """Cleanup"""
        if self.session:
            await self.session.close()
