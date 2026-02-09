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
package org.thingsboard.rule.engine.action;

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
import org.springframework.util.ConcurrentReferenceHashMap;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.DeviceStateManager;
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
import org.thingsboard.server.common.msg.tools.TbRateLimits;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
public class TbDeviceStateNodeTest {

    @Mock
    private TbContext ctxMock;
    @Mock
    private DeviceStateManager deviceStateManagerMock;
    @Captor
    private ArgumentCaptor<TbCallback> callbackCaptor;
    private TbDeviceStateNode node;
    private TbDeviceStateNodeConfiguration config;

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final DeviceId DEVICE_ID = new DeviceId(UUID.randomUUID());
    private static final long METADATA_TS = 123L;
    private TbMsg msg;

    @BeforeEach
    public void setup() {
        var metaData = new TbMsgMetaData();
        metaData.putValue("deviceName", "My humidity sensor");
        metaData.putValue("deviceType", "Humidity sensor");
        metaData.putValue("ts", String.valueOf(METADATA_TS));
        var data = JacksonUtil.newObjectNode();
        data.put("humidity", 58.3);
        msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(metaData)
                .data(JacksonUtil.toString(data))
                .build();
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
    public void givenInvalidRateLimitConfig_whenInit_thenUsesDefaultConfig() {
        // GIVEN
        given(ctxMock.getDeviceStateNodeRateLimitConfig()).willReturn("invalid rate limit config");
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);
        given(ctxMock.getSelfId()).willReturn(new RuleNodeId(UUID.randomUUID()));

        // WHEN
        try {
            initNode(TbMsgType.ACTIVITY_EVENT);
        } catch (Exception e) {
            fail("Node failed to initialize!", e);
        }

        // THEN
        String actualRateLimitConfig = (String) ReflectionTestUtils.getField(node, "rateLimitConfig");
        assertThat(actualRateLimitConfig).isEqualTo("1:1,30:60,60:3600");
    }

    @Test
    public void givenMsgArrivedTooFast_whenOnMsg_thenRateLimitsThisMsg() {
        // GIVEN
        ConcurrentReferenceHashMap<DeviceId, TbRateLimits> rateLimits = new ConcurrentReferenceHashMap<>();
        ReflectionTestUtils.setField(node, "rateLimits", rateLimits);

        var rateLimitMock = mock(TbRateLimits.class);
        rateLimits.put(DEVICE_ID, rateLimitMock);

        given(rateLimitMock.tryConsume()).willReturn(false);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        then(ctxMock).should().tellNext(msg, "Rate limited");
        then(ctxMock).should(never()).tellSuccess(any());
        then(ctxMock).should(never()).tellFailure(any(), any());
        then(ctxMock).shouldHaveNoMoreInteractions();
        then(deviceStateManagerMock).shouldHaveNoInteractions();
    }

    @Test
    public void givenHasNonLocalDevices_whenOnPartitionChange_thenRemovesEntriesForNonLocalDevices() {
        // GIVEN
        ConcurrentReferenceHashMap<DeviceId, TbRateLimits> rateLimits = new ConcurrentReferenceHashMap<>();
        ReflectionTestUtils.setField(node, "rateLimits", rateLimits);

        rateLimits.put(DEVICE_ID, new TbRateLimits("1:1"));
        given(ctxMock.isLocalEntity(eq(DEVICE_ID))).willReturn(true);

        DeviceId nonLocalDeviceId1 = new DeviceId(UUID.randomUUID());
        rateLimits.put(nonLocalDeviceId1, new TbRateLimits("2:2"));
        given(ctxMock.isLocalEntity(eq(nonLocalDeviceId1))).willReturn(false);

        DeviceId nonLocalDeviceId2 = new DeviceId(UUID.randomUUID());
        rateLimits.put(nonLocalDeviceId2, new TbRateLimits("3:3"));
        given(ctxMock.isLocalEntity(eq(nonLocalDeviceId2))).willReturn(false);

        // WHEN
        node.onPartitionChangeMsg(ctxMock, new PartitionChangeMsg(ServiceType.TB_RULE_ENGINE));

        // THEN
        assertThat(rateLimits)
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
        var msg = TbMsg.newMsg()
                .type(TbMsgType.ENTITY_CREATED)
                .originator(nonDeviceOriginator)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        then(ctxMock).should().tellFailure(eq(msg), exceptionCaptor.capture());
        assertThat(exceptionCaptor.getValue())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported originator entity type: [" + unsupportedType + "]. Only DEVICE entity type is supported.");

        then(ctxMock).shouldHaveNoMoreInteractions();
    }

