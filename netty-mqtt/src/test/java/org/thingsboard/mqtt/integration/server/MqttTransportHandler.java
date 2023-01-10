/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.mqtt.integration.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnAckVariableHeader;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

import static io.netty.handler.codec.mqtt.MqttMessageType.CONNACK;
import static io.netty.handler.codec.mqtt.MqttMessageType.CONNECT;
import static io.netty.handler.codec.mqtt.MqttMessageType.DISCONNECT;
import static io.netty.handler.codec.mqtt.MqttMessageType.PINGREQ;
import static io.netty.handler.codec.mqtt.MqttMessageType.PUBACK;
import static io.netty.handler.codec.mqtt.MqttMessageType.PUBLISH;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;

@Slf4j
public class MqttTransportHandler extends ChannelInboundHandlerAdapter implements GenericFutureListener<Future<? super Void>> {

    private final List<MqttMessageType> eventsFromClient;
    private final UUID sessionId;

    MqttTransportHandler(List<MqttMessageType> eventsFromClient) {
        this.sessionId = UUID.randomUUID();
        this.eventsFromClient = eventsFromClient;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        log.trace("[{}] Processing msg: {}", sessionId, msg);
        try {
            if (msg instanceof MqttMessage) {
                MqttMessage message = (MqttMessage) msg;
                if (message.decoderResult().isSuccess()) {
                    processMqttMsg(ctx, message);
                } else {
                    log.error("[{}] Message decoding failed: {}", sessionId, message.decoderResult().cause().getMessage());
                    ctx.close();
                }
            } else {
                log.debug("[{}] Received non mqtt message: {}", sessionId, msg.getClass().getSimpleName());
                ctx.close();
            }
        } finally {
            ReferenceCountUtil.safeRelease(msg);
        }
    }

    void processMqttMsg(ChannelHandlerContext ctx, MqttMessage msg) {
        if (msg.fixedHeader() == null) {
            ctx.close();
            return;
        }
        switch (msg.fixedHeader().messageType()) {
            case CONNECT:
                eventsFromClient.add(CONNECT);
                processConnect(ctx, (MqttConnectMessage) msg);
                break;
            case DISCONNECT:
                eventsFromClient.add(DISCONNECT);
                ctx.close();
                break;
            case PUBLISH:
                // QoS 0 and 1 supported only here
                eventsFromClient.add(PUBLISH);
                MqttPublishMessage mqttPubMsg = (MqttPublishMessage) msg;
                ack(ctx, mqttPubMsg.variableHeader().packetId());
                break;
            case PINGREQ:
                // We will not handle PINGREQ and will not send any PINGRESP to simulate the MQTT server is down
                eventsFromClient.add(PINGREQ);
                break;
            default:
                break;
        }
    }

    void processConnect(ChannelHandlerContext ctx, MqttConnectMessage msg) {
        String userName = msg.payload().userName();
        String clientId = msg.payload().clientIdentifier();

        log.warn("[{}][{}] Processing connect msg for client: {}!", sessionId, userName, clientId);
        ctx.writeAndFlush(createMqttConnAckMsg(msg));
    }

    private MqttConnAckMessage createMqttConnAckMsg(MqttConnectMessage msg) {
        MqttFixedHeader mqttFixedHeader =
                new MqttFixedHeader(CONNACK, false, AT_MOST_ONCE, false, 0);
        MqttConnAckVariableHeader mqttConnAckVariableHeader =
                new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_ACCEPTED, !msg.variableHeader().isCleanSession());
        return new MqttConnAckMessage(mqttFixedHeader, mqttConnAckVariableHeader);
    }

    private void ack(ChannelHandlerContext ctx, int msgId) {
        if (msgId > 0) {
            ctx.writeAndFlush(createMqttPubAckMsg(msgId));
        }
    }

    public static MqttPubAckMessage createMqttPubAckMsg(int requestId) {
        MqttFixedHeader mqttFixedHeader =
                new MqttFixedHeader(PUBACK, false, AT_MOST_ONCE, false, 0);
        MqttMessageIdVariableHeader mqttMsgIdVariableHeader =
                MqttMessageIdVariableHeader.from(requestId);
        return new MqttPubAckMessage(mqttFixedHeader, mqttMsgIdVariableHeader);
    }

    @Override
    public void operationComplete(Future<? super Void> future) {
        log.trace("[{}] Channel closed!", sessionId);
    }
}
