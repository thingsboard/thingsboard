/**
 * Copyright © 2016 The Thingsboard Authors
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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.security.DeviceTokenCredentials;
import org.thingsboard.server.common.data.security.DeviceX509Credentials;
import org.thingsboard.server.common.msg.session.AdaptorToSessionActorMsg;
import org.thingsboard.server.common.msg.session.BasicToDeviceActorSessionMsg;
import org.thingsboard.server.common.msg.session.MsgType;
import org.thingsboard.server.common.msg.session.ctrl.SessionCloseMsg;
import org.thingsboard.server.common.transport.SessionMsgProcessor;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.auth.DeviceAuthService;
import org.thingsboard.server.dao.EncryptionUtil;
import org.thingsboard.server.transport.mqtt.adaptors.MqttTransportAdaptor;
import org.thingsboard.server.transport.mqtt.session.MqttSessionCtx;
import org.thingsboard.server.transport.mqtt.util.SslUtil;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Andrew Shvayka
 */
@Slf4j
public class MqttTransportHandler extends ChannelInboundHandlerAdapter implements GenericFutureListener<Future<? super Void>> {

    public static final MqttQoS MAX_SUPPORTED_QOS_LVL = MqttQoS.AT_LEAST_ONCE;
    public static final String BASE_TOPIC = "v1/devices/me";
    public static final String ATTRIBUTES_TOPIC = BASE_TOPIC + "/attributes";
    public static final String TELEMETRY_TOPIC = BASE_TOPIC + "/telemetry";
    public static final String ATTRIBUTES_REQUEST_TOPIC_PREFIX = BASE_TOPIC + "/attributes/request/";
    public static final String ATTRIBUTES_RESPONSE_TOPIC_PREFIX = BASE_TOPIC + "/attributes/response/";
    public static final String ATTRIBUTES_RESPONSES_TOPIC = ATTRIBUTES_RESPONSE_TOPIC_PREFIX + "+";
    public static final String RPC_REQUESTS_TOPIC = BASE_TOPIC + "/rpc/request/";
    public static final String RPC_REQUESTS_SUB_TOPIC = RPC_REQUESTS_TOPIC + "+";
    public static final String RPC_RESPONSE_TOPIC = BASE_TOPIC + "/rpc/response/";
    public static final String RPC_RESPONSE_SUB_TOPIC = RPC_RESPONSE_TOPIC + "+";
    private final MqttSessionCtx sessionCtx;
    private final String sessionId;
    private final MqttTransportAdaptor adaptor;
    private final SessionMsgProcessor processor;
    private final SslHandler sslHandler;

