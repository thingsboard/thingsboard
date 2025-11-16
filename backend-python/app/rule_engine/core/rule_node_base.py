"""
Base classes for rule nodes
All rule nodes inherit from TbNode
"""
from abc import ABC, abstractmethod
from typing import Dict, Any, List, Callable, Optional
from app.rule_engine.models.rule_engine_msg import (
    TbMsg, TbContext, RuleNode, RuleNodeConnectionType
)
from app.core.logging import get_logger

logger = get_logger(__name__)


class TbNodeException(Exception):
    """Exception raised by rule nodes"""
    pass


class TbNode(ABC):
    """
    Base class for all rule nodes

    Each node type must implement:
    - init() - Initialize node with configuration
    - onMsg() - Process incoming message
    - destroy() - Cleanup resources
    """

    def __init__(self, node_definition: RuleNode):
        """
        Initialize node

        Args:
            node_definition: Rule node definition with configuration
        """
        self.node_id = node_definition.id
        self.node_name = node_definition.name
        self.node_type = node_definition.type
        self.debug_mode = node_definition.debug_mode
        self.config = node_definition.configuration
        self.logger = get_logger(f"{__name__}.{node_definition.name}")

    @abstractmethod
    async def init(self):
        """
        Initialize the node
        Called once when node is loaded
        Override to initialize resources, compile scripts, etc.
        """
        pass

    @abstractmethod
    async def onMsg(self, ctx: TbContext, msg: TbMsg) -> List[tuple]:
        """
        Process incoming message

        Args:
            ctx: Execution context
            msg: Incoming message

        Returns:
            List of tuples (TbMsg, connection_type)
            Each tuple represents a message to send to connected nodes

        Example:
            return [(msg, RuleNodeConnectionType.SUCCESS)]
            return [(new_msg, RuleNodeConnectionType.TRUE), (msg, RuleNodeConnectionType.FALSE)]
        """
        pass

    @abstractmethod
    async def destroy(self):
        """
        Destroy the node and cleanup resources
        Called when node is being removed or reloaded
        """
        pass

    def log_debug(self, msg: TbMsg, message: str):
        """Log debug message if debug mode is enabled"""
        if self.debug_mode:
            self.logger.debug(f"[{self.node_name}] {message} | msg_id={msg.id}")

    def log_info(self, message: str):
        """Log info message"""
        self.logger.info(f"[{self.node_name}] {message}")

    def log_error(self, msg: TbMsg, message: str, error: Exception = None):
        """Log error message"""
        if error:
            self.logger.error(f"[{self.node_name}] {message} | msg_id={msg.id} | error={error}")
        else:
            self.logger.error(f"[{self.node_name}] {message} | msg_id={msg.id}")


class TbFilterNode(TbNode):
    """
    Base class for filter nodes
    Filter nodes route messages to different paths based on conditions
    """

    @abstractmethod
    async def filter(self, ctx: TbContext, msg: TbMsg) -> bool:
        """
        Filter logic

        Returns:
            True if message passes filter, False otherwise
        """
        pass

    async def onMsg(self, ctx: TbContext, msg: TbMsg) -> List[tuple]:
        """Default implementation routes based on filter result"""
        try:
            self.log_debug(msg, "Processing filter")

            result = await self.filter(ctx, msg)

            if result:
                self.log_debug(msg, "Filter passed - routing to True")
                return [(msg, RuleNodeConnectionType.TRUE)]
            else:
                self.log_debug(msg, "Filter failed - routing to False")
                return [(msg, RuleNodeConnectionType.FALSE)]

        except Exception as e:
            self.log_error(msg, "Filter error", e)
            return [(msg, RuleNodeConnectionType.FAILURE)]


class TbTransformationNode(TbNode):
    """
    Base class for transformation nodes
    Transformation nodes modify message data or metadata
    """

    @abstractmethod
    async def transform(self, ctx: TbContext, msg: TbMsg) -> TbMsg:
        """
        Transform the message

        Args:
            ctx: Execution context
            msg: Input message

        Returns:
            Transformed message (can be same object or new copy)
        """
        pass

    async def onMsg(self, ctx: TbContext, msg: TbMsg) -> List[tuple]:
        """Default implementation applies transformation"""
        try:
            self.log_debug(msg, "Applying transformation")

            transformed_msg = await self.transform(ctx, msg)

            self.log_debug(transformed_msg, "Transformation complete")
            return [(transformed_msg, RuleNodeConnectionType.SUCCESS)]

        except Exception as e:
            self.log_error(msg, "Transformation error", e)
            return [(msg, RuleNodeConnectionType.FAILURE)]


class TbActionNode(TbNode):
    """
    Base class for action nodes
    Action nodes perform side effects (save data, send email, etc.)
    """

    @abstractmethod
    async def execute(self, ctx: TbContext, msg: TbMsg):
        """
        Execute the action

        Args:
            ctx: Execution context
            msg: Input message
        """
        pass

    async def onMsg(self, ctx: TbContext, msg: TbMsg) -> List[tuple]:
        """Default implementation executes action"""
        try:
            self.log_debug(msg, "Executing action")

            await self.execute(ctx, msg)

            self.log_debug(msg, "Action complete")
            return [(msg, RuleNodeConnectionType.SUCCESS)]

        except Exception as e:
            self.log_error(msg, "Action error", e)
            return [(msg, RuleNodeConnectionType.FAILURE)]


class TbEnrichmentNode(TbNode):
    """
    Base class for enrichment nodes
    Enrichment nodes add data to messages (fetch from DB, etc.)
    """

    @abstractmethod
    async def enrich(self, ctx: TbContext, msg: TbMsg) -> TbMsg:
        """
        Enrich the message with additional data

        Args:
            ctx: Execution context
            msg: Input message

        Returns:
            Enriched message
        """
        pass

    async def onMsg(self, ctx: TbContext, msg: TbMsg) -> List[tuple]:
        """Default implementation applies enrichment"""
        try:
            self.log_debug(msg, "Enriching message")

            enriched_msg = await self.enrich(ctx, msg)

            self.log_debug(enriched_msg, "Enrichment complete")
            return [(enriched_msg, RuleNodeConnectionType.SUCCESS)]

        except Exception as e:
            self.log_error(msg, "Enrichment error", e)
            return [(msg, RuleNodeConnectionType.FAILURE)]


class TbRuleChainInputNode(TbNode):
    """
    Special node that represents rule chain entry point
    """

    async def init(self):
        """No initialization needed"""
        pass

    async def onMsg(self, ctx: TbContext, msg: TbMsg) -> List[tuple]:
        """Forward message to next nodes"""
        return [(msg, RuleNodeConnectionType.SUCCESS)]

    async def destroy(self):
        """No cleanup needed"""
        pass
