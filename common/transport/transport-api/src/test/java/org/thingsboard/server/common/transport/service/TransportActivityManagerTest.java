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
package org.thingsboard.server.common.transport.service;

import org.junit.jupiter.api.BeforeEach;
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
import org.thingsboard.server.common.transport.SessionMsgListener;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.activity.ActivityReportCallback;
import org.thingsboard.server.common.transport.activity.ActivityState;
import org.thingsboard.server.common.transport.activity.strategy.ActivityStrategy;
import org.thingsboard.server.common.transport.activity.strategy.ActivityStrategyType;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SESSION_EVENT_MSG_CLOSED;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SESSION_EXPIRED_NOTIFICATION_PROTO;

@ExtendWith(MockitoExtension.class)
public class TransportActivityManagerTest {

    private final UUID SESSION_ID = UUID.fromString("1306648a-9b26-11ee-b9d1-0242ac120002");

    @Mock
    private DefaultTransportService transportServiceMock;
    private ConcurrentMap<UUID, SessionMetaData> sessions;

    @BeforeEach
    public void setup() {
        sessions = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(transportServiceMock, "sessions", sessions);
    }

    @Test
    void givenFirstActivityForAlreadyRemovedSessionAndFirstEventReportingStrategy_whenOnActivity_thenShouldRecordActivityAndReport() {
        // GIVEN
        ConcurrentMap<UUID, Object> states = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(transportServiceMock, "states", states);

        var strategyMock = mock(ActivityStrategy.class);
        when(transportServiceMock.getStrategy()).thenReturn(strategyMock);
        when(strategyMock.onActivity()).thenReturn(true);

        long activityTime = 123L;
        var sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .build();

        doCallRealMethod().when(transportServiceMock).getLastRecordedTime(SESSION_ID);
        doCallRealMethod().when(transportServiceMock).onActivity(SESSION_ID, sessionInfo, activityTime);

        // WHEN
        transportServiceMock.onActivity(SESSION_ID, sessionInfo, activityTime);

        // THEN
        assertThat(states).containsKey(SESSION_ID);
        assertThat(transportServiceMock.getLastRecordedTime(SESSION_ID)).isEqualTo(activityTime);
        verify(transportServiceMock).reportActivity(eq(SESSION_ID), eq(sessionInfo), eq(activityTime), any(ActivityReportCallback.class));
    }

    @Test
    void givenKeyAndTimeToReportAndSessionExists_whenReportingActivity_thenShouldReportActivityWithSubscriptionsAndSessionInfoFromSession() {
        // GIVEN
        long expectedTime = 123L;
        boolean expectedAttributesSubscription = true;
        boolean expectedRPCSubscription = true;
        TransportProtos.SessionInfoProto expectedSessionInfo = TransportProtos.SessionInfoProto.getDefaultInstance();

        SessionMsgListener listenerMock = mock(SessionMsgListener.class);
        SessionMetaData session = new SessionMetaData(expectedSessionInfo, TransportProtos.SessionType.ASYNC, listenerMock);
        session.setSubscribedToAttributes(expectedAttributesSubscription);
        session.setSubscribedToRPC(expectedRPCSubscription);
        sessions.put(SESSION_ID, session);

        ActivityReportCallback<UUID> callbackMock = mock(ActivityReportCallback.class);

        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .build();

        doCallRealMethod().when(transportServiceMock).reportActivity(SESSION_ID, sessionInfo, expectedTime, callbackMock);

        // WHEN
        transportServiceMock.reportActivity(SESSION_ID, sessionInfo, expectedTime, callbackMock);

        // THEN
        ArgumentCaptor<TransportProtos.SessionInfoProto> sessionInfoCaptor = ArgumentCaptor.forClass(TransportProtos.SessionInfoProto.class);
        ArgumentCaptor<TransportProtos.SubscriptionInfoProto> subscriptionInfoCaptor = ArgumentCaptor.forClass(TransportProtos.SubscriptionInfoProto.class);
        ArgumentCaptor<TransportServiceCallback<Void>> callbackCaptor = ArgumentCaptor.forClass(TransportServiceCallback.class);

        verify(transportServiceMock).process(sessionInfoCaptor.capture(), subscriptionInfoCaptor.capture(), callbackCaptor.capture());

        assertThat(sessionInfoCaptor.getValue()).isEqualTo(expectedSessionInfo);

        TransportProtos.SubscriptionInfoProto expectedSubscriptionInfo = TransportProtos.SubscriptionInfoProto.newBuilder()
                .setAttributeSubscription(expectedAttributesSubscription)
                .setRpcSubscription(expectedRPCSubscription)
                .setLastActivityTime(expectedTime)
                .build();
        assertThat(subscriptionInfoCaptor.getValue()).isEqualTo(expectedSubscriptionInfo);

        TransportServiceCallback<Void> queueCallback = callbackCaptor.getValue();

        queueCallback.onSuccess(null);
        verify(callbackMock).onSuccess(SESSION_ID, expectedTime);

        var throwable = new Throwable();
        queueCallback.onError(throwable);
        verify(callbackMock).onFailure(SESSION_ID, throwable);
    }

