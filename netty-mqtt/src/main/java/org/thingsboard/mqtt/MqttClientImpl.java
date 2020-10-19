/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.mqtt;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents an MqttClientImpl connected to a single MQTT server. Will try to keep the connection going at all times
 */
@SuppressWarnings({"WeakerAccess", "unused"})
final class MqttClientImpl implements MqttClient {

    private final Set<String> serverSubscriptions = new HashSet<>();
    private final IntObjectHashMap<MqttPendingUnsubscription> pendingServerUnsubscribes = new IntObjectHashMap<>();
    private final IntObjectHashMap<MqttIncomingQos2Publish> qos2PendingIncomingPublishes = new IntObjectHashMap<>();
    private final IntObjectHashMap<MqttPendingPublish> pendingPublishes = new IntObjectHashMap<>();
    private final HashMultimap<String, MqttSubscription> subscriptions = HashMultimap.create();
    private final IntObjectHashMap<MqttPendingSubscription> pendingSubscriptions = new IntObjectHashMap<>();
    private final Set<String> pendingSubscribeTopics = new HashSet<>();
    private final HashMultimap<MqttHandler, MqttSubscription> handlerToSubscribtion = HashMultimap.create();
    private final AtomicInteger nextMessageId = new AtomicInteger(1);

    private final MqttClientConfig clientConfig;

    private final MqttHandler defaultHandler;

    private EventLoopGroup eventLoop;

    private volatile Channel channel;

    private volatile boolean disconnected = false;
    private volatile boolean reconnect = false;
    private String host;
    private int port;
    private MqttClientCallback callback;


    /**
     * Construct the MqttClientImpl with default config
     */
    public MqttClientImpl(MqttHandler defaultHandler) {
        this.clientConfig = new MqttClientConfig();
        this.defaultHandler = defaultHandler;
    }

    /**
     * Construct the MqttClientImpl with additional config.
     * This config can also be changed using the {@link #getClientConfig()} function
     *
     * @param clientConfig The config object to use while looking for settings
     */
    public MqttClientImpl(MqttClientConfig clientConfig, MqttHandler defaultHandler) {
        this.clientConfig = clientConfig;
        this.defaultHandler = defaultHandler;
    }

    /**
     * Connect to the specified hostname/ip. By default uses port 1883.
     * If you want to change the port number, see {@link #connect(String, int)}
     *
     * @param host The ip address or host to connect to
     * @return A future which will be completed when the connection is opened and we received an CONNACK
     */
    @Override
    public Future<MqttConnectResult> connect(String host) {
        return connect(host, 1883);
    }

    /**
     * Connect to the specified hostname/ip using the specified port
     *
     * @param host The ip address or host to connect to
     * @param port The tcp port to connect to
     * @return A future which will be completed when the connection is opened and we received an CONNACK
     */
    @Override
    public Future<MqttConnectResult> connect(String host, int port) {
        return connect(host, port, false);
    }

