"""
Send Email Node
Sends email notifications based on message data
Equivalent to: org.thingsboard.rule.engine.mail.TbSendEmailNode
"""
import orjson
import aiohttp
from typing import Dict, Any
from app.rule_engine.core.rule_node_base import TbActionNode
from app.rule_engine.models.rule_engine_msg import TbMsg, TbContext
from app.rule_engine.core.rule_engine_executor import register_node
from app.core.config import settings


@register_node("org.thingsboard.rule.engine.mail.TbSendEmailNode")
class TbSendEmailNode(TbActionNode):
    """
    Send email notification

    Configuration:
    {
        "fromTemplate": "noreply@payvar.io",
        "toTemplate": "${userEmail}",  # Can use message metadata/data variables
        "ccTemplate": "",
        "bccTemplate": "",
        "subjectTemplate": "Alert: ${alarmType}",
        "bodyTemplate": "Device ${deviceName} triggered alarm: ${alarmType}\\n\\nDetails: ${details}",
        "mailBodyType": "text",  # "text" or "html"
        "useSystemMailSettings": true
    }

    Template variables can reference:
    - Message data: ${temperature}, ${humidity}, etc.
    - Metadata: ${deviceName}, ${deviceType}, etc.
    - Special: ${msgType}, ${originatorId}, ${originatorType}
    """

    async def init(self):
        """Initialize node"""
        self.from_template = self.config.get("fromTemplate", "noreply@payvar.io")
        self.to_template = self.config.get("toTemplate", "")
        self.cc_template = self.config.get("ccTemplate", "")
        self.bcc_template = self.config.get("bccTemplate", "")
        self.subject_template = self.config.get("subjectTemplate", "Notification")
        self.body_template = self.config.get("bodyTemplate", "")
        self.mail_body_type = self.config.get("mailBodyType", "text")
        self.use_system_settings = self.config.get("useSystemMailSettings", True)

        self.session = aiohttp.ClientSession()

        self.log_info(
            f"Initialized send email node: "
            f"to={self.to_template}, subject={self.subject_template}"
        )

    async def execute(self, ctx: TbContext, msg: TbMsg):
        """
        Send email notification

        Args:
            ctx: Execution context
            msg: Message containing notification data
        """
        try:
            # Parse message data
            msg_data = orjson.loads(msg.data)

            # Build template context
            template_ctx = {
                **msg_data,
                **msg.metadata,
                "msgType": msg.type.value,
                "originatorId": msg.originator.id,
                "originatorType": msg.originator.entity_type,
            }

            # Process templates
            from_email = self._process_template(self.from_template, template_ctx)
            to_email = self._process_template(self.to_template, template_ctx)
            cc_email = self._process_template(self.cc_template, template_ctx)
            bcc_email = self._process_template(self.bcc_template, template_ctx)
            subject = self._process_template(self.subject_template, template_ctx)
            body = self._process_template(self.body_template, template_ctx)

            # Validate recipient
            if not to_email:
                raise Exception("No recipient email address specified")

            # Send email via backend mail service
            await self._send_email(
                from_email=from_email,
                to_email=to_email,
                cc_email=cc_email,
                bcc_email=bcc_email,
                subject=subject,
                body=body,
                is_html=(self.mail_body_type == "html")
            )

            self.log_info(msg, f"Email sent to {to_email}: {subject}")

        except Exception as e:
            self.log_error(msg, f"Failed to send email: {e}")
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

    async def _send_email(
        self,
        from_email: str,
        to_email: str,
        cc_email: str,
        bcc_email: str,
        subject: str,
        body: str,
        is_html: bool = False
    ):
        """
        Send email via backend mail service

        Args:
            from_email: Sender email
            to_email: Recipient email (comma-separated for multiple)
            cc_email: CC recipients (comma-separated)
            bcc_email: BCC recipients (comma-separated)
            subject: Email subject
            body: Email body
            is_html: Whether body is HTML
        """
        url = f"{settings.get_api_url()}/api/mail/send"

        payload = {
            "from": from_email,
            "to": [email.strip() for email in to_email.split(",") if email.strip()],
            "subject": subject,
            "body": body,
            "html": is_html
        }

        # Add CC if specified
        if cc_email:
            payload["cc"] = [email.strip() for email in cc_email.split(",") if email.strip()]

        # Add BCC if specified
        if bcc_email:
            payload["bcc"] = [email.strip() for email in bcc_email.split(",") if email.strip()]

        try:
            async with self.session.post(
                url,
                json=payload,
                timeout=aiohttp.ClientTimeout(total=10)
            ) as response:
                if response.status not in [200, 201, 202]:
                    error_text = await response.text()
                    raise Exception(f"Mail service error: {response.status} - {error_text}")

        except Exception as e:
            self.log_error(None, f"Error calling mail service: {e}")
            raise

    async def destroy(self):
        """Cleanup"""
        if self.session:
            await self.session.close()
