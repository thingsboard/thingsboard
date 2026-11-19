// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus.connection;

import com.digitalpetri.modbus.exceptions.ModbusResponseException;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.transport.modbus.ModbusFunctionCode;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Reconnects a client that does not do it by itself: a serial port that is missing or busy when the connection is
 * created, or that is lost later (adapter unplugged). Attempts follow an exponential backoff: {@link #INITIAL_DELAY_MS},
 * then twice as long each time up to {@code maxDelayMs}, and from the start again once the connection is open.
 * {@link #disconnect()} ends the attempts for good. The TCP transports reconnect on their own and are not wrapped.
 */
@Slf4j
public class ModbusClientRetryDecorator implements ModbusClientAdapter {

    static final long INITIAL_DELAY_MS = 500;

    private final String key;
    private final ModbusClientAdapter delegate;
    private final ScheduledExecutorService scheduler;
    private final long maxDelayMs;

    /** Guards the fields below: they are touched by request callbacks, by the scheduler and by disconnect. */
    private final Object connectStateLock = new Object();
    private long backoffDelayMs = INITIAL_DELAY_MS;
    private ScheduledFuture<?> scheduledReconnect;
    private boolean closed;

    public ModbusClientRetryDecorator(String key, ModbusClientAdapter delegate, ScheduledExecutorService scheduler, long maxDelayMs) {
        this.key = key;
        this.delegate = delegate;
        this.scheduler = scheduler;
        this.maxDelayMs = Math.max(INITIAL_DELAY_MS, maxDelayMs);
    }

    @Override
    public CompletionStage<Void> connect() {
        synchronized (connectStateLock) {
            if (closed) {
                return CompletableFuture.failedFuture(new IllegalStateException("Modbus client " + key + " is closed"));
            }
        }
        CompletionStage<Void> attempt;
        try {
            attempt = delegate.connect();
        } catch (RuntimeException e) {
            attempt = CompletableFuture.failedFuture(e);
        }
        attempt.whenComplete((r, e) -> {
            if (e == null) {
                onConnected();
            } else {
                scheduleReconnect(e);
            }
        });
        return attempt;
    }

    private void onConnected() {
        boolean closedMeanwhile;
        synchronized (connectStateLock) {
            closedMeanwhile = closed;
            resetBackoff();
        }
        if (closedMeanwhile) {
            // disconnect ran while this attempt was in progress: do not leave the port open behind it
            delegate.disconnect();
        }
    }

    private void scheduleReconnect(Throwable cause) {
        synchronized (connectStateLock) {
            if (closed || scheduledReconnect != null) {
                return;
            }
            long delayMs = nextBackoffDelayMs();
            log.debug("[{}] Modbus connection is not open, next attempt in {} ms: {}", key, delayMs, cause.toString());
            try {
                scheduledReconnect = scheduler.schedule(this::reconnect, delayMs, TimeUnit.MILLISECONDS);
            } catch (RejectedExecutionException rejected) {
                log.debug("[{}] Reconnect scheduler is shut down, giving up", key);
            }
        }
    }

    private void reconnect() {
        synchronized (connectStateLock) {
            scheduledReconnect = null;
            if (closed) {
                return;
            }
        }
        connect();
    }

    private long nextBackoffDelayMs() {
        long delayMs = backoffDelayMs;
        backoffDelayMs = Math.min(maxDelayMs, backoffDelayMs * 2);
        return delayMs;
    }

    private void resetBackoff() {
        backoffDelayMs = INITIAL_DELAY_MS;
    }

    @Override
    public CompletionStage<Void> disconnect() {
        synchronized (connectStateLock) {
            closed = true;
            if (scheduledReconnect != null) {
                // false: an attempt that is already running is not interrupted in the middle of opening the port;
                // it sees the closed flag and closes the port again
                scheduledReconnect.cancel(false);
                scheduledReconnect = null;
            }
        }
        return delegate.disconnect();
    }

    @Override
    public CompletionStage<byte[]> read(int unitId, ModbusFunctionCode functionCode, int address, int count) {
        return reconnectIfConnectionLost(delegate.read(unitId, functionCode, address, count));
    }

    @Override
    public CompletionStage<Void> writeCoil(int unitId, ModbusFunctionCode functionCode, int address, boolean value) {
        return reconnectIfConnectionLost(delegate.writeCoil(unitId, functionCode, address, value));
    }

    @Override
    public CompletionStage<Void> writeRegisters(int unitId, ModbusFunctionCode functionCode, int address, byte[] registerBytes) {
        return reconnectIfConnectionLost(delegate.writeRegisters(unitId, functionCode, address, registerBytes));
    }

    @Override
    public CompletionStage<Void> maskWriteRegister(int unitId, int address, int andMask, int orMask) {
        return reconnectIfConnectionLost(delegate.maskWriteRegister(unitId, address, andMask, orMask));
    }

    /** The request itself still fails; the callers report it and the next poll runs on the reopened connection. */
    private <T> CompletionStage<T> reconnectIfConnectionLost(CompletionStage<T> request) {
        request.whenComplete((r, e) -> {
            if (e != null && isConnectionLost(e)) {
                onConnectionLost(e);
            }
        });
        return request;
    }

    /**
     * A timeout means the device did not answer and an exception response means it refused the request: in both
     * cases the connection works. Any other failure comes from the connection itself.
     */
    private static boolean isConnectionLost(Throwable error) {
        Throwable cause = unwrap(error);
        return !(cause instanceof TimeoutException) && !(cause instanceof ModbusResponseException);
    }

    private void onConnectionLost(Throwable cause) {
        synchronized (connectStateLock) {
            if (closed || scheduledReconnect != null) {
                return;
            }
        }
        // release the dead handle so that the port can be opened again
        delegate.disconnect();
        scheduleReconnect(cause);
    }

    private static Throwable unwrap(Throwable error) {
        Throwable current = error;
        while ((current instanceof CompletionException || current instanceof ExecutionException) && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

}
