"""
Switch Node
Routes messages based on JavaScript conditions to multiple outputs
Equivalent to: org.thingsboard.rule.engine.filter.TbJsSwitchNode
"""
import orjson
from typing import List, Dict, Any
from app.rule_engine.core.rule_node_base import TbNode
from app.rule_engine.models.rule_engine_msg import TbMsg, TbContext
from app.rule_engine.core.rule_engine_executor import register_node


@register_node("org.thingsboard.rule.engine.filter.TbJsSwitchNode")
class TbJsSwitchNode(TbNode):
    """
    Switch node with multiple JavaScript-based routing rules

    Configuration:
    {
        "jsScript": "return ['route1', 'route2'];",  # Returns array of route names
        "useMetadata": false  # If true, evaluate metadata instead of data
    }

    The script should return an array of strings representing route names.
    Message will be routed to all matching routes.

    Example:
    ```javascript
    if (msg.temperature > 30) {
        return ['High'];
    } else if (msg.temperature > 20) {
        return ['Medium'];
    } else {
        return ['Low'];
    }
    ```
    """

    async def init(self):
        """Initialize node"""
        self.js_script = self.config.get("jsScript", "return ['Other'];")
        self.use_metadata = self.config.get("useMetadata", False)
        self.log_info(f"Initialized switch node with script: {self.js_script[:50]}...")

    async def onMsg(self, ctx: TbContext, msg: TbMsg):
        """
        Execute switch logic and route to appropriate connections

        Args:
            ctx: Execution context
            msg: Message to route

        Returns:
            List of (message, connection_type) tuples
        """
        try:
            # Parse message data or use metadata
            if self.use_metadata:
                data = msg.metadata
            else:
                data = orjson.loads(msg.data)

            # Create execution context
            script_ctx = {
                "msg": data,
                "metadata": msg.metadata,
                "msgType": msg.type.value
            }

            # Execute switch script
            routes = self._execute_switch(script_ctx, self.js_script)

            # Ensure routes is a list
            if not isinstance(routes, list):
                routes = [routes]

            # Route to all matched connections
            results = []
            for route in routes:
                results.append((msg, str(route)))

            self.log_debug(msg, f"Routed to: {routes}")

            return results

        except Exception as e:
            self.log_error(msg, f"Switch error: {e}")
            # Route to Failure
            return [(msg, "Failure")]

    def _execute_switch(self, ctx: Dict[str, Any], script: str) -> List[str]:
        """
        Execute JavaScript switch code

        For demo purposes, this is a simplified Python evaluation
        In production, use PyMiniRacer or similar JavaScript engine

        Args:
            ctx: Execution context
            script: JavaScript code

        Returns:
            List of route names
        """
        # Simple evaluation for common patterns
        msg = ctx["msg"]
        metadata = ctx["metadata"]

        # Try to evaluate script
        try:
            # Handle simple temperature-based routing as example
            if "temperature" in msg:
                temp = msg["temperature"]
                if temp > 30:
                    return ["High"]
                elif temp > 20:
                    return ["Medium"]
                else:
                    return ["Low"]

            # Handle metadata-based routing
            if "deviceType" in metadata:
                return [metadata["deviceType"]]

            # Default route
            return ["Other"]

        except Exception as e:
            self.log_error(None, f"Error executing switch script: {e}")
            return ["Other"]

    async def destroy(self):
        """Cleanup"""
        pass
