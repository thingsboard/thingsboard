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
package org.thingsboard.server.transport.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonParseException;
import com.google.protobuf.InvalidProtocolBufferException;
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
import io.netty.handler.codec.mqtt.MqttProperties.IntegerProperty;
import io.netty.handler.codec.mqtt.MqttProperties.MqttPropertyType;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttReasonCodes;
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
import org.eclipse.leshan.core.ResponseCode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.common.transport.SessionMsgListener;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.SessionInfoCreator;
import org.thingsboard.server.common.transport.auth.TransportDeviceInfo;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.common.transport.service.SessionMetaData;
import org.thingsboard.server.common.transport.util.SslUtil;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ProvisionDeviceResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceX509CertRequestMsg;
import org.thingsboard.server.gen.transport.mqtt.SparkplugBProto;
import org.thingsboard.server.queue.scheduler.SchedulerComponent;
import org.thingsboard.server.transport.mqtt.adaptors.MqttTransportAdaptor;
import org.thingsboard.server.transport.mqtt.adaptors.ProtoMqttAdaptor;
import org.thingsboard.server.transport.mqtt.limits.GatewaySessionLimits;
import org.thingsboard.server.transport.mqtt.limits.SessionLimits;
import org.thingsboard.server.transport.mqtt.session.DeviceSessionCtx;
import org.thingsboard.server.transport.mqtt.session.GatewaySessionHandler;
import org.thingsboard.server.transport.mqtt.session.MqttTopicMatcher;
import org.thingsboard.server.transport.mqtt.session.SparkplugDeviceSessionContext;
import org.thingsboard.server.transport.mqtt.session.SparkplugNodeSessionHandler;
import org.thingsboard.server.transport.mqtt.util.ReturnCodeResolver;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugRpcRequestHeader;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugRpcResponseBody;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopic;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.netty.handler.codec.mqtt.MqttMessageType.CONNECT;
import static io.netty.handler.codec.mqtt.MqttMessageType.PINGRESP;
import static io.netty.handler.codec.mqtt.MqttMessageType.SUBACK;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_LEAST_ONCE;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SESSION_EVENT_MSG_CLOSED;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SESSION_EVENT_MSG_OPEN;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SUBSCRIBE_TO_ATTRIBUTE_UPDATES_ASYNC_MSG;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SUBSCRIBE_TO_RPC_ASYNC_MSG;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugConnectionState.OFFLINE;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.NBIRTH;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.NCMD;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.NDEATH;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMetricUtil.SPARKPLUG_BD_SEQUENCE_NUMBER_KEY;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMetricUtil.getTsKvProto;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMetricUtil.getTsKvProtoFromJsonNode;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopic.parseTopic;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopicService.parseTopicPublish;

@Slf4j
public class MqttTransportHandler extends ChannelInboundHandlerAdapter implements GenericFutureListener<Future<? super Void>>, SessionMsgListener {

    private static final Pattern FW_REQUEST_PATTERN = Pattern.compile(MqttTopics.DEVICE_FIRMWARE_REQUEST_TOPIC_PATTERN);
    private static final Pattern SW_REQUEST_PATTERN = Pattern.compile(MqttTopics.DEVICE_SOFTWARE_REQUEST_TOPIC_PATTERN);

    private static final String SESSION_LIMITS = "getSessionLimits";

    private static final String PAYLOAD_TOO_LARGE = "PAYLOAD_TOO_LARGE";

    private static final MqttQoS MAX_SUPPORTED_QOS_LVL = AT_LEAST_ONCE;

    private final UUID sessionId;

    protected final MqttTransportContext context;
    private final TransportService transportService;
    private final SchedulerComponent scheduler;
    private final SslHandler sslHandler;
    private final ConcurrentMap<MqttTopicMatcher, Integer> mqttQoSMap;