    @Test
    void givenKeyAndTimeToReportAndSessionDoesNotExist_whenReportingActivity_thenShouldReportActivityWithNoSubscriptionsAndPreviousSessionInfo() {
        // GIVEN
        long expectedTime = 123L;
        boolean expectedAttributesSubscription = false;
        boolean expectedRPCSubscription = false;
        TransportProtos.SessionInfoProto expectedSessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .build();

        ActivityReportCallback<UUID> callbackMock = mock(ActivityReportCallback.class);

        doCallRealMethod().when(transportServiceMock).reportActivity(SESSION_ID, expectedSessionInfo, expectedTime, callbackMock);

        // WHEN
        transportServiceMock.reportActivity(SESSION_ID, expectedSessionInfo, expectedTime, callbackMock);

        // THEN
        ArgumentCaptor<TransportProtos.SessionInfoProto> sessionInfoCaptor = ArgumentCaptor.forClass(TransportProtos.SessionInfoProto.class);
        ArgumentCaptor<TransportProtos.SubscriptionInfoProto> subscriptionInfoCaptor = ArgumentCaptor.forClass(TransportProtos.SubscriptionInfoProto.class);
        ArgumentCaptor<TransportServiceCallback<Void>> callbackCaptor = ArgumentCaptor.forClass(TransportServiceCallback.class);

        verify(transportServiceMock).process(sessionInfoCaptor.capture(), subscriptionInfoCaptor.capture(), callbackCaptor.capture());

        assertThat(sessionInfoCaptor.getValue()).isEqualTo(expectedSessionInfo);

        TransportProtos.SubscriptionInfoProto expectedSubscriptionInfo = TransportProtos.SubscriptionInfoProto.newBuilder()
                .setAttributeSubscription(expectedAttributesSubscription)
                .setRpcSubscription(expectedRPCSubscription)
                .setLastActivityTime(expectedTime)
                .build();
        assertThat(subscriptionInfoCaptor.getValue()).isEqualTo(expectedSubscriptionInfo);

        TransportServiceCallback<Void> queueCallback = callbackCaptor.getValue();

        queueCallback.onSuccess(null);
        verify(callbackMock).onSuccess(SESSION_ID, expectedTime);

        var throwable = new Throwable();
        queueCallback.onError(throwable);
        verify(callbackMock).onFailure(SESSION_ID, throwable);
    }

    @Test
    void givenActivityHappened_whenRecordActivity_thenShouldDelegateToOnActivity() {
        // GIVEN
        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .build();
        doCallRealMethod().when(transportServiceMock).recordActivity(sessionInfo);
        when(transportServiceMock.toSessionId(sessionInfo)).thenReturn(SESSION_ID);
        long expectedTime = 123L;
        when(transportServiceMock.getCurrentTimeMillis()).thenReturn(expectedTime);

        // WHEN
        transportServiceMock.recordActivity(sessionInfo);

        // THEN
        verify(transportServiceMock).onActivity(SESSION_ID, sessionInfo, expectedTime);
    }

    @ParameterizedTest
    @EnumSource(ActivityStrategyType.class)
    void givenDifferentReportingStrategies_whenGettingStrategy_thenShouldReturnCorrectStrategy(ActivityStrategyType reportingStrategyType) {
        // GIVEN
        doCallRealMethod().when(transportServiceMock).getStrategy();
        ReflectionTestUtils.setField(transportServiceMock, "reportingStrategyType", reportingStrategyType);

        // WHEN
        ActivityStrategy actualStrategy = transportServiceMock.getStrategy();

        // THEN
        assertThat(actualStrategy).isEqualTo(reportingStrategyType.toStrategy());
    }

