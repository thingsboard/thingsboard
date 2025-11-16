"""
Application lifecycle events
"""
import logging
from app.core.logging import get_logger

logger = get_logger(__name__)


async def startup_event():
    """Execute on application startup"""
    logger.info("ThingsBoard IoT Platform starting up...")
    # Initialize connections, load configurations, etc.
    logger.info("Startup complete")


async def shutdown_event():
    """Execute on application shutdown"""
    logger.info("ThingsBoard IoT Platform shutting down...")
    # Close connections, cleanup resources, etc.
    logger.info("Shutdown complete")
