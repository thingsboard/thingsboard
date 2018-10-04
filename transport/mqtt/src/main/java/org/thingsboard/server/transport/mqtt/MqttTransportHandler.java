/**
 * Copyright © 2016-2018 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.transport.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnAckVariableHeader;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttSubAckPayload;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.security.DeviceTokenCredentials;
import org.thingsboard.server.common.data.security.DeviceX509Credentials;
import org.thingsboard.server.common.msg.core.SessionOpenMsg;
import org.thingsboard.server.common.msg.session.AdaptorToSessionActorMsg;
import org.thingsboard.server.common.msg.session.BasicAdaptorToSessionActorMsg;
import org.thingsboard.server.common.msg.session.BasicTransportToDeviceSessionActorMsg;
import org.thingsboard.server.common.msg.session.ctrl.SessionCloseMsg;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.quota.QuotaService;
import org.thingsboard.server.dao.EncryptionUtil;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.mqtt.adaptors.MqttTransportAdaptor;
import org.thingsboard.server.transport.mqtt.session.DeviceSessionCtx;
import org.thingsboard.server.transport.mqtt.session.GatewaySessionCtx;
import org.thingsboard.server.transport.mqtt.util.SslUtil;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.cert.X509Certificate;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.thingsboard.server.gen.transport.TransportProtos.*;

import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_ACCEPTED;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED;
import static io.netty.handler.codec.mqtt.MqttMessageType.CONNACK;
import static io.netty.handler.codec.mqtt.MqttMessageType.PINGRESP;
import static io.netty.handler.codec.mqtt.MqttMessageType.PUBACK;
import static io.netty.handler.codec.mqtt.MqttMessageType.SUBACK;
import static io.netty.handler.codec.mqtt.MqttMessageType.UNSUBACK;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_LEAST_ONCE;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;
import static io.netty.handler.codec.mqtt.MqttQoS.FAILURE;
import static org.thingsboard.server.common.msg.session.SessionMsgType.GET_ATTRIBUTES_REQUEST;
import static org.thingsboard.server.common.msg.session.SessionMsgType.POST_ATTRIBUTES_REQUEST;
import static org.thingsboard.server.common.msg.session.SessionMsgType.POST_TELEMETRY_REQUEST;
import static org.thingsboard.server.common.msg.session.SessionMsgType.SUBSCRIBE_ATTRIBUTES_REQUEST;
import static org.thingsboard.server.common.msg.session.SessionMsgType.SUBSCRIBE_RPC_COMMANDS_REQUEST;
import static org.thingsboard.server.common.msg.session.SessionMsgType.TO_DEVICE_RPC_RESPONSE;
import static org.thingsboard.server.common.msg.session.SessionMsgType.TO_SERVER_RPC_REQUEST;
import static org.thingsboard.server.common.msg.session.SessionMsgType.UNSUBSCRIBE_ATTRIBUTES_REQUEST;
import static org.thingsboard.server.common.msg.session.SessionMsgType.UNSUBSCRIBE_RPC_COMMANDS_REQUEST;
import static org.thingsboard.server.transport.mqtt.MqttTopics.BASE_GATEWAY_API_TOPIC;
import static org.thingsboard.server.transport.mqtt.MqttTopics.DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX;
import static org.thingsboard.server.transport.mqtt.MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_TOPIC;
import static org.thingsboard.server.transport.mqtt.MqttTopics.DEVICE_ATTRIBUTES_TOPIC;
import static org.thingsboard.server.transport.mqtt.MqttTopics.DEVICE_RPC_REQUESTS_SUB_TOPIC;
import static org.thingsboard.server.transport.mqtt.MqttTopics.DEVICE_RPC_REQUESTS_TOPIC;
import static org.thingsboard.server.transport.mqtt.MqttTopics.DEVICE_RPC_RESPONSE_SUB_TOPIC;
import static org.thingsboard.server.transport.mqtt.MqttTopics.DEVICE_RPC_RESPONSE_TOPIC;
import static org.thingsboard.server.transport.mqtt.MqttTopics.DEVICE_TELEMETRY_TOPIC;
import static org.thingsboard.server.transport.mqtt.MqttTopics.GATEWAY_ATTRIBUTES_REQUEST_TOPIC;
import static org.thingsboard.server.transport.mqtt.MqttTopics.GATEWAY_ATTRIBUTES_TOPIC;
import static org.thingsboard.server.transport.mqtt.MqttTopics.GATEWAY_CONNECT_TOPIC;
import static org.thingsboard.server.transport.mqtt.MqttTopics.GATEWAY_DISCONNECT_TOPIC;
import static org.thingsboard.server.transport.mqtt.MqttTopics.GATEWAY_RPC_TOPIC;
import static org.thingsboard.server.transport.mqtt.MqttTopics.GATEWAY_TELEMETRY_TOPIC;

/**
 * @author Andrew Shvayka
 */
