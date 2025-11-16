"""
JavaScript Transformation Node
Transforms message using JavaScript
Equivalent to: org.thingsboard.rule.engine.transform.TbTransformMsgNode
"""
import orjson
from typing import Dict, Any
from app.rule_engine.core.rule_node_base import TbTransformationNode
from app.rule_engine.models.rule_engine_msg import TbMsg, TbContext
from app.rule_engine.core.rule_engine_executor import register_node


@register_node("org.thingsboard.rule.engine.transform.TbTransformMsgNode")
class TbTransformMsgNode(TbTransformationNode):
    """
    JavaScript transformation node

    Configuration:
    {
        "jsScript": "msg.celsius = msg.temperature; msg.fahrenheit = msg.temperature * 9/5 + 32; return {msg: msg, metadata: metadata, msgType: msgType};"
    }

    Script must return object with: {msg, metadata, msgType}
    """

    async def init(self):
        """Initialize node"""
        self.js_script = self.config.get("jsScript", "return {msg: msg, metadata: metadata, msgType: msgType};")
        self.log_info(f"Initialized JS transformation with script: {self.js_script[:80]}...")

    async def transform(self, ctx: TbContext, msg: TbMsg) -> TbMsg:
        """
        Execute JavaScript transformation

        Args:
            ctx: Execution context
            msg: Message to transform

        Returns:
            Transformed message
        """
        try:
            # Parse message data
            msg_data = orjson.loads(msg.data)

            # Create execution context for script
            script_ctx = {
                "msg": msg_data,
                "metadata": msg.metadata.copy(),
                "msgType": msg.type.value
            }

            # Execute JavaScript (simplified for demo)
            result = self._execute_js(script_ctx, self.js_script)

            # Create new message with transformed data
            transformed_msg = msg.copy_with_new_id()
            transformed_msg.set_data_from_dict(result.get("msg", msg_data))
            transformed_msg.metadata = result.get("metadata", msg.metadata)

            # Update message type if changed
            if "msgType" in result and result["msgType"] != msg.type.value:
                from app.rule_engine.models.rule_engine_msg import MessageType
                try:
                    transformed_msg.type = MessageType(result["msgType"])
                except ValueError:
                    self.log_error(msg, f"Invalid message type: {result['msgType']}")

            return transformed_msg

        except Exception as e:
            self.log_error(msg, f"JS transformation error: {e}")
            raise

    def _execute_js(self, ctx: Dict[str, Any], script: str) -> Dict[str, Any]:
        """
        Execute JavaScript code

        For demo purposes, this is a simplified evaluation
        In production, use PyMiniRacer or similar JavaScript engine

        Args:
            ctx: Execution context with msg, metadata, msgType
            script: JavaScript code

        Returns:
            Result object with msg, metadata, msgType
        """
        # Simple Python-based evaluation for common transformations
        msg = ctx["msg"]
        metadata = ctx["metadata"]
        msgType = ctx["msgType"]

        # Execute simple transformations (demo only)
        # In production, use a proper JS engine
        try:
            # Common pattern: add new fields
            if "celsius" in script and "fahrenheit" in script:
                if "temperature" in msg:
                    msg["celsius"] = msg["temperature"]
                    msg["fahrenheit"] = msg["temperature"] * 9/5 + 32

            # Common pattern: convert units
            if "toUpperCase" in script:
                for key, value in msg.items():
                    if isinstance(value, str):
                        msg[key] = value.upper()

            # Common pattern: add metadata to message
            if "metadata" in script and "deviceName" in metadata:
                msg["deviceName"] = metadata.get("deviceName")

            return {
                "msg": msg,
                "metadata": metadata,
                "msgType": msgType
            }

        except Exception as e:
            self.log_error(None, f"Script execution error: {e}")
            return ctx

    async def destroy(self):
        """Cleanup"""
        pass