    @Test
    public void givenMetadataDoesNotContainTs_whenOnMsg_thenMsgTsIsUsedAsEventTs() {
        // GIVEN
        given(ctxMock.getDeviceStateNodeRateLimitConfig()).willReturn("1:1");
        try {
            initNode(TbMsgType.ACTIVITY_EVENT);
        } catch (TbNodeException e) {
            fail("Node failed to initialize!", e);
        }

        given(ctxMock.getTenantId()).willReturn(TENANT_ID);
        given(ctxMock.getDeviceStateManager()).willReturn(deviceStateManagerMock);

        long msgTs = METADATA_TS + 1;
        msg = TbMsg.newMsg()
                .ts(msgTs)
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        then(deviceStateManagerMock).should().onDeviceActivity(eq(TENANT_ID), eq(DEVICE_ID), eq(msgTs), any());
    }

    @ParameterizedTest
    @MethodSource
    public void givenSupportedEventAndDeviceOriginator_whenOnMsg_thenCorrectEventIsSentWithCorrectCallback(TbMsgType supportedEventType, BiConsumer<DeviceStateManager, ArgumentCaptor<TbCallback>> actionVerification) {
        // GIVEN
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);
        given(ctxMock.getDeviceStateNodeRateLimitConfig()).willReturn("1:1");
        given(ctxMock.getDeviceStateManager()).willReturn(deviceStateManagerMock);

        try {
            initNode(supportedEventType);
        } catch (TbNodeException e) {
            fail("Node failed to initialize!", e);
        }

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        actionVerification.accept(this.deviceStateManagerMock, this.callbackCaptor);

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
                Arguments.of(TbMsgType.CONNECT_EVENT, (BiConsumer<DeviceStateManager, ArgumentCaptor<TbCallback>>) (deviceStateManagerMock, callbackCaptor) -> then(deviceStateManagerMock).should().onDeviceConnect(eq(TENANT_ID), eq(DEVICE_ID), eq(METADATA_TS), callbackCaptor.capture())),
                Arguments.of(TbMsgType.ACTIVITY_EVENT, (BiConsumer<DeviceStateManager, ArgumentCaptor<TbCallback>>) (deviceStateManagerMock, callbackCaptor) -> then(deviceStateManagerMock).should().onDeviceActivity(eq(TENANT_ID), eq(DEVICE_ID), eq(METADATA_TS), callbackCaptor.capture())),
                Arguments.of(TbMsgType.DISCONNECT_EVENT, (BiConsumer<DeviceStateManager, ArgumentCaptor<TbCallback>>) (deviceStateManagerMock, callbackCaptor) -> then(deviceStateManagerMock).should().onDeviceDisconnect(eq(TENANT_ID), eq(DEVICE_ID), eq(METADATA_TS), callbackCaptor.capture())),
                Arguments.of(TbMsgType.INACTIVITY_EVENT, (BiConsumer<DeviceStateManager, ArgumentCaptor<TbCallback>>) (deviceStateManagerMock, callbackCaptor) -> then(deviceStateManagerMock).should().onDeviceInactivity(eq(TENANT_ID), eq(DEVICE_ID), eq(METADATA_TS), callbackCaptor.capture()))
        );
    }

    private void initNode(TbMsgType event) throws TbNodeException {
        config.setEvent(event);
        var nodeConfig = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfig);
    }

}