    private Future<MqttConnectResult> connect(String host, int port, boolean reconnect) {
        if (this.eventLoop == null) {
            this.eventLoop = new NioEventLoopGroup();
        }
        this.host = host;
        this.port = port;
        Promise<MqttConnectResult> connectFuture = new DefaultPromise<>(this.eventLoop.next());
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(this.eventLoop);
        bootstrap.channel(clientConfig.getChannelClass());
        bootstrap.remoteAddress(host, port);
        bootstrap.handler(new MqttChannelInitializer(connectFuture, host, port, clientConfig.getSslContext()));
        ChannelFuture future = bootstrap.connect();

        future.addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                MqttClientImpl.this.channel = f.channel();
                MqttClientImpl.this.channel.closeFuture().addListener((ChannelFutureListener) channelFuture -> {
                    if (isConnected()) {
                        return;
                    }
                    ChannelClosedException e = new ChannelClosedException("Channel is closed!");
                    if (callback != null) {
                        callback.connectionLost(e);
                    }
                    pendingSubscriptions.clear();
                    serverSubscriptions.clear();
                    subscriptions.clear();
                    pendingServerUnsubscribes.clear();
                    qos2PendingIncomingPublishes.clear();
                    pendingPublishes.clear();
                    pendingSubscribeTopics.clear();
                    handlerToSubscribtion.clear();
                    scheduleConnectIfRequired(host, port, true);
                });
            } else {
                scheduleConnectIfRequired(host, port, reconnect);
            }
        });
        return connectFuture;
    }

    private void scheduleConnectIfRequired(String host, int port, boolean reconnect) {
        if (clientConfig.isReconnect() && !disconnected) {
            if (reconnect) {
                this.reconnect = true;
            }
            eventLoop.schedule((Runnable) () -> connect(host, port, reconnect), clientConfig.getReconnectDelay(), TimeUnit.SECONDS);
        }
    }

    @Override
    public boolean isConnected() {
        return !disconnected && channel != null && channel.isActive();
    }

    @Override
    public Future<MqttConnectResult> reconnect() {
        if (host == null) {
            throw new IllegalStateException("Cannot reconnect. Call connect() first");
        }
        return connect(host, port);
    }

    /**
     * Retrieve the netty {@link EventLoopGroup} we are using
     *
     * @return The netty {@link EventLoopGroup} we use for the connection
     */
    @Override
    public EventLoopGroup getEventLoop() {
        return eventLoop;
    }

    /**
     * By default we use the netty {@link NioEventLoopGroup}.
     * If you change the EventLoopGroup to another type, make sure to change the {@link Channel} class using {@link MqttClientConfig#setChannelClass(Class)}
     * If you want to force the MqttClient to use another {@link EventLoopGroup}, call this function before calling {@link #connect(String, int)}
     *
     * @param eventLoop The new eventloop to use
     */
    @Override
    public void setEventLoop(EventLoopGroup eventLoop) {
        this.eventLoop = eventLoop;
    }

    /**
     * Subscribe on the given topic. When a message is received, MqttClient will invoke the {@link MqttHandler#onMessage(String, ByteBuf)} function of the given handler
     *
     * @param topic   The topic filter to subscribe to
     * @param handler The handler to invoke when we receive a message
     * @return A future which will be completed when the server acknowledges our subscribe request
     */
    @Override
    public Future<Void> on(String topic, MqttHandler handler) {
        return on(topic, handler, MqttQoS.AT_MOST_ONCE);
    }

    /**
     * Subscribe on the given topic, with the given qos. When a message is received, MqttClient will invoke the {@link MqttHandler#onMessage(String, ByteBuf)} function of the given handler
     *
     * @param topic   The topic filter to subscribe to
     * @param handler The handler to invoke when we receive a message
     * @param qos     The qos to request to the server
     * @return A future which will be completed when the server acknowledges our subscribe request
     */
    @Override
    public Future<Void> on(String topic, MqttHandler handler, MqttQoS qos) {
        return createSubscription(topic, handler, false, qos);
    }

    /**
     * Subscribe on the given topic. When a message is received, MqttClient will invoke the {@link MqttHandler#onMessage(String, ByteBuf)} function of the given handler
     * This subscription is only once. If the MqttClient has received 1 message, the subscription will be removed
     *
     * @param topic   The topic filter to subscribe to
     * @param handler The handler to invoke when we receive a message
     * @return A future which will be completed when the server acknowledges our subscribe request
     */
    @Override
    public Future<Void> once(String topic, MqttHandler handler) {
        return once(topic, handler, MqttQoS.AT_MOST_ONCE);
    }

    /**
     * Subscribe on the given topic, with the given qos. When a message is received, MqttClient will invoke the {@link MqttHandler#onMessage(String, ByteBuf)} function of the given handler
     * This subscription is only once. If the MqttClient has received 1 message, the subscription will be removed
     *
     * @param topic   The topic filter to subscribe to
     * @param handler The handler to invoke when we receive a message
     * @param qos     The qos to request to the server
     * @return A future which will be completed when the server acknowledges our subscribe request
     */
    @Override
    public Future<Void> once(String topic, MqttHandler handler, MqttQoS qos) {
        return createSubscription(topic, handler, true, qos);
    }

    /**
     * Remove the subscription for the given topic and handler
     * If you want to unsubscribe from all handlers known for this topic, use {@link #off(String)}
     *
     * @param topic   The topic to unsubscribe for
     * @param handler The handler to unsubscribe
     * @return A future which will be completed when the server acknowledges our unsubscribe request
     */
    @Override
    public Future<Void> off(String topic, MqttHandler handler) {
        Promise<Void> future = new DefaultPromise<>(this.eventLoop.next());
        for (MqttSubscription subscription : this.handlerToSubscribtion.get(handler)) {
            this.subscriptions.remove(topic, subscription);
        }
        this.handlerToSubscribtion.removeAll(handler);
        this.checkSubscribtions(topic, future);
        return future;
    }

    /**
     * Remove all subscriptions for the given topic.
     * If you want to specify which handler to unsubscribe, use {@link #off(String, MqttHandler)}
     *
     * @param topic The topic to unsubscribe for
     * @return A future which will be completed when the server acknowledges our unsubscribe request
     */
    @Override
    public Future<Void> off(String topic) {
        Promise<Void> future = new DefaultPromise<>(this.eventLoop.next());
        ImmutableSet<MqttSubscription> subscriptions = ImmutableSet.copyOf(this.subscriptions.get(topic));
        for (MqttSubscription subscription : subscriptions) {
            for (MqttSubscription handSub : this.handlerToSubscribtion.get(subscription.getHandler())) {
                this.subscriptions.remove(topic, handSub);
            }
            this.handlerToSubscribtion.remove(subscription.getHandler(), subscription);
        }
        this.checkSubscribtions(topic, future);
        return future;
    }

    /**
     * Publish a message to the given payload
     *
     * @param topic   The topic to publish to
     * @param payload The payload to send
     * @return A future which will be completed when the message is sent out of the MqttClient
     */
    @Override
    public Future<Void> publish(String topic, ByteBuf payload) {
        return publish(topic, payload, MqttQoS.AT_MOST_ONCE, false);
    }

    /**
     * Publish a message to the given payload, using the given qos
     *
     * @param topic   The topic to publish to
     * @param payload The payload to send
     * @param qos     The qos to use while publishing
     * @return A future which will be completed when the message is delivered to the server
     */
    @Override
    public Future<Void> publish(String topic, ByteBuf payload, MqttQoS qos) {
        return publish(topic, payload, qos, false);
    }

    /**
     * Publish a message to the given payload, using optional retain
     *
     * @param topic   The topic to publish to
     * @param payload The payload to send
     * @param retain  true if you want to retain the message on the server, false otherwise
     * @return A future which will be completed when the message is sent out of the MqttClient
     */
    @Override
    public Future<Void> publish(String topic, ByteBuf payload, boolean retain) {
        return publish(topic, payload, MqttQoS.AT_MOST_ONCE, retain);
    }

    /**
     * Publish a message to the given payload, using the given qos and optional retain
     *
     * @param topic   The topic to publish to
     * @param payload The payload to send
     * @param qos     The qos to use while publishing
     * @param retain  true if you want to retain the message on the server, false otherwise
     * @return A future which will be completed when the message is delivered to the server
     */
    @Override
    public Future<Void> publish(String topic, ByteBuf payload, MqttQoS qos, boolean retain) {
        Promise<Void> future = new DefaultPromise<>(this.eventLoop.next());
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, qos, retain, 0);
        MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader(topic, getNewMessageId().messageId());
        MqttPublishMessage message = new MqttPublishMessage(fixedHeader, variableHeader, payload);
        MqttPendingPublish pendingPublish = new MqttPendingPublish(variableHeader.packetId(), future, payload.retain(), message, qos);

        ChannelFuture channelFuture = this.sendAndFlushPacket(message);

        if (channelFuture != null) {
            pendingPublish.setSent(true);
            if (channelFuture.cause() != null) {
                future.setFailure(channelFuture.cause());
                return future;
            }
        }
        if (pendingPublish.isSent() && pendingPublish.getQos() == MqttQoS.AT_MOST_ONCE) {
            pendingPublish.getFuture().setSuccess(null); //We don't get an ACK for QOS 0
        } else if (pendingPublish.isSent()) {
            this.pendingPublishes.put(pendingPublish.getMessageId(), pendingPublish);
            pendingPublish.startPublishRetransmissionTimer(this.eventLoop.next(), this::sendAndFlushPacket);
        }
        return future;
    }

    /**
     * Retrieve the MqttClient configuration
     *
     * @return The {@link MqttClientConfig} instance we use
     */
    @Override
    public MqttClientConfig getClientConfig() {
        return clientConfig;
    }

    @Override
    public void disconnect() {
        disconnected = true;
        if (this.channel != null) {
            MqttMessage message = new MqttMessage(new MqttFixedHeader(MqttMessageType.DISCONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0));
            this.sendAndFlushPacket(message).addListener(future1 -> channel.close());
        }
    }

    @Override
    public void setCallback(MqttClientCallback callback) {
        this.callback = callback;
    }


    ///////////////////////////////////////////// PRIVATE API /////////////////////////////////////////////

    public boolean isReconnect() {
        return reconnect;
    }

    public void onSuccessfulReconnect() {
        if (callback != null) {
            callback.onSuccessfulReconnect();
        }
    }


    ChannelFuture sendAndFlushPacket(Object message) {
        if (this.channel == null) {
            return null;
        }
        if (this.channel.isActive()) {
            return this.channel.writeAndFlush(message);
        }
        return this.channel.newFailedFuture(new ChannelClosedException("Channel is closed!"));
    }

    private MqttMessageIdVariableHeader getNewMessageId() {
        int messageId;
        synchronized (this.nextMessageId) {
            this.nextMessageId.compareAndSet(0xffff, 1);
            messageId = this.nextMessageId.getAndIncrement();
        }
        return MqttMessageIdVariableHeader.from(messageId);
    }

    private Future<Void> createSubscription(String topic, MqttHandler handler, boolean once, MqttQoS qos) {
        if (this.pendingSubscribeTopics.contains(topic)) {
            Optional<Map.Entry<Integer, MqttPendingSubscription>> subscriptionEntry = this.pendingSubscriptions.entrySet().stream().filter((e) -> e.getValue().getTopic().equals(topic)).findAny();
            if (subscriptionEntry.isPresent()) {
                subscriptionEntry.get().getValue().addHandler(handler, once);
                return subscriptionEntry.get().getValue().getFuture();
            }
        }
        if (this.serverSubscriptions.contains(topic)) {
            MqttSubscription subscription = new MqttSubscription(topic, handler, once);
            this.subscriptions.put(topic, subscription);
            this.handlerToSubscribtion.put(handler, subscription);
            return this.channel.newSucceededFuture();
        }

        Promise<Void> future = new DefaultPromise<>(this.eventLoop.next());
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.SUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, false, 0);
        MqttTopicSubscription subscription = new MqttTopicSubscription(topic, qos);
        MqttMessageIdVariableHeader variableHeader = getNewMessageId();
        MqttSubscribePayload payload = new MqttSubscribePayload(Collections.singletonList(subscription));
        MqttSubscribeMessage message = new MqttSubscribeMessage(fixedHeader, variableHeader, payload);

        final MqttPendingSubscription pendingSubscription = new MqttPendingSubscription(future, topic, message);
        pendingSubscription.addHandler(handler, once);
        this.pendingSubscriptions.put(variableHeader.messageId(), pendingSubscription);
        this.pendingSubscribeTopics.add(topic);
        pendingSubscription.setSent(this.sendAndFlushPacket(message) != null); //If not sent, we will send it when the connection is opened

        pendingSubscription.startRetransmitTimer(this.eventLoop.next(), this::sendAndFlushPacket);

        return future;
    }

    private void checkSubscribtions(String topic, Promise<Void> promise) {
        if (!(this.subscriptions.containsKey(topic) && this.subscriptions.get(topic).size() != 0) && this.serverSubscriptions.contains(topic)) {
            MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.UNSUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, false, 0);
            MqttMessageIdVariableHeader variableHeader = getNewMessageId();
            MqttUnsubscribePayload payload = new MqttUnsubscribePayload(Collections.singletonList(topic));
            MqttUnsubscribeMessage message = new MqttUnsubscribeMessage(fixedHeader, variableHeader, payload);

            MqttPendingUnsubscription pendingUnsubscription = new MqttPendingUnsubscription(promise, topic, message);
            this.pendingServerUnsubscribes.put(variableHeader.messageId(), pendingUnsubscription);
            pendingUnsubscription.startRetransmissionTimer(this.eventLoop.next(), this::sendAndFlushPacket);

            this.sendAndFlushPacket(message);
        } else {
            promise.setSuccess(null);
        }
    }

    IntObjectHashMap<MqttPendingSubscription> getPendingSubscriptions() {
        return pendingSubscriptions;
    }

    HashMultimap<String, MqttSubscription> getSubscriptions() {
        return subscriptions;
    }

    Set<String> getPendingSubscribeTopics() {
        return pendingSubscribeTopics;
    }

    HashMultimap<MqttHandler, MqttSubscription> getHandlerToSubscribtion() {
        return handlerToSubscribtion;
    }

    Set<String> getServerSubscriptions() {
        return serverSubscriptions;
    }

    IntObjectHashMap<MqttPendingUnsubscription> getPendingServerUnsubscribes() {
        return pendingServerUnsubscribes;
    }

    IntObjectHashMap<MqttPendingPublish> getPendingPublishes() {
        return pendingPublishes;
    }

    IntObjectHashMap<MqttIncomingQos2Publish> getQos2PendingIncomingPublishes() {
        return qos2PendingIncomingPublishes;
    }

    private class MqttChannelInitializer extends ChannelInitializer<SocketChannel> {

        private final Promise<MqttConnectResult> connectFuture;
        private final String host;
        private final int port;
        private final SslContext sslContext;


        public MqttChannelInitializer(Promise<MqttConnectResult> connectFuture, String host, int port, SslContext sslContext) {
            this.connectFuture = connectFuture;
            this.host = host;
            this.port = port;
            this.sslContext = sslContext;
        }

        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            if (sslContext != null) {
                ch.pipeline().addLast(sslContext.newHandler(ch.alloc(), host, port));
            }

            ch.pipeline().addLast("mqttDecoder", new MqttDecoder(clientConfig.getMaxBytesInMessage()));
            ch.pipeline().addLast("mqttEncoder", MqttEncoder.INSTANCE);
            ch.pipeline().addLast("idleStateHandler", new IdleStateHandler(MqttClientImpl.this.clientConfig.getTimeoutSeconds(), MqttClientImpl.this.clientConfig.getTimeoutSeconds(), 0));
            ch.pipeline().addLast("mqttPingHandler", new MqttPingHandler(MqttClientImpl.this.clientConfig.getTimeoutSeconds()));
            ch.pipeline().addLast("mqttHandler", new MqttChannelHandler(MqttClientImpl.this, connectFuture));
        }
    }

    MqttHandler getDefaultHandler() {
        return defaultHandler;
    }

}
