// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.subscription;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.ws.telemetry.sub.AlarmSubscriptionUpdate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;


public class TbAlarmStatusSubscription extends TbSubscription<AlarmSubscriptionUpdate> {

    @Getter
    private final Set<UUID> alarmIds = new HashSet<>();
    @Getter
    @Setter
    private boolean hasMoreAlarmsInDB;
    @Getter
    private final List<String> typeList;
    @Getter
    private final List<AlarmSeverity> severityList;

    @Builder
    public TbAlarmStatusSubscription(String serviceId, String sessionId, int subscriptionId, TenantId tenantId, EntityId entityId,
                                     BiConsumer<TbSubscription<AlarmSubscriptionUpdate>, AlarmSubscriptionUpdate> updateProcessor,
                                     List<String> typeList, List<AlarmSeverity> severityList) {
        super(serviceId, sessionId, subscriptionId, tenantId, entityId, TbSubscriptionType.ALARMS, updateProcessor);
        this.typeList = typeList;
        this.severityList = severityList;
    }

    public boolean matches(AlarmInfo alarm) {
        return !alarm.isCleared() && (this.typeList == null || this.typeList.contains(alarm.getType())) &&
                (this.severityList == null || this.severityList.contains(alarm.getSeverity()));
    }

    public boolean hasAlarms() {
        return !alarmIds.isEmpty();
    }
}
