// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.mqtt;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPubReplyMessageVariableHeader;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttReasonCodes.PubAck;
import io.netty.handler.codec.mqtt.MqttReasonCodes.PubComp;
import io.netty.handler.codec.mqtt.MqttReasonCodes.PubRec;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.mqtt.MqttOrderedAcknowledgementCtx.MqttMsgWrapper;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
final class MqttChannelHandler extends SimpleChannelInboundHandler<MqttMessage> {

    private final AtomicLong publishMsgCount = new AtomicLong(0);

    private final boolean backPressureEnabled;
    private final int highWatermark;
    private final int lowWatermark;

    private final MqttClientImpl client;
    private final Promise<MqttConnectResult> connectFuture;
    private final MqttOrderedAcknowledgementCtx mqttOrderedAcknowledgementCtxQoS1;
    private final MqttOrderedAcknowledgementCtx mqttOrderedAcknowledgementCtxQoS2;

    MqttChannelHandler(MqttClientImpl client, Promise<MqttConnectResult> connectFuture) {
        this.client = client;
        this.connectFuture = connectFuture;
        this.backPressureEnabled = client.getClientConfig().isBackPressureEnabled();
        this.highWatermark = client.getClientConfig().getBackPressureHighWatermark();
        this.lowWatermark = client.getClientConfig().getBackPressureLowWatermark();
        MqttVersion mqttVersion = client.getClientConfig().getProtocolVersion();
        this.mqttOrderedAcknowledgementCtxQoS1 = new MqttOrderedAcknowledgementCtx(client.getClientConfig().getClientId(), mqttVersion, MqttMessageType.PUBACK);
        this.mqttOrderedAcknowledgementCtxQoS2 = new MqttOrderedAcknowledgementCtx(client.getClientConfig().getClientId(), mqttVersion, MqttMessageType.PUBREC);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) {
        if (msg.decoderResult().isSuccess()) {
            switch (msg.fixedHeader().messageType()) {
                case CONNACK:
                    handleConack(ctx.channel(), (MqttConnAckMessage) msg);
                    break;
                case SUBACK:
                    handleSubAck((MqttSubAckMessage) msg);
                    break;
                case PUBLISH:
                    handlePublish(ctx.channel(), (MqttPublishMessage) msg);
                    break;
                case UNSUBACK:
                    handleUnsuback((MqttUnsubAckMessage) msg);
                    break;
                case PUBACK:
                    handlePuback((MqttPubAckMessage) msg);
                    break;
                case PUBREC:
                    handlePubrec(ctx.channel(), msg);
                    break;
                case PUBREL:
                    handlePubrel(ctx.channel(), msg);
                    break;
                case PUBCOMP:
                    handlePubcomp(msg);
                    break;
                case DISCONNECT:
                    handleDisconnect(msg);
                    break;
            }
        } else {
            log.error("[{}] Message decoding failed: {}", client.getClientConfig().getClientId(), msg.decoderResult().cause().getMessage());
            ctx.close();
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);

        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.CONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttConnectVariableHeader variableHeader = new MqttConnectVariableHeader(
                this.client.getClientConfig().getProtocolVersion().protocolName(),  // Protocol Name
                this.client.getClientConfig().getProtocolVersion().protocolLevel(), // Protocol Level
                this.client.getClientConfig().getUsername() != null,                // Has Username
                this.client.getClientConfig().getPassword() != null,                // Has Password
                this.client.getClientConfig().getLastWill() != null                 // Will Retain
                        && this.client.getClientConfig().getLastWill().isRetain(),
                this.client.getClientConfig().getLastWill() != null                 // Will QOS
                        ? this.client.getClientConfig().getLastWill().getQos().value()
                        : 0,
                this.client.getClientConfig().getLastWill() != null,                // Has Will
                this.client.getClientConfig().isCleanSession(),                     // Clean Session
                this.client.getClientConfig().getTimeoutSeconds()                   // Timeout
        );
        MqttConnectPayload payload = new MqttConnectPayload(
                this.client.getClientConfig().getClientId(),
                this.client.getClientConfig().getLastWill() != null ? this.client.getClientConfig().getLastWill().getTopic() : null,
                this.client.getClientConfig().getLastWill() != null ? this.client.getClientConfig().getLastWill().getMessage().getBytes(CharsetUtil.UTF_8) : null,
                this.client.getClientConfig().getUsername(),
                this.client.getClientConfig().getPassword() != null ? this.client.getClientConfig().getPassword().getBytes(CharsetUtil.UTF_8) : null
        );
        ctx.channel().writeAndFlush(new MqttConnectMessage(fixedHeader, variableHeader, payload));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

    ListenableFuture<Void> invokeHandlersForIncomingPublish(MqttPublishMessage message) {
        String topic = message.variableHeader().topicName();
        ByteBuf payload = message.payload();

        var future = Futures.immediateVoidFuture();
        var handlerInvoked = new AtomicBoolean();
        try {
            for (MqttSubscription subscription : ImmutableSet.copyOf(this.client.getSubscriptions().values())) {
                if (!subscription.matches(topic)) {
                    continue;
                }
                future = Futures.transformAsync(future, __ -> {
                    if (subscription.isOnce() && subscription.isCalled()) {
                        return Futures.immediateVoidFuture();
                    }
                    payload.markReaderIndex();
                    subscription.setCalled(true);
                    var handlerFuture = adaptFuture(subscription.getHandler().onMessage(topic, payload));

                    return Futures.transformAsync(handlerFuture, ___ -> {
                        if (subscription.isOnce()) {
                            this.client.off(subscription.getTopic(), subscription.getHandler());
                        }
                        payload.resetReaderIndex();
                        handlerInvoked.set(true);
                        return Futures.immediateVoidFuture();
                    }, client.getHandlerExecutor());
                }, client.getHandlerExecutor());
            }

            future = Futures.transformAsync(future, __ -> {
                if (!handlerInvoked.get() && client.getDefaultHandler() != null) {
                    payload.markReaderIndex();
                    var defaultFuture = adaptFuture(client.getDefaultHandler().onMessage(topic, payload));

                    return Futures.transformAsync(defaultFuture, ___ -> {
                        payload.resetReaderIndex();
                        return Futures.immediateVoidFuture();
                    }, client.getHandlerExecutor());
                }
                return Futures.immediateVoidFuture();
            }, client.getHandlerExecutor());
        } finally {
            Futures.addCallback(future, new FutureCallback<>() {
                @Override
                public void onSuccess(Void result) {
                    payload.release();
                }

                @Override
                public void onFailure(Throwable t) {
                    payload.release();
                }
            }, MoreExecutors.directExecutor());
        }
        return future;
    }

    private void handleConack(Channel channel, MqttConnAckMessage message) {
        if (log.isTraceEnabled()) {
            log.trace("[{}][{}] Handling CONNACK: {}", client.getClientConfig().getOwnerId(), client.getClientConfig().getClientId(), message);
        }
        switch (message.variableHeader().connectReturnCode()) {
            case CONNECTION_ACCEPTED:
                this.connectFuture.setSuccess(new MqttConnectResult(true, MqttConnectReturnCode.CONNECTION_ACCEPTED, channel.closeFuture()));

                this.client.getPendingSubscriptions().entrySet().stream().filter((e) -> !e.getValue().isSent()).forEach((e) -> {
                    channel.write(e.getValue().getSubscribeMessage());
                    e.getValue().setSent(true);
                });

                this.client.getPendingPublishes().forEach((id, publish) -> {
                    if (publish.isSent()) return;
                    channel.write(publish.getMessage());
                    publish.setSent(true);
                    if (publish.getQos() == MqttQoS.AT_MOST_ONCE) {
                        publish.getFuture().setSuccess(null); // We don't get an ACK for QOS 0
                        this.client.getPendingPublishes().remove(publish.getMessageId());
                    }
                });
                channel.flush();
                if (this.client.isReconnect()) {
                    this.client.onSuccessfulReconnect();
                }
                break;

            case CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD:
            case CONNECTION_REFUSED_IDENTIFIER_REJECTED:
            case CONNECTION_REFUSED_NOT_AUTHORIZED:
            case CONNECTION_REFUSED_SERVER_UNAVAILABLE:
            case CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION:
                this.connectFuture.setSuccess(new MqttConnectResult(false, message.variableHeader().connectReturnCode(), channel.closeFuture()));
                channel.close();
                // Don't start reconnecting logic here
                break;
        }
        if (this.client.getCallback() != null) {
            this.client.getCallback().onConnAck(message);
        }
    }

    private void handleSubAck(MqttSubAckMessage message) {
        MqttPendingSubscription pendingSubscription = this.client.getPendingSubscriptions().remove(message.variableHeader().messageId());
        if (pendingSubscription == null) {
            return;
        }
        pendingSubscription.onSubackReceived();
        for (MqttPendingSubscription.MqttPendingHandler handler : pendingSubscription.getHandlers()) {
            MqttSubscription subscription = new MqttSubscription(pendingSubscription.getTopic(), handler.handler(), handler.once());
            this.client.getSubscriptions().put(pendingSubscription.getTopic(), subscription);
            this.client.getHandlerToSubscription().put(handler.handler(), subscription);
        }
        this.client.getPendingSubscribeTopics().remove(pendingSubscription.getTopic());

        this.client.getServerSubscriptions().add(pendingSubscription.getTopic());

        if (!pendingSubscription.getFuture().isDone()) {
            pendingSubscription.getFuture().setSuccess(null);
        }
        if (this.client.getCallback() != null) {
            this.client.getCallback().onSubAck(message);
        }
    }

    private void handlePublish(Channel channel, MqttPublishMessage message) {
        if (log.isTraceEnabled()) {
            log.trace("[{}][{}] Handling PUBLISH: {}", client.getClientConfig().getOwnerId(), client.getClientConfig().getClientId(), message);
        }

        MqttQoS qoS = message.fixedHeader().qosLevel();
        checkBackPressure(channel, true, qoS);

        switch (qoS) {
            case AT_MOST_ONCE -> {
                invokeHandlersForIncomingPublish(message);
            }

            case AT_LEAST_ONCE -> {
                final int msgId = message.variableHeader().packetId();
                var msgWrapper = mqttOrderedAcknowledgementCtxQoS1.addMsgId(msgId);

                if (msgWrapper == null) {
                    checkBackPressure(channel, false, qoS);
                    return;
                }

                var future = invokeHandlersForIncomingPublish(message);
                DonAsynchron.withCallback(future,
                        (_) -> {
                            processPubAck(channel, msgWrapper, PubAck.SUCCESS.byteValue());
                            checkBackPressure(channel, false, qoS);
                        },
                        (t) -> {
                            log.error("Error invoke future for client {} with QoS {}", client.getClientConfig().getClientId(), MqttQoS.AT_LEAST_ONCE, t);
                            processPubAck(channel, msgWrapper, PubAck.UNSPECIFIED_ERROR.byteValue());
                            checkBackPressure(channel, false, qoS);
                        }
                );
            }

            case EXACTLY_ONCE -> {
                final int msgId = message.variableHeader().packetId();

                if (!client.getQos2PendingMsgIds().add(msgId)) {
                    log.debug("Duplicate QoS2 message received for client {} with msgId {}. Skipping processing.", client.getClientConfig().getClientId(), msgId);
                    processPubRec(channel, msgId, PubRec.PACKET_IDENTIFIER_IN_USE.byteValue());
                    checkBackPressure(channel, false, qoS);
                    return;
                }

                var msgWrapper = mqttOrderedAcknowledgementCtxQoS2.addMsgId(msgId);
                if (msgWrapper == null) {
                    checkBackPressure(channel, false, qoS);
                    return;
                }
                var future = invokeHandlersForIncomingPublish(message);
                DonAsynchron.withCallback(future,
                        (_) -> {
                            processPubRec(channel, msgWrapper, PubRec.SUCCESS.byteValue());
                            checkBackPressure(channel, false, qoS);
                        },
                        (t) -> {
                            log.error("Error invoke future for client {} with QoS {}", client.getClientConfig().getClientId(), MqttQoS.EXACTLY_ONCE, t);
                            processPubRec(channel, msgWrapper, PubRec.UNSPECIFIED_ERROR.byteValue());
                            client.getQos2PendingMsgIds().remove(msgId);
                            checkBackPressure(channel, false, qoS);
                        }
                );
            }
        }
    }

    private void processPubAck(Channel channel, MqttMsgWrapper msgWrapper, byte reasonCode) {
        processPubAck(channel, msgWrapper, reasonCode, true);
    }

    private void processPubAck(Channel channel, MqttMsgWrapper msgWrapper, byte reasonCode, boolean sendAck) {
        this.mqttOrderedAcknowledgementCtxQoS1.ack(channel, msgWrapper, reasonCode, sendAck);
    }

    private void processPubRec(Channel channel, MqttMsgWrapper msgWrapper, byte reasonCode) {
        processPubRec(channel, msgWrapper, reasonCode, true);
    }

    private void processPubRec(Channel channel, MqttMsgWrapper msgWrapper, byte reasonCode, boolean sendAck) {
        this.mqttOrderedAcknowledgementCtxQoS2.ack(channel, msgWrapper, reasonCode, sendAck);
    }

    private void handleUnsuback(MqttUnsubAckMessage message) {
        MqttPendingUnsubscription unsubscription = this.client.getPendingServerUnsubscribes().get(message.variableHeader().messageId());
        if (unsubscription == null) {
            return;
        }
        unsubscription.onUnsubackReceived();
        this.client.getServerSubscriptions().remove(unsubscription.getTopic());
        unsubscription.getFuture().setSuccess(null);
        this.client.getPendingServerUnsubscribes().remove(message.variableHeader().messageId());
        if (this.client.getCallback() != null) {
            this.client.getCallback().onUnsubAck(message);
        }
    }

    private void handlePuback(MqttPubAckMessage message) {
        this.client.getPendingPublishes().computeIfPresent(message.variableHeader().messageId(), (__, pendingPublish) -> {
            pendingPublish.getFuture().setSuccess(null);
            pendingPublish.onPubackReceived();
            pendingPublish.getPayload().release();
            if (this.client.getCallback() != null) {
                this.client.getCallback().onPubAck(message);
            }
            return null;
        });
    }

    private void handlePubrec(Channel channel, MqttMessage message) {
        MqttPendingPublish pendingPublish = this.client.getPendingPublishes().get(((MqttMessageIdVariableHeader) message.variableHeader()).messageId());
        pendingPublish.onPubackReceived();

        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREL, false, MqttQoS.AT_LEAST_ONCE, false, 0);
        MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) message.variableHeader();
        MqttMessage pubrelMessage = new MqttMessage(fixedHeader, variableHeader);
        channel.writeAndFlush(pubrelMessage);