@Slf4j
public class MqttTransportHandler extends ChannelInboundHandlerAdapter implements GenericFutureListener<Future<? super Void>> {

    public static final MqttQoS MAX_SUPPORTED_QOS_LVL = AT_LEAST_ONCE;

    private final UUID sessionId;
    private final MqttTransportContext context;
    private final MqttTransportAdaptor adaptor;
    private final TransportService transportService;
    private final QuotaService quotaService;
    private final SslHandler sslHandler;
    private final ConcurrentMap<String, Integer> mqttQoSMap;

    private final SessionInfoProto sessionInfo;

    private volatile InetSocketAddress address;
    private volatile DeviceSessionCtx deviceSessionCtx;
    private volatile GatewaySessionCtx gatewaySessionCtx;

    public MqttTransportHandler(MqttTransportContext context) {
        this.sessionId = UUID.randomUUID();
        this.context = context;
        this.transportService = context.getTransportService();
        this.adaptor = context.getAdaptor();
        this.quotaService = context.getQuotaService();
        this.sslHandler = context.getSslHandler();
        this.mqttQoSMap = new ConcurrentHashMap<>();
        this.sessionInfo = SessionInfoProto.newBuilder()
                .setNodeId(context.getNodeId())
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .build();
        this.deviceSessionCtx = new DeviceSessionCtx(mqttQoSMap);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        log.trace("[{}] Processing msg: {}", sessionId, msg);
        if (msg instanceof MqttMessage) {
            processMqttMsg(ctx, (MqttMessage) msg);
        } else {
            ctx.close();
        }
    }

