package org.thingsboard.server.service.notification.rule.trigger;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.info.RateLimitsNotificationInfo;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.notification.rule.trigger.RateLimitsNotificationRuleTriggerConfig;
import org.thingsboard.server.common.msg.notification.trigger.RateLimitsTrigger;
import org.thingsboard.server.dao.tenant.TenantService;

@Service
@RequiredArgsConstructor
public class RateLimitsTriggerProcessor implements NotificationRuleTriggerProcessor<RateLimitsTrigger, RateLimitsNotificationRuleTriggerConfig> {

    private final TenantService tenantService;

    @Override
    public boolean matchesFilter(RateLimitsTrigger trigger, RateLimitsNotificationRuleTriggerConfig triggerConfig) {
        return trigger.getLimitLevel() != null;
    }

    @Override
    public RuleOriginatedNotificationInfo constructNotificationInfo(RateLimitsTrigger trigger) {
        return RateLimitsNotificationInfo.builder()
                .tenantId(trigger.getTenantId())
                .tenantName(tenantService.findTenantById(trigger.getTenantId()).getName())
                .api(trigger.getApi())
                .limitLevel(trigger.getLimitLevel() instanceof TenantId ? null : trigger.getLimitLevel())
                .limitLevelEntityName(trigger.getLimitLevelEntityName())
                .build();
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.RATE_LIMITS;
    }

}
