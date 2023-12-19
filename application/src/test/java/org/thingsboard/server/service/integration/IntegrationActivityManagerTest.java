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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IntegrationActivityManagerTest {

    private final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("1306648a-9b26-11ee-b9d1-0242ac120002"));
    private final DeviceId DEVICE_ID = DeviceId.fromString("1d288a06-9b26-11ee-b9d1-0242ac120002");

    @Mock
    private DefaultPlatformIntegrationService integrationServiceMock;

    @Test
    void givenKeyIsNull_whenOnActivity_thenShouldNotRecordAndShouldNotReport() {
        // GIVEN
        ConcurrentMap<IntegrationActivityKey, Object> states = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(integrationServiceMock, "states", states);

        IntegrationActivityKey key = null;

        doCallRealMethod().when(integrationServiceMock).onActivity(eq(key), anyLong());

        // WHEN
        integrationServiceMock.onActivity(key, 123L);

        // THEN
        assertThat(states.isEmpty()).isTrue();
        verify(integrationServiceMock, never()).createNewState(any());
        verify(integrationServiceMock, never()).getStrategy();
        verify(integrationServiceMock, never()).reportActivity(any(), any(), anyLong(), any());
    }

    @Test
    void givenNewActivity_whenOnActivity_thenShouldCreateNewStateAndRecord() {
        // GIVEN
        ConcurrentMap<IntegrationActivityKey, Object> states = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(integrationServiceMock, "states", states);

        ActivityStrategy strategyMock = mock(ActivityStrategy.class);
        when(integrationServiceMock.getStrategy()).thenReturn(strategyMock);

        var key = new IntegrationActivityKey(TENANT_ID, DEVICE_ID);
        when(integrationServiceMock.createNewState(key)).thenReturn(new ActivityState<>());

        long activityTime = 123L;
        doCallRealMethod().when(integrationServiceMock).onActivity(key, activityTime);

        when(integrationServiceMock.getLastRecordedTime(key)).thenCallRealMethod();

        // WHEN
        integrationServiceMock.onActivity(key, activityTime);

        // THEN
        assertThat(integrationServiceMock.getLastRecordedTime(key)).isEqualTo(activityTime);
        verify(integrationServiceMock).createNewState(key);
        verify(integrationServiceMock).getStrategy();
    }

    @Test
    void givenSubsequentActivities_whenOnActivity_thenShouldRecordBoth() {
        // GIVEN
        ConcurrentMap<IntegrationActivityKey, Object> states = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(integrationServiceMock, "states", states);

        ActivityStrategy strategyMock = mock(ActivityStrategy.class);
        when(integrationServiceMock.getStrategy()).thenReturn(strategyMock);

        var key = new IntegrationActivityKey(TENANT_ID, DEVICE_ID);
        when(integrationServiceMock.createNewState(key)).thenReturn(new ActivityState<>());

        when(integrationServiceMock.getLastRecordedTime(key)).thenCallRealMethod();

        // WHEN-THEN
        long firstActivityTime = 100L;
        doCallRealMethod().when(integrationServiceMock).onActivity(key, firstActivityTime);
        integrationServiceMock.onActivity(key, firstActivityTime);
        assertThat(integrationServiceMock.getLastRecordedTime(key)).isEqualTo(firstActivityTime);

        long secondActivityTime = 123L;
        doCallRealMethod().when(integrationServiceMock).onActivity(key, secondActivityTime);
        integrationServiceMock.onActivity(key, secondActivityTime);
        assertThat(integrationServiceMock.getLastRecordedTime(key)).isEqualTo(secondActivityTime);
    }

    @Test
    void givenActivityAndStrategySaysThatShouldNotReport_whenOnActivity_thenShouldNotReport() {
        // GIVEN
        ConcurrentMap<IntegrationActivityKey, Object> states = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(integrationServiceMock, "states", states);

        ActivityStrategy strategyMock = mock(ActivityStrategy.class);
        when(strategyMock.onActivity()).thenReturn(false);

        var key = new IntegrationActivityKey(TENANT_ID, DEVICE_ID);
        when(integrationServiceMock.createNewState(key)).thenReturn(new ActivityState<>());
        when(integrationServiceMock.getStrategy()).thenReturn(strategyMock);

        long activityTime = 123L;
        doCallRealMethod().when(integrationServiceMock).onActivity(key, activityTime);

        // WHEN
        integrationServiceMock.onActivity(key, activityTime);

        // THEN
        verify(integrationServiceMock, never()).reportActivity(any(), any(), anyLong(), any());
    }

    @Test
    void givenActivityAndStrategySaysThatShouldReport_whenOnActivity_thenShouldReport() {
        // GIVEN
        ConcurrentMap<IntegrationActivityKey, Object> states = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(integrationServiceMock, "states", states);

        ActivityStrategy strategyMock = mock(ActivityStrategy.class);
        when(strategyMock.onActivity()).thenReturn(true);

        var key = new IntegrationActivityKey(TENANT_ID, DEVICE_ID);
        when(integrationServiceMock.createNewState(key)).thenReturn(new ActivityState<>());
        when(integrationServiceMock.getStrategy()).thenReturn(strategyMock);

        long activityTime = 123L;
        doCallRealMethod().when(integrationServiceMock).onActivity(key, activityTime);

        // WHEN
        integrationServiceMock.onActivity(key, activityTime);

        // THEN
        verify(integrationServiceMock).reportActivity(eq(key), isNull(), eq(activityTime), any());
    }

    @Test
    void givenActivityAndStrategySaysThatShouldReportButLastRecordedTimeIsEqualToLastReportedTime_whenOnActivity_thenShouldNotReport() {
        // GIVEN
        ConcurrentMap<IntegrationActivityKey, Object> states = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(integrationServiceMock, "states", states);

        ActivityStrategy strategyMock = mock(ActivityStrategy.class);
        when(strategyMock.onActivity()).thenReturn(true);

        var key = new IntegrationActivityKey(TENANT_ID, DEVICE_ID);
        when(integrationServiceMock.createNewState(key)).thenReturn(new ActivityState<>());
        when(integrationServiceMock.getStrategy()).thenReturn(strategyMock);

        // WHEN-THEN
        long firstActivityTime = 123L;
        doCallRealMethod().when(integrationServiceMock).onActivity(key, firstActivityTime);
        integrationServiceMock.onActivity(key, firstActivityTime);

        ArgumentCaptor<ActivityReportCallback<IntegrationActivityKey>> firstCallbackCaptor = ArgumentCaptor.forClass(ActivityReportCallback.class);
        verify(integrationServiceMock).reportActivity(eq(key), isNull(), eq(firstActivityTime), firstCallbackCaptor.capture());
        firstCallbackCaptor.getValue().onSuccess(key, firstActivityTime);

        long secondActivityTime = 100L;
        doCallRealMethod().when(integrationServiceMock).onActivity(key, secondActivityTime);
        integrationServiceMock.onActivity(key, secondActivityTime);

        verify(integrationServiceMock, never()).reportActivity(eq(key), any(), eq(secondActivityTime), any());
    }

    @Test
    void givenActivityAndStrategySaysThatShouldReportAndLastRecordedTimeIsGreaterThanLastReportedTime_whenOnActivity_thenShouldReport() {
        // GIVEN
        ConcurrentMap<IntegrationActivityKey, Object> states = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(integrationServiceMock, "states", states);

        ActivityStrategy strategyMock = mock(ActivityStrategy.class);
        when(strategyMock.onActivity()).thenReturn(true);

        var key = new IntegrationActivityKey(TENANT_ID, DEVICE_ID);
        when(integrationServiceMock.createNewState(key)).thenReturn(new ActivityState<>());
        when(integrationServiceMock.getStrategy()).thenReturn(strategyMock);

        // WHEN-THEN
        long firstActivityTime = 123L;
        doCallRealMethod().when(integrationServiceMock).onActivity(key, firstActivityTime);
        integrationServiceMock.onActivity(key, firstActivityTime);

        ArgumentCaptor<ActivityReportCallback<IntegrationActivityKey>> firstCallbackCaptor = ArgumentCaptor.forClass(ActivityReportCallback.class);
        verify(integrationServiceMock).reportActivity(eq(key), isNull(), eq(firstActivityTime), firstCallbackCaptor.capture());
        firstCallbackCaptor.getValue().onSuccess(key, firstActivityTime);

        long secondActivityTime = 456L;
        doCallRealMethod().when(integrationServiceMock).onActivity(key, secondActivityTime);
        integrationServiceMock.onActivity(key, secondActivityTime);

        verify(integrationServiceMock).reportActivity(eq(key), isNull(), eq(secondActivityTime), any());
    }

    @Test
    void givenUpdatedStateIsNullAndHasUnreportedEvent_whenOnReportingPeriodEnd_thenShouldRemoveStateAndReportEvent() {
        // GIVEN
        ConcurrentMap<IntegrationActivityKey, Object> states = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(integrationServiceMock, "states", states);

        var key = new IntegrationActivityKey(TENANT_ID, DEVICE_ID);
        ActivityStrategy strategyMock = mock(ActivityStrategy.class);

        // first reported event
        when(integrationServiceMock.createNewState(key)).thenReturn(new ActivityState<>());
        when(strategyMock.onActivity()).thenReturn(true);
        when(integrationServiceMock.getStrategy()).thenReturn(strategyMock);

        long firstActivityTime = 123L;
        doCallRealMethod().when(integrationServiceMock).onActivity(key, firstActivityTime);
        integrationServiceMock.onActivity(key, firstActivityTime);

        ArgumentCaptor<ActivityReportCallback<IntegrationActivityKey>> firstCallbackCaptor = ArgumentCaptor.forClass(ActivityReportCallback.class);
        verify(integrationServiceMock).reportActivity(eq(key), isNull(), eq(firstActivityTime), firstCallbackCaptor.capture());
        firstCallbackCaptor.getValue().onSuccess(key, firstActivityTime);

        // second unreported event
        long secondActivityTime = 456L;
        when(strategyMock.onActivity()).thenReturn(false);
        doCallRealMethod().when(integrationServiceMock).onActivity(key, secondActivityTime);
        integrationServiceMock.onActivity(key, secondActivityTime);

        verify(integrationServiceMock, never()).reportActivity(eq(key), isNull(), eq(secondActivityTime), any());

        verify(strategyMock, times(2)).onActivity();

        long lastRecordedTime = secondActivityTime;
        // lastReportedTime = firstActivityTime = 123L;
        ActivityState<Void> updatedState = null;

        when(integrationServiceMock.updateState(eq(key), any())).thenReturn(updatedState);

        // WHEN
        doCallRealMethod().when(integrationServiceMock).onReportingPeriodEnd();
        integrationServiceMock.onReportingPeriodEnd();

        // THEN
        assertThat(states).isEmpty();
        verify(integrationServiceMock, never()).hasExpired(anyLong());
        verifyNoMoreInteractions(strategyMock);
        verify(integrationServiceMock, never()).onStateExpiry(any(), any());
        verify(integrationServiceMock).reportActivity(eq(key), isNull(), eq(lastRecordedTime), any());
    }

    @Test
    void givenUpdatedStateIsNullAndDoesntHaveUnreportedEvent_whenOnReportingPeriodEnd_thenShouldRemoveStateAndShouldNotAnything() {
        // GIVEN
        ConcurrentMap<IntegrationActivityKey, Object> states = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(integrationServiceMock, "states", states);

        var key = new IntegrationActivityKey(TENANT_ID, DEVICE_ID);
        ActivityStrategy strategyMock = mock(ActivityStrategy.class);

        // reported event
        when(integrationServiceMock.createNewState(key)).thenReturn(new ActivityState<>());
        when(strategyMock.onActivity()).thenReturn(true);
        when(integrationServiceMock.getStrategy()).thenReturn(strategyMock);

        long activityTime = 123L;
        doCallRealMethod().when(integrationServiceMock).onActivity(key, activityTime);
        integrationServiceMock.onActivity(key, activityTime);

        ArgumentCaptor<ActivityReportCallback<IntegrationActivityKey>> firstCallbackCaptor = ArgumentCaptor.forClass(ActivityReportCallback.class);
        verify(integrationServiceMock).reportActivity(eq(key), isNull(), eq(activityTime), firstCallbackCaptor.capture());
        firstCallbackCaptor.getValue().onSuccess(key, activityTime);

        verify(strategyMock).onActivity();

        long lastRecordedTime = activityTime;
        // lastReportedTime = lastRecordedTime = 123L;
        ActivityState<Void> updatedState = null;

        when(integrationServiceMock.updateState(eq(key), any())).thenReturn(updatedState);

        // WHEN
        doCallRealMethod().when(integrationServiceMock).onReportingPeriodEnd();
        integrationServiceMock.onReportingPeriodEnd();

        // THEN
        assertThat(states).isEmpty();
        verify(integrationServiceMock, never()).hasExpired(anyLong());
        verifyNoMoreInteractions(strategyMock);
        verify(integrationServiceMock, never()).onStateExpiry(any(), any());
        verify(integrationServiceMock).reportActivity(eq(key), isNull(), eq(lastRecordedTime), any());
    }

    @Test
    void givenUpdatedStateIsNotNullAndHasNotExpiredAndStrategySaysThatShouldReportAndHasUnreportedEvents_whenOnReportingPeriodEnd_thenShouldReportEventUsingUpdatedState() {
        // GIVEN
        ConcurrentMap<IntegrationActivityKey, Object> states = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(integrationServiceMock, "states", states);

        var key = new IntegrationActivityKey(TENANT_ID, DEVICE_ID);
        ActivityStrategy strategyMock = mock(ActivityStrategy.class);

        // first reported event
        when(integrationServiceMock.createNewState(key)).thenReturn(new ActivityState<>());
        when(strategyMock.onActivity()).thenReturn(true);
        when(integrationServiceMock.getStrategy()).thenReturn(strategyMock);

        long firstActivityTime = 123L;
        doCallRealMethod().when(integrationServiceMock).onActivity(key, firstActivityTime);
        integrationServiceMock.onActivity(key, firstActivityTime);

        ArgumentCaptor<ActivityReportCallback<IntegrationActivityKey>> firstCallbackCaptor = ArgumentCaptor.forClass(ActivityReportCallback.class);
        verify(integrationServiceMock).reportActivity(eq(key), isNull(), eq(firstActivityTime), firstCallbackCaptor.capture());
        firstCallbackCaptor.getValue().onSuccess(key, firstActivityTime);

        // second unreported event
        long secondActivityTime = 456L;
        when(strategyMock.onActivity()).thenReturn(false);
        doCallRealMethod().when(integrationServiceMock).onActivity(key, secondActivityTime);
        integrationServiceMock.onActivity(key, secondActivityTime);

        verify(integrationServiceMock, never()).reportActivity(eq(key), isNull(), eq(secondActivityTime), any());

        verify(strategyMock, times(2)).onActivity();

        long updatedLastRecordedTime = 500L;
        // lastReportedTime = firstActivityTime = 123L;
        ActivityState<Void> updatedState = new ActivityState<>();
        updatedState.setLastRecordedTime(updatedLastRecordedTime);

        when(integrationServiceMock.updateState(eq(key), any())).thenReturn(updatedState);
        when(integrationServiceMock.hasExpired(updatedLastRecordedTime)).thenReturn(false);
        when(strategyMock.onReportingPeriodEnd()).thenReturn(true);

        // WHEN
        doCallRealMethod().when(integrationServiceMock).onReportingPeriodEnd();
        integrationServiceMock.onReportingPeriodEnd();

        // THEN
        assertThat(states).containsKey(key);

        when(integrationServiceMock.getLastRecordedTime(key)).thenCallRealMethod();
        assertThat(integrationServiceMock.getLastRecordedTime(key)).isEqualTo(updatedLastRecordedTime);

        verify(integrationServiceMock, never()).onStateExpiry(any(), any());

        verify(strategyMock).onReportingPeriodEnd();
        verifyNoMoreInteractions(strategyMock);

        verify(integrationServiceMock).reportActivity(eq(key), isNull(), eq(updatedLastRecordedTime), any());
    }

    @Test
    void givenUpdatedStateIsNotNullAndHasNotExpiredAndStrategySaysThatShouldReportAndDoesNotHaveUnreportedEvents_whenOnReportingPeriodEnd_thenShouldNotReportAnything() {
        // GIVEN
        ConcurrentMap<IntegrationActivityKey, Object> states = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(integrationServiceMock, "states", states);

        var key = new IntegrationActivityKey(TENANT_ID, DEVICE_ID);
        ActivityStrategy strategyMock = mock(ActivityStrategy.class);

        // first reported event
        when(integrationServiceMock.createNewState(key)).thenReturn(new ActivityState<>());
        when(strategyMock.onActivity()).thenReturn(true);
        when(integrationServiceMock.getStrategy()).thenReturn(strategyMock);

        long activityTime = 123L;
        doCallRealMethod().when(integrationServiceMock).onActivity(key, activityTime);
        integrationServiceMock.onActivity(key, activityTime);

        ArgumentCaptor<ActivityReportCallback<IntegrationActivityKey>> firstCallbackCaptor = ArgumentCaptor.forClass(ActivityReportCallback.class);
        verify(integrationServiceMock).reportActivity(eq(key), isNull(), eq(activityTime), firstCallbackCaptor.capture());
        firstCallbackCaptor.getValue().onSuccess(key, activityTime);

        verify(strategyMock).onActivity();

        long updatedLastRecordedTime = 123L;
        // lastReportedTime = firstActivityTime = 123L;
        ActivityState<Void> updatedState = new ActivityState<>();
        updatedState.setLastRecordedTime(updatedLastRecordedTime);

        when(integrationServiceMock.updateState(eq(key), any())).thenReturn(updatedState);
        when(integrationServiceMock.hasExpired(updatedLastRecordedTime)).thenReturn(false);
        when(strategyMock.onReportingPeriodEnd()).thenReturn(true);

        // WHEN
        doCallRealMethod().when(integrationServiceMock).onReportingPeriodEnd();
        integrationServiceMock.onReportingPeriodEnd();

        // THEN
        assertThat(states).containsKey(key);

        when(integrationServiceMock.getLastRecordedTime(key)).thenCallRealMethod();
        assertThat(integrationServiceMock.getLastRecordedTime(key)).isEqualTo(updatedLastRecordedTime);

        verify(integrationServiceMock, never()).onStateExpiry(any(), any());

        verify(strategyMock).onReportingPeriodEnd();
        verifyNoMoreInteractions(strategyMock);

        verify(integrationServiceMock).reportActivity(eq(key), isNull(), eq(activityTime), any());
    }

    @Test
    void givenUpdatedStateIsNotNullAndHasNotExpiredAndStrategySaysThatShouldNotReportAndHasUnreportedEvents_whenOnReportingPeriodEnd_thenShouldNotReportAnything() {
        // GIVEN
        ConcurrentMap<IntegrationActivityKey, Object> states = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(integrationServiceMock, "states", states);

        var key = new IntegrationActivityKey(TENANT_ID, DEVICE_ID);
        ActivityStrategy strategyMock = mock(ActivityStrategy.class);

        // first reported event
        when(integrationServiceMock.createNewState(key)).thenReturn(new ActivityState<>());
        when(strategyMock.onActivity()).thenReturn(true);
        when(integrationServiceMock.getStrategy()).thenReturn(strategyMock);

        long firstActivityTime = 123L;
        doCallRealMethod().when(integrationServiceMock).onActivity(key, firstActivityTime);
        integrationServiceMock.onActivity(key, firstActivityTime);

        ArgumentCaptor<ActivityReportCallback<IntegrationActivityKey>> firstCallbackCaptor = ArgumentCaptor.forClass(ActivityReportCallback.class);
        verify(integrationServiceMock).reportActivity(eq(key), isNull(), eq(firstActivityTime), firstCallbackCaptor.capture());
        firstCallbackCaptor.getValue().onSuccess(key, firstActivityTime);

        // second unreported event
        long secondActivityTime = 456L;
        when(strategyMock.onActivity()).thenReturn(false);
        doCallRealMethod().when(integrationServiceMock).onActivity(key, secondActivityTime);
        integrationServiceMock.onActivity(key, secondActivityTime);

        verify(integrationServiceMock, never()).reportActivity(eq(key), isNull(), eq(secondActivityTime), any());

        verify(strategyMock, times(2)).onActivity();

        long updatedLastRecordedTime = 500L;
        // lastReportedTime = firstActivityTime = 123L;
        ActivityState<Void> updatedState = new ActivityState<>();
        updatedState.setLastRecordedTime(updatedLastRecordedTime);

        when(integrationServiceMock.updateState(eq(key), any())).thenReturn(updatedState);
        when(integrationServiceMock.hasExpired(updatedLastRecordedTime)).thenReturn(false);
        when(strategyMock.onReportingPeriodEnd()).thenReturn(false);

        // WHEN
        doCallRealMethod().when(integrationServiceMock).onReportingPeriodEnd();
        integrationServiceMock.onReportingPeriodEnd();

        // THEN
        assertThat(states).containsKey(key);

        when(integrationServiceMock.getLastRecordedTime(key)).thenCallRealMethod();
        assertThat(integrationServiceMock.getLastRecordedTime(key)).isEqualTo(updatedLastRecordedTime);

        verify(integrationServiceMock, never()).onStateExpiry(any(), any());

        verify(strategyMock).onReportingPeriodEnd();
        verifyNoMoreInteractions(strategyMock);

        verify(integrationServiceMock, never()).reportActivity(eq(key), isNull(), eq(updatedLastRecordedTime), any());
    }

    @Test
    void givenUpdatedStateIsNotNullAndHasExpiredAndStrategySaysThatShouldReportAndHasUnreportedEvents_whenOnReportingPeriodEnd_thenShouldReportEventUsingUpdatedState() {
        // GIVEN
        ConcurrentMap<IntegrationActivityKey, Object> states = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(integrationServiceMock, "states", states);

        var key = new IntegrationActivityKey(TENANT_ID, DEVICE_ID);
        ActivityStrategy strategyMock = mock(ActivityStrategy.class);

        // first reported event
        when(integrationServiceMock.createNewState(key)).thenReturn(new ActivityState<>());
        when(strategyMock.onActivity()).thenReturn(true);
        when(integrationServiceMock.getStrategy()).thenReturn(strategyMock);

        long firstActivityTime = 123L;
        doCallRealMethod().when(integrationServiceMock).onActivity(key, firstActivityTime);
        integrationServiceMock.onActivity(key, firstActivityTime);

        ArgumentCaptor<ActivityReportCallback<IntegrationActivityKey>> firstCallbackCaptor = ArgumentCaptor.forClass(ActivityReportCallback.class);
        verify(integrationServiceMock).reportActivity(eq(key), isNull(), eq(firstActivityTime), firstCallbackCaptor.capture());
        firstCallbackCaptor.getValue().onSuccess(key, firstActivityTime);

        // second unreported event
        long secondActivityTime = 456L;
        when(strategyMock.onActivity()).thenReturn(false);
        doCallRealMethod().when(integrationServiceMock).onActivity(key, secondActivityTime);
        integrationServiceMock.onActivity(key, secondActivityTime);

        verify(integrationServiceMock, never()).reportActivity(eq(key), isNull(), eq(secondActivityTime), any());

        verify(strategyMock, times(2)).onActivity();

        long updatedLastRecordedTime = 500L;
        // lastReportedTime = firstActivityTime = 123L;
        ActivityState<Void> updatedState = new ActivityState<>();
        updatedState.setLastRecordedTime(updatedLastRecordedTime);

        when(integrationServiceMock.updateState(eq(key), any())).thenReturn(updatedState);
        when(integrationServiceMock.hasExpired(updatedLastRecordedTime)).thenReturn(true);
        when(strategyMock.onReportingPeriodEnd()).thenReturn(true);

        // WHEN
        doCallRealMethod().when(integrationServiceMock).onReportingPeriodEnd();
        integrationServiceMock.onReportingPeriodEnd();

        // THEN
        assertThat(states).isEmpty();

        verify(integrationServiceMock).onStateExpiry(key, null);

        when(integrationServiceMock.getLastRecordedTime(key)).thenCallRealMethod();
        assertThat(integrationServiceMock.getLastRecordedTime(key)).isZero();

        verify(strategyMock).onReportingPeriodEnd();
        verifyNoMoreInteractions(strategyMock);

        verify(integrationServiceMock).reportActivity(eq(key), isNull(), eq(updatedLastRecordedTime), any());
    }

    @Test
    void givenUpdatedStateIsNotNullAndHasExpiredAndStrategySaysThatShouldNotReportAndHasUnreportedEvents_whenOnReportingPeriodEnd_thenShouldReportEventUsingUpdatedState() {
        // GIVEN
        ConcurrentMap<IntegrationActivityKey, Object> states = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(integrationServiceMock, "states", states);

        var key = new IntegrationActivityKey(TENANT_ID, DEVICE_ID);
        ActivityStrategy strategyMock = mock(ActivityStrategy.class);

        // first reported event
        when(integrationServiceMock.createNewState(key)).thenReturn(new ActivityState<>());
        when(strategyMock.onActivity()).thenReturn(true);
        when(integrationServiceMock.getStrategy()).thenReturn(strategyMock);

        long firstActivityTime = 123L;
        doCallRealMethod().when(integrationServiceMock).onActivity(key, firstActivityTime);
        integrationServiceMock.onActivity(key, firstActivityTime);

        ArgumentCaptor<ActivityReportCallback<IntegrationActivityKey>> firstCallbackCaptor = ArgumentCaptor.forClass(ActivityReportCallback.class);
        verify(integrationServiceMock).reportActivity(eq(key), isNull(), eq(firstActivityTime), firstCallbackCaptor.capture());
        firstCallbackCaptor.getValue().onSuccess(key, firstActivityTime);

        // second unreported event
        long secondActivityTime = 456L;
        when(strategyMock.onActivity()).thenReturn(false);
        doCallRealMethod().when(integrationServiceMock).onActivity(key, secondActivityTime);
        integrationServiceMock.onActivity(key, secondActivityTime);

        verify(integrationServiceMock, never()).reportActivity(eq(key), isNull(), eq(secondActivityTime), any());

        verify(strategyMock, times(2)).onActivity();

        long updatedLastRecordedTime = 500L;
        // lastReportedTime = firstActivityTime = 123L;
        ActivityState<Void> updatedState = new ActivityState<>();
        updatedState.setLastRecordedTime(updatedLastRecordedTime);

        when(integrationServiceMock.updateState(eq(key), any())).thenReturn(updatedState);
        when(integrationServiceMock.hasExpired(updatedLastRecordedTime)).thenReturn(true);
        when(strategyMock.onReportingPeriodEnd()).thenReturn(false);

        // WHEN
        doCallRealMethod().when(integrationServiceMock).onReportingPeriodEnd();
        integrationServiceMock.onReportingPeriodEnd();

        // THEN
        assertThat(states).isEmpty();

        verify(integrationServiceMock).onStateExpiry(key, null);

        when(integrationServiceMock.getLastRecordedTime(key)).thenCallRealMethod();
        assertThat(integrationServiceMock.getLastRecordedTime(key)).isZero();

        verify(strategyMock).onReportingPeriodEnd();
        verifyNoMoreInteractions(strategyMock);

        verify(integrationServiceMock).reportActivity(eq(key), isNull(), eq(updatedLastRecordedTime), any());
    }

    // TODO: onReportingPeriodEnd() test for updating reported time

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
        long expectedTime = 123L;
        when(integrationServiceMock.getCurrentTimeMillis()).thenReturn(expectedTime);

        // WHEN
        integrationServiceMock.process(sessionInfo, postTelemetryMsg, null);

        // THEN
        verify(integrationServiceMock).onActivity(key, expectedTime);
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

        long activityTime = 123L;
        when(integrationServiceMock.getCurrentTimeMillis()).thenReturn(activityTime);

        // WHEN
        integrationServiceMock.process(sessionInfo, postAttributeMsg, null);

        // THEN
        verify(integrationServiceMock).onActivity(key, activityTime);
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
