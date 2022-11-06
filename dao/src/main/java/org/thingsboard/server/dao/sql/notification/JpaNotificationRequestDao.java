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
package org.thingsboard.server.dao.sql.notification;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.NotificationRequestEntity;
import org.thingsboard.server.dao.notification.NotificationRequestDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

@Component
@SqlDao
@RequiredArgsConstructor
public class JpaNotificationRequestDao extends JpaAbstractDao<NotificationRequestEntity, NotificationRequest> implements NotificationRequestDao {

    private final NotificationRequestRepository notificationRequestRepository;

    @Override
    public PageData<NotificationRequest> findByTenantIdAndPageLink(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(notificationRequestRepository.findByTenantIdAndSearchText(tenantId.getId(),
                Strings.nullToEmpty(pageLink.getTextSearch()), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public List<NotificationRequest> findByRuleIdAndAlarmId(TenantId tenantId, NotificationRuleId ruleId, AlarmId alarmId) {
        return DaoUtil.convertDataList(notificationRequestRepository.findAllByRuleIdAndAlarmId(ruleId.getId(), alarmId.getId()));
    }

    @Override
    protected Class<NotificationRequestEntity> getEntityClass() {
        return NotificationRequestEntity.class;
    }

    @Override
    protected JpaRepository<NotificationRequestEntity, UUID> getRepository() {
        return notificationRequestRepository;
    }

}
