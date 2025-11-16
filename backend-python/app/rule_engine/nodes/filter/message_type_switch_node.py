"""
Message Type Switch Node
Routes messages based on message type
Equivalent to: org.thingsboard.rule.engine.filter.TbMsgTypeSwitchNode
"""
from app.rule_engine.core.rule_node_base import TbNode
from app.rule_engine.models.rule_engine_msg import TbMsg, TbContext
from app.rule_engine.core.rule_engine_executor import register_node


@register_node("org.thingsboard.rule.engine.filter.TbMsgTypeSwitchNode")
class TbMsgTypeSwitchNode(TbNode):
    """
    Message type switch node
    Routes messages based on their message type
    """

    async def init(self):
        """Initialize node"""
        self.log_info("Initialized message type switch node")

    async def onMsg(self, ctx: TbContext, msg: TbMsg):
        """Route based on message type"""
        # Return message with connection type matching the message type
        return [(msg, msg.type.value)]

    async def destroy(self):
        """Cleanup"""
        pass
