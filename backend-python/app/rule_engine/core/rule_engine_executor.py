"""
Rule Engine Executor
Manages rule chain execution and message routing
"""
import asyncio
from typing import Dict, List, Optional, Type
from collections import defaultdict
from app.rule_engine.models.rule_engine_msg import (
    TbMsg, TbContext, RuleChainMetaData, RuleNode, RuleChainConnectionInfo
)
from app.rule_engine.core.rule_node_base import TbNode, TbRuleChainInputNode
from app.core.logging import get_logger

logger = get_logger(__name__)


class RuleNodeRegistry:
    """
    Registry of available rule node types
    Maps node type strings to node classes
    """

    def __init__(self):
        self._node_types: Dict[str, Type[TbNode]] = {}

    def register(self, node_type: str, node_class: Type[TbNode]):
        """Register a rule node type"""
        self._node_types[node_type] = node_class
        logger.info(f"Registered rule node type: {node_type}")

    def get_node_class(self, node_type: str) -> Optional[Type[TbNode]]:
        """Get node class by type"""
        return self._node_types.get(node_type)

    def is_registered(self, node_type: str) -> bool:
        """Check if node type is registered"""
        return node_type in self._node_types


# Global node registry
node_registry = RuleNodeRegistry()


def register_node(node_type: str):
    """Decorator to register rule node types"""
    def decorator(cls: Type[TbNode]):
        node_registry.register(node_type, cls)
        return cls
    return decorator


class RuleChainExecutor:
    """
    Executes a rule chain
    Manages nodes, connections, and message routing
    """

    def __init__(self, rule_chain_id: str, metadata: RuleChainMetaData):
        """
        Initialize executor

        Args:
            rule_chain_id: Rule chain ID
            metadata: Rule chain metadata (nodes and connections)
        """
        self.rule_chain_id = rule_chain_id
        self.metadata = metadata

        # Node instances (index -> TbNode)
        self.nodes: Dict[int, TbNode] = {}

        # Node index by ID
        self.node_index_by_id: Dict[str, int] = {}

        # Connections (from_index -> list of (to_index, connection_type))
        self.connections: Dict[int, List[tuple]] = defaultdict(list)

        # Initialized flag
        self.initialized = False

    async def init(self):
        """Initialize all nodes and build connection map"""
        if self.initialized:
            return

        logger.info(f"Initializing rule chain executor: {self.rule_chain_id}")

        # Create node instances
        for idx, node_def in enumerate(self.metadata.nodes):
            try:
                node_class = node_registry.get_node_class(node_def.type)

                if node_class is None:
                    logger.warning(f"Unknown node type: {node_def.type}, using base node")
                    # Use a pass-through node
                    node = TbRuleChainInputNode(node_def)
                else:
                    node = node_class(node_def)

                await node.init()

                self.nodes[idx] = node
                self.node_index_by_id[node_def.id] = idx

                logger.debug(f"Initialized node {idx}: {node_def.name} ({node_def.type})")

            except Exception as e:
                logger.error(f"Error initializing node {node_def.name}: {e}")
                raise

        # Build connection map
        for conn in self.metadata.connections:
            self.connections[conn.from_index].append((conn.to_index, conn.type))

        logger.info(
            f"Rule chain {self.rule_chain_id} initialized: "
            f"{len(self.nodes)} nodes, {len(self.metadata.connections)} connections"
        )

        self.initialized = True

    async def process_message(self, ctx: TbContext, msg: TbMsg):
        """
        Process a message through the rule chain

        Args:
            ctx: Execution context
            msg: Message to process
        """
        if not self.initialized:
            await self.init()

        # Start from first node
        if self.metadata.first_node_index is None:
            logger.warning(f"No first node defined for rule chain {self.rule_chain_id}")
            return

        logger.debug(f"Processing message {msg.id} in rule chain {self.rule_chain_id}")

        # Process starting from first node
        await self._process_node(ctx, msg, self.metadata.first_node_index)

    async def _process_node(self, ctx: TbContext, msg: TbMsg, node_index: int):
        """
        Process message through a specific node

        Args:
            ctx: Execution context
            msg: Message
            node_index: Index of node to process
        """
        if node_index not in self.nodes:
            logger.error(f"Node index {node_index} not found in rule chain {self.rule_chain_id}")
            return

        node = self.nodes[node_index]

        try:
            # Process message through node
            logger.debug(f"Processing message {msg.id} through node: {node.node_name}")

            results = await node.onMsg(ctx, msg)

            # Route results to connected nodes
            for result_msg, connection_type in results:
                await self._route_message(ctx, result_msg, node_index, connection_type)

        except Exception as e:
            logger.error(f"Error processing node {node.node_name}: {e}", exc_info=True)
            # Route to failure connections if available
            if (node_index, "Failure") in [(c[0], c[1]) for c in self.connections.get(node_index, [])]:
                await self._route_message(ctx, msg, node_index, "Failure")

    async def _route_message(
        self,
        ctx: TbContext,
        msg: TbMsg,
        from_node_index: int,
        connection_type: str
    ):
        """
        Route message to connected nodes

        Args:
            ctx: Execution context
            msg: Message to route
            from_node_index: Source node index
            connection_type: Connection type to follow
        """
        # Find matching connections
        matching_connections = [
            to_index
            for to_index, conn_type in self.connections.get(from_node_index, [])
            if conn_type == connection_type
        ]

        if not matching_connections:
            logger.debug(
                f"No connections found for node {from_node_index} "
                f"with type {connection_type}"
            )
            return

        # Process each connected node
        tasks = []
        for to_index in matching_connections:
            tasks.append(self._process_node(ctx, msg, to_index))

        # Execute in parallel
        await asyncio.gather(*tasks)

    async def destroy(self):
        """Cleanup all nodes"""
        logger.info(f"Destroying rule chain executor: {self.rule_chain_id}")

        for node in self.nodes.values():
            try:
                await node.destroy()
            except Exception as e:
                logger.error(f"Error destroying node {node.node_name}: {e}")

        self.nodes.clear()
        self.connections.clear()
        self.initialized = False


