"""
Check Relation Node
Checks if a relation exists between entities
Equivalent to: org.thingsboard.rule.engine.filter.TbCheckRelationNode
"""
import aiohttp
from typing import Dict, Any
from app.rule_engine.core.rule_node_base import TbFilterNode
from app.rule_engine.models.rule_engine_msg import TbMsg, TbContext
from app.rule_engine.core.rule_engine_executor import register_node
from app.core.config import settings


@register_node("org.thingsboard.rule.engine.filter.TbCheckRelationNode")
class TbCheckRelationNode(TbFilterNode):
    """
    Check if relation exists between message originator and specified entity

    Configuration:
    {
        "checkForSingleEntity": true,
        "direction": "FROM",  # FROM, TO
        "entityType": "ASSET",
        "entityId": "e5e5e5e5-e5e5-e5e5-e5e5-e5e5e5e5e5e5",
        "relationType": "Contains"
    }

    Or check any relation:
    {
        "checkForSingleEntity": false,
        "direction": "FROM",
        "entityType": "ASSET",
        "relationType": "Contains"
    }

    Returns True if relation exists, False otherwise
    """

    async def init(self):
        """Initialize node"""
        self.check_single = self.config.get("checkForSingleEntity", False)
        self.direction = self.config.get("direction", "FROM")
        self.entity_type = self.config.get("entityType")
        self.entity_id = self.config.get("entityId")
        self.relation_type = self.config.get("relationType", "Contains")

        self.session = aiohttp.ClientSession()

        self.log_info(
            f"Initialized check relation node: "
            f"type={self.relation_type}, direction={self.direction}, "
            f"entityType={self.entity_type}"
        )

    async def filter(self, ctx: TbContext, msg: TbMsg) -> bool:
        """
        Check if relation exists

        Args:
            ctx: Execution context
            msg: Message to filter

        Returns:
            True if relation exists, False otherwise
        """
        try:
            entity_type = msg.originator.entity_type
            entity_id = msg.originator.id

            if self.check_single and self.entity_id:
                # Check relation to specific entity
                exists = await self._check_relation(
                    entity_type,
                    entity_id,
                    self.entity_type,
                    self.entity_id,
                    self.relation_type
                )
            else:
                # Check if any relation of this type exists
                relations = await self._find_relations(
                    entity_type,
                    entity_id,
                    self.relation_type
                )
                exists = len(relations) > 0

            self.log_debug(msg, f"Relation exists: {exists}")
            return exists

        except Exception as e:
            self.log_error(msg, f"Error checking relation: {e}")
            return False

    async def _check_relation(
        self,
        from_type: str,
        from_id: str,
        to_type: str,
        to_id: str,
        relation_type: str
    ) -> bool:
        """
        Check if specific relation exists

        Args:
            from_type: Source entity type
            from_id: Source entity ID
            to_type: Target entity type
            to_id: Target entity ID
            relation_type: Relation type

        Returns:
            True if relation exists
        """
        url = f"{settings.get_api_url()}/api/relation"

        params = {
            "fromId": from_id,
            "fromType": from_type,
            "relationType": relation_type,
            "toId": to_id,
            "toType": to_type
        }

        try:
            async with self.session.get(
                url,
                params=params,
                timeout=aiohttp.ClientTimeout(total=5)
            ) as response:
                if response.status == 200:
                    relation = await response.json()
                    return relation is not None
                elif response.status == 404:
                    return False
                else:
                    error_text = await response.text()
                    raise Exception(f"Backend API error: {response.status} - {error_text}")

        except Exception as e:
            self.log_error(None, f"Error checking relation: {e}")
            return False

    async def _find_relations(
        self,
        entity_type: str,
        entity_id: str,
        relation_type: str
    ) -> list:
        """
        Find all relations of specified type

        Args:
            entity_type: Entity type
            entity_id: Entity ID
            relation_type: Relation type to find

        Returns:
            List of relations
        """
        # Build URL based on direction
        if self.direction == "FROM":
            url = f"{settings.get_api_url()}/api/relations"
            params = {
                "fromId": entity_id,
                "fromType": entity_type,
                "relationType": relation_type
            }
        else:  # TO
            url = f"{settings.get_api_url()}/api/relations"
            params = {
                "toId": entity_id,
                "toType": entity_type,
                "relationType": relation_type
            }

        try:
            async with self.session.get(
                url,
                params=params,
                timeout=aiohttp.ClientTimeout(total=5)
            ) as response:
                if response.status == 200:
                    return await response.json()
                elif response.status == 404:
                    return []
                else:
                    error_text = await response.text()
                    raise Exception(f"Backend API error: {response.status} - {error_text}")

        except Exception as e:
            self.log_error(None, f"Error finding relations: {e}")
            return []

    async def destroy(self):
        """Cleanup"""
        if self.session:
            await self.session.close()
