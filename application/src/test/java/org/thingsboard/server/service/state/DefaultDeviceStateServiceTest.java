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
package org.thingsboard.server.service.state;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.AttributesSaveResult;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.notification.rule.trigger.DeviceActivityTrigger;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.sql.query.EntityQueryRepository;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.usagestats.DefaultTbApiUsageReportClient;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.service.state.DefaultDeviceStateService.ACTIVITY_STATE;
import static org.thingsboard.server.service.state.DefaultDeviceStateService.INACTIVITY_ALARM_TIME;
import static org.thingsboard.server.service.state.DefaultDeviceStateService.LAST_ACTIVITY_TIME;
import static org.thingsboard.server.service.state.DefaultDeviceStateService.LAST_CONNECT_TIME;
import static org.thingsboard.server.service.state.DefaultDeviceStateService.LAST_DISCONNECT_TIME;

@ExtendWith(MockitoExtension.class)
class DefaultDeviceStateServiceTest {

    ListeningExecutorService deviceStateExecutor;
    ListeningExecutorService deviceStateCallbackExecutor;

    @Mock
    DeviceService deviceService;
    @Mock
    AttributesService attributesService;
    @Mock
    TimeseriesService tsService;
    @Mock
    TbClusterService clusterService;
    @Mock
    PartitionService partitionService;
    @Mock
    DeviceStateData deviceStateDataMock;
    @Mock
    EntityQueryRepository entityQueryRepository;
    @Mock
    TelemetrySubscriptionService telemetrySubscriptionService;
    @Mock
    NotificationRuleProcessor notificationRuleProcessor;
    @Mock
    DefaultTbApiUsageReportClient defaultTbApiUsageReportClient;

    long defaultInactivityTimeoutMs = Duration.ofMinutes(10L).toMillis();

    TenantId tenantId = TenantId.fromUUID(UUID.fromString("00797a3b-7aeb-4b5b-b57a-c2a810d0f112"));
    DeviceId deviceId = DeviceId.fromString("c209f718-42e5-11f0-9fe2-0242ac120002");
    TopicPartitionInfo tpi = TopicPartitionInfo.builder()
            .topic("tb_core")
            .partition(0)
            .myPartition(true)
            .build();

    DefaultDeviceStateService service;

    @BeforeEach
    void setUp() {
        service = spy(new DefaultDeviceStateService(deviceService, attributesService, tsService, clusterService, partitionService, entityQueryRepository, null, defaultTbApiUsageReportClient, notificationRuleProcessor));
        ReflectionTestUtils.setField(service, "tsSubService", telemetrySubscriptionService);
        ReflectionTestUtils.setField(service, "defaultInactivityTimeoutMs", defaultInactivityTimeoutMs);
        ReflectionTestUtils.setField(service, "defaultStateCheckIntervalInSec", 60);
        ReflectionTestUtils.setField(service, "defaultActivityStatsIntervalInSec", 60);
        ReflectionTestUtils.setField(service, "initFetchPackSize", 50000);

        deviceStateExecutor = MoreExecutors.newDirectExecutorService();
        ReflectionTestUtils.setField(service, "deviceStateExecutor", deviceStateExecutor);

        deviceStateCallbackExecutor = MoreExecutors.newDirectExecutorService();
        ReflectionTestUtils.setField(service, "deviceStateCallbackExecutor", deviceStateCallbackExecutor);

        lenient().when(partitionService.resolve(ServiceType.TB_CORE, tenantId, deviceId)).thenReturn(tpi);

        ConcurrentMap<TopicPartitionInfo, Set<DeviceId>> partitionedEntities = new ConcurrentHashMap<>();
        partitionedEntities.put(tpi, new HashSet<>());
        ReflectionTestUtils.setField(service, "partitionedEntities", partitionedEntities);
    }

    @AfterEach
    void cleanup() {
        deviceStateExecutor.shutdownNow();
        deviceStateCallbackExecutor.shutdownNow();
    }

    @Test
    void givenDeviceBelongsToExternalPartition_whenOnDeviceConnect_thenCleansStateAndDoesNotReportConnect() {
        // GIVEN
        doReturn(true).when(service).cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId);

        // WHEN
        service.onDeviceConnect(tenantId, deviceId, System.currentTimeMillis());

