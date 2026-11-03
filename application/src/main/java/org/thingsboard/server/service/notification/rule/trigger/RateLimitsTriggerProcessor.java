// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.notification.rule.trigger;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.info.RateLimitsNotificationInfo;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.RateLimitsTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.notification.rule.trigger.config.RateLimitsNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RateLimitsTriggerProcessor implements NotificationRuleTriggerProcessor<RateLimitsTrigger, RateLimitsNotificationRuleTriggerConfig> {

    private final TenantService tenantService;
    private final EntityService entityService;

    @Override
    public boolean matchesFilter(RateLimitsTrigger trigger, RateLimitsNotificationRuleTriggerConfig triggerConfig) {
        return trigger.getLimitLevel() != null && trigger.getApi().getLabel() != null &&
                CollectionsUtil.emptyOrContains(triggerConfig.getApis(), trigger.getApi());
    }

    @Override
    public RuleOriginatedNotificationInfo constructNotificationInfo(RateLimitsTrigger trigger) {
        EntityId limitLevel = trigger.getLimitLevel();
        String tenantName = tenantService.findTenantById(trigger.getTenantId()).getName();
        String limitLevelEntityName = null;
        if (limitLevel instanceof TenantId) {
            limitLevelEntityName = tenantName;
        } else if (limitLevel != null) {
            limitLevelEntityName = Optional.ofNullable(trigger.getLimitLevelEntityName())
                    .orElseGet(() -> entityService.fetchEntityName(trigger.getTenantId(), limitLevel).orElse(null));
        }
        return RateLimitsNotificationInfo.builder()
                .tenantId(trigger.getTenantId())
                .tenantName(tenantName)
                .api(trigger.getApi())
                .limitLevel(limitLevel)
                .limitLevelEntityName(limitLevelEntityName)
                .build();
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.RATE_LIMITS;
    }

}
