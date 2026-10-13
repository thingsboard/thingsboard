// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.compose;

import com.google.common.util.concurrent.FutureCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentAppUnitType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.gen.agent.v1.ContainerInfo;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.Map;

@Service
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class ComposeUnitStateWriter {

    private static final String STATE_ATTR_KEY = "state";

    private final TelemetrySubscriptionService tsSubService;

    public void writeStates(TenantId tenantId, Map<AgentAppUnitKey, AgentAppUnit> units, Map<String, ContainerInfo> containerStates) {
        for (var entry : containerStates.entrySet()) {
            AgentAppUnit unit = units.get(new AgentAppUnitKey(AgentAppUnitType.CONTAINER, entry.getKey()));
            if (unit == null) {
                continue;
            }
            String state = entry.getValue().getState();
            tsSubService.saveAttributes(AttributesSaveRequest.builder()
                    .tenantId(tenantId)
                    .entityId(unit.getId())
                    .scope(AttributeScope.SERVER_SCOPE)
                    .entry(new StringDataEntry(STATE_ATTR_KEY, state))
                    .callback(getSaveCallback(tenantId, unit, state))
                    .build());
        }
    }

    private FutureCallback<Void> getSaveCallback(TenantId tenantId, AgentAppUnit unit, String state) {
        return new FutureCallback<>() {
            @Override
            public void onSuccess(Void result) {
                log.trace("[{}] Updated {} [{}] for unit [{}]", tenantId, STATE_ATTR_KEY, state, unit.getIdentifier());
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("[{}] Failed to update {} [{}] for unit [{}]", tenantId, STATE_ATTR_KEY, state, unit.getIdentifier(), t);
            }
        };
    }
}
