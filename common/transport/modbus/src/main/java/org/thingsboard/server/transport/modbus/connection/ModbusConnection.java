// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus.connection;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.transport.modbus.ModbusFunctionCode;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * One Modbus connection shared by every device session behind it, e.g. all slaves of an RS-485 gateway.
 * Independent of the transport underneath: the client comes from {@code clientFactory}, and everything specific to an
 * endpoint type (TCP today, serial later) is decided by {@link ModbusConnectionPool}.
 * <p>
 * Requests are queued and at most {@code maxInFlight} are outstanding at a time. At most {@code maxQueueSize} wait in the
 * queue: beyond that new requests fail at once, so an unreachable device cannot pile up writes without limit.
 * Requests queued with a coalesce key ({@link #executeLatest}) take one place per key: a newer one replaces the one waiting.
 * The request timeout belongs to the client,
 * so the connection uses the highest timeout among its sessions and rebuilds the client when a session needs a higher one.
 */
@Slf4j
public class ModbusConnection {

    public static final long NO_DEADLINE = 0L;

    @Getter
    private final String key;
    /**
     * Connection-level settings every device on this key must agree on, e.g. {@code framing=MBAP}
     * (or baud rate and parity for a serial port). A device with different settings cannot share the connection.
     */
    @Getter
    private final String settings;
    @Getter
    private final int maxInFlight;
    @Getter
    private final int maxQueueSize;
    private final Function<Duration, ModbusClientAdapter> clientFactory;

    private final Set<UUID> sessionIds = ConcurrentHashMap.newKeySet();
    /**
     * Guards the request queue and the state the dispatch reads with it: in-flight count, current client, timeout and
     * closed flag. Taken by callers queuing requests, by response callbacks and by client rebuilds.
     */
    private final Object requestQueueLock = new Object();
    private final ArrayDeque<PendingRequest> queue = new ArrayDeque<>();
    /**
     * Latest waiting request per coalesce key, guarded by {@code requestQueueLock}. The queue keeps the request that
     * was queued first for the key and so holds its place; the one in this map is sent when that place comes up.
     */
    private final Map<Object, PendingRequest> activeWriteMap = new HashMap<>();
    /** Calls to {@link #dispatch} not yet accounted for; non-zero while a dispatch loop is running. */
    private final AtomicInteger dispatchCalls = new AtomicInteger();

    // guarded by requestQueueLock
    private ModbusClientAdapter client;
    @Getter
    private Duration timeout;
    private int inFlight;
    private boolean closed;

    @Builder
    public ModbusConnection(String key, String settings, int maxInFlight, int maxQueueSize, Duration timeout,
                          Function<Duration, ModbusClientAdapter> clientFactory) {
        this.key = key;
        this.settings = settings;
        this.maxInFlight = Math.max(1, maxInFlight);
        this.maxQueueSize = Math.max(1, maxQueueSize);
        this.clientFactory = clientFactory;
        this.timeout = timeout;
        this.client = newClient(timeout);
    }

    public void attach(UUID sessionId) {
        sessionIds.add(sessionId);
    }

    /**
     * @return {@code true} if no sessions use the endpoint anymore
     */
    public boolean detach(UUID sessionId) {
        sessionIds.remove(sessionId);
        return sessionIds.isEmpty();
    }

    /**
     * Makes sure the client waits at least {@code required} for a response; rebuilds the client if it has to grow.
     * Requests in flight on the old client may fail once; the poll loop reports them and polls again next cycle.
     */
    public void ensureTimeout(Duration required) {
        ModbusClientAdapter old;
        synchronized (requestQueueLock) {
            if (closed || required.compareTo(timeout) <= 0) {
                return;
            }
            log.info("[{}] Increasing request timeout {} -> {}, rebuilding client", key, timeout, required);
            old = client;
            timeout = required;
            client = newClient(required);
        }
        disconnectQuietly(old);
    }

    public CompletableFuture<byte[]> read(int unitId, ModbusFunctionCode functionCode, int address, int count) {
        return execute(client -> client.read(unitId, functionCode, address, count), NO_DEADLINE);
    }

    /**
     * Queues any operation (read or write) behind the ones already waiting on this connection.
     * Fails at once if the connection is closed or {@code maxQueueSize} operations are already waiting.
     *
     * @param deadlineMs epoch millis after which the operation is dropped instead of sent (e.g. an expired RPC), or {@link #NO_DEADLINE}
     */
    public CompletableFuture<byte[]> execute(Function<ModbusClientAdapter, CompletionStage<byte[]>> operation, long deadlineMs) {
        var futureResponse = new CompletableFuture<byte[]>();
        enqueue(new PendingRequest(operation, deadlineMs, futureResponse, null), false);
        return futureResponse;
    }

    /**
     * Like {@link #execute}, but at most one request per {@code coalesceKey} waits in the queue, in the place of the one
     * queued first. Meant for values where only the latest matters, e.g. a shared attribute of one device.
     * The request that loses fails with {@link SupersededException}; a request already sent is not affected.
     *
     * @param coalesceKey     identifies the value across all devices of this connection, so it must include the device
     * @param replaceWaiting  {@code true} for a new value: it replaces the request waiting for the key;
     *                        {@code false} for a retry of an older value: it gives way to the request waiting for the key
     */
    public CompletableFuture<byte[]> executeLatest(Object coalesceKey, Function<ModbusClientAdapter, CompletionStage<byte[]>> operation,
                                                   boolean replaceWaiting) {
        var futureResponse = new CompletableFuture<byte[]>();
        enqueue(new PendingRequest(operation, NO_DEADLINE, futureResponse, coalesceKey), replaceWaiting);
        return futureResponse;
    }

    private void enqueue(PendingRequest request, boolean replaceWaiting) {
        Object coalesceKey = request.coalesceKey();
        PendingRequest superseded = null;
        synchronized (requestQueueLock) {
            if (closed) {
                request.future().completeExceptionally(new IllegalStateException("Modbus connection " + key + " is closed"));
                return;
            }
            PendingRequest waiting = coalesceKey != null ? activeWriteMap.get(coalesceKey) : null;
            if (waiting != null) {
                // takes no new place in the queue, so it is allowed even when the queue is full
                if (replaceWaiting) {
                    activeWriteMap.put(coalesceKey, request);
                    superseded = waiting;
                } else {
                    superseded = request;
                }
            } else if (queue.size() >= maxQueueSize) {
                request.future().completeExceptionally(new IllegalStateException(
                        "Modbus connection " + key + " is overloaded: " + maxQueueSize + " requests are already waiting"));
                return;
            } else {
                if (coalesceKey != null) {
                    activeWriteMap.put(coalesceKey, request);
                }
                queue.add(request);
            }
        }
        if (superseded != null) {
            superseded.future().completeExceptionally(new SupersededException());
        }
        dispatch();
    }

    public void close() {
        List<PendingRequest> pending;
        ModbusClientAdapter old;
        synchronized (requestQueueLock) {
            if (closed) {
                return;
            }
            closed = true;
            pending = new ArrayList<>(queue.size());
            for (PendingRequest request : queue) {
                pending.add(latest(request));
            }
            queue.clear();
            activeWriteMap.clear();
            old = client;
        }
        IllegalStateException error = new IllegalStateException("Modbus connection " + key + " is closed");
        pending.forEach(request -> request.future().completeExceptionally(error));
        disconnectQuietly(old);
        log.info("[{}] Closed Modbus connection", key);
    }

    /**
     * Sends queued requests while there is a free slot. Called after every enqueue and every completion;
     * there is no sender thread.
     * <p>
     * Only one caller at a time runs the loop. A call that arrives while it is running, from another thread or from a
     * request that completed inside {@link #send}, is only counted in {@code dispatchCalls} and returns. The running loop
     * sends what it can; when nothing more can be sent it checks whether such calls were counted meanwhile and, if so,
     * looks at the queue again. So requests that fail immediately are handled one after another instead of one inside
     * another, and a request queued (or a slot freed) just as the loop is finishing is not left waiting.
     */
    private void dispatch() {
        if (dispatchCalls.getAndIncrement() != 0) {
            return;
        }
        int unhandledCalls = 1; // this call
        while (unhandledCalls != 0) {
            Dispatch next = nextToSend();
            if (next != null) {
                send(next);
            } else {
                unhandledCalls = dispatchCalls.addAndGet(-unhandledCalls);
            }
        }
    }

    /**
     * Takes the next queued request and a slot for it, or {@code null} if nothing can be sent now: the connection is
     * closed, all slots are taken or the queue is empty. Requests whose deadline has passed are dropped on the way.
     * The client is captured together with the slot, so a concurrent client rebuild cannot swap it in between.
     */
    private Dispatch nextToSend() {
        List<PendingRequest> expired = null;
        Dispatch next = null;
        long now = System.currentTimeMillis();

        synchronized (requestQueueLock) {
            while (!closed && inFlight < maxInFlight && !queue.isEmpty()) {
                PendingRequest request = latest(queue.poll());
                if (request.coalesceKey() != null) {
                    activeWriteMap.remove(request.coalesceKey());
                }
                if (request.deadlineMs() != NO_DEADLINE && now > request.deadlineMs()) {
                    if (expired == null) {
                        expired = new ArrayList<>();
                    }
                    expired.add(request);
                    continue;
                }
                inFlight++;
                next = new Dispatch(request, client);
                break;
            }
        }
        if (expired != null) {
            IllegalStateException error = new IllegalStateException("Request expired before it was sent");
            expired.forEach(request -> request.future().completeExceptionally(error));
        }
        return next;
    }

    private void send(Dispatch dispatch) {
        PendingRequest request = dispatch.request();
        CompletionStage<byte[]> response;
        try {
            response = request.operation().apply(dispatch.client());
        } catch (Exception e) {
            response = CompletableFuture.failedFuture(e);
        }
        response.whenComplete((data, error) -> {
            synchronized (requestQueueLock) {
                inFlight--;
            }
            if (error != null) {
                request.future().completeExceptionally(error);
            } else {
                request.future().complete(data);
            }
            dispatch();
        });
    }

    /** The request to send in the place of a queued one: for a coalesce key, the latest one queued for it. Call under {@code requestQueueLock}. */
    private PendingRequest latest(PendingRequest queued) {
        return queued.coalesceKey() != null ? activeWriteMap.get(queued.coalesceKey()) : queued;
    }

    private ModbusClientAdapter newClient(Duration requestTimeout) {
        ModbusClientAdapter newClient = clientFactory.apply(requestTimeout);
        newClient.connect().whenComplete((r, e) -> {
            if (e != null) {
                log.debug("[{}] Initial connect failed, the client keeps reconnecting: {}", key, e.toString());
            }
        });
        return newClient;
    }

    private void disconnectQuietly(ModbusClientAdapter old) {
        try {
            old.disconnect();
        } catch (Exception e) {
            log.debug("[{}] Failed to disconnect Modbus client", key, e);
        }
    }

    private record Dispatch(PendingRequest request, ModbusClientAdapter client) {}

    private record PendingRequest(Function<ModbusClientAdapter, CompletionStage<byte[]>> operation, long deadlineMs,
                                  CompletableFuture<byte[]> future, Object coalesceKey) {}

    /** Outcome of a request queued with {@link #executeLatest} that was not sent because a newer one took its place. */
    public static class SupersededException extends RuntimeException {

        public SupersededException() {
            super("Superseded by a newer request", null, false, false);
        }

    }

}
