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
import com.google.common.util.concurrent.ListenableScheduledFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.notification.NotificationRequestService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.partition.AbstractPartitionBasedService;

import javax.annotation.PostConstruct;
import java.util.Collections;
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

    private final NotificationSubscriptionService notificationSubscriptionService;
    private final NotificationRequestService notificationRequestService;

    private final Map<NotificationRequestId, ScheduledFuture<?>> scheduledNotificationRequests = new ConcurrentHashMap<>();

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
        int delayInMinutes = Optional.ofNullable(request)
                .map(NotificationRequest::getAdditionalConfig)
                .map(NotificationRequestConfig::getSendingDelayInMinutes)
                .orElse(0);
        if (delayInMinutes <= 0) return; // todo: think about: if server was down for some time and delayMs will be negative - need to send these requests as well (but when the value is within some range)
        long delayMs = TimeUnit.MINUTES.toMillis(delayInMinutes) - (System.currentTimeMillis() - requestTs);

        ListenableScheduledFuture<?> scheduledTask = scheduledExecutor.schedule(() -> {
            NotificationRequest notificationRequest = notificationRequestService.findNotificationRequestById(tenantId, request.getId());
            if (notificationRequest == null) return;

            notificationSubscriptionService.processNotificationRequest(tenantId, notificationRequest);
            scheduledNotificationRequests.remove(notificationRequest.getId());
        }, delayMs, TimeUnit.MILLISECONDS);
        scheduledNotificationRequests.put(request.getId(), scheduledTask);
    }

    @Override
    public void onNotificationRequestDeleted(TenantId tenantId, NotificationRequestId notificationRequestId) {
        removeAndCancel(notificationRequestId);
    }

    @Override
    protected void cleanupEntityOnPartitionRemoval(NotificationRequestId notificationRequestId) {
        removeAndCancel(notificationRequestId);
    }

    private void removeAndCancel(NotificationRequestId notificationRequestId) {
        ScheduledFuture<?> scheduledTask = scheduledNotificationRequests.remove(notificationRequestId);
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
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

}