        pendingPublish.setPubrelMessage(pubrelMessage);
        pendingPublish.startPubrelRetransmissionTimer(this.client.getEventLoop().next(), this.client::sendAndFlushPacket);
    }

    private void processPubRec(Channel channel, int msgId, byte reasonCodeValue) {
        sendMqttReply(channel, MqttMessageType.PUBREC, msgId, reasonCodeValue);
    }

    private void handlePubrel(Channel channel, MqttMessage message) {
        log.trace("[{}][{}] Handling PUBREL: {}", client.getClientConfig().getOwnerId(), client.getClientConfig().getClientId(), message);
        final int msgId = ((MqttMessageIdVariableHeader) message.variableHeader()).messageId();
        byte reasonCode = this.client.getQos2PendingMsgIds().remove(msgId)
                ? PubComp.SUCCESS.byteValue()
                : PubComp.PACKET_IDENTIFIER_NOT_FOUND.byteValue();
        processPubComp(channel, msgId, reasonCode);
    }

    private void processPubComp(Channel channel, int msgId, byte reasonCodeValue) {
        sendMqttReply(channel, MqttMessageType.PUBCOMP, msgId, reasonCodeValue);
    }

    private void sendMqttReply(Channel channel, MqttMessageType type, int msgId, byte reasonCodeValue) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(type, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttMessage message = MqttVersion.MQTT_5.equals(client.getClientConfig().getProtocolVersion())
                ? new MqttMessage(fixedHeader, new MqttPubReplyMessageVariableHeader(msgId, reasonCodeValue, null))
                : new MqttMessage(fixedHeader, MqttMessageIdVariableHeader.from(msgId));
        channel.writeAndFlush(message);
    }

    private void handlePubcomp(MqttMessage message) {
        MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) message.variableHeader();
        MqttPendingPublish pendingPublish = this.client.getPendingPublishes().get(variableHeader.messageId());
        pendingPublish.getFuture().setSuccess(null);
        this.client.getPendingPublishes().remove(variableHeader.messageId());
        pendingPublish.getPayload().release();
        pendingPublish.onPubcompReceived();
    }

    private void handleDisconnect(MqttMessage message) {
        if (this.client.getCallback() != null) {
            this.client.getCallback().onDisconnect(message);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        try {
            if (cause instanceof IOException) {
                if (log.isDebugEnabled()) {
                    log.debug("[{}] IOException: ", client.getClientConfig().getOwnerId(), cause);
                } else {
                    log.info("[{}] IOException: {}", client.getClientConfig().getOwnerId(), cause.getMessage());
                }
            } else {
                log.warn("[{}] exceptionCaught", client.getClientConfig().getOwnerId(), cause);
            }
        } finally {
            ReferenceCountUtil.release(cause);
        }
    }

    private ListenableFuture<Void> adaptFuture(Future<Void> future) {
        if (future instanceof ListenableFuture<Void> lf) {
            return lf;
        }
        if (future instanceof CompletableFuture<Void> cf) {
            SettableFuture<Void> settable = SettableFuture.create();
            cf.whenComplete((result, error) -> {
                if (error != null) {
                    settable.setException(error);
                } else {
                    settable.set(result);
                }
            });
            return settable;
        }
        return JdkFutureAdapters.listenInPoolThread(future, client.getHandlerExecutor());
    }

    private void checkBackPressure(Channel channel, boolean increment, MqttQoS qoS) {
        if (!backPressureEnabled || MqttQoS.AT_MOST_ONCE.equals(qoS)) {
            return;
        }
        long count = increment ? publishMsgCount.incrementAndGet() : publishMsgCount.decrementAndGet();
        if (increment && count >= highWatermark && channel.config().isAutoRead()) {
            channel.config().setAutoRead(false);
            log.debug("Paused MQTT reads: queue {} >= {}", count, highWatermark);
        } else if (!increment && count < lowWatermark && !channel.config().isAutoRead()) {
            channel.config().setAutoRead(true);
            log.debug("Resumed MQTT reads: queue {} < {}", count, lowWatermark);
        }
    }

}
