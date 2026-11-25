// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus.connection;

import com.digitalpetri.modbus.MbapHeader;
import com.digitalpetri.modbus.ModbusTcpFrame;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Connection lifecycle of {@link TcpClientTransport} against plain loopback sockets, with a short reconnect backoff.
 */
class TcpClientTransportTest {

    private static final Duration WAIT = Duration.ofSeconds(5);

    private EventLoopGroup eventLoopGroup;
    private ExecutorService executor;
    private int port;
    private TestServer server;
    private MbapTcpClientTransport transport;

    @BeforeEach
    void setUp() throws IOException {
        eventLoopGroup = new NioEventLoopGroup(1);
        executor = Executors.newSingleThreadExecutor();
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        transport = new MbapTcpClientTransport("127.0.0.1", port, Duration.ofSeconds(1), Duration.ofSeconds(1),
                eventLoopGroup, executor, Duration.ofMillis(50), Duration.ofMillis(200));
    }

    @AfterEach
    void tearDown() throws Exception {
        transport.disconnect().get();
        if (server != null) {
            server.close();
        }
        eventLoopGroup.shutdownGracefully(0, 1, TimeUnit.SECONDS).sync();
        executor.shutdownNow();
    }

    @Test
    void keepsRetryingUntilTheServerComesUp() throws IOException {
        CompletableFuture<Void> firstAttempt = transport.connect();

        Awaitility.await().atMost(WAIT).until(firstAttempt::isDone);
        assertThat(firstAttempt).isCompletedExceptionally();
        assertThat(transport.isConnected()).isFalse();

        server = new TestServer(port);
        Awaitility.await().atMost(WAIT).until(transport::isConnected);
    }

    @Test
    void connectAgainReflectsTheCurrentStateNotTheFirstAttempt() throws IOException {
        CompletableFuture<Void> firstAttempt = transport.connect();
        Awaitility.await().atMost(WAIT).until(firstAttempt::isDone);
        assertThat(firstAttempt).isCompletedExceptionally();

        // still nobody listening: fails at once between attempts, or as soon as the attempt in progress is refused
        assertThat(transport.connect()).failsWithin(WAIT);

        server = new TestServer(port);
        Awaitility.await().atMost(WAIT).until(transport::isConnected);
        assertThat(transport.connect()).isCompletedWithValue(null);
    }

    @Test
    void reconnectsAfterTheServerDropsTheConnection() throws Exception {
        server = new TestServer(port);
        transport.connect().get();
        Awaitility.await().atMost(WAIT).until(() -> server.accepted.size() == 1);

        server.accepted.get(0).close();

        Awaitility.await().atMost(WAIT).until(() -> server.accepted.size() == 2 && transport.isConnected());
    }

    @Test
    void sendBetweenAttemptsFailsAtOnce() {
        CompletableFuture<Void> firstAttempt = transport.connect();
        Awaitility.await().atMost(WAIT).until(firstAttempt::isDone);

        CompletableFuture<Void> sent = transport.send(frame()).toCompletableFuture();

        assertThat(sent).isCompletedExceptionally();
        assertThat(sent).failsWithin(Duration.ZERO).withThrowableOfType(ExecutionException.class)
                .withMessageContaining("Not connected to 127.0.0.1:" + port);
    }

    @Test
    void sendDuringTheFirstAttemptWaitsForIt() throws Exception {
        server = new TestServer(port);

        transport.connect();
        CompletableFuture<Void> sent = transport.send(frame()).toCompletableFuture();

        sent.get(WAIT.toSeconds(), TimeUnit.SECONDS);
        Awaitility.await().atMost(WAIT).until(() -> server.accepted.size() == 1);
        byte[] header = new byte[7];
        InputStream in = server.accepted.get(0).getInputStream();
        assertThat(in.readNBytes(header, 0, 7)).isEqualTo(7);
        assertThat(header[6]).isEqualTo((byte) 1); // unit id
    }

    @Test
    void sendDuringAnAttemptThatDoesNotCompleteFailsAsNotConnectedNotAsTimeout() throws Exception {
        // 192.0.2.1 (TEST-NET-1) is never assigned: the connect attempt hangs until the connect timeout. On a network
        // that rejects it at once the request fails between attempts instead, with the same kind of error.
        var unanswered = new MbapTcpClientTransport("192.0.2.1", 502, Duration.ofSeconds(3), Duration.ofMillis(200),
                eventLoopGroup, executor, Duration.ofMillis(50), Duration.ofMillis(200));
        try {
            unanswered.connect();

            CompletableFuture<Void> sent = unanswered.send(frame()).toCompletableFuture();

            assertThat(sent).failsWithin(Duration.ofSeconds(2)).withThrowableOfType(ExecutionException.class)
                    .withCauseInstanceOf(IllegalStateException.class)
                    .withMessageContaining("Not connected to 192.0.2.1:502");
        } finally {
            unanswered.disconnect().get();
        }
    }

    @Test
    void disconnectStopsReconnecting() throws Exception {
        CompletableFuture<Void> firstAttempt = transport.connect();
        Awaitility.await().atMost(WAIT).until(firstAttempt::isDone);

        transport.disconnect().get();
        server = new TestServer(port);

        Thread.sleep(500); // several backoff periods
        assertThat(server.accepted).isEmpty();
        assertThat(transport.isConnected()).isFalse();
    }

    /** Read Holding Registers, unit 1, address 0, count 1. */
    private static ModbusTcpFrame frame() {
        ByteBuffer pdu = ByteBuffer.wrap(new byte[]{0x03, 0x00, 0x00, 0x00, 0x01});
        return new ModbusTcpFrame(new MbapHeader(1, 0, 1 + pdu.remaining(), 1), pdu);
    }

    /** Accepts connections and keeps them, so a test can read from or close them. */
    private static class TestServer implements AutoCloseable {

        final List<Socket> accepted = new CopyOnWriteArrayList<>();
        private final ServerSocket serverSocket;

        TestServer(int port) throws IOException {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress("127.0.0.1", port));
            Thread acceptor = new Thread(() -> {
                try {
                    while (!serverSocket.isClosed()) {
                        accepted.add(serverSocket.accept());
                    }
                } catch (SocketException e) {
                    // closed
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, "test-server-acceptor");
            acceptor.setDaemon(true);
            acceptor.start();
        }

        @Override
        public void close() throws IOException {
            serverSocket.close();
            for (Socket socket : accepted) {
                socket.close();
            }
        }

    }

}
