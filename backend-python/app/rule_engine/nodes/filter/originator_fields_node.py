"""
Originator Fields Filter Node
Filters messages based on originator entity fields
Equivalent to: org.thingsboard.rule.engine.filter.TbOriginatorFieldsFilterNode
"""
import aiohttp
from typing import Dict, Any
from app.rule_engine.core.rule_node_base import TbFilterNode
from app.rule_engine.models.rule_engine_msg import TbMsg, TbContext
from app.rule_engine.core.rule_engine_executor import register_node
from app.core.config import settings


@register_node("org.thingsboard.rule.engine.filter.TbOriginatorFieldsFilterNode")
class TbOriginatorFieldsFilterNode(TbFilterNode):
    """
    Filter messages based on originator entity fields

    Configuration:
    {
        "fieldsFilter": {
            "name": "Temperature Sensor",
            "type": "thermometer",
            "label": "critical"
        },
        "checkAllKeys": true  # All fields must match vs any field matches
    }

    Fetches originator entity and checks if specified fields match.
    Returns True if fields match, False otherwise.
    """

    async def init(self):
        """Initialize node"""
        self.fields_filter = self.config.get("fieldsFilter", {})
        self.check_all_keys = self.config.get("checkAllKeys", True)
        self.session = aiohttp.ClientSession()

        self.log_info(
            f"Initialized originator fields filter: "
            f"fields={list(self.fields_filter.keys())}, checkAll={self.check_all_keys}"
        )

    async def filter(self, ctx: TbContext, msg: TbMsg) -> bool:
        """
        Check if originator fields match filter

        Args:
            ctx: Execution context
            msg: Message to filter

        Returns:
            True if fields match, False otherwise
        """
        try:
            entity_type = msg.originator.entity_type
            entity_id = msg.originator.id

            # Fetch originator entity
            entity = await self._fetch_entity(entity_type, entity_id)

            if not entity:
                self.log_debug(msg, "Entity not found")
                return False

            # Check field matches
            matches = []
            for field, expected_value in self.fields_filter.items():
                actual_value = entity.get(field)

                # Handle nested fields (e.g., "additionalInfo.description")
                if "." in field:
                    actual_value = self._get_nested_field(entity, field)

                matches.append(actual_value == expected_value)

            # Determine result based on checkAllKeys
            if self.check_all_keys:
                result = all(matches)  # All fields must match
            else:
                result = any(matches)  # Any field matches

            self.log_debug(msg, f"Fields match: {result}")
            return result

        except Exception as e:
            self.log_error(msg, f"Error checking originator fields: {e}")
            return False

    def _get_nested_field(self, data: Dict[str, Any], field_path: str) -> Any:
        """
        Get nested field value from dictionary

        Args:
            data: Dictionary to search
            field_path: Dot-separated field path (e.g., "additionalInfo.description")

        Returns:
            Field value or None if not found
        """
        parts = field_path.split(".")
        current = data

        for part in parts:
            if isinstance(current, dict) and part in current:
                current = current[part]
            else:
                return None

        return current

    async def _fetch_entity(self, entity_type: str, entity_id: str) -> Dict[str, Any]:
        """
        Fetch entity from backend API

        Args:
            entity_type: Entity type (DEVICE, ASSET, etc.)
            entity_id: Entity ID

        Returns:
            Entity data dictionary
        """
        # Map entity type to API endpoint
        endpoint_map = {
            "DEVICE": "device",
            "ASSET": "asset",
            "CUSTOMER": "customer",
            "TENANT": "tenant",
            "USER": "user",
            "DASHBOARD": "dashboard",
            "ENTITY_VIEW": "entityView"
        }

        endpoint = endpoint_map.get(entity_type, entity_type.lower())
        url = f"{settings.get_api_url()}/api/{endpoint}/{entity_id}"

        try:
            async with self.session.get(
                url,
                timeout=aiohttp.ClientTimeout(total=5)
            ) as response:
                if response.status == 200:
                    return await response.json()
                elif response.status == 404:
                    return {}
                else:
                    error_text = await response.text()
                    raise Exception(f"Backend API error: {response.status} - {error_text}")

        except Exception as e:
            self.log_error(None, f"Error fetching entity: {e}")
            return {}

    async def destroy(self):
        """Cleanup"""
        if self.session:
            await self.session.close()