    @Test
    void givenSessionDoesNotExist_whenUpdatingActivityState_thenShouldReturnNull() {
        // GIVEN
        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .build();

        ActivityState<TransportProtos.SessionInfoProto> state = new ActivityState<>();
        state.setLastRecordedTime(123L);
        state.setMetadata(sessionInfo);

        when(transportServiceMock.updateState(SESSION_ID, state)).thenCallRealMethod();

        // WHEN
        ActivityState<TransportProtos.SessionInfoProto> updatedState = transportServiceMock.updateState(SESSION_ID, state);

        // THEN
        assertThat(updatedState).isNull();
    }

    @Test
    void givenNoGwSessionId_whenUpdatingActivityState_thenShouldReturnSameInstanceWithUpdatedSessionInfo() {
        // GIVEN
        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .build();
        SessionMsgListener listenerMock = mock(SessionMsgListener.class);
        sessions.put(SESSION_ID, new SessionMetaData(sessionInfo, TransportProtos.SessionType.ASYNC, listenerMock));

        long lastRecordedTime = 123L;

        ActivityState<TransportProtos.SessionInfoProto> state = new ActivityState<>();
        state.setLastRecordedTime(lastRecordedTime);
        state.setMetadata(TransportProtos.SessionInfoProto.getDefaultInstance());

        when(transportServiceMock.updateState(SESSION_ID, state)).thenCallRealMethod();

        // WHEN
        ActivityState<TransportProtos.SessionInfoProto> updatedState = transportServiceMock.updateState(SESSION_ID, state);

        // THEN
        assertThat(updatedState).isSameAs(state);
        assertThat(updatedState.getLastRecordedTime()).isEqualTo(lastRecordedTime);
        assertThat(updatedState.getMetadata()).isEqualTo(sessionInfo);
    }

    @Test
    void givenHasGwSessionIdButGwSessionIsNotNull_whenUpdatingActivityState_thenShouldReturnSameInstanceWithUpdatedSessionInfo() {
        // GIVEN
        var gwSessionId = UUID.fromString("19864038-9b48-11ee-b9d1-0242ac120002");
        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .setGwSessionIdMSB(gwSessionId.getMostSignificantBits())
                .setGwSessionIdLSB(gwSessionId.getLeastSignificantBits())
                .build();
        SessionMsgListener listenerMock = mock(SessionMsgListener.class);
        sessions.put(SESSION_ID, new SessionMetaData(sessionInfo, TransportProtos.SessionType.ASYNC, listenerMock));

        long lastRecordedTime = 123L;

        ActivityState<TransportProtos.SessionInfoProto> state = new ActivityState<>();
        state.setLastRecordedTime(lastRecordedTime);
        state.setMetadata(TransportProtos.SessionInfoProto.getDefaultInstance());

        when(transportServiceMock.updateState(SESSION_ID, state)).thenCallRealMethod();

        // WHEN
        ActivityState<TransportProtos.SessionInfoProto> updatedState = transportServiceMock.updateState(SESSION_ID, state);

        // THEN
        assertThat(updatedState).isSameAs(state);
        assertThat(updatedState.getLastRecordedTime()).isEqualTo(lastRecordedTime);
        assertThat(updatedState.getMetadata()).isEqualTo(sessionInfo);

        verify(transportServiceMock, never()).getLastRecordedTime(gwSessionId);
    }

