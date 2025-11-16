"""
Originator Type Filter Node
Routes messages based on originator entity type
Equivalent to: org.thingsboard.rule.engine.filter.TbOriginatorTypeFilterNode
"""
from app.rule_engine.core.rule_node_base import TbNode
from app.rule_engine.models.rule_engine_msg import TbMsg, TbContext
from app.rule_engine.core.rule_engine_executor import register_node


@register_node("org.thingsboard.rule.engine.filter.TbOriginatorTypeFilterNode")
class TbOriginatorTypeFilterNode(TbNode):
    """
    Route messages based on originator entity type

    Configuration:
    {
        "originatorTypes": ["DEVICE", "ASSET", "ENTITY_VIEW"]
    }

    Routes message to connection matching the entity type.
    For example, if originator is DEVICE, routes to "DEVICE" connection.
    If type not in originatorTypes list, routes to "Other" connection.
    """

    async def init(self):
        """Initialize node"""
        self.originator_types = self.config.get("originatorTypes", [
            "DEVICE", "ASSET", "ENTITY_VIEW", "CUSTOMER", "TENANT", "DASHBOARD"
        ])
        self.log_info(f"Initialized originator type filter: {self.originator_types}")

    async def onMsg(self, ctx: TbContext, msg: TbMsg):
        """
        Route based on originator type

        Args:
            ctx: Execution context
            msg: Message to route

        Returns:
            List of (message, connection_type) tuples
        """
        entity_type = msg.originator.entity_type

        # Check if entity type is in the allowed list
        if entity_type in self.originator_types:
            connection_type = entity_type
        else:
            connection_type = "Other"

        self.log_debug(msg, f"Routing to: {connection_type}")

        return [(msg, connection_type)]

    async def destroy(self):
        """Cleanup"""
        pass
