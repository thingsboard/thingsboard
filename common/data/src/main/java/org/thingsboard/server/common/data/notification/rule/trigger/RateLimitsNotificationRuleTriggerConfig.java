package org.thingsboard.server.common.data.notification.rule.trigger;

import lombok.Data;

@Data
public class RateLimitsNotificationRuleTriggerConfig implements NotificationRuleTriggerConfig {

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.RATE_LIMITS;
    }

}
