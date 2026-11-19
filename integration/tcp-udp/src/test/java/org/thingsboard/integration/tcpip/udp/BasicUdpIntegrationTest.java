// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.tcpip.udp;

import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.MaxMessagesRecvByteBufAllocator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class BasicUdpIntegrationTest {

    private BasicUdpIntegration integration;
    private NioEventLoopGroup workerGroup;
    private Channel serverChannel;

    @BeforeEach
    void setUp() {
        integration = new BasicUdpIntegration();
        workerGroup = new NioEventLoopGroup(1);
        ReflectionTestUtils.setField(integration, "workerGroup", workerGroup);
        ReflectionTestUtils.setField(integration, "udpConfigurationParameters", JacksonUtil.fromString(
                "{\"port\": 0, \"soRcvBuf\": 128, \"handlerConfiguration\": {\"handlerType\": \"BINARY\"}}",
                UdpConfigurationParameters.class));
    }

    @AfterEach
    void tearDown() throws Exception {
        if (serverChannel != null) {
            serverChannel.close().sync();
        }
        workerGroup.shutdownGracefully(0, 1, TimeUnit.SECONDS).sync();
    }

    @ParameterizedTest
    @ValueSource(ints = {100, 3091, 65000})
    void testWholeDatagramIsRead(int size) throws Exception {
        bind();
        CompletableFuture<byte[]> received = new CompletableFuture<>();
        ChannelInboundHandlerAdapter probe = new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                received.complete(ByteBufUtil.getBytes(((DatagramPacket) msg).content()));
                ReferenceCountUtil.release(msg);
            }
        };
        // Added on the event loop so the probe is active before the first datagram is read
        serverChannel.eventLoop().submit(() -> serverChannel.pipeline().addFirst(probe)).sync();

        byte[] datagram = new byte[size];
        ThreadLocalRandom.current().nextBytes(datagram);
        int port = ((InetSocketAddress) serverChannel.localAddress()).getPort();
        try (DatagramSocket client = new DatagramSocket()) {
            client.send(new java.net.DatagramPacket(datagram, size, InetAddress.getLoopbackAddress(), port));
        }

        assertThat(received.get(5, TimeUnit.SECONDS)).isEqualTo(datagram);
    }

    @Test
    void testMaxMessagesPerReadIsSixteen() throws Exception {
        bind();

        MaxMessagesRecvByteBufAllocator allocator = serverChannel.config().getRecvByteBufAllocator();
        assertThat(allocator.maxMessagesPerRead()).isEqualTo(16);
    }

    private void bind() throws Exception {
        integration.bind();
        serverChannel = (Channel) ReflectionTestUtils.getField(integration, "serverChannel");
    }

}
