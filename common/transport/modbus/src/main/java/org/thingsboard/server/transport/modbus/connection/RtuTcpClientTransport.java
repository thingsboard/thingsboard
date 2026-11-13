// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus.connection;

import com.digitalpetri.modbus.ModbusRtuFrame;
import com.digitalpetri.modbus.ModbusRtuResponseFrameParser;
import com.digitalpetri.modbus.ModbusRtuResponseFrameParser.Accumulated;
import com.digitalpetri.modbus.ModbusRtuResponseFrameParser.ParseError;
import com.digitalpetri.modbus.client.ModbusRtuClientTransport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.Executor;

/**
 * RTU frames tunnelled over TCP (a serial gateway in transparent mode) for {@link com.digitalpetri.modbus.client.ModbusRtuClient}.
 * A frame is unit id + PDU + CRC with no length field, so responses are assembled by digitalpetri's
 * {@link ModbusRtuResponseFrameParser}, which knows each function code's response length.
 */
@Slf4j
class RtuTcpClientTransport extends TcpClientTransport<ModbusRtuFrame> implements ModbusRtuClientTransport {

    private final ModbusRtuResponseFrameParser frameParser = new ModbusRtuResponseFrameParser();

    RtuTcpClientTransport(String host, int port, Duration connectTimeout, Duration requestTimeout,
                          EventLoopGroup eventLoopGroup, Executor executor) {
        this(host, port, connectTimeout, requestTimeout, eventLoopGroup, executor, INITIAL_RECONNECT_DELAY, MAX_RECONNECT_DELAY);
    }

    RtuTcpClientTransport(String host, int port, Duration connectTimeout, Duration requestTimeout,
                          EventLoopGroup eventLoopGroup, Executor executor,
                          Duration initialReconnectDelay, Duration maxReconnectDelay) {
        super(host, port, connectTimeout, requestTimeout, eventLoopGroup, executor, initialReconnectDelay, maxReconnectDelay);
    }

    /** Called by the client after a timeout, so a late or partial response does not corrupt the next one. */
    @Override
    public void resetFrameParser() {
        frameParser.reset();
    }

    @Override
    protected void onConnected() {
        frameParser.reset(); // bytes of a response cut off by the previous connection must not prefix the next one
    }

    @Override
    protected void initPipeline(ChannelPipeline pipeline) {
        pipeline.addLast(new SimpleChannelInboundHandler<ByteBuf>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buffer) {
                byte[] data = new byte[buffer.readableBytes()];
                buffer.readBytes(data);
                var state = frameParser.parse(data);
                if (state instanceof Accumulated accumulated) {
                    frameParser.reset();
                    deliver(accumulated.frame());
                } else if (state instanceof ParseError error) {
                    log.debug("[{}] Invalid RTU response, reconnecting: {}", address(), error.error());
                    frameParser.reset();
                    ctx.close();
                }
            }
        });
    }

    @Override
    protected Object toWire(ModbusRtuFrame frame, ByteBufAllocator alloc) {
        ByteBuf buffer = alloc.buffer(1 + frame.pdu().remaining() + frame.crc().remaining());
        buffer.writeByte(frame.unitId());
        buffer.writeBytes(frame.pdu());
        buffer.writeBytes(frame.crc());
        return buffer;
    }

}
