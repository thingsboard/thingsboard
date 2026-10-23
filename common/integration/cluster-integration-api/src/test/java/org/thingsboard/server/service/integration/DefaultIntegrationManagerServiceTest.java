// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.IntegrationStatisticsService;
import org.thingsboard.integration.api.ThingsboardPlatformIntegration;
import org.thingsboard.integration.api.converter.TBUplinkDataConverter;
import org.thingsboard.integration.api.util.IntegrationUtil;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.settings.TbQueueIntegrationExecutorSettings;
import org.thingsboard.server.service.converter.DataConverterService;
import org.thingsboard.server.service.integration.state.IntegrationState;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultIntegrationManagerServiceTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final int PARTITIONS = 3;

    @Mock
    private TbServiceInfoProvider serviceInfoProvider;
    @Mock
    private PartitionService partitionService;
    @Mock
    private TopicService topicService;
    @Mock
    private IntegrationContextProvider integrationContextProvider;
    @Mock
    private IntegrationConfigurationService configurationService;
    @Mock
    private DataConverterService dataConverterService;
    @Mock
    private EventStorageService eventStorageService;
    @Mock
    private TbQueueProducerProvider producerProvider;
    @Mock
    private NotificationRuleProcessor notificationRuleProcessor;
    @Mock
    private IntegrationStatisticsService integrationStatisticsService;
    @Mock
    private TbQueueIntegrationExecutorSettings integrationExecutorSettings;

    private DefaultIntegrationManagerService service;
    private MockedStatic<IntegrationUtil> integrationUtil;

    // The partitions this node currently owns, as HashPartitionService.myPartitions would report them.
    private final Set<Integer> livePartitions = new HashSet<>();
    private final Map<UUID, Integer> partitionByEntity = new HashMap<>();
    private final Map<IntegrationId, Integration> configurations = new HashMap<>();
    private final Map<IntegrationId, ThingsboardPlatformIntegration<?>> platformIntegrations = new HashMap<>();
    // Tasks submitted to the lifecycle executor; they run only when the test calls runLifecycleTasks().
    private final Queue<Runnable> lifecycleTasks = new ArrayDeque<>();

    @BeforeEach
    void setUp() throws Exception {
        service = new DefaultIntegrationManagerService(serviceInfoProvider, partitionService, topicService, integrationContextProvider,
                configurationService, dataConverterService, eventStorageService, producerProvider, notificationRuleProcessor,
                Optional.empty(), Optional.empty(), integrationStatisticsService, integrationExecutorSettings,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        ReflectionTestUtils.setField(service, "reInitEnabled", false);
        ReflectionTestUtils.setField(service, "statisticsEnabled", false);
        when(serviceInfoProvider.getSupportedIntegrationTypes()).thenReturn(List.of(IntegrationType.MQTT, IntegrationType.HTTP));
        service.init();
        ((ScheduledExecutorService) ReflectionTestUtils.getField(service, "lifecycleExecutorService")).shutdownNow();
        ScheduledExecutorService lifecycleExecutor = mock(ScheduledExecutorService.class);
        doAnswer(inv -> {
            lifecycleTasks.add(inv.getArgument(0));
            return null;
        }).when(lifecycleExecutor).submit(any(Runnable.class));
        doAnswer(inv -> {
            lifecycleTasks.add(inv.getArgument(0));
            return null;
        }).when(lifecycleExecutor).schedule(any(Runnable.class), anyLong(), any());
        ReflectionTestUtils.setField(service, "lifecycleExecutorService", lifecycleExecutor);

        when(partitionService.resolve(eq(ServiceType.TB_INTEGRATION_EXECUTOR), anyString(), any(TenantId.class), any(EntityId.class))).thenAnswer(inv -> {
            EntityId entityId = inv.getArgument(3);
            int partition = partitionByEntity.get(entityId.getId());
            return new TopicPartitionInfo("tb_ie.downlink.mqtt", null, partition, livePartitions.contains(partition));
        });
        when(partitionService.getMyPartitions(any(QueueKey.class))).thenAnswer(inv -> List.copyOf(livePartitions));
        when(partitionService.getIntegrationExecutorPartitionsCount()).thenReturn(PARTITIONS);
        when(partitionService.resolvePartitionIndex(any(UUID.class), eq(PARTITIONS))).thenAnswer(inv -> partitionByEntity.get((UUID) inv.getArgument(0)));
        when(configurationService.getIntegration(any(TenantId.class), any(IntegrationId.class)))
                .thenAnswer(inv -> configurations.get((IntegrationId) inv.getArgument(1)));
        when(integrationContextProvider.buildIntegrationContext(any())).thenReturn(mock(IntegrationContext.class));
        when(dataConverterService.getUplinkConverterById(any(), any())).thenReturn(Optional.of(mock(TBUplinkDataConverter.class)));
        when(dataConverterService.getDownlinkConverterById(any(), any())).thenReturn(Optional.empty());

        integrationUtil = mockStatic(IntegrationUtil.class);
        integrationUtil.when(() -> IntegrationUtil.createPlatformIntegration(any(), any(), anyBoolean(), any())).thenAnswer(inv -> {
            Integration configuration = configurations.values().stream()
                    .filter(i -> i.getConfiguration() == inv.getArgument(1)).findFirst().orElseThrow();
            return platformIntegrations.get(configuration.getId());
        });
    }

    @AfterEach
    void tearDown() {
        integrationUtil.close();
        service.stop();
    }

    @Test
    void givenCreatedScheduledWhileOwned_whenPartitionMovesAwayBeforeItRuns_thenIntegrationIsNotStarted() throws Exception {
        Integration integration = mqttIntegration(0);
        ThingsboardPlatformIntegration<?> platformIntegration = platformIntegrations.get(integration.getId());
        when(configurationService.getActiveIntegrationList(IntegrationType.MQTT, false)).thenReturn(List.of(integration));

        // Rolling restart: this node owns [1], then briefly [0, 2] - the refresh schedules CREATED for the partition 0 singleton.
        ownPartitions(1);
        service.refresh(IntegrationType.MQTT, tpis(1));
        ownPartitions(0, 2);
        service.refresh(IntegrationType.MQTT, tpis(0, 2));
        assertThat(lifecycleTasks).isNotEmpty();

        // Ownership moves back to [1] before the lifecycle executor gets to the CREATED event (it is busy with a
        // backlog of slow inits, and the refresh for [1] has not stopped this one yet).
        ownPartitions(1);
        runLifecycleTasks();

        verify(platformIntegration, never()).init(any());
        assertThat(integrationStates()).doesNotContainKey(integration.getId());
    }

    @Test
    void givenCreatedScheduledWhileOwned_whenPartitionMovesAwayAndRefreshRuns_thenIntegrationIsNotStarted() throws Exception {
        Integration integration = mqttIntegration(0);
        ThingsboardPlatformIntegration<?> platformIntegration = platformIntegrations.get(integration.getId());
        when(configurationService.getActiveIntegrationList(IntegrationType.MQTT, false)).thenReturn(List.of(integration));

        ownPartitions(1);
        service.refresh(IntegrationType.MQTT, tpis(1));
        ownPartitions(0, 2);
        service.refresh(IntegrationType.MQTT, tpis(0, 2));
        ownPartitions(1);
        service.refresh(IntegrationType.MQTT, tpis(1));
        runLifecycleTasks();

        verify(platformIntegration, never()).init(any());
        assertThat(integrationStates()).doesNotContainKey(integration.getId());
    }

    @Test
    void givenRunningIntegration_whenUpdateArrivesAfterPartitionMovedAway_thenIntegrationIsStopped() throws Exception {
        Integration integration = mqttIntegration(0);
        ThingsboardPlatformIntegration<?> platformIntegration = platformIntegrations.get(integration.getId());
        when(configurationService.getActiveIntegrationList(IntegrationType.MQTT, false)).thenReturn(List.of(integration));

        ownPartitions(0, 2);
        service.refresh(IntegrationType.MQTT, tpis(0, 2));
        runLifecycleTasks();
        verify(platformIntegration).init(any());

        // An UPDATED scheduled through a path that captured ownership earlier (e.g. createIntegrations of a stale snapshot)
        ownPartitions(1);
        ReflectionTestUtils.invokeMethod(service, "scheduleIntegrationEvent", TENANT_ID, integration.getId(),
                ComponentLifecycleEvent.UPDATED);
        runLifecycleTasks();

        verify(platformIntegration, never()).update(any());
        verify(platformIntegration).destroy();
        assertThat(integrationStates()).doesNotContainKey(integration.getId());
    }

    @Test
    void givenLongRunningSingleton_whenPartitionMovesAway_thenRefreshStopsIt() throws Exception {
        Integration integration = mqttIntegration(1);
        ThingsboardPlatformIntegration<?> platformIntegration = platformIntegrations.get(integration.getId());
        when(configurationService.getActiveIntegrationList(IntegrationType.MQTT, false)).thenReturn(List.of(integration));

        ownPartitions(1);
        service.refresh(IntegrationType.MQTT, tpis(1));
        runLifecycleTasks();
        verify(platformIntegration).init(any());

        // Hours later a neighbour restarts and partition 1 goes back to it.
        ownPartitions(2);
        service.refresh(IntegrationType.MQTT, tpis(2));
        runLifecycleTasks();

        verify(platformIntegration).destroy();
        assertThat(integrationStates()).doesNotContainKey(integration.getId());
    }

    @Test
    void givenRunningSingleton_whenRefreshedWithEmptyPartitions_thenIntegrationIsStopped() throws Exception {
        Integration integration = mqttIntegration(0);
        ThingsboardPlatformIntegration<?> platformIntegration = platformIntegrations.get(integration.getId());
        when(configurationService.getActiveIntegrationList(IntegrationType.MQTT, false)).thenReturn(List.of(integration));

        ownPartitions(0);
        service.refresh(IntegrationType.MQTT, tpis(0));
        runLifecycleTasks();
        verify(platformIntegration).init(any());

        ownPartitions();
        service.refresh(IntegrationType.MQTT, Collections.emptySet());
        runLifecycleTasks();

        verify(platformIntegration).destroy();
        assertThat(integrationStates()).doesNotContainKey(integration.getId());
    }

    @Test
    void givenEmptyPartitions_whenRefreshed_thenNonSingletonIntegrationsAreNotCreated() throws Exception {
        Integration integration = mqttIntegration(0);
        integration.setType(IntegrationType.HTTP);
        when(configurationService.getActiveIntegrationList(IntegrationType.HTTP, false)).thenReturn(List.of(integration));

        ownPartitions();
        service.refresh(IntegrationType.HTTP, Collections.emptySet());
        runLifecycleTasks();

        verify(platformIntegrations.get(integration.getId()), never()).init(any());
        assertThat(integrationStates()).doesNotContainKey(integration.getId());
    }

    @Test
    void givenRunningSingletonOfAnotherType_whenRefreshedForOtherType_thenItIsNotStopped() throws Exception {
        Integration integration = mqttIntegration(0);
        ThingsboardPlatformIntegration<?> platformIntegration = platformIntegrations.get(integration.getId());
        when(configurationService.getActiveIntegrationList(IntegrationType.MQTT, false)).thenReturn(List.of(integration));

        ownPartitions(0);
        service.refresh(IntegrationType.MQTT, tpis(0));
        runLifecycleTasks();
        verify(platformIntegration).init(any());

        // HTTP partitions say nothing about MQTT ownership.
        service.refresh(IntegrationType.HTTP, Collections.emptySet());
        runLifecycleTasks();

        verify(platformIntegration, never()).destroy();
        assertThat(integrationStates()).containsKey(integration.getId());
    }

    @Test
    void givenFailedIntegration_whenPartitionMovedAway_thenReInitStopsItInsteadOfStartingIt() throws Exception {
        Integration integration = mqttIntegration(0);
        ThingsboardPlatformIntegration<?> platformIntegration = platformIntegrations.get(integration.getId());
        doThrow(new RuntimeException("Failed to connect to MQTT broker")).when(platformIntegration).init(any());
        when(configurationService.getActiveIntegrationList(IntegrationType.MQTT, false)).thenReturn(List.of(integration));

        ownPartitions(0, 2);
        service.refresh(IntegrationType.MQTT, tpis(0, 2));
        runLifecycleTasks();
        verify(platformIntegration, times(1)).init(any());
        assertThat(integrationStates()).containsKey(integration.getId());

        ownPartitions(1);
        ReflectionTestUtils.invokeMethod(service, "reInitIntegrations");

        verify(platformIntegration, times(1)).init(any());
        assertThat(integrationStates()).doesNotContainKey(integration.getId());
    }

    @Test
    void givenSingletonRunning_whenOwnershipMovesWithoutRefresh_thenOwnershipReportsMismatchAndNotMine() throws Exception {
        Integration mine = mqttIntegration(1);
        Integration stray = mqttIntegration(0);
        doThrow(new RuntimeException("Failed to connect to MQTT broker")).when(platformIntegrations.get(mine.getId())).init(any());
        when(configurationService.getActiveIntegrationList(IntegrationType.MQTT, false)).thenReturn(List.of(mine, stray));

        // The bug we fixed: [0, 1] is applied, then PartitionService moves to [1] but no refresh() follows.
        ownPartitions(0, 1);
        service.refresh(IntegrationType.MQTT, tpis(0, 1));
        runLifecycleTasks();
        ownPartitions(1);

        IntegrationOwnershipMonitor.TypeOwnership mqtt = ownershipOf(IntegrationType.MQTT);
        assertThat(mqtt.owned()).containsExactly(1);
        assertThat(mqtt.applied()).containsExactlyInAnyOrder(0, 1);
        assertThat(mqtt.isMismatch()).isTrue();
        assertThat(mqtt.notMineRunning()).containsExactly(stray.getId());
        assertThat(mqtt.started()).isEqualTo(1);
        assertThat(mqtt.failed()).isEqualTo(1);
        IntegrationOwnershipMonitor.TypeOwnership http = ownershipOf(IntegrationType.HTTP);
        assertThat(http.started() + http.failed() + http.other()).isZero();
        assertThat(http.notMineRunning()).isEmpty();
        assertThat(http.applied()).isEmpty();

        // Once the refresh runs, the stray is stopped and the picture is consistent again.
        service.refresh(IntegrationType.MQTT, tpis(1));
        runLifecycleTasks();

        mqtt = ownershipOf(IntegrationType.MQTT);
        assertThat(mqtt.isMismatch()).isFalse();
        assertThat(mqtt.notMineRunning()).isEmpty();
        assertThat(mqtt.started()).isZero();
    }

    @Test
    void givenSingletonsRunning_whenOwnershipCollected_thenTenantRoutingIsNotResolved() throws Exception {
        Integration mine = mqttIntegration(1);
        Integration stray = mqttIntegration(0);
        when(configurationService.getActiveIntegrationList(IntegrationType.MQTT, false)).thenReturn(List.of(mine, stray));
        ownPartitions(0, 1);
        service.refresh(IntegrationType.MQTT, tpis(0, 1));
        runLifecycleTasks();
        ownPartitions(1);
        clearInvocations(partitionService);

        IntegrationOwnershipMonitor.TypeOwnership mqtt = ownershipOf(IntegrationType.MQTT);

        // resolve() loads the tenant routing info, i.e. the tenant profile, for each integration: the periodic check must not
        verify(partitionService, never()).resolve(any(ServiceType.class), anyString(), any(TenantId.class), any(EntityId.class));
        assertThat(mqtt.notMineRunning()).containsExactly(stray.getId());
    }

    private IntegrationOwnershipMonitor.TypeOwnership ownershipOf(IntegrationType type) {
        return service.collectOwnership().stream().filter(t -> t.type() == type).findFirst().orElseThrow();
    }

    private Integration mqttIntegration(int partition) {
        IntegrationId id = new IntegrationId(UUID.randomUUID());
        Integration integration = new Integration(id);
        integration.setTenantId(TENANT_ID);
        integration.setName("MQTT " + id);
        integration.setType(IntegrationType.MQTT);
        integration.setRoutingKey(id.getId().toString());
        integration.setEnabled(true);
        integration.setDefaultConverterId(new ConverterId(UUID.randomUUID()));
        integration.setConfiguration(JacksonUtil.newObjectNode().put("id", id.getId().toString()));
        partitionByEntity.put(id.getId(), partition);
        configurations.put(id, integration);
        ThingsboardPlatformIntegration<?> platformIntegration = mock(ThingsboardPlatformIntegration.class);
        when(platformIntegration.getConfiguration()).thenReturn(integration);
        platformIntegrations.put(id, platformIntegration);
        return integration;
    }

    private void ownPartitions(Integer... partitions) {
        livePartitions.clear();
        Collections.addAll(livePartitions, partitions);
    }

    private Set<TopicPartitionInfo> tpis(Integer... partitions) {
        return Arrays.stream(partitions)
                .map(p -> new TopicPartitionInfo("tb_ie.downlink.mqtt", null, p, true))
                .collect(Collectors.toSet());
    }

    private void runLifecycleTasks() {
        Runnable task;
        while ((task = lifecycleTasks.poll()) != null) {
            task.run();
        }
    }

    @SuppressWarnings("unchecked")
    private ConcurrentMap<IntegrationId, IntegrationState> integrationStates() {
        return (ConcurrentMap<IntegrationId, IntegrationState>) ReflectionTestUtils.getField(service, "integrations");
    }

}