    @Test
    void givenHasGwSessionWithoutOverwriteEnabled_whenUpdatingActivityState_thenShouldReturnSameInstanceWithUpdatedSessionInfo() {
        // GIVEN
        var gwSessionId = UUID.fromString("19864038-9b48-11ee-b9d1-0242ac120002");
        TransportProtos.SessionInfoProto gwSessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(gwSessionId.getMostSignificantBits())
                .setSessionIdLSB(gwSessionId.getLeastSignificantBits())
                .build();
        SessionMsgListener gwListenerMock = mock(SessionMsgListener.class);
        sessions.put(gwSessionId, new SessionMetaData(gwSessionInfo, TransportProtos.SessionType.ASYNC, gwListenerMock));

        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .setGwSessionIdMSB(gwSessionId.getMostSignificantBits())
                .setGwSessionIdLSB(gwSessionId.getLeastSignificantBits())
                .build();
        SessionMsgListener listenerMock = mock(SessionMsgListener.class);
        sessions.put(SESSION_ID, new SessionMetaData(sessionInfo, TransportProtos.SessionType.ASYNC, listenerMock));

        long lastRecordedTime = 123L;

        ActivityState<TransportProtos.SessionInfoProto> state = new ActivityState<>();
        state.setLastRecordedTime(lastRecordedTime);
        state.setMetadata(TransportProtos.SessionInfoProto.getDefaultInstance());

        when(transportServiceMock.updateState(SESSION_ID, state)).thenCallRealMethod();

        // WHEN
        ActivityState<TransportProtos.SessionInfoProto> updatedState = transportServiceMock.updateState(SESSION_ID, state);

        // THEN
        assertThat(updatedState).isSameAs(state);
        assertThat(updatedState.getLastRecordedTime()).isEqualTo(lastRecordedTime);
        assertThat(updatedState.getMetadata()).isEqualTo(sessionInfo);

        verify(transportServiceMock, never()).getLastRecordedTime(gwSessionId);
    }

    @Test
    void givenHasGwSessionWithOverwriteEnabledAndGwLastRecordedTimeIsGreater_whenUpdatingActivityState_thenShouldReturnSameInstanceWithUpdatedSessionInfoAndLastRecordedTime() {
        // GIVEN
        var gwSessionId = UUID.fromString("19864038-9b48-11ee-b9d1-0242ac120002");
        TransportProtos.SessionInfoProto gwSessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(gwSessionId.getMostSignificantBits())
                .setSessionIdLSB(gwSessionId.getLeastSignificantBits())
                .build();
        SessionMsgListener gwListenerMock = mock(SessionMsgListener.class);
        SessionMetaData gwSession = new SessionMetaData(gwSessionInfo, TransportProtos.SessionType.ASYNC, gwListenerMock);
        gwSession.setOverwriteActivityTime(true);
        sessions.put(gwSessionId, gwSession);

        long gwLastRecordedTime = 500L;
        when(transportServiceMock.getLastRecordedTime(gwSessionId)).thenReturn(gwLastRecordedTime);

        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .setGwSessionIdMSB(gwSessionId.getMostSignificantBits())
                .setGwSessionIdLSB(gwSessionId.getLeastSignificantBits())
                .build();
        SessionMsgListener listenerMock = mock(SessionMsgListener.class);
        sessions.put(SESSION_ID, new SessionMetaData(sessionInfo, TransportProtos.SessionType.ASYNC, listenerMock));

        long lastRecordedTime = 123L;

        ActivityState<TransportProtos.SessionInfoProto> state = new ActivityState<>();
        state.setLastRecordedTime(lastRecordedTime);
        state.setMetadata(TransportProtos.SessionInfoProto.getDefaultInstance());

        when(transportServiceMock.updateState(SESSION_ID, state)).thenCallRealMethod();

        // WHEN
        ActivityState<TransportProtos.SessionInfoProto> updatedState = transportServiceMock.updateState(SESSION_ID, state);

        // THEN
        assertThat(updatedState).isSameAs(state);
        assertThat(updatedState.getLastRecordedTime()).isEqualTo(gwLastRecordedTime);
        assertThat(updatedState.getMetadata()).isEqualTo(sessionInfo);
    }

