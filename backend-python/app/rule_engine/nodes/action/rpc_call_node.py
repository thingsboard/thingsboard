"""
RPC Call Request Node
Sends RPC (Remote Procedure Call) requests to devices
Equivalent to: org.thingsboard.rule.engine.rpc.TbSendRpcRequestNode
"""
import orjson
import aiohttp
from typing import Dict, Any
from app.rule_engine.core.rule_node_base import TbActionNode
from app.rule_engine.models.rule_engine_msg import TbMsg, TbContext
from app.rule_engine.core.rule_engine_executor import register_node
from app.core.config import settings


@register_node("org.thingsboard.rule.engine.rpc.TbSendRPCRequestNode")
class TbSendRPCRequestNode(TbActionNode):
    """
    Send RPC request to device

    Configuration:
    {
        "timeoutInSeconds": 60,
        "rpcCallMethod": "${method}",  # RPC method name (can use template variables)
        "rpcCallBody": "${params}",  # RPC parameters (can use template variables)
        "sendResultToReplyTarget": true  # Send response to rule chain
    }

    Example RPC:
    - Method: "setTemperature"
    - Body: {"value": 25}

    This will call device.setTemperature({"value": 25})
    """

    async def init(self):
        """Initialize node"""
        self.timeout = self.config.get("timeoutInSeconds", 60)
        self.method_template = self.config.get("rpcCallMethod", "")
        self.body_template = self.config.get("rpcCallBody", "{}")
        self.send_result_to_reply = self.config.get("sendResultToReplyTarget", True)

        self.session = aiohttp.ClientSession()

        self.log_info(
            f"Initialized RPC call node: "
            f"method={self.method_template}, timeout={self.timeout}s"
        )

    async def execute(self, ctx: TbContext, msg: TbMsg):
        """
        Send RPC request to device

        Args:
            ctx: Execution context
            msg: Message containing RPC parameters
        """
        try:
            # Parse message data
            msg_data = orjson.loads(msg.data)

            # Build template context
            template_ctx = {
                **msg_data,
                **msg.metadata,
                "msgType": msg.type.value,
                "originatorId": msg.originator.id,
                "originatorType": msg.originator.entity_type,
            }

            # Process method name template
            method = self._process_template(self.method_template, template_ctx)

            # Process body template
            body_str = self._process_template(self.body_template, template_ctx)

            # Parse body as JSON if it's a string
            try:
                if isinstance(body_str, str):
                    body = orjson.loads(body_str)
                else:
                    body = body_str
            except:
                body = {}

            # Get device ID from message originator
            device_id = msg.originator.id

            # Send RPC request
            response = await self._send_rpc(device_id, method, body)

            self.log_info(msg, f"RPC call sent to device {device_id}: {method}")

            # Optionally send result back to rule chain
            if self.send_result_to_reply and response:
                self.log_debug(msg, f"RPC response: {response}")
                # In production, you would create a new message with the response
                # and route it to the reply chain

        except Exception as e:
            self.log_error(msg, f"Failed to send RPC request: {e}")
            raise

    def _process_template(self, template: str, context: Dict[str, Any]) -> str:
        """
        Process template string with variable substitution

        Args:
            template: Template string with ${var} placeholders
            context: Dictionary of variable values

        Returns:
            Processed string
        """
        if not template:
            return ""

        result = str(template)

        # Replace ${variable} with values from context
        for key, value in context.items():
            placeholder = f"${{{key}}}"
            if placeholder in result:
                result = result.replace(placeholder, str(value))

        return result

    async def _send_rpc(
        self,
        device_id: str,
        method: str,
        params: Dict[str, Any]
    ) -> Dict[str, Any]:
        """
        Send RPC request to device via backend API

        Args:
            device_id: Target device ID
            method: RPC method name
            params: RPC parameters

        Returns:
            RPC response
        """
        url = f"{settings.get_api_url()}/api/plugins/rpc/twoway/{device_id}"

        payload = {
            "method": method,
            "params": params,
            "timeout": self.timeout * 1000  # Convert to milliseconds
        }

        try:
            async with self.session.post(
                url,
                json=payload,
                timeout=aiohttp.ClientTimeout(total=self.timeout + 5)
            ) as response:
                if response.status not in [200, 201]:
                    error_text = await response.text()
                    raise Exception(f"RPC service error: {response.status} - {error_text}")

                return await response.json()

        except Exception as e:
            self.log_error(None, f"Error calling RPC service: {e}")
            raise

    async def destroy(self):
        """Cleanup"""
        if self.session:
            await self.session.close()
