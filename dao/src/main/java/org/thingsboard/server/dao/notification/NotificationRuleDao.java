// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.notification;

import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.NotificationRuleInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;

import java.util.List;

public interface NotificationRuleDao extends Dao<NotificationRule>, ExportableEntityDao<NotificationRuleId, NotificationRule> {

    PageData<NotificationRule> findByTenantIdAndPageLink(TenantId tenantId, PageLink pageLink);

    PageData<NotificationRuleInfo> findInfosByTenantIdAndPageLink(TenantId tenantId, PageLink pageLink);

    boolean existsByTenantIdAndTargetId(TenantId tenantId, NotificationTargetId targetId);

    List<NotificationRule> findByTenantIdAndTriggerTypeAndEnabled(TenantId tenantId, NotificationRuleTriggerType triggerType, boolean enabled);

    NotificationRuleInfo findInfoById(TenantId tenantId, NotificationRuleId id);

    void removeByTenantId(TenantId tenantId);

}
