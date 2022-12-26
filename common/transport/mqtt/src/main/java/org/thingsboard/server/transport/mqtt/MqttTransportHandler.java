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
package org.thingsboard.server.transport.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonParseException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttSubAckPayload;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.common.transport.SessionMsgListener;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.auth.SessionInfoCreator;
import org.thingsboard.server.common.transport.auth.TransportDeviceInfo;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.common.transport.service.DefaultTransportService;
import org.thingsboard.server.common.transport.service.SessionMetaData;
import org.thingsboard.server.common.transport.util.SslUtil;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ProvisionDeviceResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceX509CertRequestMsg;
import org.thingsboard.server.queue.scheduler.SchedulerComponent;
import org.thingsboard.server.transport.mqtt.adaptors.MqttTransportAdaptor;
import org.thingsboard.server.transport.mqtt.session.DeviceSessionCtx;
import org.thingsboard.server.transport.mqtt.session.GatewaySessionHandler;
import org.thingsboard.server.transport.mqtt.session.MqttTopicMatcher;
import org.thingsboard.server.transport.mqtt.util.ReturnCode;
import org.thingsboard.server.transport.mqtt.util.ReturnCodeResolver;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.amazonaws.util.StringUtils.UTF8;
import static io.netty.handler.codec.mqtt.MqttMessageType.CONNECT;
import static io.netty.handler.codec.mqtt.MqttMessageType.PINGRESP;
import static io.netty.handler.codec.mqtt.MqttMessageType.SUBACK;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_LEAST_ONCE;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SESSION_EVENT_MSG_CLOSED;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SESSION_EVENT_MSG_OPEN;

/**
 * @author Andrew Shvayka
 */
@Slf4j
public class MqttTransportHandler extends ChannelInboundHandlerAdapter implements GenericFutureListener<Future<? super Void>>, SessionMsgListener {

    private static final Pattern FW_REQUEST_PATTERN = Pattern.compile(MqttTopics.DEVICE_FIRMWARE_REQUEST_TOPIC_PATTERN);
    private static final Pattern SW_REQUEST_PATTERN = Pattern.compile(MqttTopics.DEVICE_SOFTWARE_REQUEST_TOPIC_PATTERN);


    private static final String PAYLOAD_TOO_LARGE = "PAYLOAD_TOO_LARGE";

    private static final MqttQoS MAX_SUPPORTED_QOS_LVL = AT_LEAST_ONCE;

    private final UUID sessionId;
    private final MqttTransportContext context;
    private final TransportService transportService;
    private final SchedulerComponent scheduler;
    private final SslHandler sslHandler;
    private final ConcurrentMap<MqttTopicMatcher, Integer> mqttQoSMap;

    final DeviceSessionCtx deviceSessionCtx;
    volatile InetSocketAddress address;
    volatile GatewaySessionHandler gatewaySessionHandler;

    private final ConcurrentHashMap<String, String> otaPackSessions;
    private final ConcurrentHashMap<String, Integer> chunkSizes;
    private final ConcurrentMap<Integer, TransportProtos.ToDeviceRpcRequestMsg> rpcAwaitingAck;

    private TopicType attrSubTopicType;
    private TopicType rpcSubTopicType;
    private TopicType attrReqTopicType;
    private TopicType toServerRpcSubTopicType;

