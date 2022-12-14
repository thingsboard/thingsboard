/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.service.DataValidator;

@Service
@RequiredArgsConstructor
public class DefaultNotificationRuleService implements NotificationRuleService {

    private final NotificationRuleDao notificationRuleDao;

    private final NotificationRuleValidator validator = new NotificationRuleValidator();

    @Override
    public NotificationRule saveNotificationRule(TenantId tenantId, NotificationRule notificationRule) {
        validator.validate(notificationRule, NotificationRule::getTenantId);
        return notificationRuleDao.save(tenantId, notificationRule);
    }

    @Override
    public NotificationRule findNotificationRuleById(TenantId tenantId, NotificationRuleId notificationRuleId) {
        return notificationRuleDao.findById(tenantId, notificationRuleId.getId());
    }

    @Override
    public PageData<NotificationRule> findNotificationRulesByTenantId(TenantId tenantId, PageLink pageLink) {
        return notificationRuleDao.findByTenantIdAndPageLink(tenantId, pageLink);
    }

    @Override
    public void deleteNotificationRule(TenantId tenantId, NotificationRuleId notificationRuleId) {
        notificationRuleDao.removeById(tenantId, notificationRuleId.getId());
    }

    private static class NotificationRuleValidator extends DataValidator<NotificationRule> {
    }

}
