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
package org.thingsboard.server.transport.mqtt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.ssl.SslHandler;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.JsonTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.mqtt.adaptors.JsonMqttAdaptor;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class MqttTransportHandlerTest {

    public static final int MSG_QUEUE_LIMIT = 10;
    public static final InetSocketAddress IP_ADDR = new InetSocketAddress("127.0.0.1", 9876);
    public static final int TIMEOUT = 30;

    @Mock
    MqttTransportContext context;
    @Mock
    SslHandler sslHandler;
    @Mock
    ChannelHandlerContext ctx;

    AtomicInteger packedId = new AtomicInteger();
    ExecutorService executor;
    MqttTransportHandler handler;

    @Spy
    TransportService transportService;

    @BeforeEach
    public void setUp() throws Exception {

        lenient().doReturn(MSG_QUEUE_LIMIT).when(context).getMessageQueueSizePerDeviceLimit();
        lenient().doReturn(transportService).when(context).getTransportService();

        handler = spy(new MqttTransportHandler(context, sslHandler));
        lenient().doReturn(IP_ADDR).when(handler).getAddress(any());
    }

    @AfterEach
    public void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    MqttConnectMessage getMqttConnectMessage() {
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.CONNECT, true, MqttQoS.AT_LEAST_ONCE, false, 123);
        MqttConnectVariableHeader variableHeader = new MqttConnectVariableHeader("device", packedId.incrementAndGet(), true, true, true, 1, true, false, 60);
        MqttConnectPayload payload = new MqttConnectPayload("clientId", "topic", "message".getBytes(StandardCharsets.UTF_8), "username", "password".getBytes(StandardCharsets.UTF_8));
        return new MqttConnectMessage(mqttFixedHeader, variableHeader, payload);
    }

    MqttPublishMessage getMqttPublishMessage() {
        return getMqttPublishMessage("v1/gateway/telemetry");
    }

    MqttPublishMessage getDeviceMqttPublishMessage() {
        return getMqttPublishMessage("v1/devices/me/telemetry");
    }

    MqttPublishMessage getMqttPublishMessage(String topicName) {
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, true, MqttQoS.AT_LEAST_ONCE, false, 123);
        MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader(topicName, packedId.incrementAndGet());
        ByteBuf payload = Unpooled.wrappedBuffer("{\"testKey\":\"testValue\"}".getBytes());
        return new MqttPublishMessage(mqttFixedHeader, variableHeader, payload);
    }

    @Test
    public void givenMqttConnectMessage_whenProcessMqttMsg_thenProcessConnect() {
        MqttConnectMessage msg = getMqttConnectMessage();
        willDoNothing().given(handler).processConnect(ctx, msg);

        handler.channelRead(ctx, msg);

        assertThat(handler.address, is(IP_ADDR));
        assertThat(handler.deviceSessionCtx.getChannel(), is(ctx));
        verify(handler, never()).doDisconnect();
        verify(handler, times(1)).processConnect(ctx, msg);
    }

    @Test
    public void givenQueueLimit_whenEnqueueRegularSessionMsgOverLimit_thenOK() {
        List<MqttPublishMessage> messages = Stream.generate(this::getMqttPublishMessage).limit(MSG_QUEUE_LIMIT).collect(Collectors.toList());
        messages.forEach(msg -> handler.enqueueRegularSessionMsg(ctx, msg));
        assertThat(handler.deviceSessionCtx.getMsgQueueSize(), is(MSG_QUEUE_LIMIT));
        assertThat(handler.deviceSessionCtx.getMsgQueueSnapshot(), contains(messages.toArray()));
    }

    @Test
    public void givenQueueLimit_whenEnqueueRegularSessionMsgOverLimit_thenCtxClose() {
        final int limit = MSG_QUEUE_LIMIT + 1;
        willDoNothing().given(handler).processMsgQueue(ctx);
        List<MqttPublishMessage> messages = Stream.generate(this::getMqttPublishMessage).limit(limit).collect(Collectors.toList());

        messages.forEach((msg) -> handler.enqueueRegularSessionMsg(ctx, msg));

        assertThat(handler.deviceSessionCtx.getMsgQueueSize(), is(MSG_QUEUE_LIMIT));
        verify(handler, times(limit)).enqueueRegularSessionMsg(any(), any());
        verify(handler, times(MSG_QUEUE_LIMIT)).processMsgQueue(any());
        verify(ctx, times(1)).close();
    }

    @Test
    public void givenMqttConnectMessageAndPublishImmediately_whenProcessMqttMsg_thenEnqueueRegularSessionMsg() {
        givenMqttConnectMessage_whenProcessMqttMsg_thenProcessConnect();

        List<MqttPublishMessage> messages = Stream.generate(this::getMqttPublishMessage).limit(MSG_QUEUE_LIMIT).collect(Collectors.toList());

        messages.forEach((msg) -> handler.channelRead(ctx, msg));

        assertThat(handler.address, is(IP_ADDR));
        assertThat(handler.deviceSessionCtx.getChannel(), is(ctx));
        assertThat(handler.deviceSessionCtx.isConnected(), is(false));
        assertThat(handler.deviceSessionCtx.getMsgQueueSize(), is(MSG_QUEUE_LIMIT));
        assertThat(handler.deviceSessionCtx.getMsgQueueSnapshot(), contains(messages.toArray()));
        verify(handler, never()).doDisconnect();
        verify(handler, times(1)).processConnect(any(), any());
        verify(handler, times(MSG_QUEUE_LIMIT)).enqueueRegularSessionMsg(any(), any());
        verify(handler, never()).processRegularSessionMsg(any(), any());
        messages.forEach((msg) -> verify(handler, times(1)).enqueueRegularSessionMsg(ctx, msg));
    }

    @Test
    public void givenMessageQueue_whenProcessMqttMsgConcurrently_thenEnqueueRegularSessionMsg() throws InterruptedException {
        //given
        assertThat(handler.deviceSessionCtx.isConnected(), is(false));
        assertThat(MSG_QUEUE_LIMIT, greaterThan(2));
        List<MqttPublishMessage> messages = Stream.generate(this::getMqttPublishMessage).limit(MSG_QUEUE_LIMIT).collect(Collectors.toList());
        messages.forEach((msg) -> handler.enqueueRegularSessionMsg(ctx, msg));
        willDoNothing().given(handler).processRegularSessionMsg(any(), any());
        executor = Executors.newCachedThreadPool(ThingsBoardThreadFactory.forName(getClass().getName()));

        CountDownLatch readyLatch = new CountDownLatch(MSG_QUEUE_LIMIT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(MSG_QUEUE_LIMIT);

        Stream.iterate(0, i -> i + 1).limit(MSG_QUEUE_LIMIT).forEach(x ->
                executor.submit(() -> {
                    try {
                        readyLatch.countDown();
                        assertThat(startLatch.await(TIMEOUT, TimeUnit.SECONDS), is(true));
                        handler.processMsgQueue(ctx);
                        finishLatch.countDown();
                    } catch (Exception e) {
                        log.error("Failed to run processMsgQueue", e);
                        fail("Failed to run processMsgQueue");
                    }
                }));

        //when
        assertThat(readyLatch.await(TIMEOUT, TimeUnit.SECONDS), is(true));
        handler.deviceSessionCtx.setConnected(true);
        startLatch.countDown();
        assertThat(finishLatch.await(TIMEOUT, TimeUnit.SECONDS), is(true));

        //then
        assertThat(handler.deviceSessionCtx.getMsgQueueSize(), is(0));
        assertThat(handler.deviceSessionCtx.getMsgQueueSnapshot(), empty());
        verify(handler, times(MSG_QUEUE_LIMIT)).processRegularSessionMsg(any(), any());
        messages.forEach((msg) -> verify(handler, times(1)).processRegularSessionMsg(ctx, msg));
    }

    @Test
    public void givenMqttMessage_whenDeviceProfileMqttTransport_thenTopicAddedToMetadata() {
        MqttPublishMessage message = getDeviceMqttPublishMessage();
        when(context.getJsonMqttAdaptor()).thenReturn(new JsonMqttAdaptor());
        handler.deviceSessionCtx.setConnected(true);
        DeviceProfile deviceProfile = new DeviceProfile();
        DeviceProfileData deviceProfileData = new DeviceProfileData();
        MqttDeviceProfileTransportConfiguration mqttDeviceProfileTransportConfiguration = new MqttDeviceProfileTransportConfiguration();
        mqttDeviceProfileTransportConfiguration.setTransportPayloadTypeConfiguration(new JsonTransportPayloadConfiguration());
        deviceProfileData.setTransportConfiguration(mqttDeviceProfileTransportConfiguration);
        deviceProfile.setProfileData(deviceProfileData);
        deviceProfile.setTransportType(DeviceTransportType.MQTT);
        handler.deviceSessionCtx.setDeviceProfile(deviceProfile);

        handler.processRegularSessionMsg(ctx, message);

        TbMsgMetaData expectedMd = new TbMsgMetaData();
        expectedMd.putValue(DataConstants.MQTT_TOPIC, message.variableHeader().topicName());

        verify(transportService, times(1)).process(any(), (TransportProtos.PostTelemetryMsg) any(), eq(expectedMd), any());
    }

}