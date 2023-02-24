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
package org.thingsboard.server.dao.sql.notification;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.NotificationRuleInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.NotificationRuleEntity;
import org.thingsboard.server.dao.model.sql.NotificationRuleInfoEntity;
import org.thingsboard.server.dao.notification.NotificationRuleDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.dao.DaoUtil.getId;

@Component
@SqlDao
@RequiredArgsConstructor
public class JpaNotificationRuleDao extends JpaAbstractDao<NotificationRuleEntity, NotificationRule> implements NotificationRuleDao {

    private final NotificationRuleRepository notificationRuleRepository;

    @Override
    public PageData<NotificationRule> findByTenantIdAndPageLink(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(notificationRuleRepository.findByTenantIdAndSearchText(tenantId.getId(),
                Strings.nullToEmpty(pageLink.getTextSearch()), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<NotificationRuleInfo> findInfosByTenantIdAndPageLink(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.pageToPageData(notificationRuleRepository.findInfosByTenantIdAndSearchText(tenantId.getId(),
                Strings.nullToEmpty(pageLink.getTextSearch()), DaoUtil.toPageable(pageLink))).mapData(NotificationRuleInfoEntity::toData);
    }

    @Override
    public boolean existsByTargetId(TenantId tenantId, NotificationTargetId targetId) {
        return notificationRuleRepository.existsByRecipientsConfigContaining(targetId.getId().toString());
    }

    @Override
    public List<NotificationRule> findByTenantIdAndTriggerType(TenantId tenantId, NotificationRuleTriggerType triggerType) {
        return DaoUtil.convertDataList(notificationRuleRepository.findAllByTenantIdAndTriggerType(tenantId.getId(), triggerType));
    }

    @Override
    public NotificationRuleInfo findInfoById(TenantId tenantId, NotificationRuleId id) {
        NotificationRuleInfoEntity infoEntity = notificationRuleRepository.findInfoById(id.getId());
        return infoEntity != null ? infoEntity.toData() : null;
    }

    @Override
    public void removeByTenantId(TenantId tenantId) {
        notificationRuleRepository.deleteByTenantId(tenantId.getId());
    }

    @Override
    protected Class<NotificationRuleEntity> getEntityClass() {
        return NotificationRuleEntity.class;
    }

    @Override
    protected JpaRepository<NotificationRuleEntity, UUID> getRepository() {
        return notificationRuleRepository;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.NOTIFICATION_RULE;
    }

}
