"""
Related Attributes Node
Fetches attributes from related entities and adds them to metadata
Equivalent to: org.thingsboard.rule.engine.metadata.TbGetRelatedAttributeNode
"""
import orjson
import aiohttp
from typing import List, Dict, Any
from app.rule_engine.core.rule_node_base import TbEnrichmentNode
from app.rule_engine.models.rule_engine_msg import TbMsg, TbContext
from app.rule_engine.core.rule_engine_executor import register_node
from app.core.config import settings


@register_node("org.thingsboard.rule.engine.metadata.TbGetRelatedAttributeNode")
class TbGetRelatedAttributeNode(TbEnrichmentNode):
    """
    Fetch attributes from related entities and add to message metadata

    Configuration:
    {
        "attrMapping": {
            "temperature": "relatedTemperature",
            "humidity": "relatedHumidity"
        },
        "relationsQuery": {
            "direction": "FROM",  # FROM, TO
            "maxLevel": 1,
            "filters": [
                {
                    "relationType": "Contains",
                    "entityTypes": ["ASSET", "DEVICE"]
                }
            ]
        },
        "telemetry": false
    }
    """

    async def init(self):
        """Initialize node"""
        self.attr_mapping = self.config.get("attrMapping", {})
        self.relations_query = self.config.get("relationsQuery", {})
        self.is_telemetry = self.config.get("telemetry", False)

        self.direction = self.relations_query.get("direction", "FROM")
        self.max_level = self.relations_query.get("maxLevel", 1)
        self.filters = self.relations_query.get("filters", [])

        self.session = aiohttp.ClientSession()

        self.log_info(
            f"Initialized related attributes node: "
            f"direction={self.direction}, maxLevel={self.max_level}"
        )

    async def enrich(self, ctx: TbContext, msg: TbMsg) -> TbMsg:
        """
        Fetch attributes from related entities and enrich message

        Args:
            ctx: Execution context
            msg: Message to enrich

        Returns:
            Enriched message with related attributes
        """
        try:
            entity_type = msg.originator.entity_type
            entity_id = msg.originator.id

            # Find related entities
            related_entities = await self._find_related_entities(
                entity_type, entity_id
            )

            if not related_entities:
                self.log_debug(msg, "No related entities found")
                return msg

            # Fetch attributes from first related entity
            # (In production, you might want to handle multiple related entities)
            related_entity = related_entities[0]

            attributes = {}
            if self.is_telemetry:
                attributes = await self._fetch_telemetry(
                    related_entity["toId"]["entityType"],
                    related_entity["toId"]["id"],
                    list(self.attr_mapping.keys())
                )
            else:
                # Fetch from CLIENT_SCOPE by default
                attributes = await self._fetch_attributes(
                    related_entity["toId"]["entityType"],
                    related_entity["toId"]["id"],
                    list(self.attr_mapping.keys())
                )

            # Map attributes using configured mapping
            new_metadata = msg.metadata.copy()
            for source_key, target_key in self.attr_mapping.items():
                if source_key in attributes:
                    new_metadata[target_key] = str(attributes[source_key])

            enriched_msg = msg.copy()
            enriched_msg.metadata = new_metadata

            self.log_debug(msg, f"Enriched with {len(attributes)} related attributes")

            return enriched_msg

        except Exception as e:
            self.log_error(msg, f"Failed to enrich with related attributes: {e}")
            # Return original message on error
            return msg

    async def _find_related_entities(
        self,
        entity_type: str,
        entity_id: str
    ) -> List[Dict[str, Any]]:
        """
        Find related entities using relations query

        Args:
            entity_type: Source entity type
            entity_id: Source entity ID

        Returns:
            List of related entities
        """
        url = f"{settings.get_api_url()}/api/relations/info"

        # Build query payload
        query_payload = {
            "parameters": {
                "rootId": entity_id,
                "rootType": entity_type,
                "direction": self.direction,
                "maxLevel": self.max_level
            },
            "filters": self.filters
        }

        try:
            async with self.session.post(
                url,
                json=query_payload,
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
            self.log_error(None, f"Error finding related entities: {e}")
            return []

    async def _fetch_attributes(
        self,
        entity_type: str,
        entity_id: str,
        keys: List[str]
    ) -> Dict[str, Any]:
        """Fetch attributes from entity"""
        url = (
            f"{settings.get_api_url()}/plugins/telemetry/{entity_type}/{entity_id}/values/attributes/CLIENT_SCOPE"
            f"?keys={','.join(keys)}"
        )

        try:
            async with self.session.get(
                url,
                timeout=aiohttp.ClientTimeout(total=5)
            ) as response:
                if response.status == 200:
                    data = await response.json()
                    return {item["key"]: item["value"] for item in data}
                else:
                    return {}

        except Exception as e:
            self.log_error(None, f"Error fetching attributes: {e}")
            return {}

    async def _fetch_telemetry(
        self,
        entity_type: str,
        entity_id: str,
        keys: List[str]
    ) -> Dict[str, Any]:
        """Fetch latest telemetry values"""
        url = (
            f"{settings.get_api_url()}/plugins/telemetry/{entity_type}/{entity_id}/values/timeseries"
            f"?keys={','.join(keys)}"
        )

        try:
            async with self.session.get(
                url,
                timeout=aiohttp.ClientTimeout(total=5)
            ) as response:
                if response.status == 200:
                    data = await response.json()
                    result = {}
                    for key, values in data.items():
                        if values and len(values) > 0:
                            result[key] = values[0]["value"]
                    return result
                else:
                    return {}

        except Exception as e:
            self.log_error(None, f"Error fetching telemetry: {e}")
            return {}

    async def destroy(self):
        """Cleanup"""
        if self.session:
            await self.session.close()