        // THEN
        then(service).should().cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId);
        then(service).should(never()).getOrFetchDeviceStateData(deviceId);
        then(service).should(never()).checkAndUpdateState(eq(deviceId), any());
        then(clusterService).shouldHaveNoInteractions();
        then(notificationRuleProcessor).shouldHaveNoInteractions();
        then(telemetrySubscriptionService).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @ValueSource(longs = {Long.MIN_VALUE, -100, -1})
    void givenNegativeLastConnectTime_whenOnDeviceConnect_thenSkipsThisEvent(long negativeLastConnectTime) {
        // GIVEN
        doReturn(false).when(service).cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId);

        // WHEN
        service.onDeviceConnect(tenantId, deviceId, negativeLastConnectTime);

        // THEN
        then(service).should(never()).getOrFetchDeviceStateData(deviceId);
        then(service).should(never()).checkAndUpdateState(eq(deviceId), any());
        then(clusterService).shouldHaveNoInteractions();
        then(notificationRuleProcessor).shouldHaveNoInteractions();
        then(telemetrySubscriptionService).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @MethodSource("provideOutdatedTimestamps")
    void givenOutdatedLastConnectTime_whenOnDeviceDisconnect_thenSkipsThisEvent(long outdatedLastConnectTime, long currentLastConnectTime) {
        // GIVEN
        doReturn(false).when(service).cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId);

        var deviceStateData = DeviceStateData.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .state(DeviceState.builder().lastConnectTime(currentLastConnectTime).build())
                .build();
        service.deviceStates.put(deviceId, deviceStateData);

        // WHEN
        service.onDeviceConnect(tenantId, deviceId, outdatedLastConnectTime);

        // THEN
        then(service).should(never()).checkAndUpdateState(eq(deviceId), any());
        then(clusterService).shouldHaveNoInteractions();
        then(notificationRuleProcessor).shouldHaveNoInteractions();
        then(telemetrySubscriptionService).shouldHaveNoInteractions();
    }

    @Test
    void givenDeviceBelongsToMyPartition_whenOnDeviceConnect_thenReportsConnect() {
        // GIVEN
        var deviceStateData = DeviceStateData.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .state(DeviceState.builder().build())
                .metaData(new TbMsgMetaData())
                .build();

        doReturn(false).when(service).cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId);

        service.deviceStates.put(deviceId, deviceStateData);
        long lastConnectTime = System.currentTimeMillis();

        mockSuccessfulSaveAttributes();

        // WHEN
        service.onDeviceConnect(tenantId, deviceId, lastConnectTime);

        // THEN
        then(telemetrySubscriptionService).should().saveAttributesInternal(argThat(request ->
                request.getTenantId().equals(tenantId) && request.getEntityId().equals(deviceId) &&
                        request.getScope().equals(AttributeScope.SERVER_SCOPE) &&
                        request.getEntries().get(0).getKey().equals(LAST_CONNECT_TIME) &&
                        request.getEntries().get(0).getValue().equals(lastConnectTime)
        ));

        var msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(clusterService).should().pushMsgToRuleEngine(eq(tenantId), eq(deviceId), msgCaptor.capture(), any());
        var actualMsg = msgCaptor.getValue();
        assertThat(actualMsg.getType()).isEqualTo(TbMsgType.CONNECT_EVENT.name());
        assertThat(actualMsg.getOriginator()).isEqualTo(deviceId);
    }

    @Test
    void givenDeviceBelongsToExternalPartition_whenOnDeviceDisconnect_thenCleansStateAndDoesNotReportDisconnect() {
        // GIVEN
        doReturn(true).when(service).cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId);

        // WHEN
        service.onDeviceDisconnect(tenantId, deviceId, System.currentTimeMillis());

        // THEN
        then(service).should().cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId);
        then(service).should(never()).getOrFetchDeviceStateData(deviceId);
        then(clusterService).shouldHaveNoInteractions();
        then(notificationRuleProcessor).shouldHaveNoInteractions();
        then(telemetrySubscriptionService).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @ValueSource(longs = {Long.MIN_VALUE, -100, -1})
    void givenNegativeLastDisconnectTime_whenOnDeviceDisconnect_thenSkipsThisEvent(long negativeLastDisconnectTime) {
        // GIVEN
        doReturn(false).when(service).cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId);

        // WHEN
        service.onDeviceDisconnect(tenantId, deviceId, negativeLastDisconnectTime);

        // THEN
        then(service).should(never()).getOrFetchDeviceStateData(deviceId);
        then(clusterService).shouldHaveNoInteractions();
        then(notificationRuleProcessor).shouldHaveNoInteractions();
        then(telemetrySubscriptionService).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @MethodSource("provideOutdatedTimestamps")
    void givenOutdatedLastDisconnectTime_whenOnDeviceDisconnect_thenSkipsThisEvent(long outdatedLastDisconnectTime, long currentLastDisconnectTime) {
        // GIVEN
        doReturn(false).when(service).cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId);

        var deviceStateData = DeviceStateData.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .state(DeviceState.builder().lastDisconnectTime(currentLastDisconnectTime).build())
                .build();
        service.deviceStates.put(deviceId, deviceStateData);

        // WHEN
        service.onDeviceDisconnect(tenantId, deviceId, outdatedLastDisconnectTime);

        // THEN
        then(clusterService).shouldHaveNoInteractions();
        then(notificationRuleProcessor).shouldHaveNoInteractions();
        then(telemetrySubscriptionService).shouldHaveNoInteractions();
    }

    @Test
    void givenDeviceBelongsToMyPartition_whenOnDeviceDisconnect_thenReportsDisconnect() {
        // GIVEN
        var deviceStateData = DeviceStateData.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .state(DeviceState.builder().build())
                .metaData(new TbMsgMetaData())
                .build();

        doReturn(false).when(service).cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId);

        service.deviceStates.put(deviceId, deviceStateData);
        long lastDisconnectTime = System.currentTimeMillis();

        mockSuccessfulSaveAttributes();

        // WHEN
        service.onDeviceDisconnect(tenantId, deviceId, lastDisconnectTime);

        // THEN
        then(telemetrySubscriptionService).should().saveAttributesInternal(argThat(request ->
                request.getTenantId().equals(tenantId) && request.getEntityId().equals(deviceId) &&
                        request.getScope().equals(AttributeScope.SERVER_SCOPE) &&
                        request.getEntries().get(0).getKey().equals(LAST_DISCONNECT_TIME) &&
                        request.getEntries().get(0).getValue().equals(lastDisconnectTime)
        ));

        var msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(clusterService).should().pushMsgToRuleEngine(eq(tenantId), eq(deviceId), msgCaptor.capture(), any());
        var actualMsg = msgCaptor.getValue();
        assertThat(actualMsg.getType()).isEqualTo(TbMsgType.DISCONNECT_EVENT.name());
        assertThat(actualMsg.getOriginator()).isEqualTo(deviceId);
    }

    @Test
    void givenDeviceBelongsToExternalPartition_whenOnDeviceInactivity_thenCleansStateAndDoesNotReportInactivity() {
        // GIVEN
        doReturn(true).when(service).cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId);

        // WHEN
        service.onDeviceInactivity(tenantId, deviceId, System.currentTimeMillis());

        // THEN
        then(service).should().cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId);
        then(service).should(never()).fetchDeviceStateDataUsingSeparateRequests(deviceId);
        then(clusterService).shouldHaveNoInteractions();
        then(notificationRuleProcessor).shouldHaveNoInteractions();
        then(telemetrySubscriptionService).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @ValueSource(longs = {Long.MIN_VALUE, -100, -1})
    void givenNegativeLastInactivityTime_whenOnDeviceInactivity_thenSkipsThisEvent(long negativeLastInactivityTime) {
        // GIVEN
        doReturn(false).when(service).cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId);

        // WHEN
        service.onDeviceInactivity(tenantId, deviceId, negativeLastInactivityTime);

        // THEN
        then(service).should(never()).getOrFetchDeviceStateData(deviceId);
        then(clusterService).shouldHaveNoInteractions();
        then(notificationRuleProcessor).shouldHaveNoInteractions();
        then(telemetrySubscriptionService).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @MethodSource("provideOutdatedTimestamps")
    void givenReceivedInactivityTimeIsLessThanOrEqualToCurrentInactivityTime_whenOnDeviceInactivity_thenSkipsThisEvent(
            long outdatedLastInactivityTime, long currentLastInactivityTime
    ) {
        // GIVEN
        doReturn(false).when(service).cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId);

        var deviceStateData = DeviceStateData.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .state(DeviceState.builder().lastInactivityAlarmTime(currentLastInactivityTime).build())
                .build();
        service.deviceStates.put(deviceId, deviceStateData);

        // WHEN
        service.onDeviceInactivity(tenantId, deviceId, outdatedLastInactivityTime);

        // THEN
        then(clusterService).shouldHaveNoInteractions();
        then(notificationRuleProcessor).shouldHaveNoInteractions();
        then(telemetrySubscriptionService).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @MethodSource("provideOutdatedTimestamps")
    void givenReceivedInactivityTimeIsLessThanOrEqualToCurrentActivityTime_whenOnDeviceInactivity_thenSkipsThisEvent(
            long outdatedLastInactivityTime, long currentLastActivityTime
    ) {
        // GIVEN
        doReturn(false).when(service).cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId);

        var deviceStateData = DeviceStateData.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .state(DeviceState.builder().lastActivityTime(currentLastActivityTime).build())
                .build();
        service.deviceStates.put(deviceId, deviceStateData);

        // WHEN
        service.onDeviceInactivity(tenantId, deviceId, outdatedLastInactivityTime);

        // THEN
        then(clusterService).shouldHaveNoInteractions();
        then(notificationRuleProcessor).shouldHaveNoInteractions();
        then(telemetrySubscriptionService).shouldHaveNoInteractions();
    }

    private static Stream<Arguments> provideOutdatedTimestamps() {
        return Stream.of(
                Arguments.of(0, 0),
                Arguments.of(0, 100),
                Arguments.of(50, 100),
                Arguments.of(99, 100),
                Arguments.of(100, 100)
        );
    }

    @Test
    void givenDeviceBelongsToMyPartition_whenOnDeviceInactivity_thenReportsInactivity() {
        // GIVEN
        var deviceStateData = DeviceStateData.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .state(DeviceState.builder().build())
                .metaData(new TbMsgMetaData())
                .build();

        doReturn(false).when(service).cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId);

        service.deviceStates.put(deviceId, deviceStateData);
        long lastInactivityTime = System.currentTimeMillis();

        mockSuccessfulSaveAttributes();

        // WHEN
        service.onDeviceInactivity(tenantId, deviceId, lastInactivityTime);

        // THEN
        then(telemetrySubscriptionService).should().saveAttributesInternal(argThat(request ->
                request.getTenantId().equals(tenantId) && request.getEntityId().equals(deviceId) &&
                        request.getScope().equals(AttributeScope.SERVER_SCOPE) &&
                        request.getEntries().get(0).getKey().equals(INACTIVITY_ALARM_TIME) &&
                        request.getEntries().get(0).getValue().equals(lastInactivityTime)
        ));
        then(telemetrySubscriptionService).should().saveAttributesInternal(argThat(request ->
                request.getTenantId().equals(tenantId) && request.getEntityId().equals(deviceId) &&
                        request.getScope().equals(AttributeScope.SERVER_SCOPE) &&
                        request.getEntries().get(0).getKey().equals(ACTIVITY_STATE) &&
                        request.getEntries().get(0).getValue().equals(false)
        ));

        var msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(clusterService).should()
                .pushMsgToRuleEngine(eq(tenantId), eq(deviceId), msgCaptor.capture(), any());
        var actualMsg = msgCaptor.getValue();
        assertThat(actualMsg.getType()).isEqualTo(TbMsgType.INACTIVITY_EVENT.name());
        assertThat(actualMsg.getOriginator()).isEqualTo(deviceId);

        var notificationCaptor = ArgumentCaptor.forClass(DeviceActivityTrigger.class);
        then(notificationRuleProcessor).should().process(notificationCaptor.capture());
        var actualNotification = notificationCaptor.getValue();
        assertThat(actualNotification.getTenantId()).isEqualTo(tenantId);
        assertThat(actualNotification.getDeviceId()).isEqualTo(deviceId);
        assertThat(actualNotification.isActive()).isFalse();
    }

    @Test
    void givenInactivityTimeoutReached_whenUpdateInactivityStateIfExpired_thenReportsInactivity() {
        // GIVEN
        var deviceStateData = DeviceStateData.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .state(DeviceState.builder().build())
                .metaData(new TbMsgMetaData())
                .build();

        given(partitionService.resolve(ServiceType.TB_CORE, tenantId, deviceId)).willReturn(tpi);

        mockSuccessfulSaveAttributes();

        // WHEN
        service.updateInactivityStateIfExpired(System.currentTimeMillis(), deviceId, deviceStateData);

        // THEN
        then(telemetrySubscriptionService).should().saveAttributesInternal(argThat(request ->
                request.getTenantId().equals(tenantId) && request.getEntityId().equals(deviceId) &&
                        request.getScope().equals(AttributeScope.SERVER_SCOPE) &&
                        request.getEntries().get(0).getKey().equals(INACTIVITY_ALARM_TIME)
        ));
        then(telemetrySubscriptionService).should().saveAttributesInternal(argThat(request ->
                request.getTenantId().equals(tenantId) && request.getEntityId().equals(deviceId) &&
                        request.getScope().equals(AttributeScope.SERVER_SCOPE) &&
                        request.getEntries().get(0).getKey().equals(ACTIVITY_STATE) &&
                        request.getEntries().get(0).getValue().equals(false)
        ));

        var msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(clusterService).should()
                .pushMsgToRuleEngine(eq(tenantId), eq(deviceId), msgCaptor.capture(), any());
        var actualMsg = msgCaptor.getValue();
        assertThat(actualMsg.getType()).isEqualTo(TbMsgType.INACTIVITY_EVENT.name());
        assertThat(actualMsg.getOriginator()).isEqualTo(deviceId);

        var notificationCaptor = ArgumentCaptor.forClass(DeviceActivityTrigger.class);
        then(notificationRuleProcessor).should().process(notificationCaptor.capture());
        var actualNotification = notificationCaptor.getValue();
        assertThat(actualNotification.getTenantId()).isEqualTo(tenantId);
        assertThat(actualNotification.getDeviceId()).isEqualTo(deviceId);
        assertThat(actualNotification.isActive()).isFalse();
    }

    @Test
    void givenDeviceIdFromDeviceStatesMap_whenGetOrFetchDeviceStateData_thenNoStackOverflow() {
        service.deviceStates.put(deviceId, deviceStateDataMock);
        DeviceStateData deviceStateData = service.getOrFetchDeviceStateData(deviceId);
        assertThat(deviceStateData).isEqualTo(deviceStateDataMock);
        verify(service, never()).fetchDeviceStateDataUsingSeparateRequests(deviceId);
    }

    @Test
    void givenDeviceIdWithoutDeviceStateInMap_whenGetOrFetchDeviceStateData_thenFetchDeviceStateData() {
        service.deviceStates.clear();
        willReturn(deviceStateDataMock).given(service).fetchDeviceStateDataUsingSeparateRequests(deviceId);
        DeviceStateData deviceStateData = service.getOrFetchDeviceStateData(deviceId);
        assertThat(deviceStateData).isEqualTo(deviceStateDataMock);
        verify(service).fetchDeviceStateDataUsingSeparateRequests(deviceId);
    }

    @MethodSource
    @ParameterizedTest
    void testOnDeviceInactivityTimeoutUpdate(boolean initialActivityStatus, long newInactivityTimeout, boolean expectedActivityStatus) {
        // GIVEN
        doReturn(200L).when(service).getCurrentTimeMillis();

        var deviceState = DeviceState.builder()
                .active(initialActivityStatus)
                .lastActivityTime(100L)
                .build();

        var deviceStateData = DeviceStateData.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .state(deviceState)
                .metaData(new TbMsgMetaData())
                .build();

        service.deviceStates.put(deviceId, deviceStateData);
        service.getPartitionedEntities(tpi).add(deviceId);

        mockSuccessfulSaveAttributes();

        // WHEN
        service.onDeviceInactivityTimeoutUpdate(tenantId, deviceId, newInactivityTimeout);

        // THEN
        long expectedInactivityTimeout = newInactivityTimeout != 0 ? newInactivityTimeout : defaultInactivityTimeoutMs;
        assertThat(deviceState.getInactivityTimeout()).isEqualTo(expectedInactivityTimeout);

        assertThat(deviceState.isActive()).isEqualTo(expectedActivityStatus);
        if (initialActivityStatus != expectedActivityStatus) {
            then(telemetrySubscriptionService).should().saveAttributesInternal(argThat(request -> {
                AttributeKvEntry entry = request.getEntries().get(0);
                return request.getEntityId().equals(deviceId) && entry.getKey().equals(ACTIVITY_STATE) && entry.getValue().equals(expectedActivityStatus);
            }));
        }
    }

    // to simplify test, these arguments assume that the current time is 200 and the last activity time is 100
    private static Stream<Arguments> testOnDeviceInactivityTimeoutUpdate() {
        return Stream.of(
                Arguments.of(true, 1L, false),
                Arguments.of(true, 50L, false),
                Arguments.of(true, 99L, false),
                Arguments.of(true, 100L, false),
                Arguments.of(true, 101L, true),
                Arguments.of(true, 0L, true), // should use default inactivity timeout of 10 minutes
                Arguments.of(false, 1L, false),
                Arguments.of(false, 50L, false),
                Arguments.of(false, 99L, false),
                Arguments.of(false, 100L, false),
                Arguments.of(false, 101L, true),
                Arguments.of(false, 0L, true) // should use default inactivity timeout of 10 minutes
        );
    }

    @Test
    void givenStateDataIsNull_whenUpdateActivityState_thenShouldCleanupDevice() {
        // GIVEN
        service.deviceStates.put(deviceId, deviceStateDataMock);

        // WHEN
        service.updateActivityState(deviceId, null, System.currentTimeMillis());

        // THEN
        assertThat(service.deviceStates.get(deviceId)).isNull();
        assertThat(service.deviceStates.size()).isEqualTo(0);
        assertThat(service.deviceStates.isEmpty()).isTrue();
    }


    @ParameterizedTest
    @MethodSource("provideParametersForUpdateActivityState")
    void givenTestParameters_whenUpdateActivityState_thenShouldBeInTheExpectedStateAndPerformExpectedActions(
            boolean activityState, long previousActivityTime, long lastReportedActivity, long inactivityAlarmTime,
            long expectedInactivityAlarmTime, boolean shouldSetInactivityAlarmTimeToZero,
            boolean shouldUpdateActivityStateToActive
    ) {
        // GIVEN
        DeviceState deviceState = DeviceState.builder()
                .active(activityState)
                .lastActivityTime(previousActivityTime)
                .lastInactivityAlarmTime(inactivityAlarmTime)
                .inactivityTimeout(10000)
                .build();

        DeviceStateData deviceStateData = DeviceStateData.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .state(deviceState)
                .metaData(new TbMsgMetaData())
                .build();

        mockSuccessfulSaveAttributes();

        // WHEN
        service.updateActivityState(deviceId, deviceStateData, lastReportedActivity);

        // THEN
        assertThat(deviceState.isActive()).isEqualTo(true);
        assertThat(deviceState.getLastActivityTime()).isEqualTo(lastReportedActivity);
        then(telemetrySubscriptionService).should().saveAttributesInternal(argThat(request ->
                request.getEntityId().equals(deviceId) &&
                        request.getEntries().get(0).getKey().equals(LAST_ACTIVITY_TIME) &&
                        request.getEntries().get(0).getValue().equals(lastReportedActivity)
        ));

        assertThat(deviceState.getLastInactivityAlarmTime()).isEqualTo(expectedInactivityAlarmTime);
        if (shouldSetInactivityAlarmTimeToZero) {
            then(telemetrySubscriptionService).should().saveAttributesInternal(argThat(request ->
                    request.getEntityId().equals(deviceId) &&
                            request.getEntries().get(0).getKey().equals(INACTIVITY_ALARM_TIME) &&
                            request.getEntries().get(0).getValue().equals(0L)
            ));
        }

        if (shouldUpdateActivityStateToActive) {
            then(telemetrySubscriptionService).should().saveAttributesInternal(argThat(request ->
                    request.getEntityId().equals(deviceId) &&
                            request.getEntries().get(0).getKey().equals(ACTIVITY_STATE) &&
                            request.getEntries().get(0).getValue().equals(true)
            ));

            var msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
            then(clusterService).should().pushMsgToRuleEngine(eq(tenantId), eq(deviceId), msgCaptor.capture(), any());
            var actualMsg = msgCaptor.getValue();
            assertThat(actualMsg.getType()).isEqualTo(TbMsgType.ACTIVITY_EVENT.name());
            assertThat(actualMsg.getOriginator()).isEqualTo(deviceId);

            var notificationCaptor = ArgumentCaptor.forClass(DeviceActivityTrigger.class);
            then(notificationRuleProcessor).should().process(notificationCaptor.capture());
            var actualNotification = notificationCaptor.getValue();
            assertThat(actualNotification.getTenantId()).isEqualTo(tenantId);
            assertThat(actualNotification.getDeviceId()).isEqualTo(deviceId);
            assertThat(actualNotification.isActive()).isTrue();
        }
    }

    private static Stream<Arguments> provideParametersForUpdateActivityState() {
        return Stream.of(
                Arguments.of(true, 100, 120, 80, 80, false, false),

                Arguments.of(true, 100, 120, 100, 100, false, false),

                Arguments.of(false, 100, 120, 110, 110, false, true),


                Arguments.of(true, 100, 100, 80, 80, false, false),

                Arguments.of(true, 100, 100, 100, 100, false, false),

                Arguments.of(false, 100, 100, 110, 0, true, true),


                Arguments.of(false, 100, 110, 110, 0, true, true),

                Arguments.of(false, 100, 110, 120, 0, true, true),


                Arguments.of(true, 0, 0, 0, 0, false, false),

                Arguments.of(false, 0, 0, 0, 0, true, true)
        );
    }

    @Test
    void givenStateDataIsNull_whenUpdateInactivityTimeoutIfExpired_thenShouldCleanupDevice() {
        // GIVEN
        service.deviceStates.put(deviceId, deviceStateDataMock);

        // WHEN
        service.updateInactivityStateIfExpired(System.currentTimeMillis(), deviceId, null);

        // THEN
        assertThat(service.deviceStates.get(deviceId)).isNull();
        assertThat(service.deviceStates.size()).isEqualTo(0);
        assertThat(service.deviceStates.isEmpty()).isTrue();
    }

    @Test
    void givenNotMyPartition_whenUpdateInactivityTimeoutIfExpired_thenShouldCleanupDevice() {
        // GIVEN
        long currentTime = System.currentTimeMillis();

        DeviceState deviceState = DeviceState.builder()
                .active(true)
                .lastConnectTime(currentTime - 8000)
                .lastActivityTime(currentTime - 4000)
                .lastDisconnectTime(0)
                .lastInactivityAlarmTime(0)
                .inactivityTimeout(3000)
                .build();

        DeviceStateData stateData = DeviceStateData.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .deviceCreationTime(currentTime - 10000)
                .state(deviceState)
                .build();

        service.deviceStates.put(deviceId, stateData);

        var notMyTpi = TopicPartitionInfo.builder().myPartition(false).build();
        given(partitionService.resolve(ServiceType.TB_CORE, tenantId, deviceId)).willReturn(notMyTpi);

        // WHEN
        service.updateInactivityStateIfExpired(System.currentTimeMillis(), deviceId, stateData);

        // THEN
        assertThat(service.deviceStates.get(deviceId)).isNull();
        assertThat(service.deviceStates.size()).isEqualTo(0);
        assertThat(service.deviceStates.isEmpty()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("provideParametersForUpdateInactivityStateIfExpired")
    void givenTestParameters_whenUpdateInactivityStateIfExpired_thenShouldBeInTheExpectedStateAndPerformExpectedActions(
            boolean activityState, long ts, long lastActivityTime, long lastInactivityAlarmTime, long inactivityTimeout, long deviceCreationTime,
            boolean expectedActivityState, long expectedLastInactivityAlarmTime, boolean shouldUpdateActivityStateToInactive
    ) {
        // GIVEN
        var state = DeviceState.builder()
                .active(activityState)
                .lastActivityTime(lastActivityTime)
                .lastInactivityAlarmTime(lastInactivityAlarmTime)
                .inactivityTimeout(inactivityTimeout)
                .build();

        var deviceStateData = DeviceStateData.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .deviceCreationTime(deviceCreationTime)
                .metaData(new TbMsgMetaData())
                .state(state)
                .build();

        if (shouldUpdateActivityStateToInactive) {
            given(partitionService.resolve(ServiceType.TB_CORE, tenantId, deviceId)).willReturn(tpi);
            mockSuccessfulSaveAttributes();
        }

        // WHEN
        service.updateInactivityStateIfExpired(ts, deviceId, deviceStateData);

        // THEN
        assertThat(state.isActive()).isEqualTo(expectedActivityState);
        assertThat(state.getLastInactivityAlarmTime()).isEqualTo(expectedLastInactivityAlarmTime);

        if (shouldUpdateActivityStateToInactive) {
            then(telemetrySubscriptionService).should().saveAttributesInternal(argThat(request ->
                    request.getEntityId().equals(deviceId) && request.getEntries().get(0).getKey().equals(ACTIVITY_STATE) &&
                            request.getEntries().get(0).getValue().equals(false)
            ));

            var msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
            then(clusterService).should().pushMsgToRuleEngine(eq(tenantId), eq(deviceId), msgCaptor.capture(), any());
            var actualMsg = msgCaptor.getValue();
            assertThat(actualMsg.getType()).isEqualTo(TbMsgType.INACTIVITY_EVENT.name());
            assertThat(actualMsg.getOriginator()).isEqualTo(deviceId);

            var notificationCaptor = ArgumentCaptor.forClass(DeviceActivityTrigger.class);
            then(notificationRuleProcessor).should().process(notificationCaptor.capture());
            var actualNotification = notificationCaptor.getValue();
            assertThat(actualNotification.getTenantId()).isEqualTo(tenantId);
            assertThat(actualNotification.getDeviceId()).isEqualTo(deviceId);
            assertThat(actualNotification.isActive()).isFalse();

            then(telemetrySubscriptionService).should().saveAttributesInternal(argThat(request ->
                    request.getTenantId().equals(tenantId) && request.getEntityId().equals(deviceId) &&
                            request.getScope().equals(AttributeScope.SERVER_SCOPE) &&
                            request.getEntries().get(0).getKey().equals(INACTIVITY_ALARM_TIME) &&
                            request.getEntries().get(0).getValue().equals(expectedLastInactivityAlarmTime)
            ));
        }
    }

    private static Stream<Arguments> provideParametersForUpdateInactivityStateIfExpired() {
        return Stream.of(
                Arguments.of(false, 100, 70, 90, 70, 60, false, 90, false),

                Arguments.of(false, 100, 40, 50, 70, 10, false, 50, false),

                Arguments.of(false, 100, 25, 60, 75, 25, false, 60, false),

                Arguments.of(false, 100, 60, 70, 10, 50, false, 70, false),

                Arguments.of(false, 100, 10, 15, 90, 10, false, 15, false),

                Arguments.of(false, 100, 0, 40, 75, 0, false, 40, false),

                Arguments.of(true, 100, 90, 80, 80, 50, true, 80, false),

                Arguments.of(true, 100, 95, 90, 10, 50, true, 90, false),

                Arguments.of(true, 100, 10, 10, 90, 10, false, 100, true),

                Arguments.of(true, 100, 10, 10, 90, 11, true, 10, false),

                Arguments.of(true, 100, 15, 10, 85, 5, false, 100, true),

                Arguments.of(true, 100, 15, 10, 75, 5, false, 100, true),

                Arguments.of(true, 100, 95, 90, 5, 50, false, 100, true),

                Arguments.of(true, 100, 0, 0, 101, 0, true, 0, false),

                Arguments.of(true, 100, 0, 0, 100, 0, false, 100, true),

                Arguments.of(true, 100, 0, 0, 99, 0, false, 100, true),

                Arguments.of(true, 100, 0, 0, 120, 10, true, 0, false),

                Arguments.of(true, 100, 50, 0, 100, 0, true, 0, false),

                Arguments.of(true, 100, 10, 0, 91, 0, true, 0, false),

                Arguments.of(true, 100, 90, 0, 10, 0, false, 100, true),

                Arguments.of(true, 100, 100, 100, 1, 0, true, 100, false),

                Arguments.of(true, 100, 100, 100, 100, 100, true, 100, false),

                Arguments.of(false, 100, 59, 60, 30, 10, false, 60, false),

                Arguments.of(true, 100, 60, 60, 30, 10, false, 100, true),

                Arguments.of(true, 100, 61, 60, 30, 10, false, 100, true),

                Arguments.of(true, 0, 0, 0, 1, 0, true, 0, false),

                Arguments.of(true, 0, 0, 0, 0, 0, false, 0, true),

                Arguments.of(true, 100, 90, 80, 20, 70, true, 80, false),

                Arguments.of(true, 100, 80, 90, 30, 70, true, 90, false)
        );
    }

    @Test
    void givenInactiveDevice_whenActivityStatusChangesToActiveButFailedToSaveUpdatedActivityStatus_thenShouldNotUpdateCache2() {
        // GIVEN
        var deviceState = DeviceState.builder()
                .active(false)
                .lastActivityTime(100L)
                .inactivityTimeout(50L)
                .build();

        var deviceStateData = DeviceStateData.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .state(deviceState)
                .metaData(TbMsgMetaData.EMPTY)
                .build();

        service.deviceStates.put(deviceId, deviceStateData);
        service.getPartitionedEntities(tpi).add(deviceId);

        // WHEN-THEN

        // simulating short DB outage
        given(telemetrySubscriptionService.saveAttributesInternal(any())).willReturn(Futures.immediateFailedFuture(new RuntimeException("failed to save")));
        doReturn(200L).when(service).getCurrentTimeMillis();
        service.onDeviceActivity(tenantId, deviceId, 180L);
        assertThat(deviceState.isActive()).isFalse(); // still inactive

        // 10 millis pass... and new activity message it received

        // this time DB save is successful
        when(telemetrySubscriptionService.saveAttributesInternal(any())).thenReturn(Futures.immediateFuture(AttributesSaveResult.of(generateRandomVersions(1))));
        doReturn(210L).when(service).getCurrentTimeMillis();
        service.onDeviceActivity(tenantId, deviceId, 190L);
        assertThat(deviceState.isActive()).isTrue();
    }

    @Test
    void givenActiveDevice_whenActivityStatusChangesToInactiveButFailedToSaveUpdatedActivityStatus_thenShouldNotUpdateCache() {
        // GIVEN
        var deviceState = DeviceState.builder()
                .active(true)
                .lastActivityTime(100L)
                .inactivityTimeout(50L)
                .build();

        var deviceStateData = DeviceStateData.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .state(deviceState)
                .metaData(TbMsgMetaData.EMPTY)
                .build();

        service.deviceStates.put(deviceId, deviceStateData);
        service.getPartitionedEntities(tpi).add(deviceId);

        // WHEN-THEN (assuming periodic activity states check is done every 100 millis)

        // simulating short DB outage
        given(telemetrySubscriptionService.saveAttributesInternal(any())).willReturn(Futures.immediateFailedFuture(new RuntimeException("failed to save")));
        doReturn(200L).when(service).getCurrentTimeMillis();
        service.checkStates();
        assertThat(deviceState.isActive()).isTrue(); // still active

        // waiting 100 millis... periodic activity states check is triggered again

        // this time DB save is successful
        when(telemetrySubscriptionService.saveAttributesInternal(any())).thenReturn(Futures.immediateFuture(AttributesSaveResult.of(generateRandomVersions(1))));
        doReturn(300L).when(service).getCurrentTimeMillis();
        service.checkStates();
        assertThat(deviceState.isActive()).isFalse();
    }

    @Test
    void givenConcurrentAccess_whenGetOrFetchDeviceStateData_thenFetchDeviceStateDataInvokedOnce() {
        doAnswer(invocation -> {
            Thread.sleep(100);
            return deviceStateDataMock;
        }).when(service).fetchDeviceStateDataUsingSeparateRequests(deviceId);

        int numberOfThreads = 10;
        var allThreadsReadyLatch = new CountDownLatch(numberOfThreads);

        ExecutorService executor = null;
        try {
            executor = Executors.newFixedThreadPool(numberOfThreads);
            for (int i = 0; i < numberOfThreads; i++) {
                executor.submit(() -> {
                    allThreadsReadyLatch.countDown();
                    try {
                        allThreadsReadyLatch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    service.getOrFetchDeviceStateData(deviceId);
                });
            }

            executor.shutdown();
            await().atMost(10, TimeUnit.SECONDS).until(executor::isTerminated);
        } finally {
            if (executor != null) {
                executor.shutdownNow();
            }
        }

        then(service).should().fetchDeviceStateDataUsingSeparateRequests(deviceId);
    }

    @Test
    void givenDeviceAdded_whenOnQueueMsg_thenShouldCacheAndSaveActivityToFalse() {
        // GIVEN
        given(deviceService.findDeviceById(any(TenantId.class), any(DeviceId.class))).willReturn(new Device(deviceId));
        given(attributesService.find(any(TenantId.class), any(EntityId.class), any(AttributeScope.class), anyCollection())).willReturn(Futures.immediateFuture(Collections.emptyList()));

        TransportProtos.DeviceStateServiceMsgProto proto = TransportProtos.DeviceStateServiceMsgProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setAdded(true)
                .setUpdated(false)
                .setDeleted(false)
                .build();

        mockSuccessfulSaveAttributes();

        // WHEN
        service.onQueueMsg(proto, TbCallback.EMPTY);

        // THEN
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(service.deviceStates.get(deviceId).getState().isActive()).isEqualTo(false);
            then(telemetrySubscriptionService).should().saveAttributesInternal(argThat(request ->
                    request.getEntityId().equals(deviceId) && request.getEntries().get(0).getKey().equals(ACTIVITY_STATE) &&
                            request.getEntries().get(0).getValue().equals(false)
            ));
        });
    }

    @Test
    void givenDeviceActivityEventHappenedAfterAdded_whenOnDeviceActivity_thenShouldCacheAndSaveActivityToTrue() {
        // GIVEN
        long currentTime = System.currentTimeMillis();
        DeviceState deviceState = DeviceState.builder()
                .active(false)
                .inactivityTimeout(defaultInactivityTimeoutMs)
                .build();
        DeviceStateData stateData = DeviceStateData.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .deviceCreationTime(currentTime - 10000)
                .state(deviceState)
                .metaData(TbMsgMetaData.EMPTY)
                .build();
        service.deviceStates.put(deviceId, stateData);

        mockSuccessfulSaveAttributes();

        // WHEN
        service.onDeviceActivity(tenantId, deviceId, currentTime);

        // THEN
        ArgumentCaptor<AttributesSaveRequest> attributeRequestCaptor = ArgumentCaptor.forClass(AttributesSaveRequest.class);
        then(telemetrySubscriptionService).should(times(2)).saveAttributesInternal(attributeRequestCaptor.capture());

        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(service.deviceStates.get(deviceId).getState().isActive()).isEqualTo(true);

            assertThat(attributeRequestCaptor.getAllValues()).hasSize(2)
                    .anySatisfy(request -> {
                        assertThat(request.getTenantId()).isEqualTo(tenantId);
                        assertThat(request.getEntityId()).isEqualTo(deviceId);
                        assertThat(request.getScope()).isEqualTo(AttributeScope.SERVER_SCOPE);
                        assertThat(request.getEntries()).singleElement().satisfies(attributeKvEntry -> {
                            assertThat(attributeKvEntry.getKey()).isEqualTo(LAST_ACTIVITY_TIME);
                            assertThat(attributeKvEntry.getLongValue()).hasValue(currentTime);
                        });
                    })
                    .anySatisfy(request -> {
                        assertThat(request.getTenantId()).isEqualTo(tenantId);
                        assertThat(request.getEntityId()).isEqualTo(deviceId);
                        assertThat(request.getScope()).isEqualTo(AttributeScope.SERVER_SCOPE);
                        assertThat(request.getEntries()).singleElement().satisfies(attributeKvEntry -> {
                            assertThat(attributeKvEntry.getKey()).isEqualTo(ACTIVITY_STATE);
                            assertThat(attributeKvEntry.getBooleanValue()).hasValue(true);
                        });
                    });
        });
    }

    @Test
    void givenDeviceActivityEventHappenedBeforeAdded_whenOnQueueMsg_thenShouldSaveActivityStateUsingValueFromCache() {
        // GIVEN
        given(deviceService.findDeviceById(any(TenantId.class), any(DeviceId.class))).willReturn(new Device(deviceId));
        given(attributesService.find(any(TenantId.class), any(EntityId.class), any(AttributeScope.class), anyCollection())).willReturn(Futures.immediateFuture(Collections.emptyList()));

        long currentTime = System.currentTimeMillis();

        var deviceState = DeviceState.builder()
                .active(true)
                .lastConnectTime(currentTime - 8000)
                .lastActivityTime(currentTime - 4000)
                .lastDisconnectTime(0)
                .lastInactivityAlarmTime(0)
                .inactivityTimeout(3000)
                .build();

        var stateData = DeviceStateData.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .deviceCreationTime(currentTime - 10000)
                .state(deviceState)
                .build();

        service.deviceStates.put(deviceId, stateData);

        mockSuccessfulSaveAttributes();

        // WHEN
        var proto = TransportProtos.DeviceStateServiceMsgProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setAdded(true)
                .setUpdated(false)
                .setDeleted(false)
                .build();
        service.onQueueMsg(proto, TbCallback.EMPTY);

        // THEN
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(service.deviceStates.get(deviceId).getState().isActive()).isEqualTo(true);
            then(telemetrySubscriptionService).should().saveAttributesInternal(argThat(request ->
                    request.getEntityId().equals(deviceId) && request.getEntries().get(0).getKey().equals(ACTIVITY_STATE) &&
                            request.getEntries().get(0).getValue().equals(true)
            ));
        });
    }

    private void mockSuccessfulSaveAttributes() {
        lenient().when(telemetrySubscriptionService.saveAttributesInternal(any())).thenAnswer(invocation -> {
            AttributesSaveRequest request = invocation.getArgument(0);
            return Futures.immediateFuture(generateRandomVersions(request.getEntries().size()));
        });
    }

    private static List<Long> generateRandomVersions(int n) {
        return ThreadLocalRandom.current()
                .longs(n)
                .boxed()
                .toList();
    }

}
