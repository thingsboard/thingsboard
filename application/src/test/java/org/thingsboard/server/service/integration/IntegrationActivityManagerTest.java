/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.service.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.transport.activity.ActivityReportCallback;
import org.thingsboard.server.common.transport.activity.ActivityState;
import org.thingsboard.server.common.transport.activity.strategy.ActivityStrategy;
import org.thingsboard.server.common.transport.activity.strategy.ActivityStrategyType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IntegrationActivityManagerTest {

    private final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("1306648a-9b26-11ee-b9d1-0242ac120002"));
    private final DeviceId DEVICE_ID = DeviceId.fromString("1d288a06-9b26-11ee-b9d1-0242ac120002");

    @Mock
    private DefaultPlatformIntegrationService integrationServiceMock;

    @Test
    void givenKeyAndTimeToReport_whenReportingActivity_thenShouldCorrectlyReportActivity() {
        // GIVEN
        var key = new IntegrationActivityKey(TENANT_ID, DEVICE_ID);

        TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCoreMsg>> tbCoreMsgProducerMock = mock(TbQueueProducer.class);
        ReflectionTestUtils.setField(integrationServiceMock, "tbCoreMsgProducer", tbCoreMsgProducerMock);

        PartitionService partitionServiceMock = mock(PartitionService.class);
        ReflectionTestUtils.setField(integrationServiceMock, "partitionService", partitionServiceMock);
        TopicPartitionInfo tpi = TopicPartitionInfo.builder().build();
        when(partitionServiceMock.resolve(ServiceType.TB_CORE, TENANT_ID, DEVICE_ID)).thenReturn(tpi);

        ActivityReportCallback<IntegrationActivityKey> callbackMock = mock(ActivityReportCallback.class);

        long expectedTime = 123L;

        doCallRealMethod().when(integrationServiceMock).reportActivity(key, null, expectedTime, callbackMock);

        // WHEN
        integrationServiceMock.reportActivity(key, null, expectedTime, callbackMock);

        // THEN
        verify(partitionServiceMock).resolve(ServiceType.TB_CORE, TENANT_ID, DEVICE_ID);

        ArgumentCaptor<TbProtoQueueMsg<TransportProtos.ToCoreMsg>> msgCaptor = ArgumentCaptor.forClass(TbProtoQueueMsg.class);
        ArgumentCaptor<TbQueueCallback> callbackCaptor = ArgumentCaptor.forClass(TbQueueCallback.class);
        verify(tbCoreMsgProducerMock).send(eq(tpi), msgCaptor.capture(), callbackCaptor.capture());

        TbProtoQueueMsg<TransportProtos.ToCoreMsg> queueMsg = msgCaptor.getValue();

        TransportProtos.DeviceActivityProto expectedDeviceActivityMsg = TransportProtos.DeviceActivityProto.newBuilder()
                .setTenantIdMSB(TENANT_ID.getId().getMostSignificantBits())
                .setTenantIdLSB(TENANT_ID.getId().getLeastSignificantBits())
                .setDeviceIdMSB(DEVICE_ID.getId().getMostSignificantBits())
                .setDeviceIdLSB(DEVICE_ID.getId().getLeastSignificantBits())
                .setLastActivityTime(expectedTime)
                .build();

        TransportProtos.ToCoreMsg expectedToCoreMsg = TransportProtos.ToCoreMsg.newBuilder()
                .setDeviceActivityMsg(expectedDeviceActivityMsg)
                .build();

        assertThat(queueMsg.getKey()).isEqualTo(DEVICE_ID.getId());
        assertThat(queueMsg.getValue()).isEqualTo(expectedToCoreMsg);

        TbQueueCallback queueCallback = callbackCaptor.getValue();

        queueCallback.onSuccess(null);
        verify(callbackMock).onSuccess(key, expectedTime);

        var throwable = new Throwable();
        queueCallback.onFailure(throwable);
        verify(callbackMock).onFailure(key, throwable);
    }

    @Test
    void givenPostTelemetryMsg_whenProcessingMsg_thenShouldCallOnActivity() {
        // GIVEN
        var key = new IntegrationActivityKey(TENANT_ID, DEVICE_ID);
        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setTenantIdMSB(TENANT_ID.getId().getMostSignificantBits())
                .setTenantIdLSB(TENANT_ID.getId().getLeastSignificantBits())
                .setDeviceIdMSB(DEVICE_ID.getId().getMostSignificantBits())
                .setDeviceIdLSB(DEVICE_ID.getId().getLeastSignificantBits())
                .setDeviceName("Test Device")
                .setDeviceType("default")
                .build();
        TransportProtos.PostTelemetryMsg postTelemetryMsg = TransportProtos.PostTelemetryMsg.getDefaultInstance();
        doCallRealMethod().when(integrationServiceMock).process(sessionInfo, postTelemetryMsg, null);

        // WHEN
        integrationServiceMock.process(sessionInfo, postTelemetryMsg, null);

        // THEN
        verify(integrationServiceMock).onActivity(key);
    }

    @Test
    void givenPostAttributesMsg_whenProcessingMsg_thenShouldCallOnActivity() {
        // GIVEN
        var key = new IntegrationActivityKey(TENANT_ID, DEVICE_ID);
        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setTenantIdMSB(TENANT_ID.getId().getMostSignificantBits())
                .setTenantIdLSB(TENANT_ID.getId().getLeastSignificantBits())
                .setDeviceIdMSB(DEVICE_ID.getId().getMostSignificantBits())
                .setDeviceIdLSB(DEVICE_ID.getId().getLeastSignificantBits())
                .setDeviceName("Test Device")
                .setDeviceType("default")
                .build();
        TransportProtos.PostAttributeMsg postAttributeMsg = TransportProtos.PostAttributeMsg.getDefaultInstance();
        doCallRealMethod().when(integrationServiceMock).process(sessionInfo, postAttributeMsg, null);
        doNothing().when(integrationServiceMock).sendToRuleEngine(any(), any(), any(), any(), any(), any(), any());

        // WHEN
        integrationServiceMock.process(sessionInfo, postAttributeMsg, null);

        // THEN
        verify(integrationServiceMock).onActivity(key);
    }

    @Test
    void givenKey_whenCreatingNewState_thenShouldCorrectlyCreateNewEmptyState() {
        // GIVEN
        var key = new IntegrationActivityKey(TENANT_ID, DEVICE_ID);
        when(integrationServiceMock.createNewState(key)).thenCallRealMethod();

        // WHEN
        ActivityState<Void> actualNewState = integrationServiceMock.createNewState(key);

        // THEN
        ActivityState<Void> expectedNewState = new ActivityState<>();
        assertThat(actualNewState).isEqualTo(expectedNewState);
    }

    @ParameterizedTest
    @EnumSource(ActivityStrategyType.class)
    void givenDifferentReportingStrategies_whenGettingStrategy_thenShouldReturnCorrectStrategy(ActivityStrategyType reportingStrategyType) {
        // GIVEN
        doCallRealMethod().when(integrationServiceMock).getStrategy();
        ReflectionTestUtils.setField(integrationServiceMock, "reportingStrategyType", reportingStrategyType);

        // WHEN
        ActivityStrategy actualStrategy = integrationServiceMock.getStrategy();

        // THEN
        assertThat(actualStrategy).isEqualTo(reportingStrategyType.toStrategy());
    }

    @Test
    void givenActivityState_whenUpdatingActivityState_thenShouldReturnSameInstanceWithNoInteractions() {
        // GIVEN
        var key = new IntegrationActivityKey(TENANT_ID, DEVICE_ID);

        ActivityState<Void> state = new ActivityState<>();
        state.setLastRecordedTime(123L);
        ActivityState<Void> stateSpy = spy(state);

        when(integrationServiceMock.updateState(key, state)).thenCallRealMethod();

        // WHEN
        ActivityState<Void> updatedState = integrationServiceMock.updateState(key, state);

        // THEN
        assertThat(updatedState).isSameAs(state);
        verifyNoInteractions(stateSpy);
    }

    @ParameterizedTest
    @MethodSource("provideTestParamsForHasExpiredTrue")
    public void givenExpiredLastRecordedTime_whenCheckingForExpiry_thenShouldReturnTrue(long currentTimeMillis, long lastRecordedTime, long reportingPeriodMillis) {
        // GIVEN
        ReflectionTestUtils.setField(integrationServiceMock, "reportingPeriodMillis", reportingPeriodMillis);

        when(integrationServiceMock.getCurrentTimeMillis()).thenReturn(currentTimeMillis);
        when(integrationServiceMock.hasExpired(lastRecordedTime)).thenCallRealMethod();

        // WHEN
        boolean hasExpired = integrationServiceMock.hasExpired(lastRecordedTime);

        // THEN
        assertThat(hasExpired).isTrue();
    }

    private static Stream<Arguments> provideTestParamsForHasExpiredTrue() {
        return Stream.of(
                Arguments.of(10L, 0L, 9L),
                Arguments.of(10L, 7L, 2L),
                Arguments.of(10L, 8L, 1L),
                Arguments.of(10000L, 5000L, 3000L)
        );
    }

    @ParameterizedTest
    @MethodSource("provideTestParamsForHasExpiredFalse")
    public void givenNotExpiredLastRecordedTime_whenCheckingForExpiry_thenShouldReturnFalse(long currentTimeMillis, long lastRecordedTime, long reportingPeriodMillis) {
        // GIVEN
        ReflectionTestUtils.setField(integrationServiceMock, "reportingPeriodMillis", reportingPeriodMillis);

        when(integrationServiceMock.getCurrentTimeMillis()).thenReturn(currentTimeMillis);
        when(integrationServiceMock.hasExpired(lastRecordedTime)).thenCallRealMethod();

        // WHEN
        boolean hasExpired = integrationServiceMock.hasExpired(lastRecordedTime);

        // THEN
        assertThat(hasExpired).isFalse();
    }

    private static Stream<Arguments> provideTestParamsForHasExpiredFalse() {
        return Stream.of(
                Arguments.of(10L, 9L, 2L),
                Arguments.of(10L, 0L, 11L),
                Arguments.of(10L, 8L, 3L),
                Arguments.of(10000L, 8000L, 3000L)
        );
    }

    @Test
    void givenKeyAndMetadata_whenOnStateExpiryCalled_thenShouldDoNothing() {
        // GIVEN
        var key = new IntegrationActivityKey(TENANT_ID, DEVICE_ID);
        doCallRealMethod().when(integrationServiceMock).onStateExpiry(key, null);

        // WHEN-THEN
        assertThatNoException().isThrownBy(() -> integrationServiceMock.onStateExpiry(key, null));
    }

}
