"""
ThingsBoard Gateway - Main Application
Runs all transport layers and message processing
"""
import asyncio
import signal
from app.core.config import settings
from app.core.logging_config import setup_logging, get_logger
from app.core.device_auth import device_auth_service
from app.core.message_processor import message_processor
from app.transports.mqtt.mqtt_transport import mqtt_transport
from app.transports.http.http_transport import http_transport
from app.transports.coap.coap_transport import coap_transport

# Setup logging
setup_logging()
logger = get_logger(__name__)


class GatewayApplication:
    """Main gateway application"""

    def __init__(self):
        self.running = False
        self.tasks = []

    async def start(self):
        """Start all gateway services"""
        logger.info("="*60)
        logger.info("ThingsBoard Gateway Starting...")
        logger.info(f"Environment: {settings.ENVIRONMENT}")
        logger.info(f"Service ID: {settings.SERVICE_ID}")
        logger.info(f"Backend: {settings.get_api_url()}")
        logger.info("="*60)

        try:
            # Start core services
            await device_auth_service.start()
            await message_processor.start()

            # Start transports
            if settings.MQTT_ENABLED:
                await mqtt_transport.start()

            if settings.HTTP_ENABLED:
                await http_transport.start()
                # Run HTTP transport as a background task
                import uvicorn
                config = uvicorn.Config(
                    http_transport.app,
                    host=settings.HTTP_BIND_ADDRESS,
                    port=settings.HTTP_BIND_PORT,
                    log_level="info"
                )
                server = uvicorn.Server(config)
                self.tasks.append(asyncio.create_task(server.serve()))

            if settings.COAP_ENABLED:
                await coap_transport.start()

            self.running = True
            logger.info("Gateway started successfully")
            logger.info("="*60)

            # Print enabled transports
            enabled = []
            if settings.MQTT_ENABLED:
                enabled.append(f"MQTT on port {settings.MQTT_BIND_PORT}")
            if settings.HTTP_ENABLED:
                enabled.append(f"HTTP on port {settings.HTTP_BIND_PORT}")
            if settings.COAP_ENABLED:
                enabled.append(f"CoAP on port {settings.COAP_BIND_PORT}")

            logger.info("Enabled transports:")
            for transport in enabled:
                logger.info(f"  - {transport}")
            logger.info("="*60)

        except Exception as e:
            logger.error(f"Error starting gateway: {e}")
            raise

    async def stop(self):
        """Stop all gateway services"""
        logger.info("Shutting down gateway...")

        self.running = False

        # Cancel background tasks
        for task in self.tasks:
            task.cancel()
            try:
                await task
            except asyncio.CancelledError:
                pass

        # Stop transports
        if settings.COAP_ENABLED:
            await coap_transport.stop()

        if settings.HTTP_ENABLED:
            await http_transport.stop()

        if settings.MQTT_ENABLED:
            await mqtt_transport.stop()

        # Stop core services
        await message_processor.stop()
        await device_auth_service.stop()

        logger.info("Gateway stopped")

    async def run(self):
        """Main run loop"""
        await self.start()

        # Wait until stopped
        try:
            while self.running:
                await asyncio.sleep(1)
        except asyncio.CancelledError:
            pass

        await self.stop()


# Global application instance
app = GatewayApplication()


async def main():
    """Main entry point"""

    # Setup signal handlers
    def signal_handler(sig, frame):
        logger.info(f"Received signal {sig}")
        asyncio.create_task(app.stop())

    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)

    # Run application
    try:
        await app.run()
    except KeyboardInterrupt:
        logger.info("Interrupted by user")
    except Exception as e:
        logger.error(f"Fatal error: {e}")
        raise
    finally:
        logger.info("Gateway exited")


if __name__ == "__main__":
    asyncio.run(main())