    private void processMqttMsg(ChannelHandlerContext ctx, MqttMessage msg) {
        address = (InetSocketAddress) ctx.channel().remoteAddress();
        if (msg.fixedHeader() == null) {
            log.info("[{}:{}] Invalid message received", address.getHostName(), address.getPort());
            processDisconnect(ctx);
            return;
        }

        if (quotaService.isQuotaExceeded(address.getHostName())) {
            log.warn("MQTT Quota exceeded for [{}:{}] . Disconnect", address.getHostName(), address.getPort());
            processDisconnect(ctx);
            return;
        }

        deviceSessionCtx.setChannel(ctx);
        switch (msg.fixedHeader().messageType()) {
            case CONNECT:
                processConnect(ctx, (MqttConnectMessage) msg);
                break;
//            case PUBLISH:
//                processPublish(ctx, (MqttPublishMessage) msg);
//                break;
//            case SUBSCRIBE:
//                processSubscribe(ctx, (MqttSubscribeMessage) msg);
//                break;
//            case UNSUBSCRIBE:
//                processUnsubscribe(ctx, (MqttUnsubscribeMessage) msg);
//                break;
//            case PINGREQ:
//                if (checkConnected(ctx)) {
//                    ctx.writeAndFlush(new MqttMessage(new MqttFixedHeader(PINGRESP, false, AT_MOST_ONCE, false, 0)));
//                }
//                break;
//            case DISCONNECT:
//                if (checkConnected(ctx)) {
//                    processDisconnect(ctx);
//                }
//                break;
            default:
                break;
        }

    }

//    private void processPublish(ChannelHandlerContext ctx, MqttPublishMessage mqttMsg) {
//        if (!checkConnected(ctx)) {
//            return;
//        }
//        String topicName = mqttMsg.variableHeader().topicName();
//        int msgId = mqttMsg.variableHeader().packetId();
//        log.trace("[{}] Processing publish msg [{}][{}]!", sessionId, topicName, msgId);
//
//        if (topicName.startsWith(BASE_GATEWAY_API_TOPIC)) {
//            if (gatewaySessionCtx != null) {
//                gatewaySessionCtx.setChannel(ctx);
//                handleMqttPublishMsg(topicName, msgId, mqttMsg);
//            }
//        } else {
//            processDevicePublish(ctx, mqttMsg, topicName, msgId);
//        }
//    }
//
//    private void handleMqttPublishMsg(String topicName, int msgId, MqttPublishMessage mqttMsg) {
//        try {
//            switch (topicName) {
//                case GATEWAY_TELEMETRY_TOPIC:
//                    gatewaySessionCtx.onDeviceTelemetry(mqttMsg);
//                    break;
//                case GATEWAY_ATTRIBUTES_TOPIC:
//                    gatewaySessionCtx.onDeviceAttributes(mqttMsg);
//                    break;
//                case GATEWAY_ATTRIBUTES_REQUEST_TOPIC:
//                    gatewaySessionCtx.onDeviceAttributesRequest(mqttMsg);
//                    break;
//                case GATEWAY_RPC_TOPIC:
//                    gatewaySessionCtx.onDeviceRpcResponse(mqttMsg);
//                    break;
//                case GATEWAY_CONNECT_TOPIC:
//                    gatewaySessionCtx.onDeviceConnect(mqttMsg);
//                    break;
//                case GATEWAY_DISCONNECT_TOPIC:
//                    gatewaySessionCtx.onDeviceDisconnect(mqttMsg);
//                    break;
//            }
//        } catch (RuntimeException | AdaptorException e) {
//            log.warn("[{}] Failed to process publish msg [{}][{}]", sessionId, topicName, msgId, e);
//        }
//    }
//
//    private void processDevicePublish(ChannelHandlerContext ctx, MqttPublishMessage mqttMsg, String topicName, int msgId) {
//        AdaptorToSessionActorMsg msg = null;
//        try {
//            if (topicName.equals(DEVICE_TELEMETRY_TOPIC)) {
//                msg = adaptor.convertToActorMsg(deviceSessionCtx, POST_TELEMETRY_REQUEST, mqttMsg);
//            } else if (topicName.equals(DEVICE_ATTRIBUTES_TOPIC)) {
//                msg = adaptor.convertToActorMsg(deviceSessionCtx, POST_ATTRIBUTES_REQUEST, mqttMsg);
//            } else if (topicName.startsWith(DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX)) {
//                msg = adaptor.convertToActorMsg(deviceSessionCtx, GET_ATTRIBUTES_REQUEST, mqttMsg);
//                if (msgId >= 0) {
//                    ctx.writeAndFlush(createMqttPubAckMsg(msgId));
//                }
//            } else if (topicName.startsWith(DEVICE_RPC_RESPONSE_TOPIC)) {
//                msg = adaptor.convertToActorMsg(deviceSessionCtx, TO_DEVICE_RPC_RESPONSE, mqttMsg);
//                if (msgId >= 0) {
//                    ctx.writeAndFlush(createMqttPubAckMsg(msgId));
//                }
//            } else if (topicName.startsWith(DEVICE_RPC_REQUESTS_TOPIC)) {
//                msg = adaptor.convertToActorMsg(deviceSessionCtx, TO_SERVER_RPC_REQUEST, mqttMsg);
//                if (msgId >= 0) {
//                    ctx.writeAndFlush(createMqttPubAckMsg(msgId));
//                }
//            }
//        } catch (AdaptorException e) {
//            log.warn("[{}] Failed to process publish msg [{}][{}]", sessionId, topicName, msgId, e);
//        }
//        if (msg != null) {
//            processor.process(new BasicTransportToDeviceSessionActorMsg(deviceSessionCtx.getDevice(), msg));
//        } else {
//            log.info("[{}] Closing current session due to invalid publish msg [{}][{}]", sessionId, topicName, msgId);
//            ctx.close();
//        }
//    }
//
//    private void processSubscribe(ChannelHandlerContext ctx, MqttSubscribeMessage mqttMsg) {
//        if (!checkConnected(ctx)) {
//            return;
//        }
//        log.trace("[{}] Processing subscription [{}]!", sessionId, mqttMsg.variableHeader().messageId());
//        List<Integer> grantedQoSList = new ArrayList<>();
//        for (MqttTopicSubscription subscription : mqttMsg.payload().topicSubscriptions()) {
//            String topic = subscription.topicName();
//            MqttQoS reqQoS = subscription.qualityOfService();
//            try {
//                switch (topic) {
//                    case DEVICE_ATTRIBUTES_TOPIC: {
//                        AdaptorToSessionActorMsg msg = adaptor.convertToActorMsg(deviceSessionCtx, SUBSCRIBE_ATTRIBUTES_REQUEST, mqttMsg);
//                        processor.process(new BasicTransportToDeviceSessionActorMsg(deviceSessionCtx.getDevice(), msg));
//                        registerSubQoS(topic, grantedQoSList, reqQoS);
//                        break;
//                    }
//                    case DEVICE_RPC_REQUESTS_SUB_TOPIC: {
//                        AdaptorToSessionActorMsg msg = adaptor.convertToActorMsg(deviceSessionCtx, SUBSCRIBE_RPC_COMMANDS_REQUEST, mqttMsg);
//                        processor.process(new BasicTransportToDeviceSessionActorMsg(deviceSessionCtx.getDevice(), msg));
//                        registerSubQoS(topic, grantedQoSList, reqQoS);
//                        break;
//                    }
//                    case DEVICE_RPC_RESPONSE_SUB_TOPIC:
//                    case GATEWAY_ATTRIBUTES_TOPIC:
//                    case GATEWAY_RPC_TOPIC:
//                        registerSubQoS(topic, grantedQoSList, reqQoS);
//                        break;
//                    case DEVICE_ATTRIBUTES_RESPONSES_TOPIC:
//                        deviceSessionCtx.setAllowAttributeResponses();
//                        registerSubQoS(topic, grantedQoSList, reqQoS);
//                        break;
//                    default:
//                        log.warn("[{}] Failed to subscribe to [{}][{}]", sessionId, topic, reqQoS);
//                        grantedQoSList.add(FAILURE.value());
//                        break;
//                }
//            } catch (AdaptorException e) {
//                log.warn("[{}] Failed to subscribe to [{}][{}]", sessionId, topic, reqQoS);
//                grantedQoSList.add(FAILURE.value());
//            }
//        }
//        ctx.writeAndFlush(createSubAckMessage(mqttMsg.variableHeader().messageId(), grantedQoSList));
//    }
//
//    private void registerSubQoS(String topic, List<Integer> grantedQoSList, MqttQoS reqQoS) {
//        grantedQoSList.add(getMinSupportedQos(reqQoS));
//        mqttQoSMap.put(topic, getMinSupportedQos(reqQoS));
//    }
//
//    private void processUnsubscribe(ChannelHandlerContext ctx, MqttUnsubscribeMessage mqttMsg) {
//        if (!checkConnected(ctx)) {
//            return;
//        }
//        log.trace("[{}] Processing subscription [{}]!", sessionId, mqttMsg.variableHeader().messageId());
//        for (String topicName : mqttMsg.payload().topics()) {
//            mqttQoSMap.remove(topicName);
//            try {
//                switch (topicName) {
//                    case DEVICE_ATTRIBUTES_TOPIC: {
//                        AdaptorToSessionActorMsg msg = adaptor.convertToActorMsg(deviceSessionCtx, UNSUBSCRIBE_ATTRIBUTES_REQUEST, mqttMsg);
//                        processor.process(new BasicTransportToDeviceSessionActorMsg(deviceSessionCtx.getDevice(), msg));
//                        break;
//                    }
//                    case DEVICE_RPC_REQUESTS_SUB_TOPIC: {
//                        AdaptorToSessionActorMsg msg = adaptor.convertToActorMsg(deviceSessionCtx, UNSUBSCRIBE_RPC_COMMANDS_REQUEST, mqttMsg);
//                        processor.process(new BasicTransportToDeviceSessionActorMsg(deviceSessionCtx.getDevice(), msg));
//                        break;
//                    }
//                    case DEVICE_ATTRIBUTES_RESPONSES_TOPIC:
//                        deviceSessionCtx.setDisallowAttributeResponses();
//                        break;
//                }
//            } catch (AdaptorException e) {
//                log.warn("[{}] Failed to process unsubscription [{}] to [{}]", sessionId, mqttMsg.variableHeader().messageId(), topicName);
//            }
//        }
//        ctx.writeAndFlush(createUnSubAckMessage(mqttMsg.variableHeader().messageId()));
//    }
//
//    private MqttMessage createUnSubAckMessage(int msgId) {
//        MqttFixedHeader mqttFixedHeader =
//                new MqttFixedHeader(UNSUBACK, false, AT_LEAST_ONCE, false, 0);
//        MqttMessageIdVariableHeader mqttMessageIdVariableHeader = MqttMessageIdVariableHeader.from(msgId);
//        return new MqttMessage(mqttFixedHeader, mqttMessageIdVariableHeader);
//    }

