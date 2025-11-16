"""
Rule Engine Message Model
Represents messages flowing through rule chains
"""
from typing import Dict, Any, Optional, List
from pydantic import BaseModel
from enum import Enum
from datetime import datetime
import uuid


class MessageType(str, Enum):
    """Message type enumeration"""
    POST_TELEMETRY_REQUEST = "POST_TELEMETRY_REQUEST"
    POST_ATTRIBUTES_REQUEST = "POST_ATTRIBUTES_REQUEST"
    ACTIVITY_EVENT = "ACTIVITY_EVENT"
    INACTIVITY_EVENT = "INACTIVITY_EVENT"
    CONNECT_EVENT = "CONNECT_EVENT"
    DISCONNECT_EVENT = "DISCONNECT_EVENT"
    ENTITY_CREATED = "ENTITY_CREATED"
    ENTITY_UPDATED = "ENTITY_UPDATED"
    ENTITY_DELETED = "ENTITY_DELETED"
    ENTITY_ASSIGNED = "ENTITY_ASSIGNED"
    ENTITY_UNASSIGNED = "ENTITY_UNASSIGNED"
    ATTRIBUTES_UPDATED = "ATTRIBUTES_UPDATED"
    ATTRIBUTES_DELETED = "ATTRIBUTES_DELETED"
    ALARM = "ALARM"
    REST_API_REQUEST = "REST_API_REQUEST"
    RPC_CALL_FROM_SERVER_TO_DEVICE = "RPC_CALL_FROM_SERVER_TO_DEVICE"
    TO_SERVER_RPC_REQUEST = "TO_SERVER_RPC_REQUEST"


class EntityType(str, Enum):
    """Entity type enumeration"""
    TENANT = "TENANT"
    CUSTOMER = "CUSTOMER"
    USER = "USER"
    DASHBOARD = "DASHBOARD"
    ASSET = "ASSET"
    DEVICE = "DEVICE"
    ALARM = "ALARM"
    RULE_CHAIN = "RULE_CHAIN"
    RULE_NODE = "RULE_NODE"
    ENTITY_VIEW = "ENTITY_VIEW"
    EDGE = "EDGE"


class EntityId(BaseModel):
    """Entity identifier"""
    id: str
    entity_type: EntityType


class TbMsg(BaseModel):
    """
    Rule engine message
    Core message object that flows through rule chains
    """
    # Message ID
    id: str = None

    # Message type
    type: MessageType

    # Originator (entity that generated the message)
    originator: EntityId

    # Customer ID (if applicable)
    customer_id: Optional[EntityId] = None

    # Metadata (additional context)
    metadata: Dict[str, str] = {}

    # Data payload (JSON)
    data: str = "{}"  # JSON string

    # Rule chain ID
    rule_chain_id: Optional[str] = None

    # Rule node ID (current processing node)
    rule_node_id: Optional[str] = None

    # Timestamp
    ts: int = None

    # Context data (not persisted, used during processing)
    ctx: Dict[str, Any] = {}

    def __init__(self, **data):
        super().__init__(**data)
        if self.id is None:
            self.id = str(uuid.uuid4())
        if self.ts is None:
            self.ts = int(datetime.utcnow().timestamp() * 1000)

    def copy_with_new_id(self) -> 'TbMsg':
        """Create a copy of the message with a new ID"""
        msg_dict = self.model_dump()
        msg_dict['id'] = str(uuid.uuid4())
        return TbMsg(**msg_dict)

    def copy_with_rule_chain_id(self, rule_chain_id: str) -> 'TbMsg':
        """Create a copy with updated rule chain ID"""
        msg_dict = self.model_dump()
        msg_dict['rule_chain_id'] = rule_chain_id
        msg_dict['id'] = str(uuid.uuid4())
        return TbMsg(**msg_dict)

    def get_data_as_dict(self) -> Dict[str, Any]:
        """Parse data JSON string to dict"""
        import orjson
        return orjson.loads(self.data)

    def set_data_from_dict(self, data: Dict[str, Any]):
        """Set data from dict (converts to JSON string)"""
        import orjson
        self.data = orjson.dumps(data).decode('utf-8')


class TbMsgMetaData(BaseModel):
    """Message metadata helper"""
    data: Dict[str, str] = {}

    def put_value(self, key: str, value: str):
        """Add metadata entry"""
        self.data[key] = value

    def get_value(self, key: str) -> Optional[str]:
        """Get metadata value"""
        return self.data.get(key)

    def values(self) -> Dict[str, str]:
        """Get all metadata"""
        return self.data


class RuleNodeConnectionType(str, Enum):
    """Connection types between rule nodes"""
    SUCCESS = "Success"
    FAILURE = "Failure"
    TRUE = "True"
    FALSE = "False"
    OTHER = "Other"
    # Custom types can be defined per node


class RuleChainConnectionInfo(BaseModel):
    """Connection between two rule nodes"""
    from_index: int
    to_index: int
    type: str  # Connection type (Success, Failure, True, False, etc.)


class RuleNodeConfiguration(BaseModel):
    """Base configuration for rule nodes"""
    pass


class RuleNode(BaseModel):
    """Rule node definition"""
    # Node ID
    id: str = None

    # Node type (e.g., "org.thingsboard.rule.engine.filter.TbJsFilterNode")
    type: str

    # Node name
    name: str

    # Debug mode
    debug_mode: bool = False

    # Configuration (node-specific)
    configuration: Dict[str, Any] = {}

    # Additional info
    additional_info: Optional[Dict[str, Any]] = None

    def __init__(self, **data):
        super().__init__(**data)
        if self.id is None:
            self.id = str(uuid.uuid4())


class RuleChainMetaData(BaseModel):
    """Rule chain metadata (nodes and connections)"""
    # Rule chain ID
    rule_chain_id: str

    # First node index (entry point)
    first_node_index: Optional[int] = None

    # List of nodes
    nodes: List[RuleNode] = []

    # Connections between nodes
    connections: List[RuleChainConnectionInfo] = []

    # Rule chain connections (to other rule chains)
    rule_chain_connections: Optional[Dict[str, List[RuleChainConnectionInfo]]] = None


class TbContext(BaseModel):
    """
    Rule engine execution context
    Provides access to services and data during rule chain execution
    """
    # Tenant ID
    tenant_id: str

    # Customer ID (optional)
    customer_id: Optional[str] = None

    # Rule chain ID
    rule_chain_id: str

    # Execution ID (for tracing)
    execution_id: str = None

    # Shared data between nodes (for same message)
    shared_data: Dict[str, Any] = {}

    def __init__(self, **data):
        super().__init__(**data)
        if self.execution_id is None:
            self.execution_id = str(uuid.uuid4())

    class Config:
        arbitrary_types_allowed = True
