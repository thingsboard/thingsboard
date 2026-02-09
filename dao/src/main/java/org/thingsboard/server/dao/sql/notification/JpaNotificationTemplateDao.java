/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.TenantEntityDao;
import org.thingsboard.server.dao.model.sql.NotificationTemplateEntity;
import org.thingsboard.server.dao.notification.NotificationTemplateDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Component
@SqlDao
@RequiredArgsConstructor
public class JpaNotificationTemplateDao extends JpaAbstractDao<NotificationTemplateEntity, NotificationTemplate> implements NotificationTemplateDao, TenantEntityDao<NotificationTemplate> {

    private final NotificationTemplateRepository notificationTemplateRepository;

    @Override
    protected Class<NotificationTemplateEntity> getEntityClass() {
        return NotificationTemplateEntity.class;
    }

    @Override
    public PageData<NotificationTemplate> findByTenantIdAndNotificationTypesAndPageLink(TenantId tenantId, List<NotificationType> notificationTypes, PageLink pageLink) {
        return DaoUtil.toPageData(notificationTemplateRepository.findByTenantIdAndNotificationTypesAndSearchText(tenantId.getId(),
                notificationTypes, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public int countByTenantIdAndNotificationTypes(TenantId tenantId, Collection<NotificationType> notificationTypes) {
        return notificationTemplateRepository.countByTenantIdAndNotificationTypes(tenantId.getId(), notificationTypes);
    }

    @Override
    public void removeByTenantId(TenantId tenantId) {
        notificationTemplateRepository.deleteByTenantId(tenantId.getId());
    }

    @Override
    public NotificationTemplate findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(notificationTemplateRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public NotificationTemplate findByTenantIdAndName(UUID tenantId, String name) {
        return DaoUtil.getData(notificationTemplateRepository.findByTenantIdAndName(tenantId, name));
    }

    @Override
    public PageData<NotificationTemplate> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(notificationTemplateRepository.findByTenantId(tenantId, DaoUtil.toPageable(pageLink)));
    }

    @Override
    public NotificationTemplateId getExternalIdByInternal(NotificationTemplateId internalId) {
        return DaoUtil.toEntityId(notificationTemplateRepository.getExternalIdByInternal(internalId.getId()), NotificationTemplateId::new);
    }

    @Override
    public PageData<NotificationTemplate> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return findByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    protected JpaRepository<NotificationTemplateEntity, UUID> getRepository() {
        return notificationTemplateRepository;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.NOTIFICATION_TEMPLATE;
    }

}
