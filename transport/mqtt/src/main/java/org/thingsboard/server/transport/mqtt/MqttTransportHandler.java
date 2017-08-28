/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.security.DeviceTokenCredentials;
import org.thingsboard.server.common.data.security.DeviceX509Credentials;
import org.thingsboard.server.common.msg.session.AdaptorToSessionActorMsg;
import org.thingsboard.server.common.msg.session.BasicToDeviceActorSessionMsg;
import org.thingsboard.server.common.msg.session.ctrl.SessionCloseMsg;
import org.thingsboard.server.common.transport.SessionMsgProcessor;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.auth.DeviceAuthService;
import org.thingsboard.server.dao.EncryptionUtil;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.transport.mqtt.adaptors.MqttTransportAdaptor;
import org.thingsboard.server.transport.mqtt.session.GatewaySessionCtx;
import org.thingsboard.server.transport.mqtt.session.DeviceSessionCtx;
import org.thingsboard.server.transport.mqtt.util.SslUtil;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.cert.X509Certificate;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.*;
import static io.netty.handler.codec.mqtt.MqttMessageType.*;
import static io.netty.handler.codec.mqtt.MqttQoS.*;
import static org.thingsboard.server.common.msg.session.MsgType.*;
import static org.thingsboard.server.transport.mqtt.MqttTopics.*;

/**
 * @author Andrew Shvayka
 */
@Slf4j
public class MqttTransportHandler extends ChannelInboundHandlerAdapter implements GenericFutureListener<Future<? super Void>> {

    public static final MqttQoS MAX_SUPPORTED_QOS_LVL = AT_LEAST_ONCE;

    private final DeviceSessionCtx deviceSessionCtx;
    private final String sessionId;
    private final MqttTransportAdaptor adaptor;
    private final SessionMsgProcessor processor;
    private final DeviceService deviceService;
    private final DeviceAuthService authService;
    private final RelationService relationService;
    private final SslHandler sslHandler;
    private volatile boolean connected;
    private volatile InetSocketAddress address;
    private volatile GatewaySessionCtx gatewaySessionCtx;

    public MqttTransportHandler(SessionMsgProcessor processor, DeviceService deviceService, DeviceAuthService authService, RelationService relationService,
                                MqttTransportAdaptor adaptor, SslHandler sslHandler) {
        this.processor = processor;
        this.deviceService = deviceService;
        this.relationService = relationService;
        this.authService = authService;
        this.adaptor = adaptor;
        this.deviceSessionCtx = new DeviceSessionCtx(processor, authService, adaptor);
        this.sessionId = deviceSessionCtx.getSessionId().toUidStr();
        this.sslHandler = sslHandler;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        log.trace("[{}] Processing msg: {}", sessionId, msg);
        if (msg instanceof MqttMessage) {
            processMqttMsg(ctx, (MqttMessage) msg);
        }
    }

    private void processMqttMsg(ChannelHandlerContext ctx, MqttMessage msg) {
        address = (InetSocketAddress) ctx.channel().remoteAddress();
        if (msg.fixedHeader() == null) {
            log.info("[{}:{}] Invalid message received", address.getHostName(), address.getPort());
            processDisconnect(ctx);
        } else {
            deviceSessionCtx.setChannel(ctx);
            switch (msg.fixedHeader().messageType()) {
                case CONNECT:
                    processConnect(ctx, (MqttConnectMessage) msg);
                    break;
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
                    if (checkConnected(ctx)) {
                        ctx.writeAndFlush(new MqttMessage(new MqttFixedHeader(PINGRESP, false, AT_MOST_ONCE, false, 0)));
                    }
                    break;
                case DISCONNECT:
                    if (checkConnected(ctx)) {
                        processDisconnect(ctx);
                    }
                    break;
            }
        }
    }

    private void processPublish(ChannelHandlerContext ctx, MqttPublishMessage mqttMsg) {
        if (!checkConnected(ctx)) {
            return;
        }
        String topicName = mqttMsg.variableHeader().topicName();
        int msgId = mqttMsg.variableHeader().messageId();
        log.trace("[{}] Processing publish msg [{}][{}]!", sessionId, topicName, msgId);

        if (topicName.startsWith(BASE_GATEWAY_API_TOPIC)) {
            if (gatewaySessionCtx != null) {
                gatewaySessionCtx.setChannel(ctx);
                try {
                    if (topicName.equals(GATEWAY_TELEMETRY_TOPIC)) {
                        gatewaySessionCtx.onDeviceTelemetry(mqttMsg);
                    } else if (topicName.equals(GATEWAY_ATTRIBUTES_TOPIC)) {
                        gatewaySessionCtx.onDeviceAttributes(mqttMsg);
                    } else if (topicName.equals(GATEWAY_ATTRIBUTES_REQUEST_TOPIC)) {
                        gatewaySessionCtx.onDeviceAttributesRequest(mqttMsg);
                    } else if (topicName.equals(GATEWAY_RPC_TOPIC)) {
                        gatewaySessionCtx.onDeviceRpcResponse(mqttMsg);
                    } else if (topicName.equals(GATEWAY_CONNECT_TOPIC)) {
                        gatewaySessionCtx.onDeviceConnect(mqttMsg);
                    } else if (topicName.equals(GATEWAY_DISCONNECT_TOPIC)) {
                        gatewaySessionCtx.onDeviceDisconnect(mqttMsg);
                    }
                } catch (RuntimeException | AdaptorException e) {
                    log.warn("[{}] Failed to process publish msg [{}][{}]", sessionId, topicName, msgId, e);
                }
            }
        } else {
            processDevicePublish(ctx, mqttMsg, topicName, msgId);
        }
    }

