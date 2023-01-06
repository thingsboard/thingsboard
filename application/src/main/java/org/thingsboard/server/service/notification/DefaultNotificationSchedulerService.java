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
package org.thingsboard.server.service.notification;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.NotificationCenter;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.notification.NotificationRequestService;
import org.thingsboard.server.queue.scheduler.SchedulerComponent;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.executors.NotificationExecutorService;
import org.thingsboard.server.service.partition.AbstractPartitionBasedService;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@TbCoreComponent
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("UnstableApiUsage")
public class DefaultNotificationSchedulerService extends AbstractPartitionBasedService<NotificationRequestId> implements NotificationSchedulerService {

    private final NotificationCenter notificationCenter;
    private final NotificationRequestService notificationRequestService;
    private final SchedulerComponent scheduler;
    private final NotificationExecutorService notificationExecutor;

    private final Map<NotificationRequestId, ScheduledRequestMetadata> scheduledNotificationRequests = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        super.init();
    }

    @Override
    protected Map<TopicPartitionInfo, List<ListenableFuture<?>>> onAddedPartitions(Set<TopicPartitionInfo> addedPartitions) {
        PageDataIterable<NotificationRequest> notificationRequests = new PageDataIterable<>(pageLink -> {
            return notificationRequestService.findScheduledNotificationRequests(pageLink);
        }, 1000);
        for (NotificationRequest notificationRequest : notificationRequests) {
            TopicPartitionInfo requestPartition = partitionService.resolve(ServiceType.TB_CORE, notificationRequest.getTenantId(), notificationRequest.getId());
            if (addedPartitions.contains(requestPartition)) {
                partitionedEntities.computeIfAbsent(requestPartition, k -> ConcurrentHashMap.newKeySet()).add(notificationRequest.getId());
                if (!scheduledNotificationRequests.containsKey(notificationRequest.getId())) {
                    scheduleNotificationRequest(notificationRequest.getTenantId(), notificationRequest, notificationRequest.getCreatedTime());
                }
            }
        }
        return Collections.emptyMap();
    }

    @Override
    public void scheduleNotificationRequest(TenantId tenantId, NotificationRequestId notificationRequestId, long requestTs) {
        NotificationRequest notificationRequest = notificationRequestService.findNotificationRequestById(tenantId, notificationRequestId);
        scheduleNotificationRequest(tenantId, notificationRequest, requestTs);
    }

    private void scheduleNotificationRequest(TenantId tenantId, NotificationRequest request, long requestTs) {
        int delayInSec = Optional.ofNullable(request)
                .map(NotificationRequest::getAdditionalConfig)
                .map(NotificationRequestConfig::getSendingDelayInSec)
                .orElse(0);
        if (delayInSec <= 0) return;
        long delayInMs = TimeUnit.SECONDS.toMillis(delayInSec) - (System.currentTimeMillis() - requestTs);
        if (delayInMs < 0) {
            delayInMs = 0;
        }

        ScheduledFuture<?> scheduledTask = scheduler.schedule(() -> {
            NotificationRequest notificationRequest = notificationRequestService.findNotificationRequestById(tenantId, request.getId());
            if (notificationRequest == null) return;

            notificationExecutor.executeAsync(() -> {
                try {
                    notificationCenter.processNotificationRequest(tenantId, notificationRequest);
                } catch (Exception e) {
                    log.error("Failed to process scheduled notification request {}", notificationRequest.getId(), e);
                    UserId senderId = notificationRequest.getSenderId();
                    if (senderId != null) {
                        notificationCenter.sendBasicNotification(tenantId, senderId, NotificationType.FAILURE, "Notification failure",
                                "Failed to process scheduled notification (request " + notificationRequest.getId() + "): " + e.getMessage());
                    }
                }
            });
            scheduledNotificationRequests.remove(notificationRequest.getId());
        }, delayInMs, TimeUnit.MILLISECONDS);
        scheduledNotificationRequests.put(request.getId(), new ScheduledRequestMetadata(tenantId, scheduledTask));
    }

    @EventListener(ComponentLifecycleMsg.class)
    public void handleComponentLifecycleEvent(ComponentLifecycleMsg event) {
        if (event.getEvent() == ComponentLifecycleEvent.DELETED) {
            EntityId entityId = event.getEntityId();
            switch (entityId.getEntityType()) {
                case NOTIFICATION_REQUEST:
                    cancelAndRemove((NotificationRequestId) entityId);
                    break;
                case TENANT:
                    Set<NotificationRequestId> toCancel = new HashSet<>();
                    scheduledNotificationRequests.forEach((notificationRequestId, scheduledRequestMetadata) -> {
                        if (scheduledRequestMetadata.getTenantId().equals(entityId)) {
                            toCancel.add(notificationRequestId);
                        }
                    });
                    toCancel.forEach(this::cancelAndRemove);
                    break;
            }
        }
    }

    @Override
    protected void cleanupEntityOnPartitionRemoval(NotificationRequestId notificationRequestId) {
        cancelAndRemove(notificationRequestId);
    }

    private void cancelAndRemove(NotificationRequestId notificationRequestId) {
        ScheduledRequestMetadata md = scheduledNotificationRequests.remove(notificationRequestId);
        if (md != null) {
            md.getFuture().cancel(false);
        }
    }

    @Override
    protected String getServiceName() {
        return "Notifications scheduler";
    }

    @Override
    protected String getSchedulerExecutorName() {
        return "notifications-scheduler";
    }

    @Data
    private static class ScheduledRequestMetadata {
        private final TenantId tenantId;
        private final ScheduledFuture<?> future;
    }

}
