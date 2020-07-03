/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.subscription;

import lombok.Builder;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.telemetry.sub.AlarmSubscriptionUpdate;
import org.thingsboard.server.service.telemetry.sub.TsSubscriptionUpdate;

import java.util.List;
import java.util.function.BiConsumer;

public class TbAlarmsSubscription extends TbSubscription<AlarmSubscriptionUpdate> {

    private final long ts;
    private final List<String> typeList;
    private final List<AlarmSearchStatus> statusList;
    private final List<AlarmSeverity> severityList;
    private final boolean searchPropagatedAlarms;

    @Builder
    public TbAlarmsSubscription(String serviceId, String sessionId, int subscriptionId, TenantId tenantId, EntityId entityId,
                                TbSubscriptionType type, BiConsumer<String, AlarmSubscriptionUpdate> updateConsumer,
                                long ts, List<String> typeList, List<AlarmSearchStatus> statusList,
                                List<AlarmSeverity> severityList, boolean searchPropagatedAlarms) {
        super(serviceId, sessionId, subscriptionId, tenantId, entityId, type, updateConsumer);
        this.ts = ts;
        this.typeList = typeList;
        this.statusList = statusList;
        this.severityList = severityList;
        this.searchPropagatedAlarms = searchPropagatedAlarms;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
