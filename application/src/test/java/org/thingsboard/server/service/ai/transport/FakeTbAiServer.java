// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.thingsboard.ai.common.channel.ChannelError;
import org.thingsboard.ai.common.channel.ChannelFrame;
import org.thingsboard.ai.common.channel.ChannelFrameCodec;
import org.thingsboard.ai.common.channel.ChannelHello;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.ai.common.channel.OperationResult;
import org.thingsboard.ai.common.channel.TbAiChannelClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A scripted TB AI channel endpoint. It answers {@code hello} with the given capabilities and hands every other frame
 * to the script, which replies through {@link Connection}: tagged with the frame's op on a multiplexed connection, as
 * TB AI does.
 */
class FakeTbAiServer implements AutoCloseable {

    @FunctionalInterface
    interface Script {
        void onFrame(Connection connection, ChannelFrame frame);
    }

    final ChannelFrameCodec codec = new ChannelFrameCodec(new ObjectMapper());
    final List<ChannelFrame> received = new CopyOnWriteArrayList<>();
    final AtomicInteger connections = new AtomicInteger();
    final AtomicInteger openConnections = new AtomicInteger();
    private final DisposableServer server;

    private FakeTbAiServer(List<String> capabilities, Script script) {
        server = HttpServer.create().host("127.0.0.1").port(0)
                .route(routes -> routes.ws(ChannelProtocol.WS_PATH, (inbound, outbound) -> {
                    connections.incrementAndGet();
                    openConnections.incrementAndGet();
                    var connection = new Connection(capabilities);
                    inbound.receive().asString().subscribe(text -> {
                        ChannelFrame frame = codec.decode(text);
                        received.add(frame);
                        if (ChannelProtocol.HELLO.equals(frame.type())) {
                            ChannelHello hello = codec.fromPayload(frame.payload(), ChannelHello.class);
                            connection.multiplexed = capabilities.contains(ChannelProtocol.CAPABILITY_MUX)
                                    && hello.capabilities().contains(ChannelProtocol.CAPABILITY_MUX);
                            connection.send(ChannelFrame.of(ChannelProtocol.HELLO, codec.toPayload(
                                    new ChannelHello(1, capabilities, null, (long) TbAiChannelClient.DEFAULT_MAX_FRAME_BYTES))));
                        } else {
                            script.onFrame(connection, frame);
                        }
                    });
                    return outbound.sendString(connection.out.asFlux())
                            .then()
                            .then(Mono.defer(() -> outbound.sendClose(connection.closeCode, connection.closeReason)))
                            .doFinally(signal -> openConnections.decrementAndGet());
                }))
                .bindNow();
    }

    static FakeTbAiServer start(List<String> capabilities, Script script) {
        return new FakeTbAiServer(capabilities, script);
    }

    static FakeTbAiServer rejecting(int status) {
        return new FakeTbAiServer(status);
    }

    private FakeTbAiServer(int status) {
        server = HttpServer.create().host("127.0.0.1").port(0)
                .route(routes -> routes.get(ChannelProtocol.WS_PATH, (req, res) -> res.status(status).send()))
                .bindNow();
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.port();
    }

    TbAiChannelClient client() {
        return new TbAiChannelClient(baseUrl());
    }

    List<ChannelFrame> received(String type) {
        return received.stream().filter(frame -> type.equals(frame.type())).toList();
    }

    @Override
    public void close() {
        server.disposeNow(Duration.ofSeconds(5));
    }

    final class Connection {

        private final Sinks.Many<String> out = Sinks.many().unicast().onBackpressureBuffer();
        private final List<String> capabilities;
        private volatile boolean multiplexed;
        private volatile int closeCode = ChannelProtocol.CLOSE_NORMAL;
        private volatile String closeReason = "Done";

        private Connection(List<String> capabilities) {
            this.capabilities = capabilities;
        }

        boolean isMultiplexed() {
            return multiplexed;
        }

        // A unicast sink drops concurrent emissions, so frames sent from several threads are serialized here.
        synchronized void send(ChannelFrame frame) {
            out.tryEmitNext(codec.encode(frame));
        }

        /**
         * A frame that belongs to the operation started by {@code start}.
         */
        void sendFor(ChannelFrame start, String type, Object payload) {
            send(new ChannelFrame(type, null, null, codec.toPayload(payload), multiplexed ? start.id() : null));
        }

        void request(ChannelFrame start, String type, String id, Object payload) {
            send(new ChannelFrame(type, id, null, codec.toPayload(payload), multiplexed ? start.id() : null));
        }

        void result(ChannelFrame start, OperationResult result) {
            send(new ChannelFrame(ChannelProtocol.OPERATION_RESULT, null, start.id(), codec.toPayload(result), multiplexed ? start.id() : null));
            if (!multiplexed) {
                close(ChannelProtocol.CLOSE_NORMAL, "Operation completed");
            }
        }

        void refuse(ChannelFrame start, String code) {
            send(new ChannelFrame(ChannelProtocol.ERROR, null, start.id(), codec.toPayload(new ChannelError(code, code)),
                    multiplexed ? start.id() : null));
        }

        synchronized void close(int code, String reason) {
            closeCode = code;
            closeReason = reason;
            out.tryEmitComplete();
        }

    }

}
