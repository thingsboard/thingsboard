"""
Rename Keys Node
Renames keys in message data
Equivalent to: org.thingsboard.rule.engine.transform.TbRenameKeysNode
"""
import orjson
from app.rule_engine.core.rule_node_base import TbTransformationNode
from app.rule_engine.models.rule_engine_msg import TbMsg, TbContext
from app.rule_engine.core.rule_engine_executor import register_node


@register_node("org.thingsboard.rule.engine.transform.TbRenameKeysNode")
class TbRenameKeysNode(TbTransformationNode):
    """
    Rename keys in message data

    Configuration:
    {
        "renameKeysMapping": {
            "temp": "temperature",
            "hum": "humidity"
        }
    }
    """

    async def init(self):
        """Initialize node"""
        self.rename_mapping = self.config.get("renameKeysMapping", {})
        self.log_info(f"Initialized rename keys node with {len(self.rename_mapping)} mappings")

    async def transform(self, ctx: TbContext, msg: TbMsg) -> TbMsg:
        """
        Rename keys in message data

        Args:
            ctx: Execution context
            msg: Message to transform

        Returns:
            Message with renamed keys
        """
        try:
            # Parse message data
            msg_data = orjson.loads(msg.data)

            # Rename keys
            renamed_data = {}
            for key, value in msg_data.items():
                # Check if key should be renamed
                new_key = self.rename_mapping.get(key, key)
                renamed_data[new_key] = value

            # Create new message with renamed data
            transformed_msg = msg.copy_with_new_id()
            transformed_msg.set_data_from_dict(renamed_data)

            self.log_debug(msg, f"Renamed {len(self.rename_mapping)} keys")

            return transformed_msg

        except Exception as e:
            self.log_error(msg, f"Rename keys error: {e}")
            raise

    async def destroy(self):
        """Cleanup"""
        pass
