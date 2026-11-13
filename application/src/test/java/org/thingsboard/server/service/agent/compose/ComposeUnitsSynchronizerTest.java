// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.compose;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentAppUnitType;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.id.AgentAppUnitId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppUnitService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComposeUnitsSynchronizerTest {

    @Mock
    private AgentAppUnitService unitService;
    @Mock
    private TelemetrySubscriptionService tsSubService;

    @InjectMocks
    private ComposeUnitsSynchronizer synchronizer;

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentApplicationId APP_ID = new AgentApplicationId(UUID.randomUUID());
    private static final AgentApplication APP = newApp();

    private static AgentApplication newApp() {
        AgentApplication app = new AgentApplication(APP_ID);
        app.setTenantId(TENANT_ID);
        return app;
    }

    @Test
    void syncUnits_createsContainersVolumesNetworks() {
        JsonNode composeJson = JacksonUtil.toJsonNode("""
                {
                  "services": {"web": {}, "db": {}},
                  "volumes": {"data": {}},
                  "networks": {"frontend": {}}
                }
                """);

        when(unitService.findAgentAppUnitsByAgentAppId(TENANT_ID, APP_ID)).thenReturn(List.of());
        when(unitService.saveAgentAppUnit(eq(TENANT_ID), any(), eq(APP))).thenAnswer(inv -> {
            AgentAppUnit u = inv.getArgument(1);
            u.setId(new AgentAppUnitId(UUID.randomUUID()));
            return u;
        });

        synchronizer.syncUnits(TENANT_ID, APP, composeJson);

        ArgumentCaptor<AgentAppUnit> captor = ArgumentCaptor.forClass(AgentAppUnit.class);
        verify(unitService, times(4)).saveAgentAppUnit(eq(TENANT_ID), captor.capture(), eq(APP));

        List<AgentAppUnit> saved = captor.getAllValues();
        assertThat(saved).extracting(AgentAppUnit::getIdentifier)
                .containsExactlyInAnyOrder("web", "db", "data", "frontend");
        assertThat(saved).extracting(AgentAppUnit::getType)
                .containsExactlyInAnyOrder(
                        AgentAppUnitType.CONTAINER, AgentAppUnitType.CONTAINER,
                        AgentAppUnitType.VOLUME, AgentAppUnitType.NETWORK);
    }

    @Test
    void syncUnits_keepsExistingUnits_createsNewOnes_deletesStale() {
        JsonNode composeJson = JacksonUtil.toJsonNode("""
                {
                  "services": {"web": {}, "new-svc": {}},
                  "volumes": {"data": {}}
                }
                """);

        AgentAppUnit existingWeb = newUnit("web", AgentAppUnitType.CONTAINER);
        AgentAppUnit staleOld = newUnit("old-svc", AgentAppUnitType.CONTAINER);

        when(unitService.findAgentAppUnitsByAgentAppId(TENANT_ID, APP_ID))
                .thenReturn(List.of(existingWeb, staleOld));
        when(unitService.saveAgentAppUnit(eq(TENANT_ID), any(), eq(APP))).thenAnswer(inv -> {
            AgentAppUnit u = inv.getArgument(1);
            u.setId(new AgentAppUnitId(UUID.randomUUID()));
            return u;
        });

        synchronizer.syncUnits(TENANT_ID, APP, composeJson);

        ArgumentCaptor<AgentAppUnit> saveCaptor = ArgumentCaptor.forClass(AgentAppUnit.class);
        verify(unitService, times(2)).saveAgentAppUnit(eq(TENANT_ID), saveCaptor.capture(), eq(APP));
        assertThat(saveCaptor.getAllValues()).extracting(AgentAppUnit::getIdentifier)
                .containsExactlyInAnyOrder("new-svc", "data");

        verify(unitService).deleteAgentAppUnit(TENANT_ID, staleOld.getId(), APP);

        assertThat(saveCaptor.getAllValues()).extracting(AgentAppUnit::getIdentifier)
                .doesNotContain("web");
    }

    @Test
    void syncUnits_returnsResolvedMapKeyedByIdentifier() {
        JsonNode composeJson = JacksonUtil.toJsonNode("""
                {
                  "services": {"web": {}}
                }
                """);

        AgentAppUnit existingWeb = newUnit("web", AgentAppUnitType.CONTAINER);

        when(unitService.findAgentAppUnitsByAgentAppId(TENANT_ID, APP_ID))
                .thenReturn(List.of(existingWeb));

        Map<AgentAppUnitKey, AgentAppUnit> result = synchronizer.syncUnits(TENANT_ID, APP, composeJson);

        assertThat(result).containsOnlyKeys(new AgentAppUnitKey(AgentAppUnitType.CONTAINER, "web"));
        assertThat(result.get(new AgentAppUnitKey(AgentAppUnitType.CONTAINER, "web"))).isSameAs(existingWeb);
    }

    @Test
    void syncUnits_savesImageAttributesFromComposeJson() {
        JsonNode composeJson = JacksonUtil.toJsonNode("""
                {
                  "services": {
                    "web": {"image": "nginx:latest"},
                    "db": {"image": "postgres:15"}
                  }
                }
                """);

        AgentAppUnit web = newUnit("web", AgentAppUnitType.CONTAINER);
        AgentAppUnit db = newUnit("db", AgentAppUnitType.CONTAINER);

        when(unitService.findAgentAppUnitsByAgentAppId(TENANT_ID, APP_ID))
                .thenReturn(List.of(web, db));

        synchronizer.syncUnits(TENANT_ID, APP, composeJson);

        ArgumentCaptor<AttributesSaveRequest> attrCaptor = ArgumentCaptor.forClass(AttributesSaveRequest.class);
        verify(tsSubService, times(2)).saveAttributes(attrCaptor.capture());

        List<AttributesSaveRequest> requests = attrCaptor.getAllValues();
        assertThat(requests).allSatisfy(req -> {
            assertThat(req.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(req.getScope()).isEqualTo(AttributeScope.SERVER_SCOPE);
            assertThat(req.getEntries()).hasSize(1);
            assertThat(req.getEntries().get(0).getKey()).isEqualTo("image");
        });

        assertThat(requests).anySatisfy(req -> {
            assertThat(req.getEntityId()).isEqualTo(web.getId());
            assertThat(req.getEntries().get(0).getValueAsString()).isEqualTo("nginx:latest");
        });
        assertThat(requests).anySatisfy(req -> {
            assertThat(req.getEntityId()).isEqualTo(db.getId());
            assertThat(req.getEntries().get(0).getValueAsString()).isEqualTo("postgres:15");
        });
    }

    @Test
    void loadUnits_returnsMapKeyedByIdentifier() {
        AgentAppUnit web = newUnit("web", AgentAppUnitType.CONTAINER);
        AgentAppUnit db = newUnit("db", AgentAppUnitType.CONTAINER);

        when(unitService.findAgentAppUnitsByAgentAppId(TENANT_ID, APP_ID))
                .thenReturn(List.of(web, db));

        Map<AgentAppUnitKey, AgentAppUnit> result = synchronizer.loadUnits(TENANT_ID, APP_ID);

        assertThat(result).containsOnlyKeys(
                new AgentAppUnitKey(AgentAppUnitType.CONTAINER, "web"),
                new AgentAppUnitKey(AgentAppUnitType.CONTAINER, "db"));
        assertThat(result.get(new AgentAppUnitKey(AgentAppUnitType.CONTAINER, "web"))).isSameAs(web);
        assertThat(result.get(new AgentAppUnitKey(AgentAppUnitType.CONTAINER, "db"))).isSameAs(db);
    }

    @Test
    void syncUnits_sameNameAcrossServiceAndVolume_createsTwoUnitsOfDifferentTypes() {
        JsonNode composeJson = JacksonUtil.toJsonNode("""
                {
                  "services": {"shared": {}},
                  "volumes": {"shared": {}},
                  "networks": {"shared": {}}
                }
                """);

        when(unitService.findAgentAppUnitsByAgentAppId(TENANT_ID, APP_ID)).thenReturn(List.of());
        when(unitService.saveAgentAppUnit(eq(TENANT_ID), any(), eq(APP))).thenAnswer(inv -> {
            AgentAppUnit u = inv.getArgument(1);
            u.setId(new AgentAppUnitId(UUID.randomUUID()));
            return u;
        });

        Map<AgentAppUnitKey, AgentAppUnit> result = synchronizer.syncUnits(TENANT_ID, APP, composeJson);

        ArgumentCaptor<AgentAppUnit> captor = ArgumentCaptor.forClass(AgentAppUnit.class);
        verify(unitService, times(3)).saveAgentAppUnit(eq(TENANT_ID), captor.capture(), eq(APP));

        List<AgentAppUnit> saved = captor.getAllValues();
        assertThat(saved).extracting(AgentAppUnit::getIdentifier)
                .containsExactlyInAnyOrder("shared", "shared", "shared");
        assertThat(saved).extracting(AgentAppUnit::getType)
                .containsExactlyInAnyOrder(
                        AgentAppUnitType.CONTAINER, AgentAppUnitType.VOLUME, AgentAppUnitType.NETWORK);

        assertThat(result).containsOnlyKeys(
                new AgentAppUnitKey(AgentAppUnitType.CONTAINER, "shared"),
                new AgentAppUnitKey(AgentAppUnitType.VOLUME, "shared"),
                new AgentAppUnitKey(AgentAppUnitType.NETWORK, "shared"));

        AgentAppUnit container = result.get(new AgentAppUnitKey(AgentAppUnitType.CONTAINER, "shared"));
        AgentAppUnit volume = result.get(new AgentAppUnitKey(AgentAppUnitType.VOLUME, "shared"));
        AgentAppUnit network = result.get(new AgentAppUnitKey(AgentAppUnitType.NETWORK, "shared"));
        assertThat(container.getType()).isEqualTo(AgentAppUnitType.CONTAINER);
        assertThat(volume.getType()).isEqualTo(AgentAppUnitType.VOLUME);
        assertThat(network.getType()).isEqualTo(AgentAppUnitType.NETWORK);
        assertThat(container.getId()).isNotEqualTo(volume.getId());
        assertThat(container.getId()).isNotEqualTo(network.getId());
        assertThat(volume.getId()).isNotEqualTo(network.getId());
    }

    private AgentAppUnit newUnit(String identifier, AgentAppUnitType type) {
        AgentAppUnit unit = new AgentAppUnit(new AgentAppUnitId(UUID.randomUUID()));
        unit.setAgentApplicationId(APP_ID);
        unit.setIdentifier(identifier);
        unit.setType(type);
        return unit;
    }
}
