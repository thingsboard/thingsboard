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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.NotificationCenter;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.AlarmOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.NotificationInfo;
import org.thingsboard.server.common.data.notification.NotificationOriginatorType;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.notification.NotificationRequestStatus;
import org.thingsboard.server.common.data.notification.rule.NotificationEscalation;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.NotificationRuleConfig;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.dao.notification.NotificationRequestService;
import org.thingsboard.server.dao.notification.NotificationRuleService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.executors.NotificationExecutorService;

import java.util.List;
import java.util.Map;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class DefaultNotificationRuleProcessingService implements NotificationRuleProcessingService {

    private final NotificationRuleService notificationRuleService;
    private final NotificationRequestService notificationRequestService;
    @Autowired @Lazy
    private NotificationCenter notificationCenter;
    private final NotificationExecutorService notificationExecutor;

    @Override
    public ListenableFuture<Void> onAlarmCreatedOrUpdated(TenantId tenantId, Alarm alarm) {
        return processAlarmUpdate(tenantId, alarm, false);
    }

    @Override
    public ListenableFuture<Void> onAlarmDeleted(TenantId tenantId, Alarm alarm) {
        return processAlarmUpdate(tenantId, alarm, true);
    }

    private ListenableFuture<Void> processAlarmUpdate(TenantId tenantId, Alarm alarm, boolean deleted) {
        NotificationRuleId ruleId = alarm.getNotificationRuleId();
        if (ruleId == null) return Futures.immediateFuture(null);
        return notificationExecutor.submit(() -> {
            try {
                onAlarmUpdate(tenantId, ruleId, alarm, deleted);
            } catch (Exception e) {
                log.error("Failed to process notification rule {} for alarm {}", ruleId, alarm.getId(), e);
                throw e;
            }
            return null;
        });
    }

    private void onAlarmUpdate(TenantId tenantId, NotificationRuleId notificationRuleId, Alarm alarm, boolean deleted) {
        log.debug("Processing alarm update ({}) with notification rule {}", alarm.getId(), notificationRuleId);
        List<NotificationRequest> notificationRequests = notificationRequestService.findNotificationRequestsByRuleIdAndOriginatorEntityId(tenantId, notificationRuleId, alarm.getId());
        NotificationRule notificationRule = notificationRuleService.findNotificationRuleById(tenantId, notificationRuleId);
        if (notificationRule == null) return;

        if (alarmAcknowledged(alarm) || deleted) {
            if (notificationRequests.isEmpty()) {
                return;
            }
            for (NotificationRequest notificationRequest : notificationRequests) {
                if (notificationRequest.getStatus() == NotificationRequestStatus.SCHEDULED) {
                    notificationCenter.deleteNotificationRequest(tenantId, notificationRequest.getId());
                }
            }
        }

        if (notificationRequests.isEmpty()) {
            NotificationRuleConfig config = notificationRule.getConfiguration();
            for (NotificationEscalation escalation : config.getEscalations()) {
                for (NotificationTargetId targetId : escalation.getNotificationTargets()) {
                    submitNotificationRequest(tenantId, targetId, notificationRule, alarm, escalation.getDelayInSec());
                }
            }
        } else {
            NotificationInfo newNotificationInfo = constructNotificationInfo(alarm);
            for (NotificationRequest notificationRequest : notificationRequests) {
                NotificationInfo previousNotificationInfo = notificationRequest.getInfo();
                if (!previousNotificationInfo.equals(newNotificationInfo)) {
                    notificationRequest.setInfo(newNotificationInfo);
                    notificationCenter.updateNotificationRequest(tenantId, notificationRequest);
                }
            }
        }
    }

    private boolean alarmAcknowledged(Alarm alarm) {
        return alarm.getStatus().isAck() && alarm.getStatus().isCleared();
    }

    private void submitNotificationRequest(TenantId tenantId, NotificationTargetId targetId, NotificationRule notificationRule, Alarm alarm, int delayInSec) {
        NotificationRequestConfig config = new NotificationRequestConfig();
        if (delayInSec > 0) {
            config.setSendingDelayInSec(delayInSec);
        }
        NotificationInfo notificationInfo = constructNotificationInfo(alarm);
        NotificationRequest notificationRequest = NotificationRequest.builder()
                .tenantId(tenantId)
                .targets(List.of(targetId))
                .templateId(notificationRule.getTemplateId())
                .deliveryMethods(notificationRule.getDeliveryMethods())
                .additionalConfig(config)
                .info(notificationInfo)
                .ruleId(notificationRule.getId())
                .originatorType(NotificationOriginatorType.ALARM)
                .originatorEntityId(alarm.getId())
                .build();
        notificationCenter.processNotificationRequest(tenantId, notificationRequest);
    }

    private NotificationInfo constructNotificationInfo(Alarm alarm) {
        // TODO: add info about assignee
        return AlarmOriginatedNotificationInfo.builder()
                .alarmId(alarm.getId())
                .alarmType(alarm.getType())
                .alarmOriginator(alarm.getOriginator())
                .alarmSeverity(alarm.getSeverity())
                .alarmStatus(alarm.getStatus())
                .customerId(alarm.getCustomerId())
                .build();
    }

    @EventListener(ComponentLifecycleMsg.class)
    public void onNotificationRuleDeleted(ComponentLifecycleMsg componentLifecycleMsg) {
        if (componentLifecycleMsg.getEvent() != ComponentLifecycleEvent.DELETED ||
                componentLifecycleMsg.getEntityId().getEntityType() != EntityType.NOTIFICATION_RULE) {
            return;
        }

        TenantId tenantId = componentLifecycleMsg.getTenantId();
        NotificationRuleId notificationRuleId = (NotificationRuleId) componentLifecycleMsg.getEntityId();
        List<NotificationRequestId> scheduledForRule = notificationRequestService.findNotificationRequestsIdsByStatusAndRuleId(tenantId, NotificationRequestStatus.SCHEDULED, notificationRuleId);
        for (NotificationRequestId notificationRequestId : scheduledForRule) {
            notificationCenter.deleteNotificationRequest(tenantId, notificationRequestId);
        }
    }

}
