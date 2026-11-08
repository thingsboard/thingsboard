// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.integration.ToIntegrationExecutorDownlinkMsg;
import org.thingsboard.server.gen.integration.ToIntegrationExecutorNotificationMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.provider.TbCoreIntegrationExecutorQueueFactory;
import org.thingsboard.server.queue.settings.TbQueueIntegrationExecutorSettings;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultClusterIntegrationServiceTest {

    private static final long TIMEOUT_MS = 5000;

    @Mock
    private TbServiceInfoProvider serviceInfoProvider;
    @Mock
    private TbQueueIntegrationExecutorSettings settings;
    @Mock
    private TbCoreIntegrationExecutorQueueFactory queueFactory;
    @Mock
    private IntegrationManagerService integrationManagerService;
    @Mock
    private TbQueueConsumer<TbProtoQueueMsg<ToIntegrationExecutorDownlinkMsg>> mqttConsumer;
    @Mock
    private TbQueueConsumer<TbProtoQueueMsg<ToIntegrationExecutorDownlinkMsg>> httpConsumer;

    private DefaultClusterIntegrationService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(serviceInfoProvider.getSupportedIntegrationTypes()).thenReturn(List.of(IntegrationType.MQTT, IntegrationType.HTTP));
        when(queueFactory.createToIntegrationExecutorNotificationsMsgConsumer())
                .thenReturn(mock(TbQueueConsumer.class));
        when(queueFactory.createToIntegrationExecutorDownlinkMsgConsumer(IntegrationType.MQTT)).thenReturn(mqttConsumer);
        when(queueFactory.createToIntegrationExecutorDownlinkMsgConsumer(IntegrationType.HTTP)).thenReturn(httpConsumer);
        service = new DefaultClusterIntegrationService(serviceInfoProvider, settings, queueFactory, integrationManagerService) {
            @Override
            protected void launchMainConsumers() {
            }

            @Override
            protected void launchNotificationsConsumer() {
            }
        };
        service.init();
    }

    @AfterEach
    void tearDown() {
        service.stop();
    }

    @Test
    void givenStarted_whenPartitionsChange_thenEveryNewSnapshotIsApplied() {
        // Rolling restart: [1] and [0, 2] arrive before startup completes, [1] arrives after it.
        service.onApplicationEvent(partitionChangeEvent(1));
        service.onApplicationEvent(partitionChangeEvent(0, 2));
        service.onApplicationEvent((ApplicationReadyEvent) null);
        verify(integrationManagerService, timeout(TIMEOUT_MS)).refresh(IntegrationType.MQTT, tpis(0, 2));

        service.onApplicationEvent(partitionChangeEvent(1));

        verify(integrationManagerService, timeout(TIMEOUT_MS)).refresh(IntegrationType.MQTT, tpis(1));
        verify(mqttConsumer, timeout(TIMEOUT_MS)).subscribe(tpis(1));
        InOrder inOrder = inOrder(integrationManagerService);
        inOrder.verify(integrationManagerService).refresh(IntegrationType.MQTT, tpis(0, 2));
        inOrder.verify(integrationManagerService).refresh(IntegrationType.MQTT, tpis(1));
    }

    @Test
    void givenLongRunningNode_whenPartitionsFlipFlop_thenLatestSnapshotIsApplied() {
        // Long-running node: started long ago with [2], then a neighbour restarts -> [1] -> [2].
        service.onApplicationEvent(partitionChangeEvent(2));
        service.onApplicationEvent((ApplicationReadyEvent) null);
        verify(integrationManagerService, timeout(TIMEOUT_MS)).refresh(IntegrationType.MQTT, tpis(2));

        service.onApplicationEvent(partitionChangeEvent(1));
        verify(integrationManagerService, timeout(TIMEOUT_MS)).refresh(IntegrationType.MQTT, tpis(1));
        service.onApplicationEvent(partitionChangeEvent(2));

        verify(integrationManagerService, timeout(TIMEOUT_MS).times(2)).refresh(IntegrationType.MQTT, tpis(2));
        InOrder inOrder = inOrder(integrationManagerService, mqttConsumer);
        inOrder.verify(mqttConsumer).subscribe(tpis(1));
        inOrder.verify(mqttConsumer).subscribe(tpis(2));
    }

    @Test
    void givenStarted_whenNodeLosesAllPartitions_thenEmptySnapshotIsApplied() {
        service.onApplicationEvent(partitionChangeEvent(0));
        service.onApplicationEvent((ApplicationReadyEvent) null);
        verify(integrationManagerService, timeout(TIMEOUT_MS)).refresh(IntegrationType.MQTT, tpis(0));

        service.onApplicationEvent(partitionChangeEvent());

        verify(integrationManagerService, timeout(TIMEOUT_MS)).refresh(eq(IntegrationType.MQTT), eq(Collections.emptySet()));
    }

    @Test
    void givenEventsForDifferentTypesPendingAtStartup_whenStarted_thenEveryTypeGetsItsLatestPartitions() {
        // An event carries only the queue keys whose partitions changed: a later MQTT-only event must not hide HTTP.
        service.onApplicationEvent(partitionChangeEvent(Map.of(IntegrationType.MQTT, tpis(0), IntegrationType.HTTP, tpis(1))));
        service.onApplicationEvent(partitionChangeEvent(Map.of(IntegrationType.MQTT, tpis(2))));
        service.onApplicationEvent((ApplicationReadyEvent) null);

        verify(integrationManagerService, timeout(TIMEOUT_MS)).refresh(IntegrationType.HTTP, tpis(1));
        verify(httpConsumer, timeout(TIMEOUT_MS)).subscribe(tpis(1));
        verify(integrationManagerService, timeout(TIMEOUT_MS)).refresh(IntegrationType.MQTT, tpis(2));
        verify(mqttConsumer, timeout(TIMEOUT_MS)).subscribe(tpis(2));
        verify(integrationManagerService, never()).refresh(IntegrationType.MQTT, tpis(0));
    }

    private static PartitionChangeEvent partitionChangeEvent(Integer... partitions) {
        return partitionChangeEvent(Map.of(IntegrationType.MQTT, tpis(partitions)));
    }

    private static PartitionChangeEvent partitionChangeEvent(Map<IntegrationType, Set<TopicPartitionInfo>> partitionsByType) {
        Map<QueueKey, Set<TopicPartitionInfo>> newPartitions = partitionsByType.entrySet().stream().collect(Collectors.toMap(
                e -> new QueueKey(ServiceType.TB_INTEGRATION_EXECUTOR, e.getKey().name()), Map.Entry::getValue));
        return new PartitionChangeEvent(DefaultClusterIntegrationServiceTest.class, ServiceType.TB_INTEGRATION_EXECUTOR,
                newPartitions, Collections.emptyMap());
    }

    private static Set<TopicPartitionInfo> tpis(Integer... partitions) {
        return Arrays.stream(partitions)
                .map(p -> new TopicPartitionInfo("tb_ie.downlink.mqtt", null, p, true))
                .collect(Collectors.toSet());
    }

}
