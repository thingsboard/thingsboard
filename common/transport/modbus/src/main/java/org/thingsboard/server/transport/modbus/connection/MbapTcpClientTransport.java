// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus.connection;

import com.digitalpetri.modbus.ModbusTcpFrame;
import com.digitalpetri.modbus.client.ModbusTcpClientTransport;
import com.digitalpetri.modbus.tcp.ModbusTcpCodec;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;

import java.time.Duration;
import java.util.concurrent.Executor;

/**
 * Modbus TCP (MBAP framing) client transport for {@link com.digitalpetri.modbus.client.ModbusTcpClient}.
 * Frames are encoded and decoded by digitalpetri's {@link ModbusTcpCodec}; requests are matched to responses by the client.
 */
class MbapTcpClientTransport extends TcpClientTransport<ModbusTcpFrame> implements ModbusTcpClientTransport {

    MbapTcpClientTransport(String host, int port, Duration connectTimeout, Duration requestTimeout,
                           EventLoopGroup eventLoopGroup, Executor executor) {
        this(host, port, connectTimeout, requestTimeout, eventLoopGroup, executor, INITIAL_RECONNECT_DELAY, MAX_RECONNECT_DELAY);
    }

    MbapTcpClientTransport(String host, int port, Duration connectTimeout, Duration requestTimeout,
                           EventLoopGroup eventLoopGroup, Executor executor,
                           Duration initialReconnectDelay, Duration maxReconnectDelay) {
        super(host, port, connectTimeout, requestTimeout, eventLoopGroup, executor, initialReconnectDelay, maxReconnectDelay);
    }

    @Override
    protected void initPipeline(ChannelPipeline pipeline) {
        pipeline.addLast(new ModbusTcpCodec());
        pipeline.addLast(new SimpleChannelInboundHandler<ModbusTcpFrame>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, ModbusTcpFrame frame) {
                deliver(frame);
            }
        });
    }

    @Override
    protected Object toWire(ModbusTcpFrame frame, ByteBufAllocator alloc) {
        return frame; // encoded by ModbusTcpCodec
    }

}
