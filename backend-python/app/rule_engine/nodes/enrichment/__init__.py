"""
Enrichment Nodes
Nodes that add additional data to messages from external sources
"""
from .originator_attributes_node import TbGetAttributesNode
from .customer_details_node import TbGetCustomerDetailsNode
from .related_attributes_node import TbGetRelatedAttributeNode

__all__ = [
    "TbGetAttributesNode",
    "TbGetCustomerDetailsNode",
    "TbGetRelatedAttributeNode",
]
