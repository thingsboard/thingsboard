// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.ai.common.channel.ChannelError;
import org.thingsboard.ai.common.channel.ChannelException;
import org.thingsboard.ai.common.channel.ChannelFrame;
import org.thingsboard.ai.common.channel.ChannelFrameCodec;
import org.thingsboard.ai.common.channel.ChannelFrameListener;
import org.thingsboard.ai.common.channel.ChannelHello;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.ai.common.channel.ChannelSession;
import org.thingsboard.ai.common.channel.TbAiChannelClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One WebSocket to TB AI, shared by the turns and operations of one user. Capacity is reserved before an exchange
 * starts ({@link #tryReserve}) and released when it ends ({@link #finish}); a retiring connection takes no new work
 * and closes once its last exchange ends. Exchanges are told apart by their op, the id of their start frame.
 * <p>
 * Lock order: the pool's group lock may be held while calling into a connection, never the reverse, and a connection
 * never closes its session while holding its own lock.
 */
@Slf4j
final class TbAiPooledConnection implements ChannelFrameListener {

    // Protocol version 1 is the baseline. A later feature that this ThingsBoard handles goes here, so TB AI can tell
    // before using it that this ThingsBoard understands it.
    static final List<String> CLIENT_CAPABILITIES = List.of();

    private final TbAiChannelPool.PoolKey key;
    private final String origin;
    private final boolean pooled;
    private final int maxOperations;
    private final ChannelFrameCodec codec;
    private final TbAiChannelPool pool;
    private final long createdNanos;
    private final CompletableFuture<TbAiPooledConnection> ready = new CompletableFuture<>();
    private final Map<String, TbAiChannelExchange> exchanges = new ConcurrentHashMap<>();
    private final AtomicBoolean removed = new AtomicBoolean();
    private final AtomicBoolean poolSlotHeld;

    private final Object lock = new Object();
    private int reserved;
    private boolean retiring;
    private long lastUsedNanos;

    private volatile ChannelSession session;
    private volatile int maxFrameBytes = TbAiChannelClient.DEFAULT_MAX_FRAME_BYTES;

    /**
     * Created with one reservation for the caller that opens it, so callers waiting on the same handshake cannot
     * take its place.
     */
    TbAiPooledConnection(TbAiChannelPool.PoolKey key, String origin, boolean pooled, int maxOperations,
                         ChannelFrameCodec codec, TbAiChannelPool pool, long nowNanos) {
        this.key = key;
        this.origin = origin;
        this.pooled = pooled;
        this.maxOperations = maxOperations;
        this.codec = codec;
        this.pool = pool;
        this.createdNanos = nowNanos;
        this.lastUsedNanos = nowNanos;
        this.reserved = 1;
        this.retiring = !pooled;
        this.poolSlotHeld = new AtomicBoolean(pooled);
    }

    /**
     * True exactly once for a pooled connection, when it gives its place in the pool's connection count back.
     */
    boolean releasePoolSlot() {
        return poolSlotHeld.compareAndSet(true, false);
    }

    boolean isRemoved() {
        return removed.get();
    }

    TbAiChannelPool.PoolKey key() {
        return key;
    }

    boolean isPooled() {
        return pooled;
    }

    int maxFrameBytes() {
        return maxFrameBytes;
    }

    ChannelSession session() {
        return session;
    }

    ChannelFrameCodec codec() {
        return codec;
    }

    CompletableFuture<TbAiPooledConnection> ready() {
        return ready;
    }

    /**
     * The session arrived. If the handshake already failed (the connect timed out), the session is closed: it is
     * stored first and {@link #failed} closes whatever is stored after failing {@code ready}, so one of the two always
     * sees the other and the channel never stays open outside the pool.
     */
    void opened(ChannelSession openedSession) {
        session = openedSession;
        if (ready.isDone()) {
            close(ChannelProtocol.CLOSE_NORMAL, "Handshake failed");
            return;
        }
        try {
            openedSession.send(ChannelFrame.of(ChannelProtocol.HELLO, codec.toPayload(
                    new ChannelHello(ChannelProtocol.VERSION, CLIENT_CAPABILITIES, origin, null))));
        } catch (ChannelException e) {
            ready.completeExceptionally(new TbAiChannelUnavailableException("Cannot send hello: " + e.getMessage(), e));
        }
    }

    /**
     * The handshake failed or timed out: the connection leaves the pool now and its session, if it has one or gets
     * one later, is closed.
     */
    void failed(Throwable error) {
        ready.completeExceptionally(error);
        markRemoved();
        close(ChannelProtocol.CLOSE_NORMAL, "Handshake failed");
    }

    boolean tryReserve(long nowNanos) {
        synchronized (lock) {
            if (retiring || session == null || !session.isOpen() || reserved >= maxOperations) {
                return false;
            }
            reserved++;
            lastUsedNanos = nowNanos;
            return true;
        }
    }

    /**
     * Starts an exchange on a reserved slot; its op is the id of {@code startFrame}.
     */
    void start(TbAiChannelExchange exchange, ChannelFrame startFrame) {
        exchanges.put(startFrame.id(), exchange);
        try {
            session.send(startFrame);
        } catch (ChannelException e) {
            exchanges.remove(startFrame.id());
            throw e;
        }
    }

    void send(ChannelFrame frame) {
        session.send(frame);
    }

    /**
     * Ends the exchange started with {@code op} and gives back its reservation; frames for it that still arrive are
     * dropped.
     */
    void finish(String op, long nowNanos) {
        exchanges.remove(op);
        unreserve(nowNanos);
    }

    /**
     * Gives back a reservation that never started an exchange.
     */
    void unreserve(long nowNanos) {
        boolean close;
        synchronized (lock) {
            reserved = Math.max(0, reserved - 1);
            lastUsedNanos = nowNanos;
            close = retiring && reserved == 0;
        }
        if (close) {
            close(ChannelProtocol.CLOSE_NORMAL, "Done");
        }
    }

    void retire() {
        boolean close;
        synchronized (lock) {
            retiring = true;
            close = reserved == 0;
        }
        if (close) {
            close(ChannelProtocol.CLOSE_NORMAL, "Retired");
        }
    }

    /**
     * Closes the connection when it has been idle for {@code idleNanos} or is older than {@code maxAgeNanos}; an old
     * connection still in use stops taking work and closes after its last exchange.
     */
    boolean sweep(long nowNanos, long idleNanos, long maxAgeNanos) {
        boolean close = false;
        synchronized (lock) {
            boolean aged = nowNanos - createdNanos >= maxAgeNanos;
            if (reserved == 0 && (aged || nowNanos - lastUsedNanos >= idleNanos)) {
                retiring = true;
                close = true;
            } else if (aged) {
                retiring = true;
            }
        }
        if (close) {
            close(ChannelProtocol.CLOSE_NORMAL, "Idle");
        }
        return close;
    }

    /**
     * Closes the connection if nothing runs on it, to make room in a full pool.
     */
    boolean evictIfIdle() {
        synchronized (lock) {
            if (reserved != 0 || retiring) {
                return false;
            }
            retiring = true;
        }
        close(ChannelProtocol.CLOSE_NORMAL, "Evicted");
        return true;
    }

    long lastUsedNanos() {
        synchronized (lock) {
            return lastUsedNanos;
        }
    }

    boolean isIdle() {
        synchronized (lock) {
            return reserved == 0 && !retiring;
        }
    }

    void close(int code, String reason) {
        ChannelSession current = session;
        if (current != null && current.isOpen()) {
            current.close(code, reason);
        }
    }

    @Override
    public void onFrame(ChannelSession frameSession, ChannelFrame frame) {
        if (!ready.isDone()) {
            onHandshakeFrame(frame);
            return;
        }
        if (frame.op() == null) {
            onConnectionFrame(frame);
            return;
        }
        TbAiChannelExchange exchange = exchanges.get(frame.op());
        if (exchange == null) {
            log.debug("[{}] Dropping AI channel frame '{}' for finished operation '{}'", key, frame.type(), frame.op());
            return;
        }
        exchange.onFrame(this, frame);
    }

    @Override
    public void onClose(ChannelSession closedSession, int code, String reason) {
        synchronized (lock) {
            retiring = true;
        }
        ready.completeExceptionally(new TbAiChannelUnavailableException(
                "AI channel closed before hello [" + code + "] " + reason, null));
        markRemoved();
        List<TbAiChannelExchange> running = List.copyOf(exchanges.values());
        exchanges.clear();
        running.forEach(exchange -> exchange.onClose(code, reason));
    }

    private void onHandshakeFrame(ChannelFrame frame) {
        try {
            switch (frame.type()) {
                case ChannelProtocol.HELLO -> {
                    ChannelHello hello = codec.fromPayload(frame.payload(), ChannelHello.class);
                    if (hello.maxFrameBytes() != null && hello.maxFrameBytes() > 0 && hello.maxFrameBytes() < Integer.MAX_VALUE) {
                        maxFrameBytes = hello.maxFrameBytes().intValue();
                    }
                    ready.complete(this);
                }
                case ChannelProtocol.ERROR -> {
                    ChannelError error = codec.fromPayload(frame.payload(), ChannelError.class);
                    ready.completeExceptionally(new TbAiChannelUnavailableException("AI channel refused: " + error.message(), null));
                    close(ChannelProtocol.CLOSE_NORMAL, "Error received");
                }
                default -> log.debug("[{}] Ignoring AI channel frame '{}' before hello", key, frame.type());
            }
        } catch (ChannelException e) {
            ready.completeExceptionally(new TbAiChannelUnavailableException("Malformed AI channel hello: " + e.getMessage(), e));
            close(ChannelProtocol.CLOSE_PROTOCOL_ERROR, e.getMessage());
        }
    }

    private void onConnectionFrame(ChannelFrame frame) {
        if (ChannelProtocol.ERROR.equals(frame.type())) {
            ChannelError error = codec.fromPayload(frame.payload(), ChannelError.class);
            log.warn("[{}] AI channel error {}: {}", key, error.code(), error.message());
            close(ChannelProtocol.CLOSE_NORMAL, "Error received");
        } else {
            log.debug("[{}] Ignoring AI channel frame '{}' without op", key, frame.type());
        }
    }

    private void markRemoved() {
        if (removed.compareAndSet(false, true)) {
            pool.removed(this);
        }
    }

}