    @Test
    void givenHasGwSessionWithOverwriteEnabledAndGwLastRecordedTimeIsLess_whenUpdatingActivityState_thenShouldReturnSameInstanceWithUpdatedSessionInfoOnly() {
        // GIVEN
        var gwSessionId = UUID.fromString("19864038-9b48-11ee-b9d1-0242ac120002");
        TransportProtos.SessionInfoProto gwSessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(gwSessionId.getMostSignificantBits())
                .setSessionIdLSB(gwSessionId.getLeastSignificantBits())
                .build();
        SessionMsgListener gwListenerMock = mock(SessionMsgListener.class);
        SessionMetaData gwSession = new SessionMetaData(gwSessionInfo, TransportProtos.SessionType.ASYNC, gwListenerMock);
        gwSession.setOverwriteActivityTime(true);
        sessions.put(gwSessionId, gwSession);

        long gwLastRecordedTime = 100L;
        when(transportServiceMock.getLastRecordedTime(gwSessionId)).thenReturn(gwLastRecordedTime);

        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .setGwSessionIdMSB(gwSessionId.getMostSignificantBits())
                .setGwSessionIdLSB(gwSessionId.getLeastSignificantBits())
                .build();
        SessionMsgListener listenerMock = mock(SessionMsgListener.class);
        sessions.put(SESSION_ID, new SessionMetaData(sessionInfo, TransportProtos.SessionType.ASYNC, listenerMock));

        long lastRecordedTime = 123L;

        ActivityState<TransportProtos.SessionInfoProto> state = new ActivityState<>();
        state.setLastRecordedTime(lastRecordedTime);
        state.setMetadata(TransportProtos.SessionInfoProto.getDefaultInstance());

        when(transportServiceMock.updateState(SESSION_ID, state)).thenCallRealMethod();

        // WHEN
        ActivityState<TransportProtos.SessionInfoProto> updatedState = transportServiceMock.updateState(SESSION_ID, state);

        // THEN
        assertThat(updatedState).isSameAs(state);
        assertThat(updatedState.getLastRecordedTime()).isEqualTo(lastRecordedTime);
        assertThat(updatedState.getMetadata()).isEqualTo(sessionInfo);
    }

    @ParameterizedTest
    @MethodSource("provideTestParamsForHasExpiredTrue")
    public void givenExpiredLastRecordedTime_whenCheckingForExpiry_thenShouldReturnTrue(long currentTimeMillis, long lastRecordedTime, long sessionInactivityTimeout) {
        // GIVEN
        ReflectionTestUtils.setField(transportServiceMock, "sessionInactivityTimeout", sessionInactivityTimeout);

        when(transportServiceMock.getCurrentTimeMillis()).thenReturn(currentTimeMillis);
        when(transportServiceMock.hasExpired(lastRecordedTime)).thenCallRealMethod();

        // WHEN
        boolean hasExpired = transportServiceMock.hasExpired(lastRecordedTime);

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
    public void givenNotExpiredLastRecordedTime_whenCheckingForExpiry_thenShouldReturnFalse(long currentTimeMillis, long lastRecordedTime, long sessionInactivityTimeout) {
        // GIVEN
        ReflectionTestUtils.setField(transportServiceMock, "sessionInactivityTimeout", sessionInactivityTimeout);

        when(transportServiceMock.getCurrentTimeMillis()).thenReturn(currentTimeMillis);
        when(transportServiceMock.hasExpired(lastRecordedTime)).thenCallRealMethod();

        // WHEN
        boolean hasExpired = transportServiceMock.hasExpired(lastRecordedTime);

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
    void givenSessionExists_whenOnStateExpiryCalled_thenShouldPerformExpirationActions() {
        // GIVEN
        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .build();
        SessionMsgListener listenerMock = mock(SessionMsgListener.class);
        sessions.put(SESSION_ID, new SessionMetaData(sessionInfo, TransportProtos.SessionType.ASYNC, listenerMock));
        doCallRealMethod().when(transportServiceMock).onStateExpiry(SESSION_ID, sessionInfo);

        // WHEN
        transportServiceMock.onStateExpiry(SESSION_ID, sessionInfo);

        // THEN
        assertThat(sessions.containsKey(SESSION_ID)).isFalse();
        verify(transportServiceMock).deregisterSession(sessionInfo);
        verify(transportServiceMock).process(sessionInfo, SESSION_EVENT_MSG_CLOSED, null);
        verify(listenerMock).onRemoteSessionCloseCommand(SESSION_ID, SESSION_EXPIRED_NOTIFICATION_PROTO);
    }

    @Test
    void givenSessionDoesNotExist_whenOnStateExpiryCalled_thenShouldNotPerformExpirationActions() {
        // GIVEN
        TransportProtos.SessionInfoProto sessionInfo = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(SESSION_ID.getMostSignificantBits())
                .setSessionIdLSB(SESSION_ID.getLeastSignificantBits())
                .build();
        doCallRealMethod().when(transportServiceMock).onStateExpiry(SESSION_ID, sessionInfo);

        // WHEN
        transportServiceMock.onStateExpiry(SESSION_ID, sessionInfo);

        // THEN
        assertThat(sessions.containsKey(SESSION_ID)).isFalse();
        verify(transportServiceMock, never()).deregisterSession(sessionInfo);
        verify(transportServiceMock, never()).process(sessionInfo, SESSION_EVENT_MSG_CLOSED, null);
    }

}
