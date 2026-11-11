// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.compose;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentAppUnitType;
import org.thingsboard.server.common.data.id.AgentAppUnitId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.agent.v1.ContainerInfo;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ComposeUnitStateWriterTest {

    @Mock
    private TelemetrySubscriptionService tsSubService;

    @InjectMocks
    private ComposeUnitStateWriter writer;

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentApplicationId APP_ID = new AgentApplicationId(UUID.randomUUID());

    @Test
    void writeStates_savesStateAttributeForKnownContainers() {
        AgentAppUnit web = newUnit("web");
        AgentAppUnit db = newUnit("db");
        Map<AgentAppUnitKey, AgentAppUnit> units = Map.of(
                new AgentAppUnitKey(AgentAppUnitType.CONTAINER, "web"), web,
                new AgentAppUnitKey(AgentAppUnitType.CONTAINER, "db"), db);

        Map<String, ContainerInfo> containerStates = Map.of(
                "web", containerInfo("running"),
                "db", containerInfo("exited"));

        writer.writeStates(TENANT_ID, units, containerStates);

        ArgumentCaptor<AttributesSaveRequest> captor = ArgumentCaptor.forClass(AttributesSaveRequest.class);
        verify(tsSubService, times(2)).saveAttributes(captor.capture());

        List<AttributesSaveRequest> requests = captor.getAllValues();
        assertThat(requests).allSatisfy(req -> {
            assertThat(req.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(req.getScope()).isEqualTo(AttributeScope.SERVER_SCOPE);
            assertThat(req.getEntries()).hasSize(1);
            assertThat(req.getEntries().get(0).getKey()).isEqualTo("state");
        });
        assertThat(requests).anySatisfy(req -> {
            assertThat(req.getEntityId()).isEqualTo(web.getId());
            assertThat(req.getEntries().get(0).getValueAsString()).isEqualTo("running");
        });
        assertThat(requests).anySatisfy(req -> {
            assertThat(req.getEntityId()).isEqualTo(db.getId());
            assertThat(req.getEntries().get(0).getValueAsString()).isEqualTo("exited");
        });
    }

    @Test
    void writeStates_skipsUnknownContainers() {
        AgentAppUnit web = newUnit("web");
        Map<AgentAppUnitKey, AgentAppUnit> units =
                Map.of(new AgentAppUnitKey(AgentAppUnitType.CONTAINER, "web"), web);

        Map<String, ContainerInfo> containerStates = Map.of(
                "web", containerInfo("running"),
                "unknown", containerInfo("exited"));

        writer.writeStates(TENANT_ID, units, containerStates);

        ArgumentCaptor<AttributesSaveRequest> captor = ArgumentCaptor.forClass(AttributesSaveRequest.class);
        verify(tsSubService, times(1)).saveAttributes(captor.capture());
        assertThat(captor.getValue().getEntityId()).isEqualTo(web.getId());
    }

    @Test
    void writeStates_emptyContainerStates_doesNothing() {
        writer.writeStates(TENANT_ID, Map.of(), Map.of());
        verify(tsSubService, never()).saveAttributes(any());
    }

    private AgentAppUnit newUnit(String identifier) {
        AgentAppUnit unit = new AgentAppUnit(new AgentAppUnitId(UUID.randomUUID()));
        unit.setAgentApplicationId(APP_ID);
        unit.setIdentifier(identifier);
        unit.setType(AgentAppUnitType.CONTAINER);
        return unit;
    }

    private static ContainerInfo containerInfo(String state) {
        return ContainerInfo.newBuilder().setState(state).build();
    }
}