    private void processDevicePublish(ChannelHandlerContext ctx, MqttPublishMessage mqttMsg, String topicName, int msgId) {
        AdaptorToSessionActorMsg msg = null;
        try {
            if (topicName.equals(DEVICE_TELEMETRY_TOPIC)) {
                msg = adaptor.convertToActorMsg(deviceSessionCtx, POST_TELEMETRY_REQUEST, mqttMsg);
            } else if (topicName.equals(DEVICE_ATTRIBUTES_TOPIC)) {
                msg = adaptor.convertToActorMsg(deviceSessionCtx, POST_ATTRIBUTES_REQUEST, mqttMsg);
            } else if (topicName.startsWith(DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX)) {
                msg = adaptor.convertToActorMsg(deviceSessionCtx, GET_ATTRIBUTES_REQUEST, mqttMsg);
                if (msgId >= 0) {
                    ctx.writeAndFlush(createMqttPubAckMsg(msgId));
                }
            } else if (topicName.startsWith(DEVICE_RPC_RESPONSE_TOPIC)) {
                msg = adaptor.convertToActorMsg(deviceSessionCtx, TO_DEVICE_RPC_RESPONSE, mqttMsg);
                if (msgId >= 0) {
                    ctx.writeAndFlush(createMqttPubAckMsg(msgId));
                }
            } else if (topicName.startsWith(DEVICE_RPC_REQUESTS_TOPIC)) {
                msg = adaptor.convertToActorMsg(deviceSessionCtx, TO_SERVER_RPC_REQUEST, mqttMsg);
                if (msgId >= 0) {
                    ctx.writeAndFlush(createMqttPubAckMsg(msgId));
                }
            }
        } catch (AdaptorException e) {
            log.warn("[{}] Failed to process publish msg [{}][{}]", sessionId, topicName, msgId, e);
        }
        if (msg != null) {
            processor.process(new BasicToDeviceActorSessionMsg(deviceSessionCtx.getDevice(), msg));
        } else {
            log.info("[{}] Closing current session due to invalid publish msg [{}][{}]", sessionId, topicName, msgId);
            ctx.close();
        }
    }

    private void processSubscribe(ChannelHandlerContext ctx, MqttSubscribeMessage mqttMsg) {
        if (!checkConnected(ctx)) {
            return;
        }
        log.trace("[{}] Processing subscription [{}]!", sessionId, mqttMsg.variableHeader().messageId());
        List<Integer> grantedQoSList = new ArrayList<>();
        for (MqttTopicSubscription subscription : mqttMsg.payload().topicSubscriptions()) {
            String topicName = subscription.topicName();
            //TODO: handle this qos level.
            MqttQoS reqQoS = subscription.qualityOfService();
            try {
                if (topicName.equals(DEVICE_ATTRIBUTES_TOPIC)) {
                    AdaptorToSessionActorMsg msg = adaptor.convertToActorMsg(deviceSessionCtx, SUBSCRIBE_ATTRIBUTES_REQUEST, mqttMsg);
                    processor.process(new BasicToDeviceActorSessionMsg(deviceSessionCtx.getDevice(), msg));
                    grantedQoSList.add(getMinSupportedQos(reqQoS));
                } else if (topicName.equals(DEVICE_RPC_REQUESTS_SUB_TOPIC)) {
                    AdaptorToSessionActorMsg msg = adaptor.convertToActorMsg(deviceSessionCtx, SUBSCRIBE_RPC_COMMANDS_REQUEST, mqttMsg);
                    processor.process(new BasicToDeviceActorSessionMsg(deviceSessionCtx.getDevice(), msg));
                    grantedQoSList.add(getMinSupportedQos(reqQoS));
                } else if (topicName.equals(DEVICE_RPC_RESPONSE_SUB_TOPIC)) {
                    grantedQoSList.add(getMinSupportedQos(reqQoS));
                } else if (topicName.equals(DEVICE_ATTRIBUTES_RESPONSES_TOPIC)) {
                    deviceSessionCtx.setAllowAttributeResponses();
                    grantedQoSList.add(getMinSupportedQos(reqQoS));
                } else {
                    log.warn("[{}] Failed to subscribe to [{}][{}]", sessionId, topicName, reqQoS);
                    grantedQoSList.add(FAILURE.value());
                }
            } catch (AdaptorException e) {
                log.warn("[{}] Failed to subscribe to [{}][{}]", sessionId, topicName, reqQoS);
                grantedQoSList.add(FAILURE.value());
            }
        }
        ctx.writeAndFlush(createSubAckMessage(mqttMsg.variableHeader().messageId(), grantedQoSList));
    }

