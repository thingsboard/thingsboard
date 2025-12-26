/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.telemetry;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.rule.engine.api.AttributesDeleteRequest;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.rule.engine.api.DeviceStateManager;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.id.ApiUsageStateId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.AttributesSaveResult;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TimeseriesSaveResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.objects.AttributesEntityView;
import org.thingsboard.server.common.data.objects.TelemetryEntityView;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.msg.rule.engine.DeviceAttributesEventNotificationMsg;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.cf.CalculatedFieldQueueService;
import org.thingsboard.server.service.entitiy.entityview.TbEntityViewService;
import org.thingsboard.server.service.subscription.SubscriptionManagerService;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static com.google.common.util.concurrent.Futures.immediateFailedFuture;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class DefaultTelemetrySubscriptionServiceTest {

    final TenantId tenantId = TenantId.fromUUID(UUID.fromString("a00ec470-c6b4-11ef-8c88-63b5533fb5bc"));
    final CustomerId customerId = new CustomerId(UUID.fromString("7bdc9750-c775-11ef-8e03-ff69ed8da327"));
    final EntityId entityId = DeviceId.fromString("cc51e450-53e1-11ee-883e-e56b48fd2088");

    final long sampleTtl = 10_000L;

    final List<TsKvEntry> sampleTimeseries = List.of(
            new BasicTsKvEntry(100L, new DoubleDataEntry("temperature", 65.2)),
            new BasicTsKvEntry(100L, new DoubleDataEntry("humidity", 33.1))
    );

    ApiUsageState apiUsageState;

    final TopicPartitionInfo tpi = TopicPartitionInfo.builder()
            .tenantId(tenantId)
            .myPartition(true)
            .build();

    ExecutorService wsCallBackExecutor;
    ExecutorService tsCallBackExecutor;

    @Mock
    TbClusterService clusterService;
    @Mock
    PartitionService partitionService;
    @Mock
    SubscriptionManagerService subscriptionManagerService;
    @Mock
    AttributesService attrService;
    @Mock
    TimeseriesService tsService;
    @Mock
    TbEntityViewService tbEntityViewService;
    @Mock
    TbApiUsageReportClient apiUsageClient;
    @Mock
    TbApiUsageStateService apiUsageStateService;
    @Mock
    CalculatedFieldQueueService calculatedFieldQueueService;
    @Mock
    DeviceStateManager deviceStateManager;

    DefaultTelemetrySubscriptionService telemetryService;

    @BeforeEach
    void setup() {
        telemetryService = new DefaultTelemetrySubscriptionService(attrService, tsService, tbEntityViewService, apiUsageClient, apiUsageStateService, calculatedFieldQueueService, deviceStateManager);
        ReflectionTestUtils.setField(telemetryService, "clusterService", clusterService);
        ReflectionTestUtils.setField(telemetryService, "partitionService", partitionService);
        ReflectionTestUtils.setField(telemetryService, "subscriptionManagerService", Optional.of(subscriptionManagerService));

        wsCallBackExecutor = MoreExecutors.newDirectExecutorService();
        ReflectionTestUtils.setField(telemetryService, "wsCallBackExecutor", wsCallBackExecutor);

        tsCallBackExecutor = MoreExecutors.newDirectExecutorService();
        ReflectionTestUtils.setField(telemetryService, "tsCallBackExecutor", tsCallBackExecutor);

        apiUsageState = new ApiUsageState();
        apiUsageState.setDbStorageState(ApiUsageStateValue.ENABLED);
        lenient().when(apiUsageStateService.getApiUsageState(tenantId)).thenReturn(apiUsageState);

        lenient().when(partitionService.resolve(eq(ServiceType.TB_CORE), eq(tenantId), any())).thenReturn(tpi);

        lenient().when(tsService.save(tenantId, entityId, sampleTimeseries, sampleTtl)).thenReturn(immediateFuture(TimeseriesSaveResult.of(sampleTimeseries.size(), listOfNNumbers(sampleTimeseries.size()))));
        lenient().when(tsService.saveWithoutLatest(tenantId, entityId, sampleTimeseries, sampleTtl)).thenReturn(immediateFuture(TimeseriesSaveResult.of(sampleTimeseries.size(), null)));
        lenient().when(tsService.saveLatest(tenantId, entityId, sampleTimeseries)).thenReturn(immediateFuture(TimeseriesSaveResult.of(sampleTimeseries.size(), listOfNNumbers(sampleTimeseries.size()))));

        // mock no entity views
        lenient().when(tbEntityViewService.findEntityViewsByTenantIdAndEntityIdAsync(tenantId, entityId)).thenReturn(immediateFuture(Collections.emptyList()));

        // mock that calls to CF queue service are always successful
        lenient().doAnswer(inv -> {
            FutureCallback<Void> callback = inv.getArgument(2);
            callback.onSuccess(null);
            return null;
        }).when(calculatedFieldQueueService).pushRequestToQueue(any(TimeseriesSaveRequest.class), any(), any());

        // send partition change event so currentPartitions set is populated
        telemetryService.onTbApplicationEvent(new PartitionChangeEvent(this, ServiceType.TB_CORE, Map.of(new QueueKey(ServiceType.TB_CORE), Set.of(tpi)), Collections.emptyMap()));
    }

    @AfterEach
    void cleanup() {
        wsCallBackExecutor.shutdownNow();
        tsCallBackExecutor.shutdownNow();
    }

    /* --- Save time series API --- */

    @Test
    void shouldThrowErrorWhenTryingToSaveTimeseriesForApiUsageState() {
        // GIVEN
        var request = TimeseriesSaveRequest.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .entityId(new ApiUsageStateId(UUID.randomUUID()))
                .entries(sampleTimeseries)
                .strategy(TimeseriesSaveRequest.Strategy.PROCESS_ALL)
                .build();

        // WHEN
        assertThatThrownBy(() -> telemetryService.saveTimeseries(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Can't update API Usage State!");

        // THEN
        then(tsService).shouldHaveNoInteractions();
    }

    @Test
    void shouldReportStorageDataPointsApiUsageWhenTimeSeriesIsSaved() {
        // GIVEN
        var request = TimeseriesSaveRequest.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .entityId(entityId)
                .entries(sampleTimeseries)
                .ttl(sampleTtl)
                .strategy(new TimeseriesSaveRequest.Strategy(true, false, false, false))
                .build();

        // WHEN
        telemetryService.saveTimeseries(request);

        // THEN
        then(apiUsageClient).should().report(tenantId, customerId, ApiUsageRecordKey.STORAGE_DP_COUNT, sampleTimeseries.size());
    }

    @Test
    void shouldNotReportStorageDataPointsApiUsageWhenTimeSeriesIsNotSaved() {
        // GIVEN
        var request = TimeseriesSaveRequest.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .entityId(entityId)
                .entries(sampleTimeseries)
                .ttl(sampleTtl)
                .strategy(TimeseriesSaveRequest.Strategy.LATEST_AND_WS)
                .build();

        // WHEN
        telemetryService.saveTimeseries(request);

        // THEN
        then(apiUsageClient).shouldHaveNoInteractions();
    }

    @Test
    void shouldThrowStorageDisabledWhenTimeSeriesIsSavedAndStorageIsDisabled() {
        // GIVEN
        apiUsageState.setDbStorageState(ApiUsageStateValue.DISABLED);

        SettableFuture<Void> future = SettableFuture.create();
        var request = TimeseriesSaveRequest.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .entityId(entityId)
                .entries(sampleTimeseries)
                .ttl(sampleTtl)
                .strategy(TimeseriesSaveRequest.Strategy.PROCESS_ALL)
                .future(future)
                .build();

        // WHEN
        telemetryService.saveTimeseries(request);

        // THEN
        assertThat(future).failsWithin(Duration.ofSeconds(5))
                .withThrowableOfType(ExecutionException.class)
                .withCauseInstanceOf(RuntimeException.class)
                .withMessageContaining("DB storage writes are disabled due to API limits!");
    }

    @Test
    void shouldNotThrowStorageDisabledWhenTimeSeriesIsNotSavedAndStorageIsDisabled() {
        // GIVEN
        apiUsageState.setDbStorageState(ApiUsageStateValue.DISABLED);

        SettableFuture<Void> future = SettableFuture.create();
        var request = TimeseriesSaveRequest.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .entityId(entityId)
                .entries(sampleTimeseries)
                .ttl(sampleTtl)
                .strategy(TimeseriesSaveRequest.Strategy.LATEST_AND_WS)
                .future(future)
                .build();

        // WHEN
        telemetryService.saveTimeseries(request);

        // THEN
        assertThat(future).succeedsWithin(Duration.ofSeconds(5));
    }

    @Test
    void shouldCopyLatestToEntityViewWhenLatestIsSavedOnMainEntity() {
        // GIVEN
        var entityView = new EntityView(new EntityViewId(UUID.randomUUID()));
        entityView.setTenantId(tenantId);
        entityView.setCustomerId(customerId);
        entityView.setEntityId(entityId);
        entityView.setKeys(new TelemetryEntityView(sampleTimeseries.stream().map(KvEntry::getKey).toList(), new AttributesEntityView()));

        // mock that there is one entity view
        given(tbEntityViewService.findEntityViewsByTenantIdAndEntityIdAsync(tenantId, entityId)).willReturn(immediateFuture(List.of(entityView)));
        // mock that save latest call for entity view is successful
        given(tsService.saveLatest(tenantId, entityView.getId(), sampleTimeseries)).willReturn(immediateFuture(TimeseriesSaveResult.of(sampleTimeseries.size(), listOfNNumbers(sampleTimeseries.size()))));

        var request = TimeseriesSaveRequest.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .entityId(entityId)
                .entries(sampleTimeseries)
                .ttl(sampleTtl)
                .strategy(new TimeseriesSaveRequest.Strategy(false, true, false, false))
                .build();

        // WHEN
        telemetryService.saveTimeseries(request);

        // THEN
        // should save latest to both the main entity and it's entity view
        then(tsService).should().saveLatest(tenantId, entityId, sampleTimeseries);
        then(tsService).should().saveLatest(tenantId, entityView.getId(), sampleTimeseries);
        then(tsService).shouldHaveNoMoreInteractions();

        // should send WS update only for entity view (WS update for the main entity is disabled in the save request)
        then(subscriptionManagerService).should().onTimeSeriesUpdate(tenantId, entityView.getId(), sampleTimeseries, TbCallback.EMPTY);
        then(subscriptionManagerService).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldNotCopyLatestToEntityViewWhenLatestIsNotSavedOnMainEntity() {
        // GIVEN
        var request = TimeseriesSaveRequest.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .entityId(entityId)
                .entries(sampleTimeseries)
                .ttl(sampleTtl)
                .strategy(new TimeseriesSaveRequest.Strategy(true, false, false, false))
                .build();

        // WHEN
        telemetryService.saveTimeseries(request);

        // THEN
        // should save only time series for the main entity
        then(tsService).should().saveWithoutLatest(tenantId, entityId, sampleTimeseries, sampleTtl);
        then(tsService).shouldHaveNoMoreInteractions();

        // should not send any WS updates
        then(subscriptionManagerService).shouldHaveNoInteractions();
    }

    @Test
    void shouldNotCopyLatestToEntityViewWhenTimeseriesSaveFailedOnMainEntity() {
        // GIVEN
        var entityView = new EntityView(new EntityViewId(UUID.randomUUID()));
        entityView.setTenantId(tenantId);
        entityView.setCustomerId(customerId);
        entityView.setEntityId(entityId);
        entityView.setKeys(new TelemetryEntityView(sampleTimeseries.stream().map(KvEntry::getKey).toList(), new AttributesEntityView()));

        // mock that there is one entity view
        lenient().when(tbEntityViewService.findEntityViewsByTenantIdAndEntityIdAsync(tenantId, entityId)).thenReturn(immediateFuture(List.of(entityView)));
        // mock that save latest call for entity view is successful
        lenient().when(tsService.saveLatest(tenantId, entityView.getId(), sampleTimeseries)).thenReturn(immediateFuture(TimeseriesSaveResult.of(sampleTimeseries.size(), listOfNNumbers(sampleTimeseries.size()))));

        var request = TimeseriesSaveRequest.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .entityId(entityId)
                .entries(sampleTimeseries)
                .ttl(sampleTtl)
                .strategy(new TimeseriesSaveRequest.Strategy(true, true, false, false))
                .build();

        given(tsService.save(tenantId, entityId, sampleTimeseries, sampleTtl)).willReturn(immediateFailedFuture(new RuntimeException("failed to save data on main entity")));

        // WHEN
        telemetryService.saveTimeseries(request);

        // THEN
        // should save only time series for the main entity
        then(tsService).should().save(tenantId, entityId, sampleTimeseries, sampleTtl);
        then(tsService).shouldHaveNoMoreInteractions();

        // should not send any WS updates
        then(subscriptionManagerService).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @MethodSource("allCombinationsOfFourBooleans")
    void shouldCallCorrectSaveTimeseriesApiBasedOnBooleanFlagsInTheSaveRequest(boolean saveTimeseries, boolean saveLatest, boolean sendWsUpdate, boolean processCalculatedFields) {
        // GIVEN
        var request = TimeseriesSaveRequest.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .entityId(entityId)
                .entries(sampleTimeseries)
                .ttl(sampleTtl)
                .strategy(new TimeseriesSaveRequest.Strategy(saveTimeseries, saveLatest, sendWsUpdate, processCalculatedFields))
                .build();

        // WHEN
        telemetryService.saveTimeseries(request);

        // THEN
        if (saveTimeseries && saveLatest) {
            then(tsService).should().save(tenantId, entityId, sampleTimeseries, sampleTtl);
        } else if (saveLatest) {
            then(tsService).should().saveLatest(tenantId, entityId, sampleTimeseries);
        } else if (saveTimeseries) {
            then(tsService).should().saveWithoutLatest(tenantId, entityId, sampleTimeseries, sampleTtl);
        }

        if (processCalculatedFields) {
            then(calculatedFieldQueueService).should().pushRequestToQueue(eq(request), any(), eq(request.getCallback()));
        }

        then(tsService).shouldHaveNoMoreInteractions();

        if (sendWsUpdate) {
            then(subscriptionManagerService).should().onTimeSeriesUpdate(tenantId, entityId, sampleTimeseries, TbCallback.EMPTY);
        } else {
            then(subscriptionManagerService).shouldHaveNoInteractions();
        }
    }

    private static Stream<Arguments> allCombinationsOfFourBooleans() {
        return Stream.of(
                Arguments.of(true, true, true, true),
                Arguments.of(true, true, true, false),
                Arguments.of(true, true, false, true),
                Arguments.of(true, true, false, false),
                Arguments.of(true, false, true, true),
                Arguments.of(true, false, true, false),
                Arguments.of(true, false, false, true),
                Arguments.of(true, false, false, false),
                Arguments.of(false, true, true, true),
                Arguments.of(false, true, true, false),
                Arguments.of(false, true, false, true),
                Arguments.of(false, true, false, false),
                Arguments.of(false, false, true, true),
                Arguments.of(false, false, true, false),
                Arguments.of(false, false, false, true),
                Arguments.of(false, false, false, false)
        );
    }

    /* --- Save attributes API --- */

    @ParameterizedTest
    @MethodSource("allCombinationsOfThreeBooleans")
    void shouldCallCorrectSaveAttributesApiBasedOnBooleanFlagsInTheSaveRequest(boolean saveAttributes, boolean sendWsUpdate, boolean processCalculatedFields) {
        // GIVEN
        var request = AttributesSaveRequest.builder()
                .tenantId(tenantId)
                .entityId(entityId)
                .scope(AttributeScope.SERVER_SCOPE)
                .entry(new DoubleDataEntry("temperature", 65.2))
                .notifyDevice(false)
                .strategy(new AttributesSaveRequest.Strategy(saveAttributes, sendWsUpdate, processCalculatedFields))
                .build();

        lenient().when(attrService.save(tenantId, entityId, request.getScope(), request.getEntries()))
                .thenReturn(immediateFuture(AttributesSaveResult.of(listOfNNumbers(request.getEntries().size()))));

        // WHEN
        telemetryService.saveAttributes(request);

        // THEN
        if (saveAttributes) {
            then(attrService).should().save(tenantId, entityId, request.getScope(), request.getEntries());
        } else {
            then(attrService).shouldHaveNoInteractions();
        }

        if (processCalculatedFields) {
            then(calculatedFieldQueueService).should().pushRequestToQueue(eq(request), any(), eq(request.getCallback()));
        }

        if (sendWsUpdate) {
            then(subscriptionManagerService).should().onAttributesUpdate(tenantId, entityId, request.getScope().name(), request.getEntries(), TbCallback.EMPTY);
        } else {
            then(subscriptionManagerService).shouldHaveNoInteractions();
        }
    }

    static Stream<Arguments> allCombinationsOfThreeBooleans() {
        return Stream.of(
                Arguments.of(true, true, true),
                Arguments.of(true, true, false),
                Arguments.of(true, false, true),
                Arguments.of(true, false, false),
                Arguments.of(false, true, true),
                Arguments.of(false, true, false),
                Arguments.of(false, false, true),
                Arguments.of(false, false, false)
        );
    }

    @Test
    void shouldThrowErrorWhenTryingToSaveAttributesForApiUsageState() {
        // GIVEN
        var request = AttributesSaveRequest.builder()
                .tenantId(tenantId)
                .entityId(new ApiUsageStateId(UUID.randomUUID()))
                .scope(AttributeScope.SHARED_SCOPE)
                .entry(new DoubleDataEntry("temperature", 65.2))
                .notifyDevice(true)
                .strategy(new AttributesSaveRequest.Strategy(true, false, false))
                .build();

        // WHEN
        assertThatThrownBy(() -> telemetryService.saveAttributes(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Can't update API Usage State!");

        // THEN
        then(attrService).shouldHaveNoInteractions();
    }

    @Test
    void shouldSendAttributesUpdateNotificationWhenDeviceSharedAttributesAreSavedAndNotifyDeviceIsTrue() {
        // GIVEN
        var deviceId = DeviceId.fromString("cc51e450-53e1-11ee-883e-e56b48fd2088");
        List<AttributeKvEntry> entries = List.of(
                new BaseAttributeKvEntry(123L, new DoubleDataEntry("shared1", 65.2)),
                new BaseAttributeKvEntry(456L, new StringDataEntry("shared2", "test"))
        );

        var request = AttributesSaveRequest.builder()
                .tenantId(tenantId)
                .entityId(deviceId)
                .scope(AttributeScope.SHARED_SCOPE)
                .entries(entries)
                .notifyDevice(true)
                .strategy(new AttributesSaveRequest.Strategy(true, false, false))
                .build();

        given(attrService.save(tenantId, deviceId, request.getScope(), entries))
                .willReturn(immediateFuture(AttributesSaveResult.of(listOfNNumbers(entries.size()))));

        // WHEN
        telemetryService.saveAttributes(request);

        // THEN
        var expectedAttributesUpdateMsg = DeviceAttributesEventNotificationMsg.onUpdate(tenantId, deviceId, "SHARED_SCOPE", entries);

        then(clusterService).should().pushMsgToCore(eq(expectedAttributesUpdateMsg), isNull());
    }

    @ParameterizedTest
    @EnumSource(
            value = EntityType.class,
            names = {"DEVICE", "API_USAGE_STATE"}, // API usage state excluded due to coverage in another test
            mode = EnumSource.Mode.EXCLUDE
    )
    void shouldNotSendAttributesUpdateNotificationWhenEntityIsNotDevice(EntityType entityType) {
        // GIVEN
        var nonDeviceId = EntityIdFactory.getByTypeAndUuid(entityType, "cc51e450-53e1-11ee-883e-e56b48fd2088");
        List<AttributeKvEntry> entries = List.of(
                new BaseAttributeKvEntry(123L, new DoubleDataEntry("shared1", 65.2)),
                new BaseAttributeKvEntry(456L, new StringDataEntry("shared2", "test"))
        );

        var request = AttributesSaveRequest.builder()
                .tenantId(tenantId)
                .entityId(nonDeviceId)
                .scope(AttributeScope.SHARED_SCOPE)
                .entries(entries)
                .notifyDevice(true)
                .strategy(new AttributesSaveRequest.Strategy(true, false, false))
                .build();

        given(attrService.save(tenantId, nonDeviceId, request.getScope(), entries))
                .willReturn(immediateFuture(AttributesSaveResult.of(listOfNNumbers(entries.size()))));

        // WHEN
        telemetryService.saveAttributes(request);

        // THEN
        then(clusterService).should(never()).pushMsgToCore(any(), any());
    }

    @ParameterizedTest
    @EnumSource(
            value = AttributeScope.class,
            names = "SHARED_SCOPE",
            mode = EnumSource.Mode.EXCLUDE
    )
    void shouldNotSendAttributesUpdateNotificationWhenAttributesAreNotShared(AttributeScope notSharedScope) {
        // GIVEN
        var deviceId = DeviceId.fromString("cc51e450-53e1-11ee-883e-e56b48fd2088");
        List<AttributeKvEntry> entries = List.of(
                new BaseAttributeKvEntry(123L, new DoubleDataEntry("shared1", 65.2)),
                new BaseAttributeKvEntry(456L, new StringDataEntry("shared2", "test"))
        );

        var request = AttributesSaveRequest.builder()
                .tenantId(tenantId)
                .entityId(deviceId)
                .scope(notSharedScope)
                .entries(entries)
                .notifyDevice(true)
                .strategy(new AttributesSaveRequest.Strategy(true, false, false))
                .build();

        given(attrService.save(tenantId, deviceId, request.getScope(), entries))
                .willReturn(immediateFuture(AttributesSaveResult.of(listOfNNumbers(entries.size()))));

        // WHEN
        telemetryService.saveAttributes(request);

        // THEN
        then(clusterService).should(never()).pushMsgToCore(any(), any());
    }

    @Test
    void shouldNotSendAttributesUpdateNotificationWhenNotifyDeviceIsFalse() {
        // GIVEN
        var deviceId = DeviceId.fromString("cc51e450-53e1-11ee-883e-e56b48fd2088");
        List<AttributeKvEntry> entries = List.of(
                new BaseAttributeKvEntry(123L, new DoubleDataEntry("shared1", 65.2)),
                new BaseAttributeKvEntry(456L, new StringDataEntry("shared2", "test"))
        );

        var request = AttributesSaveRequest.builder()
                .tenantId(tenantId)
                .entityId(deviceId)
                .scope(AttributeScope.SHARED_SCOPE)
                .entries(entries)
                .notifyDevice(false)
                .strategy(new AttributesSaveRequest.Strategy(true, false, false))
                .build();

        given(attrService.save(tenantId, deviceId, request.getScope(), entries))
                .willReturn(immediateFuture(AttributesSaveResult.of(listOfNNumbers(entries.size()))));

        // WHEN
        telemetryService.saveAttributes(request);

        // THEN
        then(clusterService).should(never()).pushMsgToCore(any(), any());
    }

    @Test
    void shouldNotSendAttributesUpdateNotificationWhenAttributesSaveWasSkipped() {
        // GIVEN
        var deviceId = DeviceId.fromString("cc51e450-53e1-11ee-883e-e56b48fd2088");
        List<AttributeKvEntry> entries = List.of(
                new BaseAttributeKvEntry(123L, new DoubleDataEntry("shared1", 65.2)),
                new BaseAttributeKvEntry(456L, new StringDataEntry("shared2", "test"))
        );

        var request = AttributesSaveRequest.builder()
                .tenantId(tenantId)
                .entityId(deviceId)
                .scope(AttributeScope.SHARED_SCOPE)
                .entries(entries)
                .notifyDevice(true)
                .strategy(new AttributesSaveRequest.Strategy(false, false, false))
                .build();

        // WHEN
        telemetryService.saveAttributes(request);

        // THEN
        then(clusterService).should(never()).pushMsgToCore(any(), any());
    }

    @Test
    void shouldNotSendAttributesUpdateNotificationWhenAttributesSaveFailed() {
        // GIVEN
        var deviceId = DeviceId.fromString("cc51e450-53e1-11ee-883e-e56b48fd2088");
        List<AttributeKvEntry> entries = List.of(
                new BaseAttributeKvEntry(123L, new DoubleDataEntry("shared1", 65.2)),
                new BaseAttributeKvEntry(456L, new StringDataEntry("shared2", "test"))
        );

        var request = AttributesSaveRequest.builder()
                .tenantId(tenantId)
                .entityId(deviceId)
                .scope(AttributeScope.SHARED_SCOPE)
                .entries(entries)
                .notifyDevice(true)
                .strategy(new AttributesSaveRequest.Strategy(true, false, false))
                .build();

        given(attrService.save(tenantId, deviceId, request.getScope(), entries)).willReturn(immediateFailedFuture(new RuntimeException("failed to save")));

        // WHEN
        telemetryService.saveAttributes(request);

        // THEN
        then(clusterService).should(never()).pushMsgToCore(any(), any());
    }

    @Test
    void shouldNotifyDeviceStateManagerWhenDeviceInactivityTimeoutWasUpdated() {
        // GIVEN
        var deviceId = DeviceId.fromString("cc51e450-53e1-11ee-883e-e56b48fd2088");
        var inactivityTimeout = new BaseAttributeKvEntry(123L, new LongDataEntry("inactivityTimeout", 5000L));

        var request = AttributesSaveRequest.builder()
                .tenantId(tenantId)
                .entityId(deviceId)
                .scope(AttributeScope.SERVER_SCOPE)
                .entry(inactivityTimeout)
                .strategy(new AttributesSaveRequest.Strategy(true, false, false))
                .build();

        given(attrService.save(tenantId, deviceId, request.getScope(), request.getEntries()))
                .willReturn(immediateFuture(AttributesSaveResult.of(listOfNNumbers(request.getEntries().size()))));

        // WHEN
        telemetryService.saveAttributes(request);

        // THEN
        then(deviceStateManager).should().onDeviceInactivityTimeoutUpdate(tenantId, deviceId, 5000L, TbCallback.EMPTY);
    }

    @Test
    void shouldNotNotifyDeviceStateManagerWhenDeviceInactivityTimeoutSaveWasSkipped() {
        // GIVEN
        var deviceId = DeviceId.fromString("cc51e450-53e1-11ee-883e-e56b48fd2088");
        var inactivityTimeout = new BaseAttributeKvEntry(123L, new LongDataEntry("inactivityTimeout", 5000L));

        var request = AttributesSaveRequest.builder()
                .tenantId(tenantId)
                .entityId(deviceId)
                .scope(AttributeScope.SERVER_SCOPE)
                .entry(inactivityTimeout)
                .strategy(new AttributesSaveRequest.Strategy(false, true, true))
                .build();

        // WHEN
        telemetryService.saveAttributes(request);

        // THEN
        then(deviceStateManager).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @EnumSource(
            value = EntityType.class,
            names = {"DEVICE", "API_USAGE_STATE"}, // API usage state excluded due to coverage in another test
            mode = EnumSource.Mode.EXCLUDE
    )
    void shouldNotNotifyDeviceStateManagerWhenInactivityTimeoutWasUpdatedButEntityTypeIsNotDevice(EntityType entityType) {
        // GIVEN
        var nonDeviceId = EntityIdFactory.getByTypeAndUuid(entityType, "cc51e450-53e1-11ee-883e-e56b48fd2088");
        var inactivityTimeout = new BaseAttributeKvEntry(123L, new LongDataEntry("inactivityTimeout", 5000L));

        var request = AttributesSaveRequest.builder()
                .tenantId(tenantId)
                .entityId(nonDeviceId)
                .scope(AttributeScope.SERVER_SCOPE)
                .entry(inactivityTimeout)
                .strategy(new AttributesSaveRequest.Strategy(true, false, false))
                .build();

        given(attrService.save(tenantId, nonDeviceId, request.getScope(), request.getEntries()))
                .willReturn(immediateFuture(AttributesSaveResult.of(listOfNNumbers(request.getEntries().size()))));

        // WHEN
        telemetryService.saveAttributes(request);

        // THEN
        then(deviceStateManager).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @EnumSource(
            value = AttributeScope.class,
            names = {"SERVER_SCOPE"},
            mode = EnumSource.Mode.EXCLUDE
    )
    void shouldNotNotifyDeviceStateManagerWhenInactivityTimeoutWasUpdatedButAttributeScopeIsNotServer(AttributeScope nonServerScope) {
        // GIVEN
        var deviceId = DeviceId.fromString("cc51e450-53e1-11ee-883e-e56b48fd2088");
        var inactivityTimeout = new BaseAttributeKvEntry(123L, new LongDataEntry("inactivityTimeout", 5000L));

        var request = AttributesSaveRequest.builder()
                .tenantId(tenantId)
                .entityId(deviceId)
                .scope(nonServerScope)
                .entry(inactivityTimeout)
                .strategy(new AttributesSaveRequest.Strategy(true, false, false))
                .build();

        given(attrService.save(tenantId, deviceId, request.getScope(), request.getEntries()))
                .willReturn(immediateFuture(AttributesSaveResult.of(listOfNNumbers(request.getEntries().size()))));

        // WHEN
        telemetryService.saveAttributes(request);

        // THEN
        then(deviceStateManager).shouldHaveNoInteractions();
    }

    @Test
    void shouldNotNotifyDeviceStateManagerWhenUpdatedAttributesDoNotContainInactivityTimeout() {
        // GIVEN
        var deviceId = DeviceId.fromString("cc51e450-53e1-11ee-883e-e56b48fd2088");
        var inactivityTimeout = new BaseAttributeKvEntry(123L, new LongDataEntry("notInactivityTimeout", 5000L));

        var request = AttributesSaveRequest.builder()
                .tenantId(tenantId)
                .entityId(deviceId)
                .scope(AttributeScope.SERVER_SCOPE)
                .entry(inactivityTimeout)
                .strategy(new AttributesSaveRequest.Strategy(true, false, false))
                .build();

        given(attrService.save(tenantId, deviceId, request.getScope(), request.getEntries()))
                .willReturn(immediateFuture(AttributesSaveResult.of(listOfNNumbers(request.getEntries().size()))));

        // WHEN
        telemetryService.saveAttributes(request);

        // THEN
        then(deviceStateManager).shouldHaveNoInteractions();
    }

    @Test
    void shouldUseInactivityTimeoutEntryWithTheGreatestVersion() {
        // GIVEN
        var deviceId = DeviceId.fromString("cc51e450-53e1-11ee-883e-e56b48fd2088");
        List<AttributeKvEntry> entries = List.of(
                new BaseAttributeKvEntry(new LongDataEntry("inactivityTimeout", 0L), 0L, null),
                new BaseAttributeKvEntry(new LongDataEntry("inactivityTimeout", 1000L), 3L, 1L),
                new BaseAttributeKvEntry(new LongDataEntry("inactivityTimeout", 2000L), 2L, 2L),
                new BaseAttributeKvEntry(new LongDataEntry("inactivityTimeout", 3000L), 1L, 3L)
        );

        var request = AttributesSaveRequest.builder()
                .tenantId(tenantId)
                .entityId(deviceId)
                .scope(AttributeScope.SERVER_SCOPE)
                .entries(entries)
                .strategy(new AttributesSaveRequest.Strategy(true, false, false))
                .build();

        given(attrService.save(tenantId, deviceId, request.getScope(), request.getEntries()))
                .willReturn(immediateFuture(AttributesSaveResult.of(listOfNNumbers(request.getEntries().size()))));

        // WHEN
        telemetryService.saveAttributes(request);

        // THEN
        then(deviceStateManager).should().onDeviceInactivityTimeoutUpdate(tenantId, deviceId, 3000L, TbCallback.EMPTY);
    }

    @Test
    void shouldUseInactivityTimeoutEntryWithTheGreatestLastUpdateTsWhenVersionsAreTheSame() {
        // GIVEN
        var deviceId = DeviceId.fromString("cc51e450-53e1-11ee-883e-e56b48fd2088");
        List<AttributeKvEntry> entries = List.of(
                new BaseAttributeKvEntry(new LongDataEntry("inactivityTimeout", 1000L), 1L, 1L),
                new BaseAttributeKvEntry(new LongDataEntry("inactivityTimeout", 2000L), 2L, 1L),
                new BaseAttributeKvEntry(new LongDataEntry("inactivityTimeout", 3000L), 3L, 1L)
        );

        var request = AttributesSaveRequest.builder()
                .tenantId(tenantId)
                .entityId(deviceId)
                .scope(AttributeScope.SERVER_SCOPE)
                .entries(entries)
                .strategy(new AttributesSaveRequest.Strategy(true, false, false))
                .build();

        given(attrService.save(tenantId, deviceId, request.getScope(), request.getEntries()))
                .willReturn(immediateFuture(AttributesSaveResult.of(listOfNNumbers(request.getEntries().size()))));

        // WHEN
        telemetryService.saveAttributes(request);

        // THEN
        then(deviceStateManager).should().onDeviceInactivityTimeoutUpdate(tenantId, deviceId, 3000L, TbCallback.EMPTY);
    }

    /* --- Delete attributes API --- */

    @Test
    void shouldThrowErrorWhenTryingToDeleteAttributesForApiUsageState() {
        // GIVEN
        var request = AttributesDeleteRequest.builder()
                .tenantId(tenantId)
                .entityId(new ApiUsageStateId(UUID.randomUUID()))
                .scope(AttributeScope.SHARED_SCOPE)
                .keys(List.of("attributeKeyToDelete1", "attributeKeyToDelete2"))
                .notifyDevice(true)
                .build();

        // WHEN
        assertThatThrownBy(() -> telemetryService.deleteAttributes(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Can't update API Usage State!");

        // THEN
        then(attrService).shouldHaveNoInteractions();
    }

    @Test
    void shouldSendAttributesDeletedNotificationWhenDeviceSharedAttributesAreDeletedAndNotifyDeviceIsTrue() {
        // GIVEN
        var deviceId = DeviceId.fromString("cc51e450-53e1-11ee-883e-e56b48fd2088");
        List<String> keys = List.of("attributeKeyToDelete1", "attributeKeyToDelete2");

        var request = AttributesDeleteRequest.builder()
                .tenantId(tenantId)
                .entityId(deviceId)
                .scope(AttributeScope.SHARED_SCOPE)
                .keys(keys)
                .notifyDevice(true)
                .build();

        given(attrService.removeAll(tenantId, deviceId, request.getScope(), keys)).willReturn(immediateFuture(keys));

        // WHEN
        telemetryService.deleteAttributes(request);

        // THEN
        var expectedAttributesDeletedMsg = DeviceAttributesEventNotificationMsg.onDelete(tenantId, deviceId, "SHARED_SCOPE", List.of("attributeKeyToDelete1", "attributeKeyToDelete2"));

        then(clusterService).should().pushMsgToCore(eq(expectedAttributesDeletedMsg), isNull());
    }

    @ParameterizedTest
    @EnumSource(
            value = EntityType.class,
            names = {"DEVICE", "API_USAGE_STATE"}, // API usage state excluded due to coverage in another test
            mode = EnumSource.Mode.EXCLUDE
    )
    void shouldNotSendAttributesDeletedNotificationWhenEntityIsNotDevice(EntityType entityType) {
        // GIVEN
        var nonDeviceId = EntityIdFactory.getByTypeAndUuid(entityType, "cc51e450-53e1-11ee-883e-e56b48fd2088");
        List<String> keys = List.of("attributeKeyToDelete1", "attributeKeyToDelete2");

        var request = AttributesDeleteRequest.builder()
                .tenantId(tenantId)
                .entityId(nonDeviceId)
                .scope(AttributeScope.SHARED_SCOPE)
                .keys(keys)
                .notifyDevice(true)
                .build();

        given(attrService.removeAll(tenantId, nonDeviceId, request.getScope(), keys)).willReturn(immediateFuture(keys));

        // WHEN
        telemetryService.deleteAttributes(request);

        // THEN
        then(clusterService).should(never()).pushMsgToCore(any(), any());
    }

    @ParameterizedTest
    @EnumSource(
            value = AttributeScope.class,
            names = "SHARED_SCOPE",
            mode = EnumSource.Mode.EXCLUDE
    )
    void shouldNotSendAttributesDeletedNotificationWhenAttributesAreNotShared(AttributeScope notSharedScope) {
        // GIVEN
        var deviceId = DeviceId.fromString("cc51e450-53e1-11ee-883e-e56b48fd2088");
        List<String> keys = List.of("attributeKeyToDelete1", "attributeKeyToDelete2");

        var request = AttributesDeleteRequest.builder()
                .tenantId(tenantId)
                .entityId(deviceId)
                .scope(notSharedScope)
                .keys(keys)
                .notifyDevice(true)
                .build();

        given(attrService.removeAll(tenantId, deviceId, request.getScope(), keys)).willReturn(immediateFuture(keys));

        // WHEN
        telemetryService.deleteAttributes(request);

        // THEN
        then(clusterService).should(never()).pushMsgToCore(any(), any());
    }

    @Test
    void shouldNotSendAttributesDeletedNotificationWhenNotifyDeviceIsFalse() {
        // GIVEN
        var deviceId = DeviceId.fromString("cc51e450-53e1-11ee-883e-e56b48fd2088");
        List<String> keys = List.of("attributeKeyToDelete1", "attributeKeyToDelete2");

        var request = AttributesDeleteRequest.builder()
                .tenantId(tenantId)
                .entityId(deviceId)
                .scope(AttributeScope.SHARED_SCOPE)
                .keys(keys)
                .notifyDevice(false)
                .build();

        given(attrService.removeAll(tenantId, deviceId, request.getScope(), keys)).willReturn(immediateFuture(keys));

        // WHEN
        telemetryService.deleteAttributes(request);

        // THEN
        then(clusterService).should(never()).pushMsgToCore(any(), any());
    }

    @Test
    void shouldNotSendAttributesDeletedNotificationWhenAttributesDeleteFailed() {
        // GIVEN
        var deviceId = DeviceId.fromString("cc51e450-53e1-11ee-883e-e56b48fd2088");
        List<String> keys = List.of("attributeKeyToDelete1", "attributeKeyToDelete2");

        var request = AttributesDeleteRequest.builder()
                .tenantId(tenantId)
                .entityId(deviceId)
                .scope(AttributeScope.SHARED_SCOPE)
                .keys(keys)
                .notifyDevice(true)
                .build();

        given(attrService.removeAll(tenantId, deviceId, request.getScope(), keys)).willReturn(immediateFailedFuture(new RuntimeException("failed to delete")));

        // WHEN
        telemetryService.deleteAttributes(request);

        // THEN
        then(clusterService).should(never()).pushMsgToCore(any(), any());
    }

    @Test
    void shouldNotifyDeviceStateManagerWhenDeviceInactivityTimeoutWasDeleted() {
        // GIVEN
        var deviceId = DeviceId.fromString("cc51e450-53e1-11ee-883e-e56b48fd2088");

        var request = AttributesDeleteRequest.builder()
                .tenantId(tenantId)
                .entityId(deviceId)
                .scope(AttributeScope.SERVER_SCOPE)
                .keys(List.of("inactivityTimeout", "someOtherDeletedAttribute"))
                .build();

        given(attrService.removeAll(tenantId, deviceId, request.getScope(), request.getKeys())).willReturn(immediateFuture(request.getKeys()));

        // WHEN
        telemetryService.deleteAttributes(request);

        // THEN
        then(deviceStateManager).should().onDeviceInactivityTimeoutUpdate(tenantId, deviceId, 0L, TbCallback.EMPTY);
    }

    @ParameterizedTest
    @EnumSource(
            value = EntityType.class,
            names = {"DEVICE", "API_USAGE_STATE"}, // API usage state excluded due to coverage in another test
            mode = EnumSource.Mode.EXCLUDE
    )
    void shouldNotNotifyDeviceStateManagerWhenInactivityTimeoutWasDeletedButEntityTypeIsNotDevice(EntityType entityType) {
        // GIVEN
        var nonDeviceId = EntityIdFactory.getByTypeAndUuid(entityType, "cc51e450-53e1-11ee-883e-e56b48fd2088");

        var request = AttributesDeleteRequest.builder()
                .tenantId(tenantId)
                .entityId(nonDeviceId)
                .scope(AttributeScope.SERVER_SCOPE)
                .keys(List.of("inactivityTimeout", "someOtherDeletedAttribute"))
                .build();

        given(attrService.removeAll(tenantId, nonDeviceId, request.getScope(), request.getKeys())).willReturn(immediateFuture(request.getKeys()));

        // WHEN
        telemetryService.deleteAttributes(request);

        // THEN
        then(deviceStateManager).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @EnumSource(
            value = AttributeScope.class,
            names = {"SERVER_SCOPE"},
            mode = EnumSource.Mode.EXCLUDE
    )
    void shouldNotNotifyDeviceStateManagerWhenInactivityTimeoutWasDeletedButAttributeScopeIsNotServer(AttributeScope nonServerScope) {
        // GIVEN
        var deviceId = DeviceId.fromString("cc51e450-53e1-11ee-883e-e56b48fd2088");

        var request = AttributesDeleteRequest.builder()
                .tenantId(tenantId)
                .entityId(deviceId)
                .scope(nonServerScope)
                .keys(List.of("inactivityTimeout", "someOtherDeletedAttribute"))
                .build();

        given(attrService.removeAll(tenantId, deviceId, request.getScope(), request.getKeys())).willReturn(immediateFuture(request.getKeys()));

        // WHEN
        telemetryService.deleteAttributes(request);

        // THEN
        then(deviceStateManager).shouldHaveNoInteractions();
    }

    @Test
    void shouldNotNotifyDeviceStateManagerWhenInactivityTimeoutWasNotDeleted() {
        // GIVEN
        var deviceId = DeviceId.fromString("cc51e450-53e1-11ee-883e-e56b48fd2088");

        var request = AttributesDeleteRequest.builder()
                .tenantId(tenantId)
                .entityId(deviceId)
                .scope(AttributeScope.SERVER_SCOPE)
                .keys(List.of("someOtherDeletedAttribute"))
                .build();

        given(attrService.removeAll(tenantId, deviceId, request.getScope(), request.getKeys())).willReturn(immediateFuture(request.getKeys()));

        // WHEN
        telemetryService.deleteAttributes(request);

        // THEN
        then(deviceStateManager).shouldHaveNoInteractions();
    }

    @Test
    void shouldNotNotifyDeviceStateManagerWhenDeviceInactivityTimeoutDeleteFailed() {
        // GIVEN
        var deviceId = DeviceId.fromString("cc51e450-53e1-11ee-883e-e56b48fd2088");

        var request = AttributesDeleteRequest.builder()
                .tenantId(tenantId)
                .entityId(deviceId)
                .scope(AttributeScope.SERVER_SCOPE)
                .keys(List.of("inactivityTimeout", "someOtherDeletedAttribute"))
                .build();

        given(attrService.removeAll(tenantId, deviceId, request.getScope(), request.getKeys())).willReturn(immediateFailedFuture(new RuntimeException("failed to delete")));

        // WHEN
        telemetryService.deleteAttributes(request);

        // THEN
        then(deviceStateManager).shouldHaveNoInteractions();
    }

    // used to emulate versions returned by save APIs
    private static List<Long> listOfNNumbers(int N) {
        return LongStream.range(0, N).boxed().toList();
    }

}
