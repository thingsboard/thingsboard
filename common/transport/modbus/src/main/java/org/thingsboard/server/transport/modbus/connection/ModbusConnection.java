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
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * One Modbus connection shared by every device session behind it, e.g. all slaves of an RS-485 gateway.
 * Independent of the transport underneath: the client comes from {@code clientFactory}, and everything specific to an
 * endpoint type (TCP today, serial later) is decided by {@link ModbusConnectionPool}.
 * <p>
 * Requests are queued and at most {@code maxInFlight} are outstanding at a time. The request timeout belongs to the client,
 * so the connection uses the highest timeout among its sessions and rebuilds the client when a session needs a higher one.
 */
@Slf4j
public class ModbusConnection {

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
    private final Function<Duration, ModbusClientAdapter> clientFactory;

    private final Set<UUID> sessionIds = ConcurrentHashMap.newKeySet();
    private final Object lock = new Object();
    private final ArrayDeque<PendingRequest> queue = new ArrayDeque<>();

    // guarded by lock
    private ModbusClientAdapter client;
    @Getter
    private Duration timeout;
    private int inFlight;
    private boolean closed;

    @Builder
    public ModbusConnection(String key, String settings, int maxInFlight, Duration timeout,
                          Function<Duration, ModbusClientAdapter> clientFactory) {
        this.key = key;
        this.settings = settings;
        this.maxInFlight = Math.max(1, maxInFlight);
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
        synchronized (lock) {
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
        PendingRequest request = new PendingRequest(unitId, functionCode, address, count, new CompletableFuture<>());
        synchronized (lock) {
            if (closed) {
                request.future().completeExceptionally(new IllegalStateException("Modbus connection " + key + " is closed"));
                return request.future();
            }
            queue.add(request);
        }
        dispatch();
        return request.future();
    }

    public void close() {
        List<PendingRequest> pending;
        ModbusClientAdapter old;
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
            pending = new ArrayList<>(queue);
            queue.clear();
            old = client;
        }
        IllegalStateException error = new IllegalStateException("Modbus connection " + key + " is closed");
        pending.forEach(request -> request.future().completeExceptionally(error));
        disconnectQuietly(old);
        log.info("[{}] Closed Modbus connection", key);
    }

    private void dispatch() {
        while (true) {
            PendingRequest request;
            ModbusClientAdapter target;
            synchronized (lock) {
                if (closed || inFlight >= maxInFlight || queue.isEmpty()) {
                    return;
                }
                request = queue.poll();
                inFlight++;
                target = client;
            }
            CompletionStage<byte[]> response;
            try {
                response = target.read(request.unitId(), request.functionCode(), request.address(), request.count());
            } catch (Exception e) {
                response = CompletableFuture.failedFuture(e);
            }
            response.whenComplete((data, error) -> {
                synchronized (lock) {
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

    private record PendingRequest(
            int unitId, ModbusFunctionCode functionCode, int address, int count, CompletableFuture<byte[]> future) {}

}
