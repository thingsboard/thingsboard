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
package org.thingsboard.server.dao.notification;

import com.google.common.util.concurrent.FluentFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationRequestStatus;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entity.EntityDaoService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

@Service
@RequiredArgsConstructor
public class DefaultNotificationTemplateService extends AbstractEntityService implements NotificationTemplateService, EntityDaoService {

    private final NotificationTemplateDao notificationTemplateDao;
    private final NotificationRequestDao notificationRequestDao;

    @Override
    public NotificationTemplate findNotificationTemplateById(TenantId tenantId, NotificationTemplateId id) {
        return notificationTemplateDao.findById(tenantId, id.getId());
    }

    @Override
    public NotificationTemplate saveNotificationTemplate(TenantId tenantId, NotificationTemplate notificationTemplate) {
        NotificationType notificationType = notificationTemplate.getNotificationType();
        if (notificationTemplate.getId() != null) {
            NotificationTemplate oldNotificationTemplate = findNotificationTemplateById(tenantId, notificationTemplate.getId());
            if (notificationType != oldNotificationTemplate.getNotificationType()) {
                throw new IllegalArgumentException("Notification type cannot be updated");
            }
        } else {
            if (notificationType.isSystem()) {
                int systemTemplatesCount = countNotificationTemplatesByTenantIdAndNotificationTypes(tenantId, List.of(notificationType));
                if (systemTemplatesCount > 0) {
                    throw new IllegalArgumentException("There can only be one notification template of this type");
                }
            }
        }
        try {
            NotificationTemplate savedTemplate = notificationTemplateDao.saveAndFlush(tenantId, notificationTemplate);
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId).entityId(savedTemplate.getId())
                    .created(notificationTemplate.getId() == null).build());
            return savedTemplate;
        } catch (Exception e) {
            checkConstraintViolation(e, Map.of(
                    "uq_notification_template_name", "Notification template with such name already exists"
            ));
            throw e;
        }
    }

    @Override
    public PageData<NotificationTemplate> findNotificationTemplatesByTenantIdAndNotificationTypes(TenantId tenantId, List<NotificationType> notificationTypes, PageLink pageLink) {
        return notificationTemplateDao.findByTenantIdAndNotificationTypesAndPageLink(tenantId, notificationTypes, pageLink);
    }

    @Override
    public Optional<NotificationTemplate> findTenantOrSystemNotificationTemplate(TenantId tenantId, NotificationType notificationType) {
        return findNotificationTemplateByTenantIdAndType(tenantId, notificationType)
                .or(() -> findNotificationTemplateByTenantIdAndType(TenantId.SYS_TENANT_ID, notificationType));
    }

    @Override
    public Optional<NotificationTemplate> findNotificationTemplateByTenantIdAndType(TenantId tenantId, NotificationType notificationType) {
        return findNotificationTemplatesByTenantIdAndNotificationTypes(tenantId, List.of(notificationType), new PageLink(1)).getData()
                .stream().findFirst();
    }

    @Override
    public int countNotificationTemplatesByTenantIdAndNotificationTypes(TenantId tenantId, Collection<NotificationType> notificationTypes) {
        return notificationTemplateDao.countByTenantIdAndNotificationTypes(tenantId, notificationTypes);
    }

    @Override
    public void deleteNotificationTemplateById(TenantId tenantId, NotificationTemplateId id) {
        deleteEntity(tenantId, id, false);
    }

    @Override
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        if (!force) {
            if (notificationRequestDao.existsByTenantIdAndStatusAndTemplateId(tenantId, NotificationRequestStatus.SCHEDULED, (NotificationTemplateId) id)) {
                throw new IllegalArgumentException("Notification template is referenced by scheduled notification request");
            }
            if (tenantId.isSysTenantId()) {
                NotificationTemplate notificationTemplate = findNotificationTemplateById(tenantId, (NotificationTemplateId) id);
                if (notificationTemplate.getNotificationType().isSystem()) {
                    throw new IllegalArgumentException("System notification template cannot be deleted");
                }
            }
        }
        try {
            notificationTemplateDao.removeById(tenantId, id.getId());
        } catch (Exception e) {
            checkConstraintViolation(e, Map.of(
                    "fk_notification_rule_template_id", "Notification template is referenced by notification rule"
            ));
            throw e;
        }
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(id).build());
    }

    @Override
    public void deleteNotificationTemplatesByTenantId(TenantId tenantId) {
        notificationTemplateDao.removeByTenantId(tenantId);
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        deleteNotificationTemplatesByTenantId(tenantId);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findNotificationTemplateById(tenantId, new NotificationTemplateId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(notificationTemplateDao.findByIdAsync(tenantId, entityId.getId()))
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.NOTIFICATION_TEMPLATE;
    }

}
