"""
Customer Details Node
Fetches customer information and adds it to message metadata
Equivalent to: org.thingsboard.rule.engine.metadata.TbGetCustomerDetailsNode
"""
import orjson
import aiohttp
from typing import Dict, Any
from app.rule_engine.core.rule_node_base import TbEnrichmentNode
from app.rule_engine.models.rule_engine_msg import TbMsg, TbContext
from app.rule_engine.core.rule_engine_executor import register_node
from app.core.config import settings


@register_node("org.thingsboard.rule.engine.metadata.TbGetCustomerDetailsNode")
class TbGetCustomerDetailsNode(TbEnrichmentNode):
    """
    Fetch customer details and add to message metadata

    Configuration:
    {
        "detailsList": ["title", "country", "city", "address", "email", "phone"],
        "addToMetadata": true
    }

    Adds customer details to metadata with prefix 'customer'
    Example: customerTitle, customerCountry, customerCity, etc.
    """

    async def init(self):
        """Initialize node"""
        self.details_list = self.config.get("detailsList", [
            "title", "country", "city", "address", "email", "phone"
        ])
        self.add_to_metadata = self.config.get("addToMetadata", True)
        self.session = aiohttp.ClientSession()

        self.log_info(f"Initialized customer details node: details={self.details_list}")

    async def enrich(self, ctx: TbContext, msg: TbMsg) -> TbMsg:
        """
        Fetch customer details and enrich message

        Args:
            ctx: Execution context
            msg: Message to enrich

        Returns:
            Enriched message with customer details
        """
        try:
            # Get customer ID from message
            customer_id = msg.customer_id

            if customer_id is None:
                self.log_debug(msg, "No customer ID in message, skipping enrichment")
                return msg

            # Fetch customer details
            customer = await self._fetch_customer(customer_id.id)

            if not customer:
                self.log_debug(msg, f"Customer {customer_id.id} not found")
                return msg

            # Enrich metadata
            if self.add_to_metadata:
                new_metadata = msg.metadata.copy()

                for detail in self.details_list:
                    if detail in customer:
                        # Add with 'customer' prefix
                        metadata_key = f"customer{detail.capitalize()}"
                        new_metadata[metadata_key] = str(customer[detail])

                enriched_msg = msg.copy()
                enriched_msg.metadata = new_metadata

                self.log_debug(msg, f"Enriched with customer details: {self.details_list}")

                return enriched_msg
            else:
                # Add to message data
                msg_data = orjson.loads(msg.data)
                msg_data["customer"] = {
                    detail: customer.get(detail) for detail in self.details_list
                    if detail in customer
                }

                enriched_msg = msg.copy()
                enriched_msg.data = orjson.dumps(msg_data).decode('utf-8')

                return enriched_msg

        except Exception as e:
            self.log_error(msg, f"Failed to enrich with customer details: {e}")
            # Return original message on error
            return msg

    async def _fetch_customer(self, customer_id: str) -> Dict[str, Any]:
        """
        Fetch customer from backend API

        Args:
            customer_id: Customer ID

        Returns:
            Customer details dictionary
        """
        url = f"{settings.get_api_url()}/api/customer/{customer_id}"

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
            self.log_error(None, f"Error fetching customer: {e}")
            return {}

    async def destroy(self):
        """Cleanup"""
        if self.session:
            await self.session.close()