    MqttTransportHandler(MqttTransportContext context, SslHandler sslHandler) {
        this.sessionId = UUID.randomUUID();
        this.context = context;
        this.transportService = context.getTransportService();
        this.scheduler = context.getScheduler();
        this.sslHandler = sslHandler;
        this.mqttQoSMap = new ConcurrentHashMap<>();
        this.deviceSessionCtx = new DeviceSessionCtx(sessionId, mqttQoSMap, context);
        this.otaPackSessions = new ConcurrentHashMap<>();
        this.chunkSizes = new ConcurrentHashMap<>();
        this.rpcAwaitingAck = new ConcurrentHashMap<>();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        context.channelRegistered();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        context.channelUnregistered();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        log.trace("[{}] Processing msg: {}", sessionId, msg);
        if (address == null) {
            address = getAddress(ctx);
        }
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

    InetSocketAddress getAddress(ChannelHandlerContext ctx) {
        var address = ctx.channel().attr(MqttTransportService.ADDRESS).get();
        if (address == null) {
            log.trace("[{}] Received empty address.", ctx.channel().id());
            InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            log.trace("[{}] Going to use address: {}", ctx.channel().id(), remoteAddress);
            return remoteAddress;
        } else {
            log.trace("[{}] Received address: {}", ctx.channel().id(), address);
        }
        return address;
    }

    void processMqttMsg(ChannelHandlerContext ctx, MqttMessage msg) {
        if (msg.fixedHeader() == null) {
            log.info("[{}:{}] Invalid message received", address.getHostName(), address.getPort());
            ctx.close();
            return;
        }
        deviceSessionCtx.setChannel(ctx);
        if (CONNECT.equals(msg.fixedHeader().messageType())) {
            processConnect(ctx, (MqttConnectMessage) msg);
        } else if (deviceSessionCtx.isProvisionOnly()) {
            processProvisionSessionMsg(ctx, msg);
        } else {
            enqueueRegularSessionMsg(ctx, msg);
        }
    }

    private void processProvisionSessionMsg(ChannelHandlerContext ctx, MqttMessage msg) {
        switch (msg.fixedHeader().messageType()) {
            case PUBLISH:
                MqttPublishMessage mqttMsg = (MqttPublishMessage) msg;
                String topicName = mqttMsg.variableHeader().topicName();
                int msgId = mqttMsg.variableHeader().packetId();
                try {
                    if (topicName.equals(MqttTopics.DEVICE_PROVISION_REQUEST_TOPIC)) {
                        try {
                            TransportProtos.ProvisionDeviceRequestMsg provisionRequestMsg = deviceSessionCtx.getContext().getJsonMqttAdaptor().convertToProvisionRequestMsg(deviceSessionCtx, mqttMsg);
                            transportService.process(provisionRequestMsg, new DeviceProvisionCallback(ctx, msgId, provisionRequestMsg));
                            log.trace("[{}][{}] Processing provision publish msg [{}][{}]!", sessionId, deviceSessionCtx.getDeviceId(), topicName, msgId);
                        } catch (Exception e) {
                            if (e instanceof JsonParseException || (e.getCause() != null && e.getCause() instanceof JsonParseException)) {
                                TransportProtos.ProvisionDeviceRequestMsg provisionRequestMsg = deviceSessionCtx.getContext().getProtoMqttAdaptor().convertToProvisionRequestMsg(deviceSessionCtx, mqttMsg);
                                transportService.process(provisionRequestMsg, new DeviceProvisionCallback(ctx, msgId, provisionRequestMsg));
                                deviceSessionCtx.setProvisionPayloadType(TransportPayloadType.PROTOBUF);
                                log.trace("[{}][{}] Processing provision publish msg [{}][{}]!", sessionId, deviceSessionCtx.getDeviceId(), topicName, msgId);
                            } else {
                                throw e;
                            }
                        }
                    } else {
                        log.debug("[{}] Unsupported topic for provisioning requests: {}!", sessionId, topicName);
                        ctx.close();
                    }
                } catch (RuntimeException e) {
                    log.warn("[{}] Failed to process publish msg [{}][{}]", sessionId, topicName, msgId, e);
                    ctx.close();
                } catch (AdaptorException e) {
                    log.debug("[{}] Failed to process publish msg [{}][{}]", sessionId, topicName, msgId, e);
                    ctx.close();
                }
                break;
            case PINGREQ:
                ctx.writeAndFlush(new MqttMessage(new MqttFixedHeader(PINGRESP, false, AT_MOST_ONCE, false, 0)));
                break;
            case DISCONNECT:
                ctx.close();
                break;
        }
    }

    void enqueueRegularSessionMsg(ChannelHandlerContext ctx, MqttMessage msg) {
        final int queueSize = deviceSessionCtx.getMsgQueueSize();
        if (queueSize >= context.getMessageQueueSizePerDeviceLimit()) {
            log.info("Closing current session because msq queue size for device {} exceed limit {} with msgQueueSize counter {} and actual queue size {}",
                    deviceSessionCtx.getDeviceId(), context.getMessageQueueSizePerDeviceLimit(), queueSize, deviceSessionCtx.getMsgQueueSize());
            ctx.close();
            return;
        }

        deviceSessionCtx.addToQueue(msg);
        processMsgQueue(ctx); //Under the normal conditions the msg queue will contain 0 messages. Many messages will be processed on device connect event in separate thread pool
    }

    void processMsgQueue(ChannelHandlerContext ctx) {
        if (!deviceSessionCtx.isConnected()) {
            log.trace("[{}][{}] Postpone processing msg due to device is not connected. Msg queue size is {}", sessionId, deviceSessionCtx.getDeviceId(), deviceSessionCtx.getMsgQueueSize());
            return;
        }
        deviceSessionCtx.tryProcessQueuedMsgs(msg -> processRegularSessionMsg(ctx, msg));
    }

    void processRegularSessionMsg(ChannelHandlerContext ctx, MqttMessage msg) {
        switch (msg.fixedHeader().messageType()) {
            case PUBLISH:
                processPublish(ctx, (MqttPublishMessage) msg);
                break;
            case SUBSCRIBE:
                processSubscribe(ctx, (MqttSubscribeMessage) msg);
                break;
            case UNSUBSCRIBE:
                processUnsubscribe(ctx, (MqttUnsubscribeMessage) msg);
                break;
            case PINGREQ:
                if (checkConnected(ctx, msg)) {
                    ctx.writeAndFlush(new MqttMessage(new MqttFixedHeader(PINGRESP, false, AT_MOST_ONCE, false, 0)));
                    log.info("PINGREQ: {}", deviceSessionCtx.getSessionInfo());
                    transportService.reportActivity(deviceSessionCtx.getSessionInfo());
                }
                break;
            case DISCONNECT:
                ctx.close();
                break;
            case PUBACK:
                int msgId = ((MqttPubAckMessage) msg).variableHeader().messageId();
                TransportProtos.ToDeviceRpcRequestMsg rpcRequest = rpcAwaitingAck.remove(msgId);
                if (rpcRequest != null) {
                    transportService.process(deviceSessionCtx.getSessionInfo(), rpcRequest, RpcStatus.DELIVERED, TransportServiceCallback.EMPTY);
                }
                break;
            default:
                break;
        }
    }

    private void processPublish(ChannelHandlerContext ctx, MqttPublishMessage mqttMsg) {
        if (!checkConnected(ctx, mqttMsg)) {
            return;
        }
        String topicName = mqttMsg.variableHeader().topicName();
        int msgId = mqttMsg.variableHeader().packetId();
        log.trace("[{}][{}] Processing publish msg [{}][{}]!", sessionId, deviceSessionCtx.getDeviceId(), topicName, msgId);

        if (topicName.startsWith(MqttTopics.BASE_GATEWAY_API_TOPIC)) {
            if (gatewaySessionHandler != null) {
                handleGatewayPublishMsg(ctx, topicName, msgId, mqttMsg);
                transportService.reportActivity(deviceSessionCtx.getSessionInfo());
            }
        } else {
            processDevicePublish(ctx, mqttMsg, topicName, msgId);
        }
    }

    private void handleGatewayPublishMsg(ChannelHandlerContext ctx, String topicName, int msgId, MqttPublishMessage mqttMsg) {
        try {
            switch (topicName) {
                case MqttTopics.GATEWAY_TELEMETRY_TOPIC:
                    gatewaySessionHandler.onDeviceTelemetry(mqttMsg);
                    break;
                case MqttTopics.GATEWAY_CLAIM_TOPIC:
                    gatewaySessionHandler.onDeviceClaim(mqttMsg);
                    break;
                case MqttTopics.GATEWAY_ATTRIBUTES_TOPIC:
                    gatewaySessionHandler.onDeviceAttributes(mqttMsg);
                    break;
                case MqttTopics.GATEWAY_ATTRIBUTES_REQUEST_TOPIC:
                    gatewaySessionHandler.onDeviceAttributesRequest(mqttMsg);
                    break;
                case MqttTopics.GATEWAY_RPC_TOPIC:
                    gatewaySessionHandler.onDeviceRpcResponse(mqttMsg);
                    break;
                case MqttTopics.GATEWAY_CONNECT_TOPIC:
                    gatewaySessionHandler.onDeviceConnect(mqttMsg);
                    break;
                case MqttTopics.GATEWAY_DISCONNECT_TOPIC:
                    gatewaySessionHandler.onDeviceDisconnect(mqttMsg);
                    break;
                default:
                    ack(ctx, msgId, ReturnCode.TOPIC_NAME_INVALID);
            }
        } catch (RuntimeException e) {
            log.warn("[{}] Failed to process publish msg [{}][{}]", sessionId, topicName, msgId, e);
            ack(ctx, msgId, ReturnCode.IMPLEMENTATION_SPECIFIC);
            ctx.close();
        } catch (AdaptorException e) {
            log.debug("[{}] Failed to process publish msg [{}][{}]", sessionId, topicName, msgId, e);
            sendAckOrCloseSession(ctx, topicName, msgId);
        }
    }

    private void processDevicePublish(ChannelHandlerContext ctx, MqttPublishMessage mqttMsg, String topicName, int msgId) {
        try {
            Matcher fwMatcher;
            MqttTransportAdaptor payloadAdaptor = deviceSessionCtx.getPayloadAdaptor();
            if (deviceSessionCtx.isDeviceAttributesTopic(topicName)) {
                TransportProtos.PostAttributeMsg postAttributeMsg = payloadAdaptor.convertToPostAttributes(deviceSessionCtx, mqttMsg);
                transportService.process(deviceSessionCtx.getSessionInfo(), postAttributeMsg, getPubAckCallback(ctx, msgId, postAttributeMsg));
            } else if (deviceSessionCtx.isDeviceTelemetryTopic(topicName)) {
                TransportProtos.PostTelemetryMsg postTelemetryMsg = payloadAdaptor.convertToPostTelemetry(deviceSessionCtx, mqttMsg);
                transportService.process(deviceSessionCtx.getSessionInfo(), postTelemetryMsg, getPubAckCallback(ctx, msgId, postTelemetryMsg));
            } else if (topicName.startsWith(MqttTopics.DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX)) {
                TransportProtos.GetAttributeRequestMsg getAttributeMsg = payloadAdaptor.convertToGetAttributes(deviceSessionCtx, mqttMsg, MqttTopics.DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX);
                transportService.process(deviceSessionCtx.getSessionInfo(), getAttributeMsg, getPubAckCallback(ctx, msgId, getAttributeMsg));
                attrReqTopicType = TopicType.V1;
            } else if (topicName.startsWith(MqttTopics.DEVICE_RPC_RESPONSE_TOPIC)) {
                TransportProtos.ToDeviceRpcResponseMsg rpcResponseMsg = payloadAdaptor.convertToDeviceRpcResponse(deviceSessionCtx, mqttMsg, MqttTopics.DEVICE_RPC_RESPONSE_TOPIC);
                transportService.process(deviceSessionCtx.getSessionInfo(), rpcResponseMsg, getPubAckCallback(ctx, msgId, rpcResponseMsg));
            } else if (topicName.startsWith(MqttTopics.DEVICE_RPC_REQUESTS_TOPIC)) {
                TransportProtos.ToServerRpcRequestMsg rpcRequestMsg = payloadAdaptor.convertToServerRpcRequest(deviceSessionCtx, mqttMsg, MqttTopics.DEVICE_RPC_REQUESTS_TOPIC);
                transportService.process(deviceSessionCtx.getSessionInfo(), rpcRequestMsg, getPubAckCallback(ctx, msgId, rpcRequestMsg));
                toServerRpcSubTopicType = TopicType.V1;
            } else if (topicName.equals(MqttTopics.DEVICE_CLAIM_TOPIC)) {
                TransportProtos.ClaimDeviceMsg claimDeviceMsg = payloadAdaptor.convertToClaimDevice(deviceSessionCtx, mqttMsg);
                transportService.process(deviceSessionCtx.getSessionInfo(), claimDeviceMsg, getPubAckCallback(ctx, msgId, claimDeviceMsg));
            } else if ((fwMatcher = FW_REQUEST_PATTERN.matcher(topicName)).find()) {
                getOtaPackageCallback(ctx, mqttMsg, msgId, fwMatcher, OtaPackageType.FIRMWARE);
            } else if ((fwMatcher = SW_REQUEST_PATTERN.matcher(topicName)).find()) {
                getOtaPackageCallback(ctx, mqttMsg, msgId, fwMatcher, OtaPackageType.SOFTWARE);
            } else if (topicName.equals(MqttTopics.DEVICE_TELEMETRY_SHORT_TOPIC)) {
                TransportProtos.PostTelemetryMsg postTelemetryMsg = payloadAdaptor.convertToPostTelemetry(deviceSessionCtx, mqttMsg);
                transportService.process(deviceSessionCtx.getSessionInfo(), postTelemetryMsg, getPubAckCallback(ctx, msgId, postTelemetryMsg));
            } else if (topicName.equals(MqttTopics.DEVICE_TELEMETRY_SHORT_JSON_TOPIC)) {
                TransportProtos.PostTelemetryMsg postTelemetryMsg = context.getJsonMqttAdaptor().convertToPostTelemetry(deviceSessionCtx, mqttMsg);
                transportService.process(deviceSessionCtx.getSessionInfo(), postTelemetryMsg, getPubAckCallback(ctx, msgId, postTelemetryMsg));
            } else if (topicName.equals(MqttTopics.DEVICE_TELEMETRY_SHORT_PROTO_TOPIC)) {
                TransportProtos.PostTelemetryMsg postTelemetryMsg = context.getProtoMqttAdaptor().convertToPostTelemetry(deviceSessionCtx, mqttMsg);
                transportService.process(deviceSessionCtx.getSessionInfo(), postTelemetryMsg, getPubAckCallback(ctx, msgId, postTelemetryMsg));
            } else if (topicName.equals(MqttTopics.DEVICE_ATTRIBUTES_SHORT_TOPIC)) {
                TransportProtos.PostAttributeMsg postAttributeMsg = payloadAdaptor.convertToPostAttributes(deviceSessionCtx, mqttMsg);
                transportService.process(deviceSessionCtx.getSessionInfo(), postAttributeMsg, getPubAckCallback(ctx, msgId, postAttributeMsg));
            } else if (topicName.equals(MqttTopics.DEVICE_ATTRIBUTES_SHORT_JSON_TOPIC)) {
                TransportProtos.PostAttributeMsg postAttributeMsg = context.getJsonMqttAdaptor().convertToPostAttributes(deviceSessionCtx, mqttMsg);
                transportService.process(deviceSessionCtx.getSessionInfo(), postAttributeMsg, getPubAckCallback(ctx, msgId, postAttributeMsg));
            } else if (topicName.equals(MqttTopics.DEVICE_ATTRIBUTES_SHORT_PROTO_TOPIC)) {
                TransportProtos.PostAttributeMsg postAttributeMsg = context.getProtoMqttAdaptor().convertToPostAttributes(deviceSessionCtx, mqttMsg);
                transportService.process(deviceSessionCtx.getSessionInfo(), postAttributeMsg, getPubAckCallback(ctx, msgId, postAttributeMsg));
            } else if (topicName.startsWith(MqttTopics.DEVICE_RPC_RESPONSE_SHORT_JSON_TOPIC)) {
                TransportProtos.ToDeviceRpcResponseMsg rpcResponseMsg = context.getJsonMqttAdaptor().convertToDeviceRpcResponse(deviceSessionCtx, mqttMsg, MqttTopics.DEVICE_RPC_RESPONSE_SHORT_JSON_TOPIC);
                transportService.process(deviceSessionCtx.getSessionInfo(), rpcResponseMsg, getPubAckCallback(ctx, msgId, rpcResponseMsg));
            } else if (topicName.startsWith(MqttTopics.DEVICE_RPC_RESPONSE_SHORT_PROTO_TOPIC)) {
                TransportProtos.ToDeviceRpcResponseMsg rpcResponseMsg = context.getProtoMqttAdaptor().convertToDeviceRpcResponse(deviceSessionCtx, mqttMsg, MqttTopics.DEVICE_RPC_RESPONSE_SHORT_PROTO_TOPIC);
                transportService.process(deviceSessionCtx.getSessionInfo(), rpcResponseMsg, getPubAckCallback(ctx, msgId, rpcResponseMsg));
            } else if (topicName.startsWith(MqttTopics.DEVICE_RPC_RESPONSE_SHORT_TOPIC)) {
                TransportProtos.ToDeviceRpcResponseMsg rpcResponseMsg = payloadAdaptor.convertToDeviceRpcResponse(deviceSessionCtx, mqttMsg, MqttTopics.DEVICE_RPC_RESPONSE_SHORT_TOPIC);
                transportService.process(deviceSessionCtx.getSessionInfo(), rpcResponseMsg, getPubAckCallback(ctx, msgId, rpcResponseMsg));
            } else if (topicName.startsWith(MqttTopics.DEVICE_RPC_REQUESTS_SHORT_JSON_TOPIC)) {
                TransportProtos.ToServerRpcRequestMsg rpcRequestMsg = context.getJsonMqttAdaptor().convertToServerRpcRequest(deviceSessionCtx, mqttMsg, MqttTopics.DEVICE_RPC_REQUESTS_SHORT_JSON_TOPIC);
                transportService.process(deviceSessionCtx.getSessionInfo(), rpcRequestMsg, getPubAckCallback(ctx, msgId, rpcRequestMsg));
                toServerRpcSubTopicType = TopicType.V2_JSON;
            } else if (topicName.startsWith(MqttTopics.DEVICE_RPC_REQUESTS_SHORT_PROTO_TOPIC)) {
                TransportProtos.ToServerRpcRequestMsg rpcRequestMsg = context.getProtoMqttAdaptor().convertToServerRpcRequest(deviceSessionCtx, mqttMsg, MqttTopics.DEVICE_RPC_REQUESTS_SHORT_PROTO_TOPIC);
                transportService.process(deviceSessionCtx.getSessionInfo(), rpcRequestMsg, getPubAckCallback(ctx, msgId, rpcRequestMsg));
                toServerRpcSubTopicType = TopicType.V2_PROTO;
            } else if (topicName.startsWith(MqttTopics.DEVICE_RPC_REQUESTS_SHORT_TOPIC)) {
                TransportProtos.ToServerRpcRequestMsg rpcRequestMsg = payloadAdaptor.convertToServerRpcRequest(deviceSessionCtx, mqttMsg, MqttTopics.DEVICE_RPC_REQUESTS_SHORT_TOPIC);
                transportService.process(deviceSessionCtx.getSessionInfo(), rpcRequestMsg, getPubAckCallback(ctx, msgId, rpcRequestMsg));
                toServerRpcSubTopicType = TopicType.V2;
            } else if (topicName.startsWith(MqttTopics.DEVICE_ATTRIBUTES_REQUEST_SHORT_JSON_TOPIC_PREFIX)) {
                TransportProtos.GetAttributeRequestMsg getAttributeMsg = context.getJsonMqttAdaptor().convertToGetAttributes(deviceSessionCtx, mqttMsg, MqttTopics.DEVICE_ATTRIBUTES_REQUEST_SHORT_JSON_TOPIC_PREFIX);
                transportService.process(deviceSessionCtx.getSessionInfo(), getAttributeMsg, getPubAckCallback(ctx, msgId, getAttributeMsg));
                attrReqTopicType = TopicType.V2_JSON;
            } else if (topicName.startsWith(MqttTopics.DEVICE_ATTRIBUTES_REQUEST_SHORT_PROTO_TOPIC_PREFIX)) {
                TransportProtos.GetAttributeRequestMsg getAttributeMsg = context.getProtoMqttAdaptor().convertToGetAttributes(deviceSessionCtx, mqttMsg, MqttTopics.DEVICE_ATTRIBUTES_REQUEST_SHORT_PROTO_TOPIC_PREFIX);
                transportService.process(deviceSessionCtx.getSessionInfo(), getAttributeMsg, getPubAckCallback(ctx, msgId, getAttributeMsg));
                attrReqTopicType = TopicType.V2_PROTO;
            } else if (topicName.startsWith(MqttTopics.DEVICE_ATTRIBUTES_REQUEST_SHORT_TOPIC_PREFIX)) {
                TransportProtos.GetAttributeRequestMsg getAttributeMsg = payloadAdaptor.convertToGetAttributes(deviceSessionCtx, mqttMsg, MqttTopics.DEVICE_ATTRIBUTES_REQUEST_SHORT_TOPIC_PREFIX);
                transportService.process(deviceSessionCtx.getSessionInfo(), getAttributeMsg, getPubAckCallback(ctx, msgId, getAttributeMsg));
                attrReqTopicType = TopicType.V2;
            } else {
                log.info("[processDevicePublish]: {}", deviceSessionCtx.getSessionInfo());
                transportService.reportActivity(deviceSessionCtx.getSessionInfo());
                ack(ctx, msgId, ReturnCode.TOPIC_NAME_INVALID);
            }
        } catch (AdaptorException e) {
            log.debug("[{}] Failed to process publish msg [{}][{}]", sessionId, topicName, msgId, e);
            sendAckOrCloseSession(ctx, topicName, msgId);
        }
    }

    private void sendAckOrCloseSession(ChannelHandlerContext ctx, String topicName, int msgId) {
        if ((deviceSessionCtx.isSendAckOnValidationException() || MqttVersion.MQTT_5.equals(deviceSessionCtx.getMqttVersion())) && msgId > 0) {
            log.debug("[{}] Send pub ack on invalid publish msg [{}][{}]", sessionId, topicName, msgId);
            ctx.writeAndFlush(createMqttPubAckMsg(deviceSessionCtx, msgId, ReturnCode.PAYLOAD_FORMAT_INVALID));
        } else {
            log.info("[{}] Closing current session due to invalid publish msg [{}][{}]", sessionId, topicName, msgId);
            ctx.close();
        }
    }

    private void getOtaPackageCallback(ChannelHandlerContext ctx, MqttPublishMessage mqttMsg, int msgId, Matcher fwMatcher, OtaPackageType type) {
        String payload = mqttMsg.content().toString(UTF8);
        int chunkSize = StringUtils.isNotEmpty(payload) ? Integer.parseInt(payload) : 0;
        String requestId = fwMatcher.group("requestId");
        int chunk = Integer.parseInt(fwMatcher.group("chunk"));

        if (chunkSize > 0) {
            this.chunkSizes.put(requestId, chunkSize);
        } else {
            chunkSize = chunkSizes.getOrDefault(requestId, 0);
        }

        if (chunkSize > context.getMaxPayloadSize()) {
            sendOtaPackageError(ctx, PAYLOAD_TOO_LARGE);
            return;
        }

        String otaPackageId = otaPackSessions.get(requestId);

        if (otaPackageId != null) {
            sendOtaPackage(ctx, mqttMsg.variableHeader().packetId(), otaPackageId, requestId, chunkSize, chunk, type);
        } else {
            TransportProtos.SessionInfoProto sessionInfo = deviceSessionCtx.getSessionInfo();
            TransportProtos.GetOtaPackageRequestMsg getOtaPackageRequestMsg = TransportProtos.GetOtaPackageRequestMsg.newBuilder()
                    .setDeviceIdMSB(sessionInfo.getDeviceIdMSB())
                    .setDeviceIdLSB(sessionInfo.getDeviceIdLSB())
                    .setTenantIdMSB(sessionInfo.getTenantIdMSB())
                    .setTenantIdLSB(sessionInfo.getTenantIdLSB())
                    .setType(type.name())
                    .build();
            transportService.process(deviceSessionCtx.getSessionInfo(), getOtaPackageRequestMsg,
                    new OtaPackageCallback(ctx, msgId, getOtaPackageRequestMsg, requestId, chunkSize, chunk));
        }
    }

    private void ack(ChannelHandlerContext ctx, int msgId, ReturnCode returnCode) {
        if (msgId > 0) {
            ctx.writeAndFlush(createMqttPubAckMsg(deviceSessionCtx, msgId, returnCode));
        }
    }

    private <T> TransportServiceCallback<Void> getPubAckCallback(final ChannelHandlerContext ctx, final int msgId, final T msg) {
        return new TransportServiceCallback<>() {
            @Override
            public void onSuccess(Void dummy) {
                log.trace("[{}] Published msg: {}", sessionId, msg);
                ack(ctx, msgId, ReturnCode.SUCCESS);
            }

            @Override
            public void onError(Throwable e) {
                log.trace("[{}] Failed to publish msg: {}", sessionId, msg, e);
                ctx.close();
            }
        };
    }

    private class DeviceProvisionCallback implements TransportServiceCallback<ProvisionDeviceResponseMsg> {
        private final ChannelHandlerContext ctx;
        private final int msgId;
        private final TransportProtos.ProvisionDeviceRequestMsg msg;

        DeviceProvisionCallback(ChannelHandlerContext ctx, int msgId, TransportProtos.ProvisionDeviceRequestMsg msg) {
            this.ctx = ctx;
            this.msgId = msgId;
            this.msg = msg;
        }

        @Override
        public void onSuccess(TransportProtos.ProvisionDeviceResponseMsg provisionResponseMsg) {
            log.trace("[{}] Published msg: {}", sessionId, msg);
            ack(ctx, msgId, ReturnCode.SUCCESS);
            try {
                if (deviceSessionCtx.getProvisionPayloadType().equals(TransportPayloadType.JSON)) {
                    deviceSessionCtx.getContext().getJsonMqttAdaptor().convertToPublish(deviceSessionCtx, provisionResponseMsg).ifPresent(deviceSessionCtx.getChannel()::writeAndFlush);
                } else {
                    deviceSessionCtx.getContext().getProtoMqttAdaptor().convertToPublish(deviceSessionCtx, provisionResponseMsg).ifPresent(deviceSessionCtx.getChannel()::writeAndFlush);
                }
                scheduler.schedule((Callable<ChannelFuture>) ctx::close, 60, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.trace("[{}] Failed to convert device provision response to MQTT msg", sessionId, e);
            }
        }

        @Override
        public void onError(Throwable e) {
            log.trace("[{}] Failed to publish msg: {}", sessionId, msg, e);
            ack(ctx, msgId, ReturnCode.IMPLEMENTATION_SPECIFIC);
            ctx.close();
        }
    }

    private class OtaPackageCallback implements TransportServiceCallback<TransportProtos.GetOtaPackageResponseMsg> {
        private final ChannelHandlerContext ctx;
        private final int msgId;
        private final TransportProtos.GetOtaPackageRequestMsg msg;
        private final String requestId;
        private final int chunkSize;
        private final int chunk;

        OtaPackageCallback(ChannelHandlerContext ctx, int msgId, TransportProtos.GetOtaPackageRequestMsg msg, String requestId, int chunkSize, int chunk) {
            this.ctx = ctx;
            this.msgId = msgId;
            this.msg = msg;
            this.requestId = requestId;
            this.chunkSize = chunkSize;
            this.chunk = chunk;
        }

        @Override
        public void onSuccess(TransportProtos.GetOtaPackageResponseMsg response) {
            if (TransportProtos.ResponseStatus.SUCCESS.equals(response.getResponseStatus())) {
                OtaPackageId firmwareId = new OtaPackageId(new UUID(response.getOtaPackageIdMSB(), response.getOtaPackageIdLSB()));
                otaPackSessions.put(requestId, firmwareId.toString());
                sendOtaPackage(ctx, msgId, firmwareId.toString(), requestId, chunkSize, chunk, OtaPackageType.valueOf(response.getType()));
            } else {
                sendOtaPackageError(ctx, response.getResponseStatus().toString());
            }
        }

        @Override
        public void onError(Throwable e) {
            log.trace("[{}] Failed to get firmware: {}", sessionId, msg, e);
            ctx.close();
        }
    }

    private void sendOtaPackage(ChannelHandlerContext ctx, int msgId, String firmwareId, String requestId, int chunkSize, int chunk, OtaPackageType type) {
        log.trace("[{}] Send firmware [{}] to device!", sessionId, firmwareId);
        ack(ctx, msgId, ReturnCode.SUCCESS);
        try {
            byte[] firmwareChunk = context.getOtaPackageDataCache().get(firmwareId, chunkSize, chunk);
            deviceSessionCtx.getPayloadAdaptor()
                    .convertToPublish(deviceSessionCtx, firmwareChunk, requestId, chunk, type)
                    .ifPresent(deviceSessionCtx.getChannel()::writeAndFlush);
        } catch (Exception e) {
            log.trace("[{}] Failed to send firmware response!", sessionId, e);
        }
    }

    private void sendOtaPackageError(ChannelHandlerContext ctx, String error) {
        log.warn("[{}] {}", sessionId, error);
        deviceSessionCtx.getChannel().writeAndFlush(deviceSessionCtx
                .getPayloadAdaptor()
                .createMqttPublishMsg(deviceSessionCtx, MqttTopics.DEVICE_FIRMWARE_ERROR_TOPIC, error.getBytes()));
        ctx.close();
    }

    private void processSubscribe(ChannelHandlerContext ctx, MqttSubscribeMessage mqttMsg) {
        if (!checkConnected(ctx, mqttMsg)) {
            int returnCode = ReturnCodeResolver.getSubscriptionReturnCode(deviceSessionCtx.getMqttVersion(), ReturnCode.NOT_AUTHORIZED_5);
            ctx.writeAndFlush(createSubAckMessage(mqttMsg.variableHeader().messageId(), Collections.singletonList(returnCode)));
            return;
        }
        log.trace("[{}] Processing subscription [{}]!", sessionId, mqttMsg.variableHeader().messageId());
        List<Integer> grantedQoSList = new ArrayList<>();
        boolean activityReported = false;
        for (MqttTopicSubscription subscription : mqttMsg.payload().topicSubscriptions()) {
            String topic = subscription.topicName();
            MqttQoS reqQoS = subscription.qualityOfService();
            try {
                switch (topic) {
                    case MqttTopics.DEVICE_ATTRIBUTES_TOPIC: {
                        processAttributesSubscribe(grantedQoSList, topic, reqQoS, TopicType.V1);
                        activityReported = true;
                        break;
                    }
                    case MqttTopics.DEVICE_ATTRIBUTES_SHORT_TOPIC: {
                        processAttributesSubscribe(grantedQoSList, topic, reqQoS, TopicType.V2);
                        activityReported = true;
                        break;
                    }
                    case MqttTopics.DEVICE_ATTRIBUTES_SHORT_JSON_TOPIC: {
                        processAttributesSubscribe(grantedQoSList, topic, reqQoS, TopicType.V2_JSON);
                        activityReported = true;
                        break;
                    }
                    case MqttTopics.DEVICE_ATTRIBUTES_SHORT_PROTO_TOPIC: {
                        processAttributesSubscribe(grantedQoSList, topic, reqQoS, TopicType.V2_PROTO);
                        activityReported = true;
                        break;
                    }
                    case MqttTopics.DEVICE_RPC_REQUESTS_SUB_TOPIC: {
                        processRpcSubscribe(grantedQoSList, topic, reqQoS, TopicType.V1);
                        activityReported = true;
                        break;
                    }
                    case MqttTopics.DEVICE_RPC_REQUESTS_SUB_SHORT_TOPIC: {
                        processRpcSubscribe(grantedQoSList, topic, reqQoS, TopicType.V2);
                        activityReported = true;
                        break;
                    }
                    case MqttTopics.DEVICE_RPC_REQUESTS_SUB_SHORT_JSON_TOPIC: {
                        processRpcSubscribe(grantedQoSList, topic, reqQoS, TopicType.V2_JSON);
                        activityReported = true;
                        break;
                    }
                    case MqttTopics.DEVICE_RPC_REQUESTS_SUB_SHORT_PROTO_TOPIC: {
                        processRpcSubscribe(grantedQoSList, topic, reqQoS, TopicType.V2_PROTO);
                        activityReported = true;
                        break;
                    }
                    case MqttTopics.DEVICE_RPC_RESPONSE_SUB_TOPIC:
                    case MqttTopics.DEVICE_RPC_RESPONSE_SUB_SHORT_TOPIC:
                    case MqttTopics.DEVICE_RPC_RESPONSE_SUB_SHORT_JSON_TOPIC:
                    case MqttTopics.DEVICE_RPC_RESPONSE_SUB_SHORT_PROTO_TOPIC:
                    case MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_TOPIC:
                    case MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_SHORT_TOPIC:
                    case MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_SHORT_JSON_TOPIC:
                    case MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_SHORT_PROTO_TOPIC:
                    case MqttTopics.GATEWAY_ATTRIBUTES_TOPIC:
                    case MqttTopics.GATEWAY_RPC_TOPIC:
                    case MqttTopics.GATEWAY_ATTRIBUTES_RESPONSE_TOPIC:
                    case MqttTopics.DEVICE_PROVISION_RESPONSE_TOPIC:
                    case MqttTopics.DEVICE_FIRMWARE_RESPONSES_TOPIC:
                    case MqttTopics.DEVICE_FIRMWARE_ERROR_TOPIC:
                    case MqttTopics.DEVICE_SOFTWARE_RESPONSES_TOPIC:
                    case MqttTopics.DEVICE_SOFTWARE_ERROR_TOPIC:
                        registerSubQoS(topic, grantedQoSList, reqQoS);
                        break;
                    default:
                        log.warn("[{}] Failed to subscribe to [{}][{}]", sessionId, topic, reqQoS);
                        grantedQoSList.add(ReturnCodeResolver.getSubscriptionReturnCode(deviceSessionCtx.getMqttVersion(), ReturnCode.TOPIC_FILTER_INVALID));
                        break;
                }
            } catch (Exception e) {
                log.warn("[{}] Failed to subscribe to [{}][{}]", sessionId, topic, reqQoS, e);
                grantedQoSList.add(ReturnCodeResolver.getSubscriptionReturnCode(deviceSessionCtx.getMqttVersion(), ReturnCode.IMPLEMENTATION_SPECIFIC));
            }
        }
        if (!activityReported) {
            log.info("[processSubscribe]: {}", deviceSessionCtx.getSessionInfo());
            transportService.reportActivity(deviceSessionCtx.getSessionInfo());
        }
        ctx.writeAndFlush(createSubAckMessage(mqttMsg.variableHeader().messageId(), grantedQoSList));
    }

    private void processRpcSubscribe(List<Integer> grantedQoSList, String topic, MqttQoS reqQoS, TopicType topicType) {
        transportService.process(deviceSessionCtx.getSessionInfo(), TransportProtos.SubscribeToRPCMsg.newBuilder().build(), null);
        rpcSubTopicType = topicType;
        registerSubQoS(topic, grantedQoSList, reqQoS);
    }

    private void processAttributesSubscribe(List<Integer> grantedQoSList, String topic, MqttQoS reqQoS, TopicType topicType) {
        transportService.process(deviceSessionCtx.getSessionInfo(), TransportProtos.SubscribeToAttributeUpdatesMsg.newBuilder().build(), null);
        attrSubTopicType = topicType;
        registerSubQoS(topic, grantedQoSList, reqQoS);
    }

    private void registerSubQoS(String topic, List<Integer> grantedQoSList, MqttQoS reqQoS) {
        grantedQoSList.add(getMinSupportedQos(reqQoS));
        mqttQoSMap.put(new MqttTopicMatcher(topic), getMinSupportedQos(reqQoS));
    }

    private void processUnsubscribe(ChannelHandlerContext ctx, MqttUnsubscribeMessage mqttMsg) {
        if (!checkConnected(ctx, mqttMsg)) {
            ctx.writeAndFlush(createUnSubAckMessage(mqttMsg.variableHeader().messageId(), Collections.singletonList(ReturnCode.NOT_AUTHORIZED_5.shortValue())));
            return;
        }
        boolean activityReported = false;
        List<Short> unSubResults = new ArrayList<>();
        log.trace("[{}] Processing subscription [{}]!", sessionId, mqttMsg.variableHeader().messageId());
        for (String topicName : mqttMsg.payload().topics()) {
            MqttTopicMatcher matcher = new MqttTopicMatcher(topicName);
            if (mqttQoSMap.containsKey(matcher)) {
                mqttQoSMap.remove(matcher);
                try {
                    short resultValue = ReturnCode.SUCCESS.shortValue();
                    switch (topicName) {
                        case MqttTopics.DEVICE_ATTRIBUTES_TOPIC:
                        case MqttTopics.DEVICE_ATTRIBUTES_SHORT_TOPIC:
                        case MqttTopics.DEVICE_ATTRIBUTES_SHORT_PROTO_TOPIC:
                        case MqttTopics.DEVICE_ATTRIBUTES_SHORT_JSON_TOPIC: {
                            transportService.process(deviceSessionCtx.getSessionInfo(),
                                    TransportProtos.SubscribeToAttributeUpdatesMsg.newBuilder().setUnsubscribe(true).build(), null);
                            activityReported = true;
                            break;
                        }
                        case MqttTopics.DEVICE_RPC_REQUESTS_SUB_TOPIC:
                        case MqttTopics.DEVICE_RPC_REQUESTS_SUB_SHORT_TOPIC:
                        case MqttTopics.DEVICE_RPC_REQUESTS_SUB_SHORT_JSON_TOPIC:
                        case MqttTopics.DEVICE_RPC_REQUESTS_SUB_SHORT_PROTO_TOPIC: {
                            transportService.process(deviceSessionCtx.getSessionInfo(),
                                    TransportProtos.SubscribeToRPCMsg.newBuilder().setUnsubscribe(true).build(), null);
                            activityReported = true;
                            break;
                        }
                        case MqttTopics.DEVICE_RPC_RESPONSE_SUB_TOPIC:
                        case MqttTopics.DEVICE_RPC_RESPONSE_SUB_SHORT_TOPIC:
                        case MqttTopics.DEVICE_RPC_RESPONSE_SUB_SHORT_JSON_TOPIC:
                        case MqttTopics.DEVICE_RPC_RESPONSE_SUB_SHORT_PROTO_TOPIC:
                        case MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_TOPIC:
                        case MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_SHORT_TOPIC:
                        case MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_SHORT_JSON_TOPIC:
                        case MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_SHORT_PROTO_TOPIC:
                        case MqttTopics.GATEWAY_ATTRIBUTES_TOPIC:
                        case MqttTopics.GATEWAY_RPC_TOPIC:
                        case MqttTopics.GATEWAY_ATTRIBUTES_RESPONSE_TOPIC:
                        case MqttTopics.DEVICE_PROVISION_RESPONSE_TOPIC:
                        case MqttTopics.DEVICE_FIRMWARE_RESPONSES_TOPIC:
                        case MqttTopics.DEVICE_FIRMWARE_ERROR_TOPIC:
                        case MqttTopics.DEVICE_SOFTWARE_RESPONSES_TOPIC:
                        case MqttTopics.DEVICE_SOFTWARE_ERROR_TOPIC: {
                            activityReported = true;
                            break;
                        }
                        default:
                            log.trace("[{}] Failed to process unsubscription [{}] to [{}]", sessionId, mqttMsg.variableHeader().messageId(), topicName);
                            resultValue = ReturnCode.TOPIC_FILTER_INVALID.shortValue();
                    }
                    unSubResults.add(resultValue);
                } catch (Exception e) {
                    log.debug("[{}] Failed to process unsubscription [{}] to [{}]", sessionId, mqttMsg.variableHeader().messageId(), topicName);
                    unSubResults.add(ReturnCode.IMPLEMENTATION_SPECIFIC.shortValue());
                }
            } else {
                log.debug("[{}] Failed to process unsubscription [{}] to [{}] - Subscription not found", sessionId, mqttMsg.variableHeader().messageId(), topicName);
                unSubResults.add(ReturnCode.NO_SUBSCRIPTION_EXISTED.shortValue());
            }
        }
        if (!activityReported) {
            log.info("[processUnsubscribe]: {}", deviceSessionCtx.getSessionInfo());
            transportService.reportActivity(deviceSessionCtx.getSessionInfo());
        }
        ctx.writeAndFlush(createUnSubAckMessage(mqttMsg.variableHeader().messageId(), unSubResults));
    }

    private MqttMessage createUnSubAckMessage(int msgId, List<Short> resultCodes) {
        MqttMessageBuilders.UnsubAckBuilder unsubAckBuilder = MqttMessageBuilders.unsubAck();
        unsubAckBuilder.packetId(msgId);
        if (MqttVersion.MQTT_5.equals(deviceSessionCtx.getMqttVersion())) {
            unsubAckBuilder.addReasonCodes(resultCodes.toArray(Short[]::new));
        }
        return unsubAckBuilder.build();
    }

    void processConnect(ChannelHandlerContext ctx, MqttConnectMessage msg) {
        log.debug("[{}][{}] Processing connect msg for client: {}!", address, sessionId, msg.payload().clientIdentifier());
        String userName = msg.payload().userName();
        String clientId = msg.payload().clientIdentifier();
        deviceSessionCtx.setMqttVersion(getMqttVersion(msg.variableHeader().version()));
        if (DataConstants.PROVISION.equals(userName) || DataConstants.PROVISION.equals(clientId)) {
            deviceSessionCtx.setProvisionOnly(true);
            ctx.writeAndFlush(createMqttConnAckMsg(ReturnCode.SUCCESS, msg));
        } else {
            X509Certificate cert;
            if (sslHandler != null && (cert = getX509Certificate()) != null) {
                processX509CertConnect(ctx, cert, msg);
            } else {
                processAuthTokenConnect(ctx, msg);
            }
        }
    }

    private void processAuthTokenConnect(ChannelHandlerContext ctx, MqttConnectMessage connectMessage) {
        String userName = connectMessage.payload().userName();
        log.debug("[{}][{}] Processing connect msg for client with user name: {}!", address, sessionId, userName);
        TransportProtos.ValidateBasicMqttCredRequestMsg.Builder request = TransportProtos.ValidateBasicMqttCredRequestMsg.newBuilder()
                .setClientId(connectMessage.payload().clientIdentifier());
        if (userName != null) {
            request.setUserName(userName);
        }
        byte[] passwordBytes = connectMessage.payload().passwordInBytes();
        if (passwordBytes != null) {
            String password = new String(passwordBytes, CharsetUtil.UTF_8);
            request.setPassword(password);
        }
        transportService.process(DeviceTransportType.MQTT, request.build(),
                new TransportServiceCallback<>() {
                    @Override
                    public void onSuccess(ValidateDeviceCredentialsResponse msg) {
                        onValidateDeviceResponse(msg, ctx, connectMessage);
                    }

                    @Override
                    public void onError(Throwable e) {
                        log.trace("[{}] Failed to process credentials: {}", address, userName, e);
                        ctx.writeAndFlush(createMqttConnAckMsg(ReturnCode.SERVER_UNAVAILABLE_5, connectMessage));
                        ctx.close();
                    }
                });
    }

    private void processX509CertConnect(ChannelHandlerContext ctx, X509Certificate cert, MqttConnectMessage connectMessage) {
        try {
            if (!context.isSkipValidityCheckForClientCert()) {
                cert.checkValidity();
            }
            String strCert = SslUtil.getCertificateString(cert);
            String sha3Hash = EncryptionUtil.getSha3Hash(strCert);
            transportService.process(DeviceTransportType.MQTT, ValidateDeviceX509CertRequestMsg.newBuilder().setHash(sha3Hash).build(),
                    new TransportServiceCallback<>() {
                        @Override
                        public void onSuccess(ValidateDeviceCredentialsResponse msg) {
                            onValidateDeviceResponse(msg, ctx, connectMessage);
                        }

                        @Override
                        public void onError(Throwable e) {
                            log.trace("[{}] Failed to process credentials: {}", address, sha3Hash, e);
                            ctx.writeAndFlush(createMqttConnAckMsg(ReturnCode.SERVER_UNAVAILABLE_5, connectMessage));
                            ctx.close();
                        }
                    });
        } catch (Exception e) {
            context.onAuthFailure(address);
            ctx.writeAndFlush(createMqttConnAckMsg(ReturnCode.NOT_AUTHORIZED_5, connectMessage));
            log.trace("[{}] X509 auth failure: {}", sessionId, address, e);
            ctx.close();
        }
    }

    private X509Certificate getX509Certificate() {
        try {
            Certificate[] certChain = sslHandler.engine().getSession().getPeerCertificates();
            if (certChain.length > 0) {
                return (X509Certificate) certChain[0];
            }
        } catch (SSLPeerUnverifiedException e) {
            log.warn(e.getMessage());
            return null;
        }
        return null;
    }

    private MqttConnAckMessage createMqttConnAckMsg(ReturnCode returnCode, MqttConnectMessage msg) {
        MqttMessageBuilders.ConnAckBuilder connAckBuilder = MqttMessageBuilders.connAck();
        connAckBuilder.sessionPresent(!msg.variableHeader().isCleanSession());
        MqttConnectReturnCode finalReturnCode = ReturnCodeResolver.getConnectionReturnCode(deviceSessionCtx.getMqttVersion(), returnCode);
        connAckBuilder.returnCode(finalReturnCode);
        return connAckBuilder.build();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof IOException) {
            if (log.isDebugEnabled()) {
                log.debug("[{}][{}][{}] IOException: {}", sessionId,
                        Optional.ofNullable(this.deviceSessionCtx.getDeviceInfo()).map(TransportDeviceInfo::getDeviceId).orElse(null),
                        Optional.ofNullable(this.deviceSessionCtx.getDeviceInfo()).map(TransportDeviceInfo::getDeviceName).orElse(""),
                        cause);
            } else if (log.isInfoEnabled()) {
                log.info("[{}][{}][{}] IOException: {}", sessionId,
                        Optional.ofNullable(this.deviceSessionCtx.getDeviceInfo()).map(TransportDeviceInfo::getDeviceId).orElse(null),
                        Optional.ofNullable(this.deviceSessionCtx.getDeviceInfo()).map(TransportDeviceInfo::getDeviceName).orElse(""),
                        cause.getMessage());
            }
        } else {
            log.error("[{}] Unexpected Exception", sessionId, cause);
        }

        ctx.close();
        if (cause instanceof OutOfMemoryError) {
            log.error("Received critical error. Going to shutdown the service.");
            System.exit(1);
        }
    }

    private static MqttSubAckMessage createSubAckMessage(Integer msgId, List<Integer> grantedQoSList) {
        MqttFixedHeader mqttFixedHeader =
                new MqttFixedHeader(SUBACK, false, AT_MOST_ONCE, false, 0);
        MqttMessageIdVariableHeader mqttMessageIdVariableHeader = MqttMessageIdVariableHeader.from(msgId);
        MqttSubAckPayload mqttSubAckPayload = new MqttSubAckPayload(grantedQoSList);
        return new MqttSubAckMessage(mqttFixedHeader, mqttMessageIdVariableHeader, mqttSubAckPayload);
    }

    private static int getMinSupportedQos(MqttQoS reqQoS) {
        return Math.min(reqQoS.value(), MAX_SUPPORTED_QOS_LVL.value());
    }

    private static MqttVersion getMqttVersion(int versionCode) {
        switch (versionCode) {
            case 3:
                return MqttVersion.MQTT_3_1;
            case 5:
                return MqttVersion.MQTT_5;
            default:
                return MqttVersion.MQTT_3_1_1;
        }
    }

    public static MqttMessage createMqttPubAckMsg(DeviceSessionCtx deviceSessionCtx, int requestId, ReturnCode returnCode) {
        MqttMessageBuilders.PubAckBuilder pubAckMsgBuilder = MqttMessageBuilders.pubAck().packetId(requestId);
        if (MqttVersion.MQTT_5.equals(deviceSessionCtx.getMqttVersion())) {
            pubAckMsgBuilder.reasonCode(returnCode.byteValue());
        }
        return pubAckMsgBuilder.build();
    }

    private boolean checkConnected(ChannelHandlerContext ctx, MqttMessage msg) {
        if (deviceSessionCtx.isConnected()) {
            return true;
        } else {
            log.info("[{}] Closing current session due to invalid msg order: {}", sessionId, msg);
            return false;
        }
    }

    private void checkGatewaySession(SessionMetaData sessionMetaData) {
        TransportDeviceInfo device = deviceSessionCtx.getDeviceInfo();
        try {
            JsonNode infoNode = context.getMapper().readTree(device.getAdditionalInfo());
            if (infoNode != null) {
                JsonNode gatewayNode = infoNode.get("gateway");
                if (gatewayNode != null && gatewayNode.asBoolean()) {
                    gatewaySessionHandler = new GatewaySessionHandler(deviceSessionCtx, sessionId);
                    if (infoNode.has(DefaultTransportService.OVERWRITE_ACTIVITY_TIME) && infoNode.get(DefaultTransportService.OVERWRITE_ACTIVITY_TIME).isBoolean()) {
                        sessionMetaData.setOverwriteActivityTime(infoNode.get(DefaultTransportService.OVERWRITE_ACTIVITY_TIME).asBoolean());
                    }
                }
            }
        } catch (IOException e) {
            log.trace("[{}][{}] Failed to fetch device additional info", sessionId, device.getDeviceName(), e);
        }
    }

    @Override
    public void operationComplete(Future<? super Void> future) throws Exception {
        log.trace("[{}] Channel closed!", sessionId);
        doDisconnect();
    }

    public void doDisconnect() {
        if (deviceSessionCtx.isConnected()) {
            log.debug("[{}] Client disconnected!", sessionId);
            transportService.process(deviceSessionCtx.getSessionInfo(), SESSION_EVENT_MSG_CLOSED, null);
            transportService.deregisterSession(deviceSessionCtx.getSessionInfo());
            if (gatewaySessionHandler != null) {
                gatewaySessionHandler.onGatewayDisconnect();
            }
            deviceSessionCtx.setDisconnected();
        }
        deviceSessionCtx.release();
    }


    private void onValidateDeviceResponse(ValidateDeviceCredentialsResponse msg, ChannelHandlerContext ctx, MqttConnectMessage connectMessage) {
        if (!msg.hasDeviceInfo()) {
            context.onAuthFailure(address);
            ReturnCode returnCode = ReturnCode.NOT_AUTHORIZED_5;
            if (sslHandler == null || getX509Certificate() == null) {
                String username = connectMessage.payload().userName();
                byte[] passwordBytes = connectMessage.payload().passwordInBytes();
                String clientId = connectMessage.payload().clientIdentifier();
                if ((username != null && passwordBytes != null && clientId != null)
                        || (username == null ^ passwordBytes == null)) {
                    returnCode = ReturnCode.BAD_USERNAME_OR_PASSWORD;
                } else if (!StringUtils.isBlank(clientId)) {
                    returnCode = ReturnCode.CLIENT_IDENTIFIER_NOT_VALID;
                }
            }
            ctx.writeAndFlush(createMqttConnAckMsg(returnCode, connectMessage));
            ctx.close();
        } else {
            context.onAuthSuccess(address);
            deviceSessionCtx.setDeviceInfo(msg.getDeviceInfo());
            deviceSessionCtx.setDeviceProfile(msg.getDeviceProfile());
            deviceSessionCtx.setSessionInfo(SessionInfoCreator.create(msg, context, sessionId));
            transportService.process(deviceSessionCtx.getSessionInfo(), SESSION_EVENT_MSG_OPEN, new TransportServiceCallback<Void>() {
                @Override
                public void onSuccess(Void msg) {
                    SessionMetaData sessionMetaData = transportService.registerAsyncSession(deviceSessionCtx.getSessionInfo(), MqttTransportHandler.this);
                    checkGatewaySession(sessionMetaData);
                    ctx.writeAndFlush(createMqttConnAckMsg(ReturnCode.SUCCESS, connectMessage));
                    deviceSessionCtx.setConnected(true);
                    log.debug("[{}] Client connected!", sessionId);
                    transportService.getCallbackExecutor().execute(() -> processMsgQueue(ctx)); //this callback will execute in Producer worker thread and hard or blocking work have to be submitted to the separate thread.
                }

                @Override
                public void onError(Throwable e) {
                    if (e instanceof TbRateLimitsException) {
                        log.trace("[{}] Failed to submit session event: {}", sessionId, e.getMessage());
                    } else {
                        log.warn("[{}] Failed to submit session event", sessionId, e);
                    }
                    ctx.writeAndFlush(createMqttConnAckMsg(ReturnCode.SERVER_UNAVAILABLE_5, connectMessage));
                    ctx.close();
                }
            });
        }
    }

    @Override
    public void onGetAttributesResponse(TransportProtos.GetAttributeResponseMsg response) {
        log.trace("[{}] Received get attributes response", sessionId);
        String topicBase = attrReqTopicType.getAttributesResponseTopicBase();
        MqttTransportAdaptor adaptor = deviceSessionCtx.getAdaptor(attrReqTopicType);
        try {
            adaptor.convertToPublish(deviceSessionCtx, response, topicBase).ifPresent(deviceSessionCtx.getChannel()::writeAndFlush);
        } catch (Exception e) {
            log.trace("[{}] Failed to convert device attributes response to MQTT msg", sessionId, e);
        }
    }

    @Override
    public void onAttributeUpdate(UUID sessionId, TransportProtos.AttributeUpdateNotificationMsg notification) {
        log.trace("[{}] Received attributes update notification to device", sessionId);
        String topic = attrSubTopicType.getAttributesSubTopic();
        MqttTransportAdaptor adaptor = deviceSessionCtx.getAdaptor(attrSubTopicType);
        try {
            adaptor.convertToPublish(deviceSessionCtx, notification, topic).ifPresent(deviceSessionCtx.getChannel()::writeAndFlush);
        } catch (Exception e) {
            log.trace("[{}] Failed to convert device attributes update to MQTT msg", sessionId, e);
        }
    }

    @Override
    public void onRemoteSessionCloseCommand(UUID sessionId, TransportProtos.SessionCloseNotificationProto sessionCloseNotification) {
        log.trace("[{}] Received the remote command to close the session: {}", sessionId, sessionCloseNotification.getMessage());
        deviceSessionCtx.getChannel().close();
    }

    @Override
    public void onToDeviceRpcRequest(UUID sessionId, TransportProtos.ToDeviceRpcRequestMsg rpcRequest) {
        log.trace("[{}] Received RPC command to device", sessionId);
        String baseTopic = rpcSubTopicType.getRpcRequestTopicBase();
        MqttTransportAdaptor adaptor = deviceSessionCtx.getAdaptor(rpcSubTopicType);
        try {
            adaptor.convertToPublish(deviceSessionCtx, rpcRequest, baseTopic).ifPresent(payload -> {
                int msgId = ((MqttPublishMessage) payload).variableHeader().packetId();
                if (isAckExpected(payload)) {
                    rpcAwaitingAck.put(msgId, rpcRequest);
                    context.getScheduler().schedule(() -> {
                        TransportProtos.ToDeviceRpcRequestMsg msg = rpcAwaitingAck.remove(msgId);
                        if (msg != null) {
                            transportService.process(deviceSessionCtx.getSessionInfo(), rpcRequest, RpcStatus.TIMEOUT, false, TransportServiceCallback.EMPTY);
                        }
                    }, Math.max(0, Math.min(deviceSessionCtx.getContext().getTimeout(), rpcRequest.getExpirationTime() - System.currentTimeMillis())), TimeUnit.MILLISECONDS);
                }
                var cf = publish(payload, deviceSessionCtx);
                cf.addListener(result -> {
                    if (result.cause() == null) {
                        if (!isAckExpected(payload)) {
                            transportService.process(deviceSessionCtx.getSessionInfo(), rpcRequest, RpcStatus.DELIVERED, rpcRequest.getOneway(), TransportServiceCallback.EMPTY);
                        } else if (rpcRequest.getPersisted()) {
                            transportService.process(deviceSessionCtx.getSessionInfo(), rpcRequest, RpcStatus.SENT, false, TransportServiceCallback.EMPTY);
                        }
                    } else {
                        // TODO: send error
                    }
                });
            });
        } catch (Exception e) {
            transportService.process(deviceSessionCtx.getSessionInfo(),
                    TransportProtos.ToDeviceRpcResponseMsg.newBuilder()
                            .setRequestId(rpcRequest.getRequestId()).setError("Failed to convert device RPC command to MQTT msg").build(), TransportServiceCallback.EMPTY);
            log.trace("[{}] Failed to convert device RPC command to MQTT msg", sessionId, e);
        }
    }

    @Override
    public void onToServerRpcResponse(TransportProtos.ToServerRpcResponseMsg rpcResponse) {
        log.trace("[{}] Received RPC response from server", sessionId);
        String baseTopic = toServerRpcSubTopicType.getRpcResponseTopicBase();
        MqttTransportAdaptor adaptor = deviceSessionCtx.getAdaptor(toServerRpcSubTopicType);
        try {
            adaptor.convertToPublish(deviceSessionCtx, rpcResponse, baseTopic).ifPresent(deviceSessionCtx.getChannel()::writeAndFlush);
        } catch (Exception e) {
            log.trace("[{}] Failed to convert device RPC command to MQTT msg", sessionId, e);
        }
    }

    private ChannelFuture publish(MqttMessage message, DeviceSessionCtx deviceSessionCtx) {
        return deviceSessionCtx.getChannel().writeAndFlush(message);
    }

    private boolean isAckExpected(MqttMessage message) {
        return message.fixedHeader().qosLevel().value() > 0;
    }

    @Override
    public void onDeviceProfileUpdate(TransportProtos.SessionInfoProto sessionInfo, DeviceProfile deviceProfile) {
        deviceSessionCtx.onDeviceProfileUpdate(sessionInfo, deviceProfile);
    }

    @Override
    public void onDeviceUpdate(TransportProtos.SessionInfoProto sessionInfo, Device device, Optional<DeviceProfile> deviceProfileOpt) {
        deviceSessionCtx.onDeviceUpdate(sessionInfo, device, deviceProfileOpt);
    }

    @Override
    public void onDeviceDeleted(DeviceId deviceId) {
        context.onAuthFailure(address);
        ChannelHandlerContext ctx = deviceSessionCtx.getChannel();
        ctx.close();
    }

}
