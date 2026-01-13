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
package org.thingsboard.server.service.subscription;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.query.OriginatorAlarmFilter;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.ws.WebSocketService;
import org.thingsboard.server.service.ws.WebSocketSessionRef;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmStatusCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmStatusUpdate;
import org.thingsboard.server.service.ws.telemetry.sub.AlarmSubscriptionUpdate;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@ToString(callSuper = true)
public class TbAlarmStatusSubCtx extends TbAbstractSubCtx {

    private final AlarmService alarmService;
    private final int alarmsPerAlarmStatusSubscriptionCacheSize;

    private volatile TbAlarmStatusSubscription subscription;

    public TbAlarmStatusSubCtx(String serviceId, WebSocketService wsService,
                               TbLocalSubscriptionService localSubscriptionService,
                               SubscriptionServiceStatistics stats, AlarmService alarmService,
                               int alarmsPerAlarmStatusSubscriptionCacheSize,
                               WebSocketSessionRef sessionRef, int cmdId) {
        super(serviceId, wsService, localSubscriptionService, stats, sessionRef, cmdId);
        this.alarmService = alarmService;
        this.alarmsPerAlarmStatusSubscriptionCacheSize = alarmsPerAlarmStatusSubscriptionCacheSize;
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    @Override
    public void stop() {
        super.stop();
        localSubscriptionService.cancelSubscription(getTenantId(), sessionRef.getSessionId(), subscription.getSubscriptionId());
    }

    public void createSubscription(AlarmStatusCmd cmd) {
        SecurityUser securityCtx = sessionRef.getSecurityCtx();
        subscription = TbAlarmStatusSubscription.builder()
                .serviceId(serviceId)
                .sessionId(sessionRef.getSessionId())
                .subscriptionId(sessionRef.getSessionSubIdSeq().incrementAndGet())
                .tenantId(securityCtx.getTenantId())
                .entityId(cmd.getOriginatorId())
                .typeList(cmd.getTypeList())
                .severityList(cmd.getSeverityList())
                .updateProcessor(this::handleAlarmStatusSubscriptionUpdate)
                .build();
        localSubscriptionService.addSubscription(subscription, sessionRef);
    }

    public void sendUpdate() {
        sendWsMsg(AlarmStatusUpdate.builder()
                .cmdId(cmdId)
                .active(subscription.hasAlarms())
                .build());
    }

    public void fetchActiveAlarms() {
        log.trace("[{}, subId: {}] Fetching active alarms from DB", subscription.getSessionId(), subscription.getSubscriptionId());
        OriginatorAlarmFilter originatorAlarmFilter = new OriginatorAlarmFilter(subscription.getEntityId(), subscription.getTypeList(), subscription.getSeverityList());
        List<UUID> alarmIds = alarmService.findActiveOriginatorAlarms(subscription.getTenantId(), originatorAlarmFilter, alarmsPerAlarmStatusSubscriptionCacheSize);

        subscription.getAlarmIds().addAll(alarmIds);
        subscription.setHasMoreAlarmsInDB(alarmIds.size() == alarmsPerAlarmStatusSubscriptionCacheSize);
    }

    private void handleAlarmStatusSubscriptionUpdate(TbSubscription<AlarmSubscriptionUpdate> sub, AlarmSubscriptionUpdate subscriptionUpdate) {
        try {
            AlarmInfo alarm = subscriptionUpdate.getAlarm();
            if (!alarm.getOriginator().equals(subscription.getEntityId())) {
                return;
            }
            Set<UUID> alarmsIds = subscription.getAlarmIds();
            if (alarmsIds.contains(alarm.getId().getId())) {
                if (!subscription.matches(alarm) || subscriptionUpdate.isAlarmDeleted()) {
                    alarmsIds.remove(alarm.getId().getId());
                    if (alarmsIds.isEmpty()) {
                        if (subscription.isHasMoreAlarmsInDB()) {
                            fetchActiveAlarms();
                            if (alarmsIds.isEmpty()) {
                                sendUpdate();
                            }
                        } else {
                            sendUpdate();
                        }
                    }
                }
            } else if (subscription.matches(alarm)) {
                if (alarmsIds.size() < alarmsPerAlarmStatusSubscriptionCacheSize) {
                    alarmsIds.add(alarm.getId().getId());
                    if (alarmsIds.size() == 1) {
                        sendUpdate();
                    }
                } else {
                    subscription.setHasMoreAlarmsInDB(true);
                }
            }
        } catch (Exception e) {
            log.error("[{}, subId: {}] Failed to handle update for alarm status subscription: {}", subscription.getSessionId(), subscription.getSubscriptionId(), subscriptionUpdate, e);
        }
    }
}
