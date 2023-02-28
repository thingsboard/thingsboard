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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestInfo;
import org.thingsboard.server.common.data.notification.NotificationRequestStats;
import org.thingsboard.server.common.data.notification.NotificationRequestStatus;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.entity.EntityDaoService;
import org.thingsboard.server.dao.notification.cache.NotificationRequestCacheKey;
import org.thingsboard.server.dao.notification.cache.NotificationRequestCacheValue;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultNotificationRequestService extends AbstractCachedEntityService<NotificationRequestCacheKey, NotificationRequestCacheValue, NotificationRequest> implements NotificationRequestService, EntityDaoService {

    private final NotificationRequestDao notificationRequestDao;

    private final NotificationRequestValidator notificationRequestValidator = new NotificationRequestValidator();

    @Override
    public NotificationRequest saveNotificationRequest(TenantId tenantId, NotificationRequest notificationRequest) {
        notificationRequestValidator.validate(notificationRequest, NotificationRequest::getTenantId);
        try {
            notificationRequest = notificationRequestDao.save(tenantId, notificationRequest);
            publishEvictEvent(notificationRequest);
        } catch (Exception e) {
            handleEvictEvent(notificationRequest);
            throw e;
        }
        return notificationRequest;
    }

    @Override
    public NotificationRequest findNotificationRequestById(TenantId tenantId, NotificationRequestId id) {
        return notificationRequestDao.findById(tenantId, id.getId());
    }

    @Override
    public NotificationRequestInfo findNotificationRequestInfoById(TenantId tenantId, NotificationRequestId id) {
        return notificationRequestDao.findInfoById(tenantId, id);
    }

    @Override
    public PageData<NotificationRequest> findNotificationRequestsByTenantIdAndOriginatorType(TenantId tenantId, EntityType originatorType, PageLink pageLink) {
        return notificationRequestDao.findByTenantIdAndOriginatorTypeAndPageLink(tenantId, originatorType, pageLink);
    }

    @Override
    public PageData<NotificationRequestInfo> findNotificationRequestsInfosByTenantIdAndOriginatorType(TenantId tenantId, EntityType originatorType, PageLink pageLink) {
        return notificationRequestDao.findInfosByTenantIdAndOriginatorTypeAndPageLink(tenantId, originatorType, pageLink);
    }

    @Override
    public List<NotificationRequestId> findNotificationRequestsIdsByStatusAndRuleId(TenantId tenantId, NotificationRequestStatus requestStatus, NotificationRuleId ruleId) {
        return notificationRequestDao.findIdsByRuleId(tenantId, requestStatus, ruleId);
    }

    @Override
    public List<NotificationRequest> findNotificationRequestsByRuleIdAndOriginatorEntityId(TenantId tenantId, NotificationRuleId ruleId, EntityId originatorEntityId) {
        NotificationRequestCacheKey cacheKey = NotificationRequestCacheKey.builder()
                .originatorEntityId(originatorEntityId)
                .ruleId(ruleId)
                .build();
        return cache.getAndPutInTransaction(cacheKey, () -> NotificationRequestCacheValue.builder()
                        .notificationRequests(notificationRequestDao.findByRuleIdAndOriginatorEntityId(tenantId, ruleId, originatorEntityId))
                        .build(), false)
                .getNotificationRequests();
    }

    // ON DELETE CASCADE is used: notifications for request are deleted as well
    @Override
    public void deleteNotificationRequest(TenantId tenantId, NotificationRequest notificationRequest) {
        publishEvictEvent(notificationRequest);
        notificationRequestDao.removeById(tenantId, notificationRequest.getUuidId());
    }

    @Override
    public PageData<NotificationRequest> findScheduledNotificationRequests(PageLink pageLink) {
        return notificationRequestDao.findAllByStatus(NotificationRequestStatus.SCHEDULED, pageLink);
    }

    @Override
    public void updateNotificationRequest(TenantId tenantId, NotificationRequestId requestId, NotificationRequestStatus requestStatus, NotificationRequestStats stats) {
        notificationRequestDao.updateById(tenantId, requestId, requestStatus, stats);
    }

    @Override
    public void deleteNotificationRequestsByTenantId(TenantId tenantId) {
        notificationRequestDao.removeByTenantId(tenantId);
    }

    @Override
    public void handleEvictEvent(NotificationRequest notificationRequest) {
        if (notificationRequest.getRuleId() == null) return;

        NotificationRequestCacheKey cacheKey = NotificationRequestCacheKey.builder()
                .originatorEntityId(notificationRequest.getOriginatorEntityId())
                .ruleId(notificationRequest.getRuleId())
                .build();
        cache.evict(cacheKey);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findNotificationRequestById(tenantId, new NotificationRequestId(entityId.getId())));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.NOTIFICATION_REQUEST;
    }


    private static class NotificationRequestValidator extends DataValidator<NotificationRequest> {

    }

}
