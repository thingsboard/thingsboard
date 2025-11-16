"""
Rule Chain entity model
"""
from sqlalchemy import Column, String, Text, JSON, Boolean, Index, Enum as SQLEnum
from enum import Enum
from app.models.base import NamedEntity, TenantEntity


class RuleChainType(str, Enum):
    """Rule chain type"""
    CORE = "CORE"
    EDGE = "EDGE"


class RuleChain(NamedEntity, TenantEntity):
    """
    Rule Chain entity
    Defines data processing pipeline with nodes and connections
    """
    __tablename__ = "rule_chain"

    # Type
    type = Column(
        SQLEnum(RuleChainType, name="rule_chain_type_enum"),
        nullable=False,
        default=RuleChainType.CORE,
    )

    # First rule node ID (entry point)
    first_rule_node_id = Column(String(255), nullable=True)

    # Root flag
    root = Column(Boolean, default=False, nullable=False)

    # Debug mode
    debug_mode = Column(Boolean, default=False, nullable=False)

    # Configuration (JSON) - nodes, connections, metadata
    configuration = Column(JSON, nullable=True)

    # Additional info
    additional_info = Column(JSON, nullable=True)

    # External ID
    external_id = Column(String(255), nullable=True, index=True)

    __table_args__ = (
        Index('idx_rule_chain_tenant_id', 'tenant_id'),
        Index('idx_rule_chain_root', 'root'),
        Index('idx_rule_chain_search_text', 'search_text'),
    )

    def __repr__(self):
        return f"<RuleChain(id={self.id}, name={self.name}, root={self.root})>"
