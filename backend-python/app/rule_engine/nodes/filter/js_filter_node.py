"""
JavaScript Filter Node
Filters messages based on JavaScript expression
Equivalent to: org.thingsboard.rule.engine.filter.TbJsFilterNode
"""
import orjson
from typing import Dict, Any
from app.rule_engine.core.rule_node_base import TbFilterNode
from app.rule_engine.models.rule_engine_msg import TbMsg, TbContext
from app.rule_engine.core.rule_engine_executor import register_node


@register_node("org.thingsboard.rule.engine.filter.TbJsFilterNode")
class TbJsFilterNode(TbFilterNode):
    """
    JavaScript filter node

    Configuration:
    {
        "jsScript": "return msg.temperature > 20;"
    }

    Returns True/False to route message
    """

    async def init(self):
        """Initialize node"""
        self.js_script = self.config.get("jsScript", "return true;")
        self.log_info(f"Initialized JS filter with script: {self.js_script[:50]}...")

    async def filter(self, ctx: TbContext, msg: TbMsg) -> bool:
        """
        Execute JavaScript filter

        Args:
            ctx: Execution context
            msg: Message to filter

        Returns:
            True if message passes filter, False otherwise
        """
        try:
            # Parse message data
            msg_data = orjson.loads(msg.data)

            # Create execution context for script
            script_ctx = {
                "msg": msg_data,
                "metadata": msg.metadata,
                "msgType": msg.type.value
            }

            # Execute JavaScript (using simple eval for demo - in production use a proper JS engine)
            # For production, use PyMini-racer or similar
            result = self._execute_js(script_ctx, self.js_script)

            return bool(result)

        except Exception as e:
            self.log_error(msg, f"JS filter error: {e}")
            return False

    def _execute_js(self, ctx: Dict[str, Any], script: str) -> bool:
        """
        Execute JavaScript code

        For demo purposes, this is a simplified Python evaluation
        In production, use PyMiniRacer or similar JavaScript engine

        Args:
            ctx: Execution context
            script: JavaScript code

        Returns:
            Boolean result
        """
        # Simple Python-based evaluation for demo
        # Convert common JS patterns to Python
        python_script = script.replace("msg.", "msg['").replace(";", "'")

        # Create local context
        local_ctx = {
            "msg": ctx["msg"],
            "metadata": ctx["metadata"],
            "msgType": ctx["msgType"]
        }

        try:
            # Evaluate (UNSAFE - for demo only)
            # In production, use a proper sandboxed JS engine
            result = eval(python_script, {"__builtins__": {}}, local_ctx)
            return result
        except:
            # Fallback to checking simple conditions
            # temperature > 20 pattern
            if ">" in script:
                parts = script.replace("return", "").replace("msg.", "").split(">")
                if len(parts) == 2:
                    key = parts[0].strip()
                    value = float(parts[1].strip().replace(";", ""))
                    if key in ctx["msg"]:
                        return ctx["msg"][key] > value
            return False

    async def destroy(self):
        """Cleanup"""
        pass
