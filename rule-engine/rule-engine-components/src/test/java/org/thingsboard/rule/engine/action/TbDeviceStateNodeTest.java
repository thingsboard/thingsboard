/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.rule.engine.action;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleEngineDeviceStateManager;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
public class TbDeviceStateNodeTest {

    @Mock
    private TbContext ctxMock;
    @Mock
    private static RuleEngineDeviceStateManager deviceStateManagerMock;
    @Captor
    private static ArgumentCaptor<TbCallback> callbackCaptor;
    private TbDeviceStateNode node;
    private RuleNodeId nodeId;
    private TbDeviceStateNodeConfiguration config;

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final DeviceId DEVICE_ID = new DeviceId(UUID.randomUUID());
    private static final long METADATA_TS = 123L;
    private TbMsg cleanupMsg;
    private TbMsg msg;
    private long nowNanos;
    private final Ticker controlledTicker = new Ticker() {
        @Override
        public long read() {
            return nowNanos;
        }
    };

    @BeforeEach
    public void setup() {
        var metaData = new TbMsgMetaData();
        metaData.putValue("deviceName", "My humidity sensor");
        metaData.putValue("deviceType", "Humidity sensor");
        metaData.putValue("ts", String.valueOf(METADATA_TS));
        var data = JacksonUtil.newObjectNode();
        data.put("humidity", 58.3);
        msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, metaData, JacksonUtil.toString(data));
        nodeId = new RuleNodeId(UUID.randomUUID());
        cleanupMsg = TbMsg.newMsg(null, TbMsgType.DEVICE_STATE_STALE_ENTRIES_CLEANUP_SELF_MSG, nodeId, TbMsgMetaData.EMPTY, TbMsg.EMPTY_STRING);
    }

    @BeforeEach
    public void setUp() {
        node = new TbDeviceStateNode();
        config = new TbDeviceStateNodeConfiguration().defaultConfiguration();
    }

    @Test
    public void givenDefaultConfiguration_whenInvoked_thenCorrectValuesAreSet() {
        assertThat(config.getEvent()).isEqualTo(TbMsgType.ACTIVITY_EVENT);
    }

    @Test
    public void givenNullEventInConfig_whenInit_thenThrowsUnrecoverableTbNodeException() {
        // GIVEN-WHEN-THEN
        assertThatThrownBy(() -> initNode(null))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("Event cannot be null!")
                .matches(e -> ((TbNodeException) e).isUnrecoverable());
    }

    @Test
    public void givenValidConfig_whenInit_thenSchedulesCleanupMsg() {
        // GIVEN
        given(ctxMock.getSelfId()).willReturn(nodeId);
        given(ctxMock.newMsg(isNull(), eq(TbMsgType.DEVICE_STATE_STALE_ENTRIES_CLEANUP_SELF_MSG), eq(nodeId), eq(TbMsgMetaData.EMPTY), eq(TbMsg.EMPTY_STRING))).willReturn(cleanupMsg);

        // WHEN
        try {
            initNode(TbMsgType.ACTIVITY_EVENT);
        } catch (Exception e) {
            fail("Node failed to initialize.");
        }

        // THEN
        verifyCleanupMsgSent();
    }

    @Test
    public void givenCleanupMsg_whenOnMsg_thenCleansStaleEntries() {
        // GIVEN
        given(ctxMock.getSelfId()).willReturn(nodeId);
        given(ctxMock.newMsg(isNull(), eq(TbMsgType.DEVICE_STATE_STALE_ENTRIES_CLEANUP_SELF_MSG), eq(nodeId), eq(TbMsgMetaData.EMPTY), eq(TbMsg.EMPTY_STRING))).willReturn(cleanupMsg);

        ConcurrentMap<DeviceId, Duration> lastActivityEventTimestamps = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(node, "lastActivityEventTimestamps", lastActivityEventTimestamps);

        Stopwatch stopwatch = Stopwatch.createStarted(controlledTicker);
        ReflectionTestUtils.setField(node, "stopwatch", stopwatch);

        // WHEN
        Duration expirationTime = Duration.ofDays(1L);

        DeviceId staleId = DEVICE_ID;
        Duration staleTs = Duration.ofHours(4L);
        lastActivityEventTimestamps.put(staleId, staleTs);

        DeviceId goodId = new DeviceId(UUID.randomUUID());
        Duration goodTs = staleTs.plus(expirationTime);
        lastActivityEventTimestamps.put(goodId, goodTs);

        nowNanos = staleTs.toNanos() + expirationTime.toNanos() + 1;
        node.onMsg(ctxMock, cleanupMsg);

        // THEN
        assertThat(lastActivityEventTimestamps)
                .containsKey(goodId)
                .doesNotContainKey(staleId)
                .size().isOne();

        verifyCleanupMsgSent();
        then(ctxMock).shouldHaveNoMoreInteractions();
    }

    @Test
    public void givenMsgArrivedTooFast_whenOnMsg_thenRateLimitsThisMsg() {
        // GIVEN
        ConcurrentMap<DeviceId, Duration> lastActivityEventTimestamps = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(node, "lastActivityEventTimestamps", lastActivityEventTimestamps);

        Stopwatch stopwatch = Stopwatch.createStarted(controlledTicker);
        ReflectionTestUtils.setField(node, "stopwatch", stopwatch);

        // WHEN
        Duration firstEventTs = Duration.ofMillis(1000L);
        lastActivityEventTimestamps.put(DEVICE_ID, firstEventTs);

        Duration tooFastEventTs = firstEventTs.plus(Duration.ofMillis(999L));
        nowNanos = tooFastEventTs.toNanos();
        node.onMsg(ctxMock, msg);

        // THEN
        Duration actualEventTs = lastActivityEventTimestamps.get(DEVICE_ID);
        assertThat(actualEventTs).isEqualTo(firstEventTs);

        then(ctxMock).should().tellSuccess(msg);
        then(ctxMock).shouldHaveNoMoreInteractions();
        then(deviceStateManagerMock).shouldHaveNoInteractions();
    }

    @Test
    public void givenHasNonLocalDevices_whenOnPartitionChange_thenRemovesEntriesForNonLocalDevices() {
        // GIVEN
        ConcurrentMap<DeviceId, Duration> lastActivityEventTimestamps = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(node, "lastActivityEventTimestamps", lastActivityEventTimestamps);

        lastActivityEventTimestamps.put(DEVICE_ID, Duration.ofHours(24L));
        given(ctxMock.isLocalEntity(eq(DEVICE_ID))).willReturn(true);

        DeviceId nonLocalDeviceId1 = new DeviceId(UUID.randomUUID());
        lastActivityEventTimestamps.put(nonLocalDeviceId1, Duration.ofHours(30L));
        given(ctxMock.isLocalEntity(eq(nonLocalDeviceId1))).willReturn(false);

        DeviceId nonLocalDeviceId2 = new DeviceId(UUID.randomUUID());
        lastActivityEventTimestamps.put(nonLocalDeviceId2, Duration.ofHours(32L));
        given(ctxMock.isLocalEntity(eq(nonLocalDeviceId2))).willReturn(false);

        // WHEN
        node.onPartitionChangeMsg(ctxMock, new PartitionChangeMsg(ServiceType.TB_RULE_ENGINE));

        // THEN
        assertThat(lastActivityEventTimestamps)
                .containsKey(DEVICE_ID)
                .doesNotContainKey(nonLocalDeviceId1)
                .doesNotContainKey(nonLocalDeviceId2)
                .size().isOne();
    }

    @ParameterizedTest
    @EnumSource(
            value = TbMsgType.class,
            names = {"CONNECT_EVENT", "ACTIVITY_EVENT", "DISCONNECT_EVENT", "INACTIVITY_EVENT"},
            mode = EnumSource.Mode.EXCLUDE
    )
    public void givenUnsupportedEventInConfig_whenInit_thenThrowsUnrecoverableTbNodeException(TbMsgType unsupportedEvent) {
        // GIVEN-WHEN-THEN
        assertThatThrownBy(() -> initNode(unsupportedEvent))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("Unsupported event: " + unsupportedEvent)
                .matches(e -> ((TbNodeException) e).isUnrecoverable());
    }

    @ParameterizedTest
    @EnumSource(value = EntityType.class, names = "DEVICE", mode = EnumSource.Mode.EXCLUDE)
    public void givenNonDeviceOriginator_whenOnMsg_thenTellsSuccessAndNoActivityActionsTriggered(EntityType unsupportedType) {
        // GIVEN
        var nonDeviceOriginator = new EntityId() {

            @Override
            public UUID getId() {
                return UUID.randomUUID();
            }

            @Override
            public EntityType getEntityType() {
                return unsupportedType;
            }
        };
        var msg = TbMsg.newMsg(TbMsgType.ENTITY_CREATED, nonDeviceOriginator, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        then(ctxMock).should().tellSuccess(msg);
        then(ctxMock).shouldHaveNoMoreInteractions();
    }

    @Test
    public void givenMetadataDoesNotContainTs_whenOnMsg_thenMsgTsIsUsedAsEventTs() {
        // GIVEN
        try {
            initNode(TbMsgType.ACTIVITY_EVENT);
        } catch (TbNodeException e) {
            fail("Node failed to initialize!", e);
        }

        given(ctxMock.getTenantId()).willReturn(TENANT_ID);
        given(ctxMock.getDeviceStateManager()).willReturn(deviceStateManagerMock);

        long msgTs = METADATA_TS + 1;
        msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT, msgTs);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        then(deviceStateManagerMock).should().onDeviceActivity(eq(TENANT_ID), eq(DEVICE_ID), eq(msgTs), any());
    }

    @ParameterizedTest
    @MethodSource
    public void givenSupportedEventAndDeviceOriginator_whenOnMsg_thenCorrectEventIsSentWithCorrectCallback(TbMsgType supportedEventType, Runnable actionVerification) {
        // GIVEN
        given(ctxMock.getSelfId()).willReturn(nodeId);
        given(ctxMock.newMsg(isNull(), eq(TbMsgType.DEVICE_STATE_STALE_ENTRIES_CLEANUP_SELF_MSG), eq(nodeId), eq(TbMsgMetaData.EMPTY), eq(TbMsg.EMPTY_STRING))).willReturn(cleanupMsg);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);
        given(ctxMock.getDeviceStateManager()).willReturn(deviceStateManagerMock);

        try {
            initNode(supportedEventType);
        } catch (TbNodeException e) {
            fail("Node failed to initialize!", e);
        }
        verifyCleanupMsgSent();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        actionVerification.run();

        TbCallback actualCallback = callbackCaptor.getValue();

        actualCallback.onSuccess();
        then(ctxMock).should().tellSuccess(msg);

        var throwable = new Throwable();
        actualCallback.onFailure(throwable);
        then(ctxMock).should().tellFailure(msg, throwable);


        then(deviceStateManagerMock).shouldHaveNoMoreInteractions();
        then(ctxMock).shouldHaveNoMoreInteractions();
    }

    private static Stream<Arguments> givenSupportedEventAndDeviceOriginator_whenOnMsg_thenCorrectEventIsSentWithCorrectCallback() {
        return Stream.of(
                Arguments.of(TbMsgType.CONNECT_EVENT, (Runnable) () -> then(deviceStateManagerMock).should().onDeviceConnect(eq(TENANT_ID), eq(DEVICE_ID), eq(METADATA_TS), callbackCaptor.capture())),
                Arguments.of(TbMsgType.ACTIVITY_EVENT, (Runnable) () -> then(deviceStateManagerMock).should().onDeviceActivity(eq(TENANT_ID), eq(DEVICE_ID), eq(METADATA_TS), callbackCaptor.capture())),
                Arguments.of(TbMsgType.DISCONNECT_EVENT, (Runnable) () -> then(deviceStateManagerMock).should().onDeviceDisconnect(eq(TENANT_ID), eq(DEVICE_ID), eq(METADATA_TS), callbackCaptor.capture())),
                Arguments.of(TbMsgType.INACTIVITY_EVENT, (Runnable) () -> then(deviceStateManagerMock).should().onDeviceInactivity(eq(TENANT_ID), eq(DEVICE_ID), eq(METADATA_TS), callbackCaptor.capture()))
        );
    }

    private void initNode(TbMsgType event) throws TbNodeException {
        config.setEvent(event);
        var nodeConfig = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfig);
    }

    private void verifyCleanupMsgSent() {
        then(ctxMock).should().getSelfId();
        then(ctxMock).should().newMsg(isNull(), eq(TbMsgType.DEVICE_STATE_STALE_ENTRIES_CLEANUP_SELF_MSG), eq(nodeId), eq(TbMsgMetaData.EMPTY), eq(TbMsg.EMPTY_STRING));
        then(ctxMock).should().tellSelf(eq(cleanupMsg), eq(Duration.ofHours(1L).toMillis()));
    }

}
