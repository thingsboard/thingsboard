// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent;

import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.config.AgentAppArgument;
import org.thingsboard.server.common.data.agent.config.AgentAppArgumentSource;
import org.thingsboard.server.common.data.agent.config.AgentAppArgumentValueType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.dao.agent.AgentAppArgumentSourceResolver;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AgentAppArgumentResolverTest {

    @Mock
    private AttributesService attributesService;
    @Mock
    private TimeseriesService timeseriesService;
    @Mock
    private AgentAppArgumentSourceResolver sourceEntityResolver;

    @InjectMocks
    private AgentAppArgumentResolver resolver;

    private TenantId tenantId;
    private AgentId agentId;

    @BeforeEach
    void setUp() {
        tenantId = TenantId.fromUUID(UUID.randomUUID());
        agentId = new AgentId(UUID.randomUUID());
        lenient().when(sourceEntityResolver.resolveContextSource(eq(tenantId), any(), eq(AgentAppArgumentSource.AGENT)))
                .thenReturn(agentId);
    }

    @Test
    void resolvesEmptyWhenNoArguments() throws Exception {
        AgentApplication app = application();
        Map<String, String> result = resolver.resolve(tenantId, app).get();
        assertThat(result).isEmpty();
    }

    @Test
    void batchesAttributeLookupsPerEntityAndScope() throws Exception {
        AgentApplication app = application(
                argument("a", AgentAppArgumentSource.AGENT, AgentAppArgumentValueType.ATTRIBUTE, AttributeScope.SERVER_SCOPE, "ka", null),
                argument("b", AgentAppArgumentSource.AGENT, AgentAppArgumentValueType.ATTRIBUTE, AttributeScope.SERVER_SCOPE, "kb", null)
        );
        lenient().when(attributesService.find(eq(tenantId), eq(agentId), eq(AttributeScope.SERVER_SCOPE), anyCollection()))
                .thenReturn(Futures.immediateFuture(List.of(
                        new BaseAttributeKvEntry(new StringDataEntry("ka", "va"), 1L),
                        new BaseAttributeKvEntry(new StringDataEntry("kb", "vb"), 1L))));

        Map<String, String> result = resolver.resolve(tenantId, app).get();

        assertThat(result).containsEntry("a", "va").containsEntry("b", "vb");
        verify(attributesService, times(1)).find(eq(tenantId), eq(agentId), eq(AttributeScope.SERVER_SCOPE), anyCollection());
    }

    @Test
    void batchesTelemetryLookupPerEntity() throws Exception {
        AgentApplication app = application(
                argument("a", AgentAppArgumentSource.AGENT, AgentAppArgumentValueType.LATEST_TELEMETRY, null, "ta", null),
                argument("b", AgentAppArgumentSource.AGENT, AgentAppArgumentValueType.LATEST_TELEMETRY, null, "tb", null)
        );
        lenient().when(timeseriesService.findLatest(eq(tenantId), eq(agentId), anyCollection()))
                .thenReturn(Futures.immediateFuture(List.of(
                        new BasicTsKvEntry(1L, new StringDataEntry("ta", "1")),
                        new BasicTsKvEntry(1L, new StringDataEntry("tb", "2")))));

        Map<String, String> result = resolver.resolve(tenantId, app).get();

        assertThat(result).containsEntry("a", "1").containsEntry("b", "2");
        verify(timeseriesService, times(1)).findLatest(eq(tenantId), eq(agentId), anyCollection());
    }

    @Test
    void appliesDefaultWhenValueMissing() throws Exception {
        AgentApplication app = application(
                argument("a", AgentAppArgumentSource.AGENT, AgentAppArgumentValueType.ATTRIBUTE, AttributeScope.SERVER_SCOPE, "ka", "fallback")
        );
        lenient().when(attributesService.find(eq(tenantId), eq(agentId), eq(AttributeScope.SERVER_SCOPE), anyCollection()))
                .thenReturn(Futures.immediateFuture(List.of()));

        Map<String, String> result = resolver.resolve(tenantId, app).get();

        assertThat(result).containsEntry("a", "fallback");
    }

    @Test
    void failsWhenValueMissingAndNoDefault() {
        AgentApplication app = application(
                argument("a", AgentAppArgumentSource.AGENT, AgentAppArgumentValueType.ATTRIBUTE, AttributeScope.SERVER_SCOPE, "ka", null)
        );
        lenient().when(attributesService.find(eq(tenantId), eq(agentId), eq(AttributeScope.SERVER_SCOPE), anyCollection()))
                .thenReturn(Futures.immediateFuture(List.of()));

        assertThatThrownBy(() -> resolver.resolve(tenantId, app).get())
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No value resolved");
    }

    @Test
    void failsWhenSourceEntityMissingAndNoDefault() {
        AgentApplication app = application(
                argument("r", AgentAppArgumentSource.RELATED_ENTITY, AgentAppArgumentValueType.ATTRIBUTE, AttributeScope.SERVER_SCOPE, "kr", null)
        );
        lenient().when(sourceEntityResolver.resolveContextSource(eq(tenantId), any(), eq(AgentAppArgumentSource.RELATED_ENTITY)))
                .thenReturn(null);

        assertThatThrownBy(() -> resolver.resolve(tenantId, app).get())
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No value resolved");
    }

    @Test
    void resolvesOwnerAndRelatedEntitySources() throws ExecutionException, InterruptedException {
        EntityId ownerId = new CustomerId(UUID.randomUUID());
        EntityId relatedId = new EdgeId(UUID.randomUUID());
        AgentApplication app = application(
                argument("o", AgentAppArgumentSource.OWNER, AgentAppArgumentValueType.ATTRIBUTE, AttributeScope.SHARED_SCOPE, "ko", null),
                argument("r", AgentAppArgumentSource.RELATED_ENTITY, AgentAppArgumentValueType.ATTRIBUTE, AttributeScope.SERVER_SCOPE, "kr", null)
        );
        lenient().when(sourceEntityResolver.resolveContextSource(eq(tenantId), any(), eq(AgentAppArgumentSource.OWNER)))
                .thenReturn(ownerId);
        lenient().when(sourceEntityResolver.resolveContextSource(eq(tenantId), any(), eq(AgentAppArgumentSource.RELATED_ENTITY)))
                .thenReturn(relatedId);
        lenient().when(attributesService.find(eq(tenantId), eq(ownerId), eq(AttributeScope.SHARED_SCOPE), anyCollection()))
                .thenReturn(Futures.immediateFuture(List.of(new BaseAttributeKvEntry(new StringDataEntry("ko", "ownerVal"), 1L))));
        lenient().when(attributesService.find(eq(tenantId), eq(relatedId), eq(AttributeScope.SERVER_SCOPE), anyCollection()))
                .thenReturn(Futures.immediateFuture(List.of(new BaseAttributeKvEntry(new StringDataEntry("kr", "relatedVal"), 1L))));

        Map<String, String> result = resolver.resolve(tenantId, app).get();

        assertThat(result).containsEntry("o", "ownerVal").containsEntry("r", "relatedVal");
    }

    @Test
    void resolvesConcreteEntitySource() throws ExecutionException, InterruptedException {
        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        AgentAppArgument argument = argument("d", AgentAppArgumentSource.DEVICE, AgentAppArgumentValueType.ATTRIBUTE,
                AttributeScope.SERVER_SCOPE, "kd", null);
        argument.setSourceEntityId(deviceId);
        AgentApplication app = application(argument);
        lenient().when(attributesService.find(eq(tenantId), eq(deviceId), eq(AttributeScope.SERVER_SCOPE), anyCollection()))
                .thenReturn(Futures.immediateFuture(List.of(new BaseAttributeKvEntry(new StringDataEntry("kd", "deviceVal"), 1L))));

        Map<String, String> result = resolver.resolve(tenantId, app).get();

        assertThat(result).containsEntry("d", "deviceVal");
        verify(attributesService, times(1)).find(eq(tenantId), eq(deviceId), eq(AttributeScope.SERVER_SCOPE), anyCollection());
    }

    @Test
    void resolvesTenantSource() throws ExecutionException, InterruptedException {
        AgentApplication app = application(
                argument("t", AgentAppArgumentSource.TENANT, AgentAppArgumentValueType.ATTRIBUTE, AttributeScope.SERVER_SCOPE, "kt", null));
        lenient().when(sourceEntityResolver.resolveContextSource(eq(tenantId), any(), eq(AgentAppArgumentSource.TENANT)))
                .thenReturn(tenantId);
        lenient().when(attributesService.find(eq(tenantId), eq(tenantId), eq(AttributeScope.SERVER_SCOPE), anyCollection()))
                .thenReturn(Futures.immediateFuture(List.of(new BaseAttributeKvEntry(new StringDataEntry("kt", "tenantVal"), 1L))));

        Map<String, String> result = resolver.resolve(tenantId, app).get();

        assertThat(result).containsEntry("t", "tenantVal");
        verify(attributesService, times(1)).find(eq(tenantId), eq(tenantId), eq(AttributeScope.SERVER_SCOPE), anyCollection());
    }

    private AgentApplication application(AgentAppArgument... arguments) {
        AgentApplication app = new AgentApplication();
        app.setAgentId(agentId);
        DockerComposeConfig config = new DockerComposeConfig();
        if (arguments.length > 0) {
            config.setArguments(List.of(arguments));
        }
        app.setConfig(config);
        return app;
    }

    private AgentAppArgument argument(String name, AgentAppArgumentSource source, AgentAppArgumentValueType valueType,
                                      AttributeScope scope, String key, String defaultValue) {
        AgentAppArgument argument = new AgentAppArgument();
        argument.setName(name);
        argument.setSourceType(source);
        argument.setValueType(valueType);
        argument.setScope(scope);
        argument.setKey(key);
        argument.setDefaultValue(defaultValue);
        return argument;
    }

}