    private void processConnect(ChannelHandlerContext ctx, MqttConnectMessage msg) {
        log.info("[{}] Processing connect msg for client: {}!", sessionId, msg.payload().clientIdentifier());
        X509Certificate cert;
        if (sslHandler != null && (cert = getX509Certificate()) != null) {
            processX509CertConnect(ctx, cert);
        } else {
            processAuthTokenConnect(ctx, msg);
        }
    }

    private void processAuthTokenConnect(ChannelHandlerContext ctx, MqttConnectMessage msg) {
        String userName = msg.payload().userName();
        if (StringUtils.isEmpty(userName)) {
            ctx.writeAndFlush(createMqttConnAckMsg(CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD));
            ctx.close();
        } else {
            transportService.process(ValidateDeviceTokenRequestMsg.newBuilder().setToken(userName).build(),
                    new TransportServiceCallback<ValidateDeviceTokenResponseMsg>() {
                        @Override
                        public void onSuccess(ValidateDeviceTokenResponseMsg msg) {
                            if (!msg.hasDeviceInfo()) {
                                ctx.writeAndFlush(createMqttConnAckMsg(CONNECTION_REFUSED_NOT_AUTHORIZED));
                                ctx.close();
                            } else {
                                ctx.writeAndFlush(createMqttConnAckMsg(CONNECTION_ACCEPTED));
                                deviceSessionCtx.setDeviceInfo(deviceSessionCtx.getDeviceInfo());
                                transportService.process(getSessionEventMsg(SessionEvent.OPEN), null);
                                checkGatewaySession();
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            log.trace("[{}] Failed to process credentials: {}", address, userName, e);
                            ctx.writeAndFlush(createMqttConnAckMsg(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE));
                            ctx.close();
                        }
                    });
        }
    }

    protected SessionEventMsg getSessionEventMsg(SessionEvent event) {
        return SessionEventMsg.newBuilder()
                .setSessionInfo(sessionInfo)
                .setDeviceIdMSB(deviceSessionCtx.getDeviceIdMSB())
                .setDeviceIdLSB(deviceSessionCtx.getDeviceIdLSB())
                .setEvent(event).build();
    }

    private void processX509CertConnect(ChannelHandlerContext ctx, X509Certificate cert) {
//        try {
//            String strCert = SslUtil.getX509CertificateString(cert);
//            String sha3Hash = EncryptionUtil.getSha3Hash(strCert);
//            if (deviceSessionCtx.login(new DeviceX509Credentials(sha3Hash))) {
//                ctx.writeAndFlush(createMqttConnAckMsg(CONNECTION_ACCEPTED));
//                connected = true;
//                processor.process(new BasicTransportToDeviceSessionActorMsg(deviceSessionCtx.getDevice(),
//                        new BasicAdaptorToSessionActorMsg(deviceSessionCtx, new SessionOpenMsg())));
//                checkGatewaySession();
//            } else {
//                ctx.writeAndFlush(createMqttConnAckMsg(CONNECTION_REFUSED_NOT_AUTHORIZED));
//                ctx.close();
//            }
//        } catch (Exception e) {
//            ctx.writeAndFlush(createMqttConnAckMsg(CONNECTION_REFUSED_NOT_AUTHORIZED));
//            ctx.close();
//        }
    }

    private X509Certificate getX509Certificate() {
        try {
            X509Certificate[] certChain = sslHandler.engine().getSession().getPeerCertificateChain();
            if (certChain.length > 0) {
                return certChain[0];
            }
        } catch (SSLPeerUnverifiedException e) {
            log.warn(e.getMessage());
            return null;
        }
        return null;
    }

    private void processDisconnect(ChannelHandlerContext ctx) {
        ctx.close();
        if (deviceSessionCtx.isConnected()) {
            transportService.process(getSessionEventMsg(SessionEvent.CLOSED), null);
            if (gatewaySessionCtx != null) {
                gatewaySessionCtx.onGatewayDisconnect();
            }
        }
    }

    private MqttConnAckMessage createMqttConnAckMsg(MqttConnectReturnCode returnCode) {
        MqttFixedHeader mqttFixedHeader =
                new MqttFixedHeader(CONNACK, false, AT_MOST_ONCE, false, 0);
        MqttConnAckVariableHeader mqttConnAckVariableHeader =
                new MqttConnAckVariableHeader(returnCode, true);
        return new MqttConnAckMessage(mqttFixedHeader, mqttConnAckVariableHeader);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("[{}] Unexpected Exception", sessionId, cause);
        ctx.close();
    }

    private static MqttSubAckMessage createSubAckMessage(Integer msgId, List<Integer> grantedQoSList) {
        MqttFixedHeader mqttFixedHeader =
                new MqttFixedHeader(SUBACK, false, AT_LEAST_ONCE, false, 0);
        MqttMessageIdVariableHeader mqttMessageIdVariableHeader = MqttMessageIdVariableHeader.from(msgId);
        MqttSubAckPayload mqttSubAckPayload = new MqttSubAckPayload(grantedQoSList);
        return new MqttSubAckMessage(mqttFixedHeader, mqttMessageIdVariableHeader, mqttSubAckPayload);
    }

    private static int getMinSupportedQos(MqttQoS reqQoS) {
        return Math.min(reqQoS.value(), MAX_SUPPORTED_QOS_LVL.value());
    }

    public static MqttPubAckMessage createMqttPubAckMsg(int requestId) {
        MqttFixedHeader mqttFixedHeader =
                new MqttFixedHeader(PUBACK, false, AT_LEAST_ONCE, false, 0);
        MqttMessageIdVariableHeader mqttMsgIdVariableHeader =
                MqttMessageIdVariableHeader.from(requestId);
        return new MqttPubAckMessage(mqttFixedHeader, mqttMsgIdVariableHeader);
    }

    private boolean checkConnected(ChannelHandlerContext ctx) {
        if (deviceSessionCtx.isConnected()) {
            return true;
        } else {
            log.info("[{}] Closing current session due to invalid msg order [{}][{}]", sessionId);
            ctx.close();
            return false;
        }
    }

    private void checkGatewaySession() {
        DeviceInfoProto device = deviceSessionCtx.getDeviceInfo();
        try {
            JsonNode infoNode = context.getMapper().readTree(device.getAdditionalInfo());
            if (infoNode != null) {
                JsonNode gatewayNode = infoNode.get("gateway");
                if (gatewayNode != null && gatewayNode.asBoolean()) {
                    gatewaySessionCtx = new GatewaySessionCtx(deviceSessionCtx);
                }
            }
        } catch (IOException e) {
            log.trace("[{}][{}] Failed to fetch device additional info", sessionId, device.getDeviceName(), e);
        }
    }

    @Override
    public void operationComplete(Future<? super Void> future) throws Exception {
        transportService.process(getSessionEventMsg(SessionEvent.CLOSED), null);
    }
}