    private void processUnsubscribe(ChannelHandlerContext ctx, MqttUnsubscribeMessage mqttMsg) {
        if (!checkConnected(ctx)) {
            return;
        }
        log.trace("[{}] Processing subscription [{}]!", sessionId, mqttMsg.variableHeader().messageId());
        for (String topicName : mqttMsg.payload().topics()) {
            try {
                if (topicName.equals(DEVICE_ATTRIBUTES_TOPIC)) {
                    AdaptorToSessionActorMsg msg = adaptor.convertToActorMsg(deviceSessionCtx, UNSUBSCRIBE_ATTRIBUTES_REQUEST, mqttMsg);
                    processor.process(new BasicToDeviceActorSessionMsg(deviceSessionCtx.getDevice(), msg));
                } else if (topicName.equals(DEVICE_RPC_REQUESTS_SUB_TOPIC)) {
                    AdaptorToSessionActorMsg msg = adaptor.convertToActorMsg(deviceSessionCtx, UNSUBSCRIBE_RPC_COMMANDS_REQUEST, mqttMsg);
                    processor.process(new BasicToDeviceActorSessionMsg(deviceSessionCtx.getDevice(), msg));
                } else if (topicName.equals(DEVICE_ATTRIBUTES_RESPONSES_TOPIC)) {
                    deviceSessionCtx.setDisallowAttributeResponses();
                }
            } catch (AdaptorException e) {
                log.warn("[{}] Failed to process unsubscription [{}] to [{}]", sessionId, mqttMsg.variableHeader().messageId(), topicName);
            }
        }
        ctx.writeAndFlush(createUnSubAckMessage(mqttMsg.variableHeader().messageId()));
    }

    private MqttMessage createUnSubAckMessage(int msgId) {
        MqttFixedHeader mqttFixedHeader =
                new MqttFixedHeader(UNSUBACK, false, AT_LEAST_ONCE, false, 0);
        MqttMessageIdVariableHeader mqttMessageIdVariableHeader = MqttMessageIdVariableHeader.from(msgId);
        return new MqttMessage(mqttFixedHeader, mqttMessageIdVariableHeader);
    }

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
        } else if (!deviceSessionCtx.login(new DeviceTokenCredentials(msg.payload().userName()))) {
            ctx.writeAndFlush(createMqttConnAckMsg(CONNECTION_REFUSED_NOT_AUTHORIZED));
            ctx.close();
        } else {
            ctx.writeAndFlush(createMqttConnAckMsg(CONNECTION_ACCEPTED));
            connected = true;
            checkGatewaySession();
        }
    }

    private void processX509CertConnect(ChannelHandlerContext ctx, X509Certificate cert) {
        try {
            String strCert = SslUtil.getX509CertificateString(cert);
            String sha3Hash = EncryptionUtil.getSha3Hash(strCert);
            if (deviceSessionCtx.login(new DeviceX509Credentials(sha3Hash))) {
                ctx.writeAndFlush(createMqttConnAckMsg(CONNECTION_ACCEPTED));
                connected = true;
                checkGatewaySession();
            } else {
                ctx.writeAndFlush(createMqttConnAckMsg(CONNECTION_REFUSED_NOT_AUTHORIZED));
                ctx.close();
            }
        } catch (Exception e) {
            ctx.writeAndFlush(createMqttConnAckMsg(CONNECTION_REFUSED_NOT_AUTHORIZED));
            ctx.close();
        }
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
        if (connected) {
            processor.process(SessionCloseMsg.onDisconnect(deviceSessionCtx.getSessionId()));
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
        if (connected) {
            return true;
        } else {
            log.info("[{}] Closing current session due to invalid msg order [{}][{}]", sessionId);
            ctx.close();
            return false;
        }
    }

    private void checkGatewaySession() {
        Device device = deviceSessionCtx.getDevice();
        JsonNode infoNode = device.getAdditionalInfo();
        if (infoNode != null) {
            JsonNode gatewayNode = infoNode.get("gateway");
            if (gatewayNode != null && gatewayNode.asBoolean()) {
                gatewaySessionCtx = new GatewaySessionCtx(processor, deviceService, authService, relationService, deviceSessionCtx);
            }
        }
    }

    @Override
    public void operationComplete(Future<? super Void> future) throws Exception {
        processor.process(SessionCloseMsg.onError(deviceSessionCtx.getSessionId()));
    }
}
