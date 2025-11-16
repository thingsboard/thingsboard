"""
Save Telemetry Node
Saves telemetry data to database
Equivalent to: org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNode
"""
import orjson
import aiohttp
from app.rule_engine.core.rule_node_base import TbActionNode
from app.rule_engine.models.rule_engine_msg import TbMsg, TbContext
from app.rule_engine.core.rule_engine_executor import register_node
from app.core.config import settings


@register_node("org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNode")
class TbMsgTimeseriesNode(TbActionNode):
    """
    Save telemetry to time-series database

    Configuration:
    {
        "defaultTTL": 0  # Time to live in days (0 = infinite)
    }
    """

    async def init(self):
        """Initialize node"""
        self.default_ttl = self.config.get("defaultTTL", 0)
        self.session = aiohttp.ClientSession()
        self.log_info(f"Initialized save telemetry node (TTL: {self.default_ttl})")

    async def execute(self, ctx: TbContext, msg: TbMsg):
        """
        Save telemetry data

        Args:
            ctx: Execution context
            msg: Message containing telemetry data
        """
        try:
            # Parse telemetry data
            telemetry_data = orjson.loads(msg.data)

            # Get device ID from originator
            device_id = msg.originator.id

            # Prepare telemetry payload
            # Format: {key: [{ts: timestamp, value: value}]}
            telemetry_payload = {}

            # Get timestamp from message or use current
            ts = msg.ts

            # Convert flat data to time-series format
            for key, value in telemetry_data.items():
                telemetry_payload[key] = [{"ts": ts, "value": value}]

            # Save to backend API
            await self._save_to_backend(device_id, telemetry_payload, ctx)

            self.log_debug(msg, f"Saved telemetry for device {device_id}: {list(telemetry_data.keys())}")

        except Exception as e:
            self.log_error(msg, f"Failed to save telemetry: {e}")
            raise

    async def _save_to_backend(self, device_id: str, telemetry_data: dict, ctx: TbContext):
        """
        Save telemetry to backend API

        Args:
            device_id: Device ID
            telemetry_data: Telemetry data
            ctx: Execution context
        """
        # Call backend telemetry API
        url = f"{settings.get_api_url()}/telemetry/DEVICE/{device_id}/timeseries/RULE_ENGINE"

        try:
            async with self.session.post(
                url,
                json=telemetry_data,
                headers={"Content-Type": "application/json"},
                timeout=aiohttp.ClientTimeout(total=5)
            ) as response:
                if response.status not in [200, 201]:
                    error_text = await response.text()
                    raise Exception(f"Backend API error: {response.status} - {error_text}")

        except Exception as e:
            self.log_error(None, f"Error calling backend API: {e}")
            raise

    async def destroy(self):
        """Cleanup"""
        if self.session:
            await self.session.close()


# Helper function to get API URL (will use actual backend URL in production)
def _get_api_url():
    """Get backend API URL"""
    # In production, this would come from configuration
    return "http://localhost:8080/api"
