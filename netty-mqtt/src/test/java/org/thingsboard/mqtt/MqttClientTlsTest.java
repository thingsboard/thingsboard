// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.mqtt;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.AbstractListeningExecutor;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class MqttClientTlsTest {

    private EventLoopGroup serverGroup;
    private Channel serverChannel;
    private CompletableFuture<Future<Channel>> serverHandshake;
    private AbstractListeningExecutor handlerExecutor;
    private MqttClient client;

    @BeforeEach
    void setup() throws Exception {
        SslContext serverSslContext;
        try (InputStream cert = resource("mismatched-host-cert.pem"); InputStream key = resource("mismatched-host-key.pem")) {
            serverSslContext = SslContextBuilder.forServer(cert, key).build();
        }
        serverHandshake = new CompletableFuture<>();
        serverGroup = new NioEventLoopGroup(1);
        serverChannel = new ServerBootstrap()
                .group(serverGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        SslHandler sslHandler = serverSslContext.newHandler(ch.alloc());
                        serverHandshake.complete(sslHandler.handshakeFuture());
                        ch.pipeline().addLast(sslHandler, new MqttDecoder(), MqttEncoder.INSTANCE, new ConnAckHandler());
                    }
                })
                .bind("localhost", 0).sync().channel();

        handlerExecutor = new AbstractListeningExecutor() {
            @Override
            protected int getThreadPollSize() {
                return 1;
            }
        };
        handlerExecutor.init();
    }

    @AfterEach
    void cleanup() {
        if (client != null) {
            client.disconnect();
        }
        handlerExecutor.destroy();
        serverChannel.close().syncUninterruptibly();
        serverGroup.shutdownGracefully(0, 1, TimeUnit.SECONDS).syncUninterruptibly();
    }

    @Test
    void connectsOverTlsWhenCertificateNameDoesNotMatchHostAndEndpointIdentificationIsOff() throws Exception {
        SslContext clientSslContext;
        try (InputStream cert = resource("mismatched-host-cert.pem")) {
            clientSslContext = SslContextBuilder.forClient().endpointIdentificationAlgorithm(null).trustManager(cert).build();
        }

        Promise<MqttConnectResult> connectFuture = connect(clientSslContext);

        Awaitility.await("waiting for client to connect")
                .atMost(Duration.ofSeconds(10))
                .until(connectFuture::isDone);
        assertThat(connectFuture.isSuccess()).isTrue();
        assertThat(connectFuture.getNow().getReturnCode()).isEqualTo(MqttConnectReturnCode.CONNECTION_ACCEPTED);
        assertThat(client.isConnected()).isTrue();
    }

    @Test
    void rejectsCertificateNameMismatchWithNettyDefaultEndpointIdentification() throws Exception {
        SslContext clientSslContext;
        try (InputStream cert = resource("mismatched-host-cert.pem")) {
            clientSslContext = SslContextBuilder.forClient().trustManager(cert).build();
        }

        connect(clientSslContext);

        Future<Channel> handshake = serverHandshake.get(10, TimeUnit.SECONDS);
        Awaitility.await("waiting for server handshake to finish")
                .atMost(Duration.ofSeconds(10))
                .until(handshake::isDone);
        assertThat(handshake.isSuccess()).isFalse();
        assertThat(client.isConnected()).isFalse();
    }

    private Promise<MqttConnectResult> connect(SslContext clientSslContext) {
        MqttClientConfig config = new MqttClientConfig(clientSslContext);
        config.setOwnerId("Test[Tls]");
        config.setClientId("tls");
        config.setReconnect(false);
        client = MqttClient.create(config, null, handlerExecutor);
        int port = ((InetSocketAddress) serverChannel.localAddress()).getPort();
        return client.connect("localhost", port);
    }

    private static InputStream resource(String name) {
        return MqttClientTlsTest.class.getClassLoader().getResourceAsStream(name);
    }

    private static class ConnAckHandler extends SimpleChannelInboundHandler<MqttMessage> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) {
            if (msg.fixedHeader().messageType() == MqttMessageType.CONNECT) {
                MqttConnAckMessage connAck = MqttMessageBuilders.connAck()
                        .returnCode(MqttConnectReturnCode.CONNECTION_ACCEPTED)
                        .sessionPresent(false)
                        .build();
                ctx.writeAndFlush(connAck);
            }
        }

    }

}
