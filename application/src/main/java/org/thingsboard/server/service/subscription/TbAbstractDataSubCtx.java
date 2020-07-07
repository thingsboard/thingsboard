/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.query.AbstractDataQuery;
import org.thingsboard.server.service.telemetry.TelemetryWebSocketService;
import org.thingsboard.server.service.telemetry.TelemetryWebSocketSessionRef;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Data
public abstract class TbAbstractDataSubCtx<T extends AbstractDataQuery> {

    protected final String serviceId;
    protected final TelemetryWebSocketService wsService;
    protected final TbLocalSubscriptionService localSubscriptionService;
    protected final TelemetryWebSocketSessionRef sessionRef;
    protected final int cmdId;
    @Getter
    @Setter
    protected T query;
    protected Map<Integer, EntityId> subToEntityIdMap;
    @Setter
    protected volatile ScheduledFuture<?> refreshTask;

    public TbAbstractDataSubCtx(String serviceId, TelemetryWebSocketService wsService, TbLocalSubscriptionService localSubscriptionService,
                                TelemetryWebSocketSessionRef sessionRef, int cmdId) {
        this.serviceId = serviceId;
        this.wsService = wsService;
        this.localSubscriptionService = localSubscriptionService;
        this.sessionRef = sessionRef;
        this.cmdId = cmdId;
    }

    public String getSessionId() {
        return sessionRef.getSessionId();
    }

    public TenantId getTenantId() {
        return sessionRef.getSecurityCtx().getTenantId();
    }

    public CustomerId getCustomerId() {
        return sessionRef.getSecurityCtx().getCustomerId();
    }

    public void clearSubscriptions(){
        if (subToEntityIdMap != null) {
            for (Integer subId : subToEntityIdMap.keySet()) {
                localSubscriptionService.cancelSubscription(sessionRef.getSessionId(), subId);
            }
            subToEntityIdMap.clear();
        }
    }

    public void setRefreshTask(ScheduledFuture<?> task) {
        this.refreshTask = task;
    }

    public void cancelTasks() {
        if (this.refreshTask != null) {
            log.trace("[{}][{}] Canceling old refresh task", sessionRef.getSessionId(), cmdId);
            this.refreshTask.cancel(true);
        }
    }

}