    final DeviceSessionCtx deviceSessionCtx;
    volatile InetSocketAddress address;
    volatile GatewaySessionHandler gatewaySessionHandler;
    volatile SparkplugNodeSessionHandler sparkplugSessionHandler;

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
                    closeCtx(ctx, MqttReasonCodes.Disconnect.MALFORMED_PACKET);
                }
            } else {
                log.debug("[{}] Received non mqtt message: {}", sessionId, msg.getClass().getSimpleName());
                closeCtx(ctx, (MqttMessage) null);
            }
        } finally {
            ReferenceCountUtil.safeRelease(msg);
        }
    }

    private void closeCtx(ChannelHandlerContext ctx, MqttReasonCodes.Disconnect returnCode) {
        closeCtx(ctx, returnCode.byteValue());
    }

    private void closeCtx(ChannelHandlerContext ctx, MqttConnectReturnCode returnCode) {
        closeCtx(ctx, ReturnCodeResolver.getConnectionReturnCode(deviceSessionCtx.getMqttVersion(), returnCode).byteValue());
    }

    private void closeCtx(ChannelHandlerContext ctx, byte returnCode) {
        closeCtx(ctx, createMqttDisconnectMsg(deviceSessionCtx, returnCode));
    }

    private void closeCtx(ChannelHandlerContext ctx, MqttMessage msg) {
        if (!rpcAwaitingAck.isEmpty()) {
            log.debug("[{}] Cleanup RPC awaiting ack map due to session close!", sessionId);
            rpcAwaitingAck.clear();
        }

        if (ctx.channel() == null) {
            log.debug("[{}] Channel is null, closing ctx...", sessionId);
            ctx.close();
        } else if (ctx.channel().isOpen()) {
            if (msg != null && MqttVersion.MQTT_5 == deviceSessionCtx.getMqttVersion()) {
                ChannelFuture channelFuture = ctx.writeAndFlush(msg).addListener(future -> ctx.close());
                scheduler.schedule(() -> {
                    if (!channelFuture.isDone()) {
                        log.debug("[{}] Closing channel due to timeout!", sessionId);
                        ctx.close();
                    }
                }, context.getDisconnectTimeout(), TimeUnit.MILLISECONDS);
            } else {
                ctx.close();
            }
        } else {
            log.debug("[{}] Channel is already closed!", sessionId);
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
            closeCtx(ctx, MqttReasonCodes.Disconnect.PROTOCOL_ERROR);
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
                        ack(ctx, msgId, MqttReasonCodes.PubAck.TOPIC_NAME_INVALID);
                        closeCtx(ctx, MqttReasonCodes.Disconnect.TOPIC_NAME_INVALID);
                    }
                } catch (RuntimeException e) {
                    log.warn("[{}] Failed to process publish msg [{}][{}]", sessionId, topicName, msgId, e);
                    ack(ctx, msgId, MqttReasonCodes.PubAck.IMPLEMENTATION_SPECIFIC_ERROR);
                    closeCtx(ctx, MqttReasonCodes.Disconnect.IMPLEMENTATION_SPECIFIC_ERROR);
                } catch (AdaptorException e) {
                    log.debug("[{}] Failed to process publish msg [{}][{}]", sessionId, topicName, msgId, e);
                    sendResponseForAdaptorErrorOrCloseContext(ctx, topicName, msgId);
                }
                break;
            case SUBSCRIBE:
                MqttSubscribeMessage subscribeMessage = (MqttSubscribeMessage) msg;
                processSubscribe(ctx, subscribeMessage);
                break;
            case UNSUBSCRIBE:
                MqttUnsubscribeMessage unsubscribeMessage = (MqttUnsubscribeMessage) msg;
                processUnsubscribe(ctx, unsubscribeMessage);
                break;
            case PINGREQ:
                ctx.writeAndFlush(new MqttMessage(new MqttFixedHeader(PINGRESP, false, AT_MOST_ONCE, false, 0)));
                break;
            case DISCONNECT:
                closeCtx(ctx, MqttReasonCodes.Disconnect.NORMAL_DISCONNECT);
                break;
        }
    }

    void enqueueRegularSessionMsg(ChannelHandlerContext ctx, MqttMessage msg) {
        final int queueSize = deviceSessionCtx.getMsgQueueSize();
        if (queueSize >= context.getMessageQueueSizePerDeviceLimit()) {
            log.info("Closing current session because msq queue size for device {} exceed limit {} with msgQueueSize counter {} and actual queue size {}",
                    deviceSessionCtx.getDeviceId(), context.getMessageQueueSizePerDeviceLimit(), queueSize, deviceSessionCtx.getMsgQueueSize());
            closeCtx(ctx, MqttReasonCodes.Disconnect.QUOTA_EXCEEDED);
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
                    transportService.recordActivity(deviceSessionCtx.getSessionInfo());
                    if (gatewaySessionHandler != null) {
                        gatewaySessionHandler.onGatewayPing();
                    }
                }
                break;
            case DISCONNECT:
                closeCtx(ctx, MqttReasonCodes.Disconnect.NORMAL_DISCONNECT);
                break;
            case PUBACK:
                int msgId = ((MqttPubAckMessage) msg).variableHeader().messageId();
                TransportProtos.ToDeviceRpcRequestMsg rpcRequest = rpcAwaitingAck.remove(msgId);
                if (rpcRequest != null) {
                    transportService.process(deviceSessionCtx.getSessionInfo(), rpcRequest, RpcStatus.DELIVERED, true, TransportServiceCallback.EMPTY);
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
                transportService.recordActivity(deviceSessionCtx.getSessionInfo());
            } else {
                log.error("[gatewaySessionHandler] is null, [{}] Failed to process publish msg [{}][{}]", sessionId, topicName, msgId);
            }
        } else if (sparkplugSessionHandler != null) {
            handleSparkplugPublishMsg(ctx, topicName, mqttMsg);
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
                    ack(ctx, msgId, MqttReasonCodes.PubAck.TOPIC_NAME_INVALID);
            }
        } catch (RuntimeException e) {
            log.warn("[{}] Failed to process publish msg [{}][{}]", sessionId, topicName, msgId, e);
            ack(ctx, msgId, MqttReasonCodes.PubAck.IMPLEMENTATION_SPECIFIC_ERROR);
            closeCtx(ctx, MqttReasonCodes.Disconnect.IMPLEMENTATION_SPECIFIC_ERROR);
        } catch (AdaptorException e) {
            log.debug("[{}] Failed to process publish msg [{}][{}]", sessionId, topicName, msgId, e);
            sendResponseForAdaptorErrorOrCloseContext(ctx, topicName, msgId);
        }
    }

    /**
     * It may be the case that an Edge Node has many dynamic associated devices.
     * Publish: spBv1.0/G1/DBIRTH/E1/+
     * Publish: spBv1.0/G1/DDATA/E1/+
     * Publish: spBv1.0/G1/DCMD/E1/+
     * Publish: spBv1.0/G1/DDEATH/E1/+
     * @param ctx
     * @param topicName
     * @param mqttMsg
     */

    private void handleSparkplugPublishMsg(ChannelHandlerContext ctx, String topicName, MqttPublishMessage mqttMsg) {
        int msgId = mqttMsg.variableHeader().packetId();
        try {
            SparkplugTopic sparkplugTopic = parseTopicPublish(topicName);
            boolean isWildcardInPublish = topicName.contains("+");
            if (!isWildcardInPublish && sparkplugTopic.isNode()) {
                // A node topic
                SparkplugBProto.Payload sparkplugBProtoNode = SparkplugBProto.Payload.parseFrom(ProtoMqttAdaptor.toBytes(mqttMsg.payload()));
                switch (sparkplugTopic.getType()) {
                    case NBIRTH:
                    case NCMD:
                    case NDATA:
                        sparkplugSessionHandler.onAttributesTelemetryProto(msgId, sparkplugBProtoNode, sparkplugTopic);
                        break;
                    case NDEATH:
                        if (sparkplugSessionHandler.onValidateNDEATH(sparkplugBProtoNode)) {
                            doDisconnect();
                            break;
                        } else {
                            throw new ThingsboardException(SPARKPLUG_BD_SEQUENCE_NUMBER_KEY + " of " + NDEATH.name() + " is not equals " +
                                    SPARKPLUG_BD_SEQUENCE_NUMBER_KEY + " of " + NBIRTH.name(), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
                        }
                    default:
                }
            } else {
                // A device topic
                SparkplugBProto.Payload sparkplugBProtoDevice = SparkplugBProto.Payload.parseFrom(ProtoMqttAdaptor.toBytes(mqttMsg.payload()));
                if (isWildcardInPublish) {
                    for (Entry<String, SparkplugDeviceSessionContext> entry : sparkplugSessionHandler.getDevices().entrySet()) {
                        String deviceName = entry.getKey();
                        SparkplugTopic sparkplugTopicDevice = sparkplugTopic;
                        sparkplugTopicDevice.updateDeviceIdPlus(deviceName);
                        handleSparkplugPublishDeviceMsg(sparkplugTopicDevice, msgId, mqttMsg, sparkplugBProtoDevice);
                    }
                } else {
                    handleSparkplugPublishDeviceMsg(sparkplugTopic, msgId, mqttMsg, sparkplugBProtoDevice);
                }
            }
        } catch (RuntimeException e) {
            log.error("[{}] Failed to process publish msg [{}][{}]", sessionId, topicName, msgId, e);
            ack(ctx, msgId, MqttReasonCodes.PubAck.IMPLEMENTATION_SPECIFIC_ERROR);
            closeCtx(ctx, MqttReasonCodes.Disconnect.IMPLEMENTATION_SPECIFIC_ERROR);
        } catch (AdaptorException | ThingsboardException | InvalidProtocolBufferException e) {
            log.error("[{}] Failed to process publish msg [{}][{}]", sessionId, topicName, msgId, e);
            sendResponseForAdaptorErrorOrCloseContext(ctx, topicName, msgId);
        }
    }

    /**
     * It may be the case that an Edge Node has many dynamic associated devices.
     * Publish: spBv1.0/G1/DBIRTH/E1/+
     * Publish: spBv1.0/G1/DDATA/E1/+
     * Publish: spBv1.0/G1/DCMD/E1/+
     * Publish: spBv1.0/G1/DDEATH/E1/+
     * @param sparkplugTopic
     * @param msgId
     * @param mqttMsg
     * @throws AdaptorException
     * @throws ThingsboardException
     * @throws InvalidProtocolBufferException
     */
    private void handleSparkplugPublishDeviceMsg(SparkplugTopic sparkplugTopic, int msgId,
                                                 MqttPublishMessage mqttMsg, SparkplugBProto.Payload sparkplugBProtoDevice)
            throws AdaptorException, ThingsboardException, InvalidProtocolBufferException {
        // A device topic
        switch (sparkplugTopic.getType()) {
            case DBIRTH:
            case DCMD:
            case DDATA:
                sparkplugSessionHandler.onAttributesTelemetryProto(msgId, sparkplugBProtoDevice, sparkplugTopic);
                break;
            case DDEATH:
                sparkplugSessionHandler.onDeviceDisconnect(mqttMsg, sparkplugTopic.getDeviceId());
                break;
            default:
        }
    }

    private void processDevicePublish(ChannelHandlerContext ctx, MqttPublishMessage mqttMsg, String topicName, int msgId) {
        try {
            Matcher fwMatcher;
            MqttTransportAdaptor payloadAdaptor = deviceSessionCtx.getPayloadAdaptor();
            if (deviceSessionCtx.isDeviceAttributesTopic(topicName)) {
                TransportProtos.PostAttributeMsg postAttributeMsg = payloadAdaptor.convertToPostAttributes(deviceSessionCtx, mqttMsg);
                transportService.process(deviceSessionCtx.getSessionInfo(), postAttributeMsg, getMetadata(deviceSessionCtx, topicName),
                        getPubAckCallback(ctx, msgId, postAttributeMsg));
            } else if (deviceSessionCtx.isDeviceTelemetryTopic(topicName)) {
                TransportProtos.PostTelemetryMsg postTelemetryMsg = payloadAdaptor.convertToPostTelemetry(deviceSessionCtx, mqttMsg);
                transportService.process(deviceSessionCtx.getSessionInfo(), postTelemetryMsg, getMetadata(deviceSessionCtx, topicName),
                        getPubAckCallback(ctx, msgId, postTelemetryMsg));
            } else if (topicName.startsWith(MqttTopics.DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX)) {
                TransportProtos.GetAttributeRequestMsg getAttributeMsg = payloadAdaptor.convertToGetAttributes(deviceSessionCtx, mqttMsg, MqttTopics.DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX);
                transportService.process(deviceSessionCtx.getSessionInfo(), getAttributeMsg, getPubAckCallback(ctx, msgId, getAttributeMsg));
                attrReqTopicType = TopicType.V1;
            } else if (topicName.startsWith(MqttTopics.DEVICE_RPC_RESPONSE_TOPIC)) {
                TransportProtos.ToDeviceRpcResponseMsg rpcResponseMsg = payloadAdaptor.convertToDeviceRpcResponse(deviceSessionCtx, mqttMsg, MqttTopics.DEVICE_RPC_RESPONSE_TOPIC);
                transportService.process(deviceSessionCtx.getSessionInfo(), rpcResponseMsg, getPubAckCallback(ctx, msgId, rpcResponseMsg));
            } else if (topicName.startsWith(MqttTopics.DEVICE_RPC_REQUESTS_TOPIC)) {
                TransportProtos.ToServerRpcRequestMsg rpcRequestMsg = payloadAdaptor.convertToServerRpcRequest(deviceSessionCtx, mqttMsg, MqttTopics.DEVICE_RPC_REQUESTS_TOPIC);
                toServerRpcSubTopicType = TopicType.V1;
                if (SESSION_LIMITS.equals(rpcRequestMsg.getMethodName())) {
                    onGetSessionLimitsRpc(deviceSessionCtx.getSessionInfo(), ctx, msgId, rpcRequestMsg);
                } else {
                    transportService.process(deviceSessionCtx.getSessionInfo(), rpcRequestMsg, getPubAckCallback(ctx, msgId, rpcRequestMsg));
                }
            } else if (topicName.equals(MqttTopics.DEVICE_CLAIM_TOPIC)) {
                TransportProtos.ClaimDeviceMsg claimDeviceMsg = payloadAdaptor.convertToClaimDevice(deviceSessionCtx, mqttMsg);
                transportService.process(deviceSessionCtx.getSessionInfo(), claimDeviceMsg, getPubAckCallback(ctx, msgId, claimDeviceMsg));
            } else if ((fwMatcher = FW_REQUEST_PATTERN.matcher(topicName)).find()) {
                getOtaPackageCallback(ctx, mqttMsg, msgId, fwMatcher, OtaPackageType.FIRMWARE);
            } else if ((fwMatcher = SW_REQUEST_PATTERN.matcher(topicName)).find()) {
                getOtaPackageCallback(ctx, mqttMsg, msgId, fwMatcher, OtaPackageType.SOFTWARE);
            } else if (topicName.equals(MqttTopics.DEVICE_TELEMETRY_SHORT_TOPIC)) {
                TransportProtos.PostTelemetryMsg postTelemetryMsg = payloadAdaptor.convertToPostTelemetry(deviceSessionCtx, mqttMsg);
                transportService.process(deviceSessionCtx.getSessionInfo(), postTelemetryMsg, getMetadata(deviceSessionCtx, topicName),
                        getPubAckCallback(ctx, msgId, postTelemetryMsg));
            } else if (topicName.equals(MqttTopics.DEVICE_TELEMETRY_SHORT_JSON_TOPIC)) {
                TransportProtos.PostTelemetryMsg postTelemetryMsg = context.getJsonMqttAdaptor().convertToPostTelemetry(deviceSessionCtx, mqttMsg);
                transportService.process(deviceSessionCtx.getSessionInfo(), postTelemetryMsg, getMetadata(deviceSessionCtx, topicName),
                        getPubAckCallback(ctx, msgId, postTelemetryMsg));
            } else if (topicName.equals(MqttTopics.DEVICE_TELEMETRY_SHORT_PROTO_TOPIC)) {
                TransportProtos.PostTelemetryMsg postTelemetryMsg = context.getProtoMqttAdaptor().convertToPostTelemetry(deviceSessionCtx, mqttMsg);
                transportService.process(deviceSessionCtx.getSessionInfo(), postTelemetryMsg, getMetadata(deviceSessionCtx, topicName),
                        getPubAckCallback(ctx, msgId, postTelemetryMsg));
            } else if (topicName.equals(MqttTopics.DEVICE_ATTRIBUTES_SHORT_TOPIC)) {
                TransportProtos.PostAttributeMsg postAttributeMsg = payloadAdaptor.convertToPostAttributes(deviceSessionCtx, mqttMsg);
                transportService.process(deviceSessionCtx.getSessionInfo(), postAttributeMsg, getMetadata(deviceSessionCtx, topicName),
                        getPubAckCallback(ctx, msgId, postAttributeMsg));
            } else if (topicName.equals(MqttTopics.DEVICE_ATTRIBUTES_SHORT_JSON_TOPIC)) {
                TransportProtos.PostAttributeMsg postAttributeMsg = context.getJsonMqttAdaptor().convertToPostAttributes(deviceSessionCtx, mqttMsg);
                transportService.process(deviceSessionCtx.getSessionInfo(), postAttributeMsg, getMetadata(deviceSessionCtx, topicName),
                        getPubAckCallback(ctx, msgId, postAttributeMsg));
            } else if (topicName.equals(MqttTopics.DEVICE_ATTRIBUTES_SHORT_PROTO_TOPIC)) {
                TransportProtos.PostAttributeMsg postAttributeMsg = context.getProtoMqttAdaptor().convertToPostAttributes(deviceSessionCtx, mqttMsg);
                transportService.process(deviceSessionCtx.getSessionInfo(), postAttributeMsg, getMetadata(deviceSessionCtx, topicName),
                        getPubAckCallback(ctx, msgId, postAttributeMsg));
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
                transportService.recordActivity(deviceSessionCtx.getSessionInfo());
                ack(ctx, msgId, MqttReasonCodes.PubAck.TOPIC_NAME_INVALID);
            }
        } catch (AdaptorException e) {
            log.debug("[{}] Failed to process publish msg [{}][{}]", sessionId, topicName, msgId, e);
            sendResponseForAdaptorErrorOrCloseContext(ctx, topicName, msgId);
        }
    }

    private TbMsgMetaData getMetadata(DeviceSessionCtx ctx, String topicName) {
        if (ctx.isDeviceProfileMqttTransportType()) {
            TbMsgMetaData md = new TbMsgMetaData();
            md.putValue(DataConstants.MQTT_TOPIC, topicName);
            return md;
        } else {
            return null;
        }
    }

    private void sendResponseForAdaptorErrorOrCloseContext(ChannelHandlerContext ctx, String topicName, int msgId) {
        if ((deviceSessionCtx.isSendAckOnValidationException() || MqttVersion.MQTT_5.equals(deviceSessionCtx.getMqttVersion())) && msgId > 0) {
            log.debug("[{}] Send pub ack on invalid publish msg [{}][{}]", sessionId, topicName, msgId);
            ctx.writeAndFlush(createMqttPubAckMsg(deviceSessionCtx, msgId, MqttReasonCodes.PubAck.PAYLOAD_FORMAT_INVALID.byteValue()));
        } else {
            log.info("[{}] Closing current session due to invalid publish msg [{}][{}]", sessionId, topicName, msgId);
            closeCtx(ctx, MqttReasonCodes.Disconnect.PAYLOAD_FORMAT_INVALID);
        }
    }

    private void getOtaPackageCallback(ChannelHandlerContext ctx, MqttPublishMessage mqttMsg, int msgId, Matcher fwMatcher, OtaPackageType type) {
        String payload = mqttMsg.content().toString(StandardCharsets.UTF_8);
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

    private void ack(ChannelHandlerContext ctx, int msgId, MqttReasonCodes.PubAck returnCode) {
        ack(ctx, msgId, returnCode.byteValue());
    }

    private void ack(ChannelHandlerContext ctx, int msgId, byte returnCode) {
        if (msgId > 0) {
            ctx.writeAndFlush(createMqttPubAckMsg(deviceSessionCtx, msgId, returnCode));
        }
    }

    private <T> TransportServiceCallback<Void> getPubAckCallback(final ChannelHandlerContext ctx, final int msgId, final T msg) {
        return new TransportServiceCallback<>() {
            @Override
            public void onSuccess(Void dummy) {
                log.trace("[{}] Published msg: {}", sessionId, msg);
                ack(ctx, msgId, MqttReasonCodes.PubAck.SUCCESS);
            }

            @Override
            public void onError(Throwable e) {
                log.trace("[{}] Failed to publish msg: {}", sessionId, msg, e);
                closeCtx(ctx, MqttReasonCodes.Disconnect.IMPLEMENTATION_SPECIFIC_ERROR);
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
            ack(ctx, msgId, MqttReasonCodes.PubAck.SUCCESS);
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
            ack(ctx, msgId, MqttReasonCodes.PubAck.IMPLEMENTATION_SPECIFIC_ERROR);
            closeCtx(ctx, MqttReasonCodes.Disconnect.IMPLEMENTATION_SPECIFIC_ERROR);
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
            closeCtx(ctx, MqttReasonCodes.Disconnect.IMPLEMENTATION_SPECIFIC_ERROR);
        }
    }

    private void sendOtaPackage(ChannelHandlerContext ctx, int msgId, String firmwareId, String requestId, int chunkSize, int chunk, OtaPackageType type) {
        log.trace("[{}] Send firmware [{}] to device!", sessionId, firmwareId);
        ack(ctx, msgId, MqttReasonCodes.PubAck.SUCCESS);
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
        closeCtx(ctx, MqttReasonCodes.Disconnect.IMPLEMENTATION_SPECIFIC_ERROR);
    }

    private void processSubscribe(ChannelHandlerContext ctx, MqttSubscribeMessage mqttMsg) {
        if (!checkConnected(ctx, mqttMsg) && !deviceSessionCtx.isProvisionOnly()) {
            ctx.writeAndFlush(createSubAckMessage(mqttMsg.variableHeader().messageId(), Collections.singletonList(MqttReasonCodes.SubAck.NOT_AUTHORIZED.byteValue() & 0xFF)));
            return;
        }
        //TODO consume the rate limit
        log.trace("[{}][{}] Processing subscription [{}]!", deviceSessionCtx.getTenantId(), sessionId, mqttMsg.variableHeader().messageId());
        List<Integer> grantedQoSList = new ArrayList<>();
        boolean activityReported = false;
        for (MqttTopicSubscription subscription : mqttMsg.payload().topicSubscriptions()) {
            String topic = subscription.topicName();
            MqttQoS reqQoS = subscription.qualityOfService();
            if (deviceSessionCtx.isProvisionOnly()) {
                if (MqttTopics.DEVICE_PROVISION_RESPONSE_TOPIC.equals(topic)) {
                    registerSubQoS(topic, grantedQoSList, reqQoS);
                } else {
                    log.debug("[{}][{}] Failed to subscribe because this session is provision only [{}][{}]", deviceSessionCtx.getTenantId(), sessionId, topic, reqQoS);
                    grantedQoSList.add(ReturnCodeResolver.getSubscriptionReturnCode(deviceSessionCtx.getMqttVersion(), MqttReasonCodes.SubAck.TOPIC_FILTER_INVALID));
                }
                activityReported = true;
                continue;
            }
            if (deviceSessionCtx.isDeviceSubscriptionAttributesTopic(topic)) {
                processAttributesSubscribe(grantedQoSList, topic, reqQoS, TopicType.V1);
                activityReported = true;
                continue;
            }
            try {
                if (sparkplugSessionHandler != null) {
                    sparkplugSessionHandler.handleSparkplugSubscribeMsg(subscription);
                    activityReported = true;
                } else {
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
                        case MqttTopics.DEVICE_FIRMWARE_RESPONSES_TOPIC:
                        case MqttTopics.DEVICE_FIRMWARE_ERROR_TOPIC:
                        case MqttTopics.DEVICE_SOFTWARE_RESPONSES_TOPIC:
                        case MqttTopics.DEVICE_SOFTWARE_ERROR_TOPIC:
                            registerSubQoS(topic, grantedQoSList, reqQoS);
                            break;
                        default:
                            //TODO increment an error counter if any exists
                            log.warn("[{}][{}] Failed to subscribe because topic is not supported [{}][{}]", deviceSessionCtx.getTenantId(), sessionId, topic, reqQoS);
                            grantedQoSList.add(ReturnCodeResolver.getSubscriptionReturnCode(deviceSessionCtx.getMqttVersion(), MqttReasonCodes.SubAck.TOPIC_FILTER_INVALID));
                            break;
                    }
                }
            } catch (Exception e) {
                log.warn("[{}][{}] Failed to subscribe to [{}][{}]", deviceSessionCtx.getTenantId(), sessionId, topic, reqQoS, e);
                grantedQoSList.add(ReturnCodeResolver.getSubscriptionReturnCode(deviceSessionCtx.getMqttVersion(), MqttReasonCodes.SubAck.IMPLEMENTATION_SPECIFIC_ERROR));
            }
        }
        if (!activityReported) {
            transportService.recordActivity(deviceSessionCtx.getSessionInfo());
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

    /**
     * 3.0.0 Edge Node Session Establishment:
     * ncmd-subscribe
     * [tck-id-message-flow-edge-node-ncmd-subscribe] The MQTT client associated with the Edge
     * Node MUST subscribe to a topic of the form spBv1.0/group_id/NCMD/edge_node_id where
     * group_id is the Sparkplug Group ID and the edge_node_id is the Sparkplug Edge Node ID for
     * this Edge Node. It MUST subscribe on this topic with a QoS of 1.
     */
    public void processAttributesRpcSubscribeSparkplugNode() {
        List<Integer> grantedQoSList = new ArrayList<>();
        transportService.process(TransportProtos.TransportToDeviceActorMsg.newBuilder()
                .setSessionInfo(deviceSessionCtx.getSessionInfo())
                .setSubscribeToAttributes(SUBSCRIBE_TO_ATTRIBUTE_UPDATES_ASYNC_MSG)
                .setSubscribeToRPC(SUBSCRIBE_TO_RPC_ASYNC_MSG)
                .build(), null);
        registerSubQoS(MqttTopics.DEVICE_ATTRIBUTES_TOPIC, grantedQoSList, AT_LEAST_ONCE);
    }

    public void registerSubQoS(String topic, List<Integer> grantedQoSList, MqttQoS reqQoS) {
        grantedQoSList.add(getMinSupportedQos(reqQoS));
        mqttQoSMap.put(new MqttTopicMatcher(topic), getMinSupportedQos(reqQoS));
    }

    private void processUnsubscribe(ChannelHandlerContext ctx, MqttUnsubscribeMessage mqttMsg) {
        if (!checkConnected(ctx, mqttMsg) && !deviceSessionCtx.isProvisionOnly()) {
            ctx.writeAndFlush(createUnSubAckMessage(mqttMsg.variableHeader().messageId(),
                    Collections.singletonList((short) MqttReasonCodes.UnsubAck.NOT_AUTHORIZED.byteValue())));
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
                    short resultValue = MqttReasonCodes.UnsubAck.SUCCESS.byteValue();
                    if (deviceSessionCtx.isProvisionOnly()) {
                        if (!matcher.matches(MqttTopics.DEVICE_PROVISION_RESPONSE_TOPIC)) {
                            resultValue = MqttReasonCodes.UnsubAck.TOPIC_FILTER_INVALID.byteValue();
                        }
                        unSubResults.add(resultValue);
                        activityReported = true;
                        continue;
                    }
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
                        case MqttTopics.DEVICE_FIRMWARE_RESPONSES_TOPIC:
                        case MqttTopics.DEVICE_FIRMWARE_ERROR_TOPIC:
                        case MqttTopics.DEVICE_SOFTWARE_RESPONSES_TOPIC:
                        case MqttTopics.DEVICE_SOFTWARE_ERROR_TOPIC: {
                            activityReported = true;
                            break;
                        }
                        default:
                            log.trace("[{}] Failed to process unsubscription [{}] to [{}]", sessionId, mqttMsg.variableHeader().messageId(), topicName);
                            resultValue = MqttReasonCodes.UnsubAck.TOPIC_FILTER_INVALID.byteValue();
                    }
                    unSubResults.add(resultValue);
                } catch (Exception e) {
                    log.debug("[{}] Failed to process unsubscription [{}] to [{}]", sessionId, mqttMsg.variableHeader().messageId(), topicName);
                    unSubResults.add((short) MqttReasonCodes.UnsubAck.IMPLEMENTATION_SPECIFIC_ERROR.byteValue());
                }
            } else {
                log.debug("[{}] Failed to process unsubscription [{}] to [{}] - Subscription not found", sessionId, mqttMsg.variableHeader().messageId(), topicName);
                unSubResults.add((short) MqttReasonCodes.UnsubAck.NO_SUBSCRIPTION_EXISTED.byteValue());
            }
        }
        if (!activityReported && !deviceSessionCtx.isProvisionOnly()) {
            transportService.recordActivity(deviceSessionCtx.getSessionInfo());
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
            ctx.writeAndFlush(createMqttConnAckMsg(MqttConnectReturnCode.CONNECTION_ACCEPTED, msg));
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
                        ctx.writeAndFlush(createMqttConnAckMsg(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE_5, connectMessage));
                        closeCtx(ctx, MqttReasonCodes.Disconnect.SERVER_BUSY);
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
                            ctx.writeAndFlush(createMqttConnAckMsg(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE_5, connectMessage));
                            closeCtx(ctx, MqttReasonCodes.Disconnect.IMPLEMENTATION_SPECIFIC_ERROR);
                        }
                    });
        } catch (Exception e) {
            context.onAuthFailure(address);
            ctx.writeAndFlush(createMqttConnAckMsg(MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED_5, connectMessage));
            log.trace("[{}] X509 auth failure: {}", sessionId, address, e);
            closeCtx(ctx, MqttReasonCodes.Disconnect.NOT_AUTHORIZED);
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

    private MqttConnAckMessage createMqttConnAckMsg(MqttConnectReturnCode returnCode, MqttConnectMessage msg) {
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
                        cause.getMessage(),
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

        closeCtx(ctx, MqttReasonCodes.Disconnect.SERVER_SHUTTING_DOWN);
        if (cause instanceof OutOfMemoryError) {
            log.error("Received critical error. Going to shutdown the service.");
            System.exit(1);
        }
    }

    private static MqttSubAckMessage createSubAckMessage(Integer msgId, List<Integer> reasonCodes) {
        MqttFixedHeader mqttFixedHeader =
                new MqttFixedHeader(SUBACK, false, AT_MOST_ONCE, false, 0);
        MqttMessageIdVariableHeader mqttMessageIdVariableHeader = MqttMessageIdVariableHeader.from(msgId);
        MqttSubAckPayload mqttSubAckPayload = new MqttSubAckPayload(reasonCodes);
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

    public static MqttMessage createMqttPubAckMsg(DeviceSessionCtx deviceSessionCtx, int requestId, byte returnCode) {
        MqttMessageBuilders.PubAckBuilder pubAckMsgBuilder = MqttMessageBuilders.pubAck().packetId(requestId);
        if (MqttVersion.MQTT_5.equals(deviceSessionCtx.getMqttVersion())) {
            pubAckMsgBuilder.reasonCode(returnCode);
        }
        return pubAckMsgBuilder.build();
    }

    public static MqttMessage createMqttDisconnectMsg(DeviceSessionCtx deviceSessionCtx, byte returnCode) {
        MqttMessageBuilders.DisconnectBuilder disconnectBuilder = MqttMessageBuilders.disconnect();
        if (MqttVersion.MQTT_5.equals(deviceSessionCtx.getMqttVersion())) {
            disconnectBuilder.reasonCode(returnCode);
        }
        return disconnectBuilder.build();
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
                JsonNode gatewayNode = infoNode.get(DataConstants.GATEWAY_PARAMETER);
                if (gatewayNode != null && gatewayNode.asBoolean()) {
                    boolean overwriteDevicesActivity = false;
                    if (infoNode.has(DataConstants.OVERWRITE_ACTIVITY_TIME_PARAMETER)
                            && infoNode.get(DataConstants.OVERWRITE_ACTIVITY_TIME_PARAMETER).isBoolean()) {
                        overwriteDevicesActivity = infoNode.get(DataConstants.OVERWRITE_ACTIVITY_TIME_PARAMETER).asBoolean();
                        sessionMetaData.setOverwriteActivityTime(overwriteDevicesActivity);
                    }
                    gatewaySessionHandler = new GatewaySessionHandler(deviceSessionCtx, sessionId, overwriteDevicesActivity);
                }
            }
        } catch (IOException e) {
            log.trace("[{}][{}] Failed to fetch device additional info", sessionId, device.getDeviceName(), e);
        }
    }

    private void checkSparkplugNodeSession(MqttConnectMessage connectMessage, ChannelHandlerContext ctx, SessionMetaData sessionMetaData) {
        try {
            if (sparkplugSessionHandler == null) {
                SparkplugTopic sparkplugTopic = validatedSparkplugConnectedWillTopic(connectMessage);
                if (sparkplugTopic != null) {
                    SparkplugBProto.Payload sparkplugBProtoNode = SparkplugBProto.Payload.parseFrom(connectMessage.payload().willMessageInBytes());
                    sparkplugSessionHandler = new SparkplugNodeSessionHandler(this, deviceSessionCtx, sessionId, true, sparkplugTopic);
                    sparkplugSessionHandler.onAttributesTelemetryProto(0, sparkplugBProtoNode, sparkplugTopic);
                    sessionMetaData.setOverwriteActivityTime(true);
                    // ncmd-subscribe
                    processAttributesRpcSubscribeSparkplugNode();
                } else {
                    log.trace("[{}][{}] Failed to fetch sparkplugDevice connect:  sparkplugTopicName without SparkplugMessageType.NDEATH.", sessionId, deviceSessionCtx.getDeviceInfo().getDeviceName());
                    throw new ThingsboardException("Invalid request body", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
                }
            }
        } catch (Exception e) {
            log.trace("[{}][{}] Failed to fetch sparkplugDevice connect, sparkplugTopicName", sessionId, deviceSessionCtx.getDeviceInfo().getDeviceName(), e);
            ctx.writeAndFlush(createMqttConnAckMsg(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE_5, connectMessage));
            closeCtx(ctx, MqttReasonCodes.Disconnect.IMPLEMENTATION_SPECIFIC_ERROR);
        }
    }

    /**
     *  The Death Certificate topic and payload described here are not “published” as an MQTT message by a client,
     *  but provided as parameters within the MQTT CONNECT control packet when this Sparkplug Edge Node first establishes the MQTT Client session.
     * - NDEATH message MUST be registered as a Will Message in the MQTT CONNECT packet.
     * -- in the MQTT CONNECT packet The NDEATH message MUST set the MQTT Will QoS to 1.
     * -- in the MQTT CONNECT packet The NDEATH message MUST set the MQTT Will Retained flag to false.
     * -- If the MQTT client is using MQTT v3.1.1, the Edge Node’s MQTT CONNECT packet MUST set the Clean Session flag to true.
     * -- If the MQTT client is using MQTT v5.0, the Edge Node’s MQTT CONNECT packet MUST set the Clean Start flag to true and the Session Expiry Interval to 0
     * @param connectMessage
     * @return
     * @throws ThingsboardException
     */
    private SparkplugTopic validatedSparkplugConnectedWillTopic(MqttConnectMessage connectMessage) throws ThingsboardException {
        if (StringUtils.isNotBlank(connectMessage.payload().willTopic())
                && connectMessage.payload().willMessageInBytes() != null
                && connectMessage.payload().willMessageInBytes().length > 0) {
            SparkplugTopic sparkplugTopicNode = parseTopic(connectMessage.payload().willTopic());
            if (NDEATH.equals(sparkplugTopicNode.getType())) {
                if (connectMessage.variableHeader().willQos() != 1 || connectMessage.variableHeader().isWillRetain())
                    return null;
                if (!connectMessage.variableHeader().isCleanSession()) return null;
                int mqttVer = connectMessage.variableHeader().version();
                if (mqttVer == 5) {
                    Object sessionExpiryIntervalObj = connectMessage.variableHeader().properties().isEmpty() ? null : connectMessage.variableHeader().properties().getProperty(MqttPropertyType.SESSION_EXPIRY_INTERVAL.value());
                    Integer sessionExpiryInterval = sessionExpiryIntervalObj == null ? null : ((IntegerProperty) sessionExpiryIntervalObj).value();
                    if (sessionExpiryInterval == null || sessionExpiryInterval != 0) return null;
                }
                return sparkplugTopicNode;
            }
        }
        return null;
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
                gatewaySessionHandler.onDevicesDisconnect();
            }
            if (sparkplugSessionHandler != null) {
                // add Msg Telemetry node: key STATE type: String value: OFFLINE ts: sparkplugBProto.getTimestamp()
                sparkplugSessionHandler.sendSparkplugStateOnTelemetry(deviceSessionCtx.getSessionInfo(),
                        deviceSessionCtx.getDeviceInfo().getDeviceName(), OFFLINE, new Date().getTime());
                sparkplugSessionHandler.onDevicesDisconnect();
            }
            deviceSessionCtx.setDisconnected();
        }
        deviceSessionCtx.release();
    }

    private void onValidateDeviceResponse(ValidateDeviceCredentialsResponse msg, ChannelHandlerContext ctx, MqttConnectMessage connectMessage) {
        if (!msg.hasDeviceInfo()) {
            context.onAuthFailure(address);
            MqttConnectReturnCode returnCode = MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED_5;
            if (sslHandler == null || getX509Certificate() == null) {
                String username = connectMessage.payload().userName();
                byte[] passwordBytes = connectMessage.payload().passwordInBytes();
                String clientId = connectMessage.payload().clientIdentifier();
                if ((username != null && passwordBytes != null && clientId != null)
                        || (username == null ^ passwordBytes == null)) {
                    returnCode = MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USERNAME_OR_PASSWORD;
                } else if (!StringUtils.isBlank(clientId)) {
                    returnCode = MqttConnectReturnCode.CONNECTION_REFUSED_CLIENT_IDENTIFIER_NOT_VALID;
                }
            }
            ctx.writeAndFlush(createMqttConnAckMsg(returnCode, connectMessage));
            closeCtx(ctx, returnCode);
        } else {
            context.onAuthSuccess(address);
            deviceSessionCtx.setDeviceInfo(msg.getDeviceInfo());
            deviceSessionCtx.setDeviceProfile(msg.getDeviceProfile());
            deviceSessionCtx.setSessionInfo(SessionInfoCreator.create(msg, context, sessionId));
            transportService.process(deviceSessionCtx.getSessionInfo(), SESSION_EVENT_MSG_OPEN, new TransportServiceCallback<Void>() {
                @Override
                public void onSuccess(Void msg) {
                    SessionMetaData sessionMetaData = transportService.registerAsyncSession(deviceSessionCtx.getSessionInfo(), MqttTransportHandler.this);
                    if (deviceSessionCtx.isSparkplug()) {
                        checkSparkplugNodeSession(connectMessage, ctx, sessionMetaData);
                    } else {
                        checkGatewaySession(sessionMetaData);
                    }
                    ctx.writeAndFlush(createMqttConnAckMsg(MqttConnectReturnCode.CONNECTION_ACCEPTED, connectMessage));
                    deviceSessionCtx.setConnected(true);
                    log.debug("[{}] Client connected!", sessionId);
                    transportService.getCallbackExecutor().execute(() -> processMsgQueue(ctx)); //this callback will execute in Producer worker thread and hard or blocking work have to be submitted to the separate thread.
                }

                @Override
                public void onError(Throwable e) {
                    if (e instanceof TbRateLimitsException) {
                        log.trace("[{}] Failed to submit session event: {}", sessionId, e.getMessage());
                        ctx.writeAndFlush(createMqttConnAckMsg(MqttConnectReturnCode.CONNECTION_REFUSED_CONNECTION_RATE_EXCEEDED, connectMessage));
                        closeCtx(ctx, MqttReasonCodes.Disconnect.MESSAGE_RATE_TOO_HIGH);
                    } else {
                        log.warn("[{}] Failed to submit session event", sessionId, e);
                        ctx.writeAndFlush(createMqttConnAckMsg(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE_5, connectMessage));
                        closeCtx(ctx, MqttReasonCodes.Disconnect.IMPLEMENTATION_SPECIFIC_ERROR);
                    }
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
        try {
            if (sparkplugSessionHandler != null) {
                log.trace("[{}] Received attributes update notification to sparkplug Edge Node", sessionId);
                notification.getSharedUpdatedList().forEach(tsKvProtoShared -> {
                    SparkplugMessageType messageType = NCMD;
                    TransportProtos.TsKvProto tsKvProto = tsKvProtoShared;
                    if ("JSON_V".equals(tsKvProtoShared.getKv().getType().name())) {
                        try {
                            messageType = SparkplugMessageType.parseMessageType(tsKvProtoShared.getKv().getKey());
                            tsKvProto = getTsKvProtoFromJsonNode(JacksonUtil.toJsonNode(tsKvProtoShared.getKv().getJsonV()), tsKvProtoShared.getTs());
                        } catch (ThingsboardException e) {
                            messageType = null;
                            log.error("Failed attributes update notification to sparkplug Edge Node [{}]. ", sparkplugSessionHandler.getSparkplugTopicNode().getEdgeNodeId(), e);
                        }
                    }
                    if (messageType != null && messageType.isSubscribe() && messageType.isNode()
                            && sparkplugSessionHandler.getNodeBirthMetrics().containsKey(tsKvProto.getKv().getKey())) {
                        SparkplugTopic sparkplugTopic = new SparkplugTopic(sparkplugSessionHandler.getSparkplugTopicNode(), messageType);
                        sparkplugSessionHandler.createSparkplugMqttPublishMsg(tsKvProto,
                                        sparkplugTopic.toString(),
                                        sparkplugSessionHandler.getNodeBirthMetrics().get(tsKvProto.getKv().getKey()))
                                .ifPresent(sparkplugSessionHandler::writeAndFlush);
                    } else {
                        log.trace("Failed attributes update notification to sparkplug Edge Node [{}]. ", sparkplugSessionHandler.getSparkplugTopicNode().getEdgeNodeId());
                    }
                });
            } else {
                String topic = attrSubTopicType.getAttributesSubTopic();
                MqttTransportAdaptor adaptor = deviceSessionCtx.getAdaptor(attrSubTopicType);
                adaptor.convertToPublish(deviceSessionCtx, notification, topic).ifPresent(deviceSessionCtx.getChannel()::writeAndFlush);
            }
        } catch (Exception e) {
            log.trace("[{}] Failed to convert device/Edge Node attributes update to MQTT msg", sessionId, e);
        }
    }

    @Override
    public void onRemoteSessionCloseCommand(UUID sessionId, TransportProtos.SessionCloseNotificationProto sessionCloseNotification) {
        log.trace("[{}] Received the remote command to close the session: {}", sessionId, sessionCloseNotification.getMessage());
        transportService.deregisterSession(deviceSessionCtx.getSessionInfo());
        MqttReasonCodes.Disconnect returnCode = switch (sessionCloseNotification.getReason()) {
            case CREDENTIALS_UPDATED, RPC_DELIVERY_TIMEOUT -> MqttReasonCodes.Disconnect.ADMINISTRATIVE_ACTION;
            case MAX_CONCURRENT_SESSIONS_LIMIT_REACHED -> MqttReasonCodes.Disconnect.SESSION_TAKEN_OVER;
            case SESSION_TIMEOUT -> MqttReasonCodes.Disconnect.MAXIMUM_CONNECT_TIME;
            default -> MqttReasonCodes.Disconnect.IMPLEMENTATION_SPECIFIC_ERROR;
        };
        closeCtx(deviceSessionCtx.getChannel(), returnCode);
    }

    @Override
    public void onToDeviceRpcRequest(UUID sessionId, TransportProtos.ToDeviceRpcRequestMsg rpcRequest) {
        log.trace("[{}][{}] Received RPC command to device: {}", deviceSessionCtx.getDeviceId(), sessionId, rpcRequest);
        try {
            if (sparkplugSessionHandler != null) {
                handleToSparkplugDeviceRpcRequest(rpcRequest);
            } else {
                String baseTopic = rpcSubTopicType.getRpcRequestTopicBase();
                MqttTransportAdaptor adaptor = deviceSessionCtx.getAdaptor(rpcSubTopicType);
                adaptor.convertToPublish(deviceSessionCtx, rpcRequest, baseTopic)
                        .ifPresent(payload -> sendToDeviceRpcRequest(payload, rpcRequest, deviceSessionCtx.getSessionInfo()));
            }
        } catch (Exception e) {
            log.trace("[{}][{}] Failed to convert device RPC command to MQTT msg", deviceSessionCtx.getDeviceId(), sessionId, e);
            this.sendErrorRpcResponse(deviceSessionCtx.getSessionInfo(), rpcRequest.getRequestId(),
                    ThingsboardErrorCode.INVALID_ARGUMENTS,
                    "Failed to convert device RPC command to MQTT msg: " + rpcRequest.getMethodName() + rpcRequest.getParams());
        }
    }

    private void onGetSessionLimitsRpc(TransportProtos.SessionInfoProto sessionInfo, ChannelHandlerContext ctx, int msgId, TransportProtos.
            ToServerRpcRequestMsg rpcRequestMsg) {
        var tenantProfile = context.getTenantProfileCache().get(deviceSessionCtx.getTenantId());
        DefaultTenantProfileConfiguration profile = tenantProfile.getDefaultProfileConfiguration();

        SessionLimits sessionLimits;

        if (sessionInfo.getIsGateway()) {
            var gatewaySessionLimits = new GatewaySessionLimits();
            var gatewayLimits = new SessionLimits.SessionRateLimits(profile.getTransportGatewayMsgRateLimit(),
                    profile.getTransportGatewayTelemetryMsgRateLimit(),
                    profile.getTransportGatewayTelemetryDataPointsRateLimit());
            var gatewayDeviceLimits = new SessionLimits.SessionRateLimits(profile.getTransportGatewayDeviceMsgRateLimit(),
                    profile.getTransportGatewayDeviceTelemetryMsgRateLimit(),
                    profile.getTransportGatewayDeviceTelemetryDataPointsRateLimit());
            gatewaySessionLimits.setGatewayRateLimits(gatewayLimits);
            gatewaySessionLimits.setRateLimits(gatewayDeviceLimits);
            sessionLimits = gatewaySessionLimits;
        } else {
            var rateLimits = new SessionLimits.SessionRateLimits(profile.getTransportDeviceMsgRateLimit(),
                    profile.getTransportDeviceTelemetryMsgRateLimit(),
                    profile.getTransportDeviceTelemetryDataPointsRateLimit());
            sessionLimits = new SessionLimits();
            sessionLimits.setRateLimits(rateLimits);
        }
        sessionLimits.setMaxPayloadSize(context.getMaxPayloadSize());
        sessionLimits.setMaxInflightMessages(context.getMessageQueueSizePerDeviceLimit());

        ack(ctx, msgId, MqttReasonCodes.PubAck.SUCCESS);

        TransportProtos.ToServerRpcResponseMsg responseMsg = TransportProtos.ToServerRpcResponseMsg.newBuilder()
                .setRequestId(rpcRequestMsg.getRequestId())
                .setPayload(JacksonUtil.toString(sessionLimits))
                .build();

        onToServerRpcResponse(responseMsg);
    }

    private void handleToSparkplugDeviceRpcRequest(TransportProtos.ToDeviceRpcRequestMsg rpcRequest) throws ThingsboardException {
        SparkplugMessageType messageType = SparkplugMessageType.parseMessageType(rpcRequest.getMethodName());
        SparkplugRpcRequestHeader header;
        if (StringUtils.isNotEmpty(rpcRequest.getParams())) {
            header = JacksonUtil.fromString(rpcRequest.getParams(), SparkplugRpcRequestHeader.class);
        } else {
            header = new SparkplugRpcRequestHeader();
        }
        header.setMessageType(messageType.name());
        TransportProtos.TsKvProto tsKvProto = getTsKvProto(header.getMetricName(), header.getValue(), new Date().getTime());
        if (sparkplugSessionHandler.getNodeBirthMetrics().containsKey(tsKvProto.getKv().getKey())) {
            SparkplugTopic sparkplugTopic = new SparkplugTopic(sparkplugSessionHandler.getSparkplugTopicNode(),
                    messageType);
            sparkplugSessionHandler.createSparkplugMqttPublishMsg(tsKvProto,
                            sparkplugTopic.toString(),
                            sparkplugSessionHandler.getNodeBirthMetrics().get(tsKvProto.getKv().getKey()))
                    .ifPresent(payload -> sendToDeviceRpcRequest(payload, rpcRequest, deviceSessionCtx.getSessionInfo()));
        } else {
            sendErrorRpcResponse(deviceSessionCtx.getSessionInfo(), rpcRequest.getRequestId(),
                    ThingsboardErrorCode.BAD_REQUEST_PARAMS, "Failed send To Node Rpc Request: " +
                            rpcRequest.getMethodName() + ". This node does not have a metricName: [" + tsKvProto.getKv().getKey() + "]");
        }
    }

    public void sendToDeviceRpcRequest(MqttMessage payload, TransportProtos.ToDeviceRpcRequestMsg rpcRequest, TransportProtos.SessionInfoProto sessionInfo) {
        int msgId = ((MqttPublishMessage) payload).variableHeader().packetId();
        int requestId = rpcRequest.getRequestId();
        if (isAckExpected(payload)) {
            rpcAwaitingAck.put(msgId, rpcRequest);
            context.getScheduler().schedule(() -> {
                TransportProtos.ToDeviceRpcRequestMsg msg = rpcAwaitingAck.remove(msgId);
                if (msg != null) {
                    log.trace("[{}][{}][{}] Going to send to device actor RPC request TIMEOUT status update ...", deviceSessionCtx.getDeviceId(), sessionId, requestId);
                    transportService.process(sessionInfo, rpcRequest, RpcStatus.TIMEOUT, TransportServiceCallback.EMPTY);
                }
            }, Math.max(0, Math.min(deviceSessionCtx.getContext().getTimeout(), rpcRequest.getExpirationTime() - System.currentTimeMillis())), TimeUnit.MILLISECONDS);
        }
        var cf = publish(payload, deviceSessionCtx);
        cf.addListener(result -> {
            Throwable throwable = result.cause();
            if (throwable != null) {
                log.trace("[{}][{}][{}] Failed send RPC request to device due to: ", deviceSessionCtx.getDeviceId(), sessionId, requestId, throwable);
                this.sendErrorRpcResponse(sessionInfo, requestId,
                        ThingsboardErrorCode.INVALID_ARGUMENTS, " Failed send To Device Rpc Request: " + rpcRequest.getMethodName());
                return;
            }
            if (!isAckExpected(payload)) {
                log.trace("[{}][{}][{}] Going to send to device actor RPC request DELIVERED status update ...", deviceSessionCtx.getDeviceId(), sessionId, requestId);
                transportService.process(sessionInfo, rpcRequest, RpcStatus.DELIVERED, TransportServiceCallback.EMPTY);
            } else if (rpcRequest.getPersisted()) {
                log.trace("[{}][{}][{}] Going to send to device actor RPC request SENT status update ...", deviceSessionCtx.getDeviceId(), sessionId, requestId);
                transportService.process(sessionInfo, rpcRequest, RpcStatus.SENT, TransportServiceCallback.EMPTY);
            }
            if (sparkplugSessionHandler != null) {
                this.sendSuccessRpcResponse(sessionInfo, requestId, ResponseCode.CONTENT, "Success: " + rpcRequest.getMethodName());
            }
        });
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
        if (gatewaySessionHandler != null) {
            gatewaySessionHandler.onGatewayUpdate(sessionInfo, device, deviceProfileOpt);
        }
    }

    @Override
    public void onDeviceDeleted(DeviceId deviceId) {
        context.onAuthFailure(address);
        ChannelHandlerContext ctx = deviceSessionCtx.getChannel();
        closeCtx(ctx, MqttReasonCodes.Disconnect.ADMINISTRATIVE_ACTION);
        if (gatewaySessionHandler != null) {
            gatewaySessionHandler.onGatewayDelete(deviceId);
        }
    }

    public void sendErrorRpcResponse(TransportProtos.SessionInfoProto sessionInfo, int requestId, ThingsboardErrorCode result, String errorMsg) {
        String payload = JacksonUtil.toString(SparkplugRpcResponseBody.builder().result(result.name()).error(errorMsg).build());
        TransportProtos.ToDeviceRpcResponseMsg msg = TransportProtos.ToDeviceRpcResponseMsg.newBuilder().setRequestId(requestId).setError(payload).build();
        transportService.process(sessionInfo, msg, null);
    }

    public void sendSuccessRpcResponse(TransportProtos.SessionInfoProto sessionInfo, int requestId, ResponseCode result, String successMsg) {
        String payload = JacksonUtil.toString(SparkplugRpcResponseBody.builder().result(result.getName()).result(successMsg).build());
        TransportProtos.ToDeviceRpcResponseMsg msg = TransportProtos.ToDeviceRpcResponseMsg.newBuilder().setRequestId(requestId).setError(payload).build();
        transportService.process(sessionInfo, msg, null);
    }

}
