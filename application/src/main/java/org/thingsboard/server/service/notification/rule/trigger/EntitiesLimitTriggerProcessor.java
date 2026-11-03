// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.notification.rule.trigger;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.notification.info.EntitiesLimitNotificationInfo;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.EntitiesLimitTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.config.EntitiesLimitNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.entity.EntityCountService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantService;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

@Service
@RequiredArgsConstructor
public class EntitiesLimitTriggerProcessor implements NotificationRuleTriggerProcessor<EntitiesLimitTrigger, EntitiesLimitNotificationRuleTriggerConfig> {

    private final EntityCountService entityCountService;
    private final TbTenantProfileCache tenantProfileCache;
    private final TenantService tenantService;

    @Override
    public boolean matchesFilter(EntitiesLimitTrigger trigger, EntitiesLimitNotificationRuleTriggerConfig triggerConfig) {
        if (isNotEmpty(triggerConfig.getEntityTypes()) && !triggerConfig.getEntityTypes().contains(trigger.getEntityType())) {
            return false;
        }
        DefaultTenantProfileConfiguration profileConfiguration = tenantProfileCache.get(trigger.getTenantId()).getDefaultProfileConfiguration();
        long limit = profileConfiguration.getEntitiesLimit(trigger.getEntityType());
        if (limit <= 0) {
            return false;
        }
        long currentCount = entityCountService.countByTenantIdAndEntityType(trigger.getTenantId(), trigger.getEntityType());
        if (currentCount == 0) {
            return false;
        }
        trigger.setLimit(limit);
        trigger.setCurrentCount(currentCount);
        return (int) (limit * triggerConfig.getThreshold()) == currentCount; // strict comparing not to send notification on each new entity
    }

    @Override
    public RuleOriginatedNotificationInfo constructNotificationInfo(EntitiesLimitTrigger trigger) {
        return EntitiesLimitNotificationInfo.builder()
                .entityType(trigger.getEntityType())
                .currentCount(trigger.getCurrentCount())
                .limit(trigger.getLimit())
                .percents((int) (((float) trigger.getCurrentCount() / trigger.getLimit()) * 100))
                .tenantId(trigger.getTenantId())
                .tenantName(tenantService.findTenantById(trigger.getTenantId()).getName())
                .build();
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.ENTITIES_LIMIT;
    }

}