class RuleEngineManager:
    """
    Manages multiple rule chain executors
    Singleton service that loads and caches rule chains
    """

    def __init__(self):
        self.executors: Dict[str, RuleChainExecutor] = {}
        self._lock = asyncio.Lock()

    async def get_executor(
        self,
        rule_chain_id: str,
        metadata: Optional[RuleChainMetaData] = None
    ) -> Optional[RuleChainExecutor]:
        """
        Get or create rule chain executor

        Args:
            rule_chain_id: Rule chain ID
            metadata: Rule chain metadata (if creating new)

        Returns:
            Rule chain executor or None if not found
        """
        async with self._lock:
            if rule_chain_id in self.executors:
                return self.executors[rule_chain_id]

            if metadata is None:
                # Load metadata from database
                metadata = await self._load_metadata(rule_chain_id)

            if metadata is None:
                logger.error(f"No metadata found for rule chain {rule_chain_id}")
                return None

            # Create new executor
            executor = RuleChainExecutor(rule_chain_id, metadata)
            await executor.init()

            self.executors[rule_chain_id] = executor
            return executor

    async def _load_metadata(self, rule_chain_id: str) -> Optional[RuleChainMetaData]:
        """
        Load rule chain metadata from database

        Args:
            rule_chain_id: Rule chain ID

        Returns:
            Metadata or None if not found
        """
        # TODO: Implement database lookup
        # For now, return None (will be implemented with rule chain API)
        return None

    async def reload_executor(self, rule_chain_id: str, metadata: RuleChainMetaData):
        """
        Reload rule chain executor with new metadata

        Args:
            rule_chain_id: Rule chain ID
            metadata: New metadata
        """
        async with self._lock:
            # Destroy old executor
            if rule_chain_id in self.executors:
                await self.executors[rule_chain_id].destroy()
                del self.executors[rule_chain_id]

            # Create new executor
            executor = RuleChainExecutor(rule_chain_id, metadata)
            await executor.init()
            self.executors[rule_chain_id] = executor

            logger.info(f"Reloaded rule chain executor: {rule_chain_id}")

    async def destroy_executor(self, rule_chain_id: str):
        """
        Destroy and remove rule chain executor

        Args:
            rule_chain_id: Rule chain ID
        """
        async with self._lock:
            if rule_chain_id in self.executors:
                await self.executors[rule_chain_id].destroy()
                del self.executors[rule_chain_id]
                logger.info(f"Destroyed rule chain executor: {rule_chain_id}")

    async def shutdown(self):
        """Shutdown all executors"""
        logger.info("Shutting down rule engine manager")

        async with self._lock:
            for executor in self.executors.values():
                await executor.destroy()

            self.executors.clear()


# Global rule engine manager
rule_engine_manager = RuleEngineManager()
