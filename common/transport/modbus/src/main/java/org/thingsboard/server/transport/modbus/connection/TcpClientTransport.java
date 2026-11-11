// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus.connection;

import com.google.common.util.concurrent.MoreExecutors;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * One TCP connection to a Modbus device or gateway that keeps reconnecting until {@link #disconnect()}.
 * Used in place of digitalpetri's Netty client transports, which depend on a {@code netty-channel-fsm} version that
 * conflicts with the one Eclipse Milo (OPC-UA integration) needs on the same classpath.
 * <p>
 * Reconnect: after a failed attempt or a lost connection the next attempt follows after 1 s, doubling up to 32 s,
 * back to 1 s once connected (the same schedule as the library).
 * <p>
 * Sending: a frame goes out at once when connected; during a connect attempt it waits for that attempt, at most the
 * request timeout; between attempts (device unreachable) it fails at once. A request is therefore never written after
 * the client has already given up on it, and no timeout is spent waiting for a device that is known to be down.
 *
 * @param <F> frame type exchanged with the digitalpetri client
 */
@Slf4j
abstract class TcpClientTransport<F> {

    static final Duration INITIAL_RECONNECT_DELAY = Duration.ofSeconds(1);
    static final Duration MAX_RECONNECT_DELAY = Duration.ofSeconds(32);

    private final String host;
    private final int port;
    private final Duration connectTimeout;
    private final Duration requestTimeout;
    private final EventLoopGroup eventLoopGroup;
    /** Hands received frames to the client off the event loop, one at a time and in arrival order. */
    private final Executor receiveExecutor;
    private final long initialReconnectDelayMs;
    private final long maxReconnectDelayMs;
    private final AtomicReference<Consumer<F>> frameReceiver = new AtomicReference<>();

    /** Guards the connection state below, which connect, disconnect, send and the Netty callbacks all read and change. */
    private final Object connectionStateLock = new Object();
    // guarded by connectionStateLock
    private boolean stopped = true;
    private Channel channel;
    private CompletableFuture<Channel> connecting;
    private ScheduledFuture<?> scheduledReconnect;
    private long nextReconnectDelayMs;

    TcpClientTransport(String host, int port, Duration connectTimeout, Duration requestTimeout,
                       EventLoopGroup eventLoopGroup, Executor executor,
                       Duration initialReconnectDelay, Duration maxReconnectDelay) {
        this.host = host;
        this.port = port;
        this.connectTimeout = connectTimeout;
        this.requestTimeout = requestTimeout;
        this.eventLoopGroup = eventLoopGroup;
        this.receiveExecutor = MoreExecutors.newSequentialExecutor(executor);
        this.initialReconnectDelayMs = initialReconnectDelay.toMillis();
        this.maxReconnectDelayMs = maxReconnectDelay.toMillis();
    }

    /** Adds the frame codec and the handler that passes complete frames to {@link #deliver}. */
    protected abstract void initPipeline(ChannelPipeline pipeline);

    /**
     * What to write to the channel for a frame. A buffer must come from {@code alloc} (the channel's pooled allocator);
     * Netty releases it once written, or when the write fails.
     */
    protected abstract Object toWire(F frame, ByteBufAllocator alloc);

    /** Called for every new connection, before any frame is sent on it. */
    protected void onConnected() {
    }

    /**
     * Starts connecting and keeps reconnecting until {@link #disconnect()}.
     *
     * @return completes when the first attempt succeeds, fails when it fails; attempts continue either way.
     * When already running: completed if connected, the result of the attempt in progress if there is one,
     * otherwise failed (waiting for the next attempt, which happens anyway)
     */
    public CompletableFuture<Void> connect() {
        synchronized (connectionStateLock) {
            if (!stopped) {
                if (channel != null && channel.isActive()) {
                    return CompletableFuture.completedFuture(null);
                }
                if (connecting != null) {
                    return connecting.thenAccept(_ -> {});
                }
                return CompletableFuture.failedFuture(new IllegalStateException("Not connected to " + address() + ", reconnecting"));
            }
            stopped = false;
            nextReconnectDelayMs = initialReconnectDelayMs;
        }
        CompletableFuture<Channel> firstAttempt = attempt();
        return firstAttempt != null
                ? firstAttempt.thenAccept(_ -> {})
                : CompletableFuture.failedFuture(new IllegalStateException("Disconnected from " + address()));
    }

    public CompletableFuture<Void> disconnect() {
        Channel current;
        ScheduledFuture<?> pendingReconnect;
        CompletableFuture<Channel> pendingAttempt;
        synchronized (connectionStateLock) {
            stopped = true;
            current = channel;
            channel = null;
            pendingReconnect = scheduledReconnect;
            scheduledReconnect = null;
            pendingAttempt = connecting;
            connecting = null;
        }
        if (pendingReconnect != null) {
            pendingReconnect.cancel(false);
        }
        if (pendingAttempt != null) {
            // the attempt closes its channel when it completes
            pendingAttempt.completeExceptionally(new IllegalStateException("Disconnected from " + address()));
        }
        if (current == null) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Void> closed = new CompletableFuture<>();
        current.close().addListener(f -> closed.complete(null));
        return closed;
    }

    public boolean isConnected() {
        synchronized (connectionStateLock) {
            return channel != null && channel.isActive();
        }
    }

    public CompletionStage<Void> send(F frame) {
        Channel current;
        CompletableFuture<Channel> pendingAttempt;
        synchronized (connectionStateLock) {
            current = channel;
            pendingAttempt = connecting;
        }
        if (current != null && current.isActive()) {
            return write(current, frame);
        }
        if (pendingAttempt != null) {
            // copy: the timeout must not complete the attempt itself, which other senders and the reconnect logic share
            return pendingAttempt.copy()
                    .orTimeout(requestTimeout.toMillis(), TimeUnit.MILLISECONDS)
                    .thenCompose(connected -> write(connected, frame));
        }
        return CompletableFuture.failedFuture(new IllegalStateException("Not connected to " + address()));
    }

    public void receive(Consumer<F> frameReceiver) {
        this.frameReceiver.set(frameReceiver);
    }

    protected void deliver(F frame) {
        Consumer<F> receiver = frameReceiver.get();
        if (receiver == null) {
            return;
        }
        try {
            receiveExecutor.execute(() -> receiver.accept(frame));
        } catch (RejectedExecutionException e) {
            log.debug("[{}] Dropped a received frame: the transport is shutting down", address());
        }
    }

    protected String address() {
        return host + ":" + port;
    }

    /** @return the attempt's result, or {@code null} if the transport was disconnected meanwhile */
    private CompletableFuture<Channel> attempt() {
        CompletableFuture<Channel> attemptResult = new CompletableFuture<>();
        synchronized (connectionStateLock) {
            if (stopped) {
                return null;
            }
            scheduledReconnect = null;
            connecting = attemptResult;
        }
        log.trace("[{}] Connecting", address());
        ChannelFuture connectFuture;
        try {
            connectFuture = new Bootstrap()
                    .group(eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeout.toMillis())
                    .option(ChannelOption.TCP_NODELAY, Boolean.TRUE)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {
                            initPipeline(socketChannel.pipeline());
                            socketChannel.pipeline().addLast(new CloseOnError());
                        }
                    })
                    .connect(host, port);
        } catch (RejectedExecutionException e) {
            log.debug("[{}] Not connecting: the event loop is shutting down", address());
            onAttemptFailed(attemptResult, e);
            return attemptResult;
        }
        connectFuture.addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                onAttemptSucceeded(attemptResult, f.channel());
            } else {
                onAttemptFailed(attemptResult, f.cause());
            }
        });
        return attemptResult;
    }

    private void onAttemptSucceeded(CompletableFuture<Channel> attemptResult, Channel connected) {
        boolean keep;
        synchronized (connectionStateLock) {
            keep = !stopped && connecting == attemptResult;
            if (keep) {
                channel = connected;
                connecting = null;
                nextReconnectDelayMs = initialReconnectDelayMs;
            }
        }
        if (!keep) {
            connected.close();
            attemptResult.completeExceptionally(new IllegalStateException("Disconnected from " + address()));
            return;
        }
        log.debug("[{}] Connected", address());
        onConnected();
        connected.closeFuture().addListener(f -> onChannelClosed(connected));
        attemptResult.complete(connected);
    }

    private void onAttemptFailed(CompletableFuture<Channel> attemptResult, Throwable cause) {
        synchronized (connectionStateLock) {
            if (connecting == attemptResult) {
                connecting = null;
            }
        }
        log.debug("[{}] Connect failed: {}", address(), cause.toString());
        attemptResult.completeExceptionally(cause);
        scheduleReconnect();
    }

    private void onChannelClosed(Channel closed) {
        synchronized (connectionStateLock) {
            if (channel != closed) {
                return; // replaced or disconnected on purpose
            }
            channel = null;
        }
        log.debug("[{}] Connection lost", address());
        scheduleReconnect();
    }

    private void scheduleReconnect() {
        synchronized (connectionStateLock) {
            if (stopped || scheduledReconnect != null || connecting != null || channel != null) {
                return;
            }
            long delayMs = nextReconnectDelayMs;
            nextReconnectDelayMs = Math.min(delayMs * 2, maxReconnectDelayMs);
            try {
                scheduledReconnect = eventLoopGroup.schedule(() -> {
                    attempt();
                }, delayMs, TimeUnit.MILLISECONDS);
            } catch (RejectedExecutionException e) {
                log.debug("[{}] Not reconnecting: the event loop is shutting down", address());
            }
        }
    }

    private CompletableFuture<Void> write(Channel channel, F frame) {
        CompletableFuture<Void> written = new CompletableFuture<>();
        channel.writeAndFlush(toWire(frame, channel.alloc())).addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                written.complete(null);
            } else {
                written.completeExceptionally(f.cause());
            }
        });
        return written;
    }

    /** Any pipeline error closes the connection; the transport then reconnects. */
    private class CloseOnError extends ChannelInboundHandlerAdapter {

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.debug("[{}] Closing connection after error: {}", address(), cause.toString());
            ctx.close();
        }
    }
}
