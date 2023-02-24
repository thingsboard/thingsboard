/**
 * Copyright © 2016-2023 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.NotificationRuleInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.notification.cache.NotificationRuleCacheKey;
import org.thingsboard.server.dao.notification.cache.NotificationRuleCacheValue;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DefaultNotificationRuleService extends AbstractCachedEntityService<NotificationRuleCacheKey, NotificationRuleCacheValue, NotificationRule> implements NotificationRuleService {

    private final NotificationRuleDao notificationRuleDao;

    @Override
    public NotificationRule saveNotificationRule(TenantId tenantId, NotificationRule notificationRule) {
        boolean created = notificationRule.getId() == null;
        if (!created) {
            NotificationRule oldNotificationRule = findNotificationRuleById(tenantId, notificationRule.getId());
            if (notificationRule.getTriggerType() != oldNotificationRule.getTriggerType()) {
                throw new IllegalArgumentException("Notification rule trigger type cannot be updated");
            }
        }
        try {
            notificationRule = notificationRuleDao.saveAndFlush(tenantId, notificationRule);
            publishEvictEvent(notificationRule);
        } catch (Exception e) {
            handleEvictEvent(notificationRule);
            checkConstraintViolation(e, Map.of(
                    "uq_notification_rule_name", "Notification rule with such name already exists"
            ));
            throw e;
        }
        return notificationRule;
    }

    @Override
    public NotificationRule findNotificationRuleById(TenantId tenantId, NotificationRuleId id) {
        return notificationRuleDao.findById(tenantId, id.getId());
    }

    @Override
    public NotificationRuleInfo findNotificationRuleInfoById(TenantId tenantId, NotificationRuleId id) {
        return notificationRuleDao.findInfoById(tenantId, id);
    }

    @Override
    public PageData<NotificationRuleInfo> findNotificationRulesInfosByTenantId(TenantId tenantId, PageLink pageLink) {
        return notificationRuleDao.findInfosByTenantIdAndPageLink(tenantId, pageLink);
    }

    @Override
    public PageData<NotificationRule> findNotificationRulesByTenantId(TenantId tenantId, PageLink pageLink) {
        return notificationRuleDao.findByTenantIdAndPageLink(tenantId, pageLink);
    }

    @Override
    public List<NotificationRule> findNotificationRulesByTenantIdAndTriggerType(TenantId tenantId, NotificationRuleTriggerType triggerType) {
        NotificationRuleCacheKey cacheKey = NotificationRuleCacheKey.builder()
                .tenantId(tenantId)
                .triggerType(triggerType)
                .build();
        return cache.getAndPutInTransaction(cacheKey, () -> NotificationRuleCacheValue.builder()
                        .notificationRules(notificationRuleDao.findByTenantIdAndTriggerType(tenantId, triggerType))
                        .build(), false)
                .getNotificationRules();
    }

    @Override
    public void deleteNotificationRuleById(TenantId tenantId, NotificationRuleId id) {
        NotificationRule notificationRule = findNotificationRuleById(tenantId, id);
        publishEvictEvent(notificationRule);
        notificationRuleDao.removeById(tenantId, id.getId());
    }

    @Override
    public void deleteNotificationRulesByTenantId(TenantId tenantId) {
        notificationRuleDao.removeByTenantId(tenantId);
    }

    @Override
    public void handleEvictEvent(NotificationRule notificationRule) {
        NotificationRuleCacheKey cacheKey = NotificationRuleCacheKey.builder()
                .tenantId(notificationRule.getTenantId())
                .triggerType(notificationRule.getTriggerType())
                .build();
        cache.evict(cacheKey);
    }

}
