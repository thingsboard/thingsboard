"""
REST API Call Node
Makes HTTP requests to external REST APIs
Equivalent to: org.thingsboard.rule.engine.rest.TbRestApiCallNode
"""
import orjson
import aiohttp
from typing import Dict, Any
from app.rule_engine.core.rule_node_base import TbActionNode
from app.rule_engine.models.rule_engine_msg import TbMsg, TbContext
from app.rule_engine.core.rule_engine_executor import register_node


@register_node("org.thingsboard.rule.engine.rest.TbRestApiCallNode")
class TbRestApiCallNode(TbActionNode):
    """
    Make REST API call to external service

    Configuration:
    {
        "restEndpointUrlPattern": "https://api.example.com/device/${deviceId}",
        "requestMethod": "POST",  # GET, POST, PUT, DELETE, PATCH
        "headers": {
            "Content-Type": "application/json",
            "Authorization": "Bearer ${token}"
        },
        "useSimpleClientHttpFactory": true,
        "readTimeoutMs": 5000,
        "maxParallelRequestsCount": 50,
        "enableProxy": false,
        "useRedirectPolicy": false,
        "credentials": {
            "type": "anonymous"  # anonymous, basic, cert, oauth2
        }
    }

    The request body will be the message data.
    Response will replace message data if successful.
    """

    async def init(self):
        """Initialize node"""
        self.url_pattern = self.config.get("restEndpointUrlPattern", "")
        self.method = self.config.get("requestMethod", "POST").upper()
        self.headers = self.config.get("headers", {})
        self.timeout_ms = self.config.get("readTimeoutMs", 5000)
        self.credentials = self.config.get("credentials", {"type": "anonymous"})

        # Create HTTP session
        timeout = aiohttp.ClientTimeout(total=self.timeout_ms / 1000)
        self.session = aiohttp.ClientSession(timeout=timeout)

        self.log_info(
            f"Initialized REST API call node: "
            f"method={self.method}, url={self.url_pattern}"
        )

    async def execute(self, ctx: TbContext, msg: TbMsg):
        """
        Execute REST API call

        Args:
            ctx: Execution context
            msg: Message to process
        """
        try:
            # Parse message data
            msg_data = orjson.loads(msg.data)

            # Build template context for URL and headers
            template_ctx = {
                **msg_data,
                **msg.metadata,
                "msgType": msg.type.value,
                "originatorId": msg.originator.id,
                "originatorType": msg.originator.entity_type,
            }

            # Process URL template
            url = self._process_template(self.url_pattern, template_ctx)

            # Process header templates
            processed_headers = {}
            for key, value in self.headers.items():
                processed_headers[key] = self._process_template(str(value), template_ctx)

            # Add authentication if configured
            if self.credentials.get("type") == "basic":
                username = self.credentials.get("username", "")
                password = self.credentials.get("password", "")
                auth = aiohttp.BasicAuth(username, password)
            else:
                auth = None

            # Make HTTP request
            response_data = await self._make_request(
                method=self.method,
                url=url,
                headers=processed_headers,
                data=msg_data,
                auth=auth
            )

            self.log_info(msg, f"REST API call successful: {self.method} {url}")

            # Update message data with response (optional)
            # For now, keep original message data
            # In production, you might want to transform the message based on response

        except Exception as e:
            self.log_error(msg, f"Failed to call REST API: {e}")
            raise

    def _process_template(self, template: str, context: Dict[str, Any]) -> str:
        """
        Process template string with variable substitution

        Args:
            template: Template string with ${var} placeholders
            context: Dictionary of variable values

        Returns:
            Processed string
        """
        if not template:
            return ""

        result = template

        # Replace ${variable} with values from context
        for key, value in context.items():
            placeholder = f"${{{key}}}"
            if placeholder in result:
                result = result.replace(placeholder, str(value))

        return result

    async def _make_request(
        self,
        method: str,
        url: str,
        headers: Dict[str, str],
        data: Any,
        auth: Any = None
    ) -> Dict[str, Any]:
        """
        Make HTTP request

        Args:
            method: HTTP method
            url: Request URL
            headers: Request headers
            data: Request body data
            auth: Authentication

        Returns:
            Response data
        """
        try:
            async with self.session.request(
                method=method,
                url=url,
                headers=headers,
                json=data if method in ["POST", "PUT", "PATCH"] else None,
                auth=auth
            ) as response:
                # Check response status
                if response.status >= 400:
                    error_text = await response.text()
                    raise Exception(
                        f"HTTP {response.status} error: {error_text}"
                    )

                # Try to parse JSON response
                try:
                    return await response.json()
                except:
                    # Return text if not JSON
                    text = await response.text()
                    return {"response": text}

        except Exception as e:
            self.log_error(None, f"Error making HTTP request: {e}")
            raise

    async def destroy(self):
        """Cleanup"""
        if self.session:
            await self.session.close()
