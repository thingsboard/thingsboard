// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.notification;

import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.NotificationRuleInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.List;

public interface NotificationRuleService {

    NotificationRule saveNotificationRule(TenantId tenantId, NotificationRule notificationRule);

    NotificationRule findNotificationRuleById(TenantId tenantId, NotificationRuleId id);

    NotificationRuleInfo findNotificationRuleInfoById(TenantId tenantId, NotificationRuleId id);

    PageData<NotificationRuleInfo> findNotificationRulesInfosByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<NotificationRule> findNotificationRulesByTenantId(TenantId tenantId, PageLink pageLink);

    List<NotificationRule> findEnabledNotificationRulesByTenantIdAndTriggerType(TenantId tenantId, NotificationRuleTriggerType triggerType);

    void deleteNotificationRuleById(TenantId tenantId, NotificationRuleId id);

    void deleteNotificationRulesByTenantId(TenantId tenantId);

}