    public MqttTransportHandler(SessionMsgProcessor processor, DeviceAuthService authService,
                                MqttTransportAdaptor adaptor, SslHandler sslHandler) {
        this.processor = processor;
        this.adaptor = adaptor;
        this.sessionCtx = new MqttSessionCtx(processor, authService, adaptor);
        this.sessionId = sessionCtx.getSessionId().toUidStr();
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
        sessionCtx.setChannel(ctx);
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
                ctx.writeAndFlush(new MqttMessage(new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0)));
                break;
            case DISCONNECT:
                processDisconnect(ctx);
                break;
        }
    }

    private void processPublish(ChannelHandlerContext ctx, MqttPublishMessage mqttMsg) {
        String topicName = mqttMsg.variableHeader().topicName();
        int msgId = mqttMsg.variableHeader().messageId();
        log.trace("[{}] Processing publish msg [{}][{}]!", sessionId, topicName, msgId);
        AdaptorToSessionActorMsg msg = null;
        try {
            if (topicName.equals(ATTRIBUTES_TOPIC)) {
                msg = adaptor.convertToActorMsg(sessionCtx, MsgType.POST_ATTRIBUTES_REQUEST, mqttMsg);
            } else if (topicName.equals(TELEMETRY_TOPIC)) {
                msg = adaptor.convertToActorMsg(sessionCtx, MsgType.POST_TELEMETRY_REQUEST, mqttMsg);
            } else if (topicName.startsWith(ATTRIBUTES_REQUEST_TOPIC_PREFIX)) {
                msg = adaptor.convertToActorMsg(sessionCtx, MsgType.GET_ATTRIBUTES_REQUEST, mqttMsg);
                if (msgId >= 0) {
                    ctx.writeAndFlush(createMqttPubAckMsg(msgId));
                }
            } else if (topicName.startsWith(RPC_RESPONSE_TOPIC)) {
                msg = adaptor.convertToActorMsg(sessionCtx, MsgType.TO_DEVICE_RPC_RESPONSE, mqttMsg);
                if (msgId >= 0) {
                    ctx.writeAndFlush(createMqttPubAckMsg(msgId));
                }
            } else if (topicName.startsWith(RPC_REQUESTS_TOPIC)) {
                msg = adaptor.convertToActorMsg(sessionCtx, MsgType.TO_SERVER_RPC_REQUEST, mqttMsg);
                if (msgId >= 0) {
                    ctx.writeAndFlush(createMqttPubAckMsg(msgId));
                }
            }
        } catch (AdaptorException e) {
            log.warn("[{}] Failed to process publish msg [{}][{}]", sessionId, topicName, msgId, e);
        }

        if (msg != null) {
            processor.process(new BasicToDeviceActorSessionMsg(sessionCtx.getDevice(), msg));
        } else {
            log.warn("[{}] Closing current session due to invalid publish msg [{}][{}]", sessionId, topicName, msgId);
            ctx.close();
        }
    }

    private void processSubscribe(ChannelHandlerContext ctx, MqttSubscribeMessage mqttMsg) {
        log.info("[{}] Processing subscription [{}]!", sessionId, mqttMsg.variableHeader().messageId());
        List<Integer> grantedQoSList = new ArrayList<>();
        for (MqttTopicSubscription subscription : mqttMsg.payload().topicSubscriptions()) {
            String topicName = subscription.topicName();
            //TODO: handle this qos level.
            MqttQoS reqQoS = subscription.qualityOfService();
            try {
                if (topicName.equals(ATTRIBUTES_TOPIC)) {
                    AdaptorToSessionActorMsg msg = adaptor.convertToActorMsg(sessionCtx, MsgType.SUBSCRIBE_ATTRIBUTES_REQUEST, mqttMsg);
                    processor.process(new BasicToDeviceActorSessionMsg(sessionCtx.getDevice(), msg));
                    grantedQoSList.add(getMinSupportedQos(reqQoS));
                } else if (topicName.equals(RPC_REQUESTS_SUB_TOPIC)) {
                    AdaptorToSessionActorMsg msg = adaptor.convertToActorMsg(sessionCtx, MsgType.SUBSCRIBE_RPC_COMMANDS_REQUEST, mqttMsg);
                    processor.process(new BasicToDeviceActorSessionMsg(sessionCtx.getDevice(), msg));
                    grantedQoSList.add(getMinSupportedQos(reqQoS));
                } else if (topicName.equals(RPC_RESPONSE_SUB_TOPIC)) {
                    grantedQoSList.add(getMinSupportedQos(reqQoS));
                } else if (topicName.equals(ATTRIBUTES_RESPONSES_TOPIC)) {
                    sessionCtx.setAllowAttributeResponses();
                    grantedQoSList.add(getMinSupportedQos(reqQoS));
                } else {
                    log.warn("[{}] Failed to subscribe to [{}][{}]", sessionId, topicName, reqQoS);
                    grantedQoSList.add(MqttQoS.FAILURE.value());
                }
            } catch (AdaptorException e) {
                log.warn("[{}] Failed to subscribe to [{}][{}]", sessionId, topicName, reqQoS);
                grantedQoSList.add(MqttQoS.FAILURE.value());
            }
        }
        ctx.writeAndFlush(createSubAckMessage(mqttMsg.variableHeader().messageId(), grantedQoSList));
    }

    private void processUnsubscribe(ChannelHandlerContext ctx, MqttUnsubscribeMessage mqttMsg) {
        log.info("[{}] Processing subscription [{}]!", sessionId, mqttMsg.variableHeader().messageId());
        for (String topicName : mqttMsg.payload().topics()) {
            try {
                if (topicName.equals(ATTRIBUTES_TOPIC)) {
                    AdaptorToSessionActorMsg msg = adaptor.convertToActorMsg(sessionCtx, MsgType.UNSUBSCRIBE_ATTRIBUTES_REQUEST, mqttMsg);
                    processor.process(new BasicToDeviceActorSessionMsg(sessionCtx.getDevice(), msg));
                } else if (topicName.equals(RPC_REQUESTS_SUB_TOPIC)) {
                    AdaptorToSessionActorMsg msg = adaptor.convertToActorMsg(sessionCtx, MsgType.UNSUBSCRIBE_RPC_COMMANDS_REQUEST, mqttMsg);
                    processor.process(new BasicToDeviceActorSessionMsg(sessionCtx.getDevice(), msg));
                } else if (topicName.equals(ATTRIBUTES_RESPONSES_TOPIC)) {
                    sessionCtx.setDisallowAttributeResponses();
                }
            } catch (AdaptorException e) {
                log.warn("[{}] Failed to process unsubscription [{}] to [{}]", sessionId, mqttMsg.variableHeader().messageId(), topicName);
            }
        }
        ctx.writeAndFlush(createUnSubAckMessage(mqttMsg.variableHeader().messageId()));
    }

    private MqttMessage createUnSubAckMessage(int msgId) {
        MqttFixedHeader mqttFixedHeader =
                new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.AT_LEAST_ONCE, false, 0);
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
            ctx.writeAndFlush(createMqttConnAckMsg(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD));
            ctx.close();
        } else if (sessionCtx.login(new DeviceTokenCredentials(msg.payload().userName()))) {
            ctx.writeAndFlush(createMqttConnAckMsg(MqttConnectReturnCode.CONNECTION_ACCEPTED));
        } else {
            ctx.writeAndFlush(createMqttConnAckMsg(MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED));
            ctx.close();
        }
    }

    private void processX509CertConnect(ChannelHandlerContext ctx, X509Certificate cert) {
        try {
            String strCert = SslUtil.getX509CertificateString(cert);
            String sha3Hash = EncryptionUtil.getSha3Hash(strCert);
            if (sessionCtx.login(new DeviceX509Credentials(sha3Hash))) {
                ctx.writeAndFlush(createMqttConnAckMsg(MqttConnectReturnCode.CONNECTION_ACCEPTED));
            } else {
                ctx.writeAndFlush(createMqttConnAckMsg(MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED));
                ctx.close();
            }
        } catch (Exception e) {
            ctx.writeAndFlush(createMqttConnAckMsg(MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED));
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
    }

    private MqttConnAckMessage createMqttConnAckMsg(MqttConnectReturnCode returnCode) {
        MqttFixedHeader mqttFixedHeader =
                new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
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
                new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.AT_LEAST_ONCE, false, 0);
        MqttMessageIdVariableHeader mqttMessageIdVariableHeader = MqttMessageIdVariableHeader.from(msgId);
        MqttSubAckPayload mqttSubAckPayload = new MqttSubAckPayload(grantedQoSList);
        return new MqttSubAckMessage(mqttFixedHeader, mqttMessageIdVariableHeader, mqttSubAckPayload);
    }

    private static int getMinSupportedQos(MqttQoS reqQoS) {
        return Math.min(reqQoS.value(), MAX_SUPPORTED_QOS_LVL.value());
    }

    public static MqttPubAckMessage createMqttPubAckMsg(int requestId) {
        MqttFixedHeader mqttFixedHeader =
                new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_LEAST_ONCE, false, 0);
        MqttMessageIdVariableHeader mqttMsgIdVariableHeader =
                MqttMessageIdVariableHeader.from(requestId);
        return new MqttPubAckMessage(mqttFixedHeader, mqttMsgIdVariableHeader);
    }

    @Override
    public void operationComplete(Future<? super Void> future) throws Exception {
        processor.process(SessionCloseMsg.onError(sessionCtx.getSessionId()));
    }
}
