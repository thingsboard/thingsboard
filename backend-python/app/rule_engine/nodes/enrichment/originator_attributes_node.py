"""
Originator Attributes Node
Fetches attributes from the message originator and adds them to metadata
Equivalent to: org.thingsboard.rule.engine.metadata.TbGetAttributesNode
"""
import orjson
import aiohttp
from typing import List, Dict, Any
from app.rule_engine.core.rule_node_base import TbEnrichmentNode
from app.rule_engine.models.rule_engine_msg import TbMsg, TbContext
from app.rule_engine.core.rule_engine_executor import register_node
from app.core.config import settings


@register_node("org.thingsboard.rule.engine.metadata.TbGetAttributesNode")
class TbGetAttributesNode(TbEnrichmentNode):
    """
    Fetch attributes from originator entity and add to message metadata

    Configuration:
    {
        "clientAttributeNames": ["model", "location"],  # CLIENT_SCOPE attributes
        "sharedAttributeNames": ["threshold", "config"],  # SHARED_SCOPE attributes
        "serverAttributeNames": ["lastActivityTime"],  # SERVER_SCOPE attributes
        "latestTsKeyNames": ["temperature", "humidity"],  # Latest telemetry
        "tellFailureIfAbsent": false,  # Route to Failure if attributes not found
        "getLatestValueWithTs": false  # Include timestamp with telemetry values
    }
    """

    async def init(self):
        """Initialize node"""
        self.client_attributes = self.config.get("clientAttributeNames", [])
        self.shared_attributes = self.config.get("sharedAttributeNames", [])
        self.server_attributes = self.config.get("serverAttributeNames", [])
        self.latest_ts_keys = self.config.get("latestTsKeyNames", [])
        self.fail_if_absent = self.config.get("tellFailureIfAbsent", False)
        self.get_ts = self.config.get("getLatestValueWithTs", False)

        self.session = aiohttp.ClientSession()

        self.log_info(
            f"Initialized originator attributes node: "
            f"client={self.client_attributes}, shared={self.shared_attributes}, "
            f"server={self.server_attributes}, telemetry={self.latest_ts_keys}"
        )

    async def enrich(self, ctx: TbContext, msg: TbMsg) -> TbMsg:
        """
        Fetch attributes and enrich message metadata

        Args:
            ctx: Execution context
            msg: Message to enrich

        Returns:
            Enriched message with attributes in metadata
        """
        try:
            entity_type = msg.originator.entity_type
            entity_id = msg.originator.id

            # Fetch all requested attributes
            attributes = {}

            # CLIENT_SCOPE attributes
            if self.client_attributes:
                client_attrs = await self._fetch_attributes(
                    entity_type, entity_id, "CLIENT_SCOPE", self.client_attributes
                )
                attributes.update(client_attrs)

            # SHARED_SCOPE attributes
            if self.shared_attributes:
                shared_attrs = await self._fetch_attributes(
                    entity_type, entity_id, "SHARED_SCOPE", self.shared_attributes
                )
                attributes.update(shared_attrs)

            # SERVER_SCOPE attributes
            if self.server_attributes:
                server_attrs = await self._fetch_attributes(
                    entity_type, entity_id, "SERVER_SCOPE", self.server_attributes
                )
                attributes.update(server_attrs)

            # Latest telemetry
            if self.latest_ts_keys:
                telemetry = await self._fetch_telemetry(
                    entity_type, entity_id, self.latest_ts_keys
                )
                attributes.update(telemetry)

            # Check if all requested attributes were found
            if self.fail_if_absent and not attributes:
                raise Exception("No attributes found for originator")

            # Create new message with enriched metadata
            new_metadata = msg.metadata.copy()
            for key, value in attributes.items():
                new_metadata[key] = str(value) if not isinstance(value, str) else value

            enriched_msg = msg.copy()
            enriched_msg.metadata = new_metadata

            self.log_debug(msg, f"Enriched with {len(attributes)} attributes")

            return enriched_msg

        except Exception as e:
            self.log_error(msg, f"Failed to enrich with attributes: {e}")
            raise

    async def _fetch_attributes(
        self,
        entity_type: str,
        entity_id: str,
        scope: str,
        keys: List[str]
    ) -> Dict[str, Any]:
        """
        Fetch attributes from backend API

        Args:
            entity_type: Entity type (DEVICE, ASSET, etc.)
            entity_id: Entity ID
            scope: Attribute scope (CLIENT_SCOPE, SHARED_SCOPE, SERVER_SCOPE)
            keys: Attribute keys to fetch

        Returns:
            Dictionary of attribute key-value pairs
        """
        url = (
            f"{settings.get_api_url()}/plugins/telemetry/{entity_type}/{entity_id}/values/attributes/{scope}"
            f"?keys={','.join(keys)}"
        )

        try:
            async with self.session.get(
                url,
                timeout=aiohttp.ClientTimeout(total=5)
            ) as response:
                if response.status == 200:
                    data = await response.json()
                    # API returns list of {key, value} objects
                    return {item["key"]: item["value"] for item in data}
                elif response.status == 404:
                    # No attributes found - return empty dict
                    return {}
                else:
                    error_text = await response.text()
                    raise Exception(f"Backend API error: {response.status} - {error_text}")

        except Exception as e:
            self.log_error(None, f"Error fetching attributes: {e}")
            return {}

    async def _fetch_telemetry(
        self,
        entity_type: str,
        entity_id: str,
        keys: List[str]
    ) -> Dict[str, Any]:
        """
        Fetch latest telemetry values

        Args:
            entity_type: Entity type (DEVICE, ASSET, etc.)
            entity_id: Entity ID
            keys: Telemetry keys to fetch

        Returns:
            Dictionary of telemetry key-value pairs
        """
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
                    # API returns {key: [{ts, value}]} format
                    result = {}
                    for key, values in data.items():
                        if values and len(values) > 0:
                            latest = values[0]
                            if self.get_ts:
                                result[key] = {"value": latest["value"], "ts": latest["ts"]}
                            else:
                                result[key] = latest["value"]
                    return result
                elif response.status == 404:
                    return {}
                else:
                    error_text = await response.text()
                    raise Exception(f"Backend API error: {response.status} - {error_text}")

        except Exception as e:
            self.log_error(None, f"Error fetching telemetry: {e}")
            return {}

    async def destroy(self):
        """Cleanup"""
        if self.session:
            await self.session.close()
