// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus.poll;

import com.digitalpetri.modbus.ExceptionCode;
import com.digitalpetri.modbus.exceptions.ModbusResponseException;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.transport.modbus.connection.ModbusConnection;
import org.thingsboard.server.transport.modbus.session.ModbusSessionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Polls a fixed set of read blocks for one session. The next cycle is scheduled {@code periodMs} after the previous
 * one has completed (all blocks answered, failed or timed out), so a slow device never has overlapping cycles.
 */
@Slf4j
public class ModbusPollLoop {

    static final String ERROR_METHOD = "modbusPoll";

    private final ModbusSessionContext session;
    private final ModbusConnection connection;
    private final List<ReadBlock> blocks;
    private final long periodMs;
    private final int retries;
    private final ScheduledExecutorService scheduler;
    private final TransportService transportService;
    /** Last value posted per target and key, used when the profile sends data only on change. */
    private final Map<String, JsonElement> lastValues = new ConcurrentHashMap<>();

    private volatile boolean stopped;
    private volatile ScheduledFuture<?> scheduled;

    public ModbusPollLoop(ModbusSessionContext session, ModbusConnection connection, List<ReadBlock> blocks, long periodMs,
                          int retries, ScheduledExecutorService scheduler, TransportService transportService) {
        this.session = session;
        this.connection = connection;
        this.blocks = new CopyOnWriteArrayList<>(blocks);
        this.periodMs = periodMs;
        this.retries = retries;
        this.scheduler = scheduler;
        this.transportService = transportService;
    }

    public void start() {
        schedule(0);
    }

    public void stop() {
        stopped = true;
        ScheduledFuture<?> current = scheduled;
        if (current != null) {
            current.cancel(false);
        }
    }

    public long getPeriodMs() {
        return periodMs;
    }

    public List<ReadBlock> getBlocks() {
        return List.copyOf(blocks);
    }

    private void schedule(long delayMs) {
        if (!stopped) {
            scheduled = scheduler.schedule(this::runCycle, delayMs, TimeUnit.MILLISECONDS);
        }
    }

    void runCycle() {
        if (stopped) {
            return;
        }
        JsonObject telemetry = new JsonObject();
        JsonObject attributes = new JsonObject();
        List<CompletableFuture<Boolean>> reads = new ArrayList<>();

        for (ReadBlock block : List.copyOf(blocks)) {
            CompletableFuture<byte[]> completableFuture = readWithRetries(block, retries);
            reads.add(completableFuture.handle((data, error) -> {
                if (error != null) {
                    onBlockError(block, unwrap(error));
                    return false;
                }
                decodeInto(block, data, telemetry, attributes);
                return true;
            }));
        }
        CompletableFuture.allOf(reads.toArray(new CompletableFuture[0])).whenComplete((r, e) -> {
            try {
                if (!stopped) {
                    boolean anySuccess = reads.stream().anyMatch(CompletableFuture::join);
                    publish(telemetry, attributes, anySuccess);
                }
            } catch (Exception ex) {
                log.warn("[{}] Failed to publish polled values", session.getDeviceId(), ex);
            } finally {
                schedule(periodMs);
            }
        });
    }

    private CompletableFuture<byte[]> readWithRetries(ReadBlock block, int attemptsLeft) {
        int unitId = session.getDeviceConfig().getUnitId();
        CompletableFuture<byte[]> read = connection.read(unitId, block.functionCode(), block.address(), block.count());

        return read.exceptionallyCompose(error -> {
            if (attemptsLeft > 0 && !stopped && unwrap(error) instanceof TimeoutException) {
                log.trace("[{}] Read {} timed out, retrying ({} left)", session.getDeviceId(), block, attemptsLeft);
                return readWithRetries(block, attemptsLeft - 1);
            }
            return CompletableFuture.failedFuture(error);
        });
    }

    private void decodeInto(ReadBlock block, byte[] data, JsonObject telemetry, JsonObject attributes) {
        var profileConfig = session.getProfileConfig();
        for (PollItem item : block.items()) {
            try {
                Object value = RegisterCodec.decode(data, block, item.mapping(), profileConfig.getByteOrder(), profileConfig.getWordOrder());
                JsonObject target = item.target() == PollItem.Target.TELEMETRY ? telemetry : attributes;
                synchronized (target) {
                    target.add(item.mapping().getTag(), toJson(value));
                }
            } catch (Exception e) {
                log.debug("[{}] Failed to decode {}", session.getDeviceId(), item.mapping().getTag(), e);
                transportService.errorEvent(session.getTenantId(), session.getDeviceId(), ERROR_METHOD,
                        new IllegalArgumentException("Failed to decode '" + item.mapping().getTag() + "': " + e.getMessage(), e));
            }
        }
    }

    private void onBlockError(ReadBlock block, Throwable error) {
        if (error instanceof ModbusResponseException mre
                && mre.getExceptionCode() == ExceptionCode.ILLEGAL_DATA_ADDRESS.getCode()
                && block.items().size() > 1
                && blocks.remove(block)) {
            log.info("[{}] Device rejected merged read {} with exception 02, polling its mappings one by one", session.getDeviceId(), block);
            blocks.addAll(block.splitPerItem());
            return;
        }
        log.debug("[{}] Failed to read {}: {}", session.getDeviceId(), block, error.toString());
        transportService.errorEvent(session.getTenantId(), session.getDeviceId(), ERROR_METHOD, error);
    }

    private void publish(JsonObject telemetry, JsonObject attributes, boolean anySuccess) {
        var sessionInfo = session.getSessionInfo();
        boolean onlyOnChange = session.getProfileConfig().isSendDataOnlyOnChange();
        JsonObject changedTelemetry = onlyOnChange ? changed("ts:", telemetry) : telemetry;
        JsonObject changedAttributes = onlyOnChange ? changed("attr:", attributes) : attributes;
        if (!changedTelemetry.isEmpty()) {
            transportService.process(sessionInfo, JsonConverter.convertToTelemetryProto(changedTelemetry), TransportServiceCallback.EMPTY);
        }
        if (!changedAttributes.isEmpty()) {
            transportService.process(sessionInfo, JsonConverter.convertToAttributesProto(changedAttributes), TransportServiceCallback.EMPTY);
        }
        if (anySuccess) {
            transportService.recordActivity(sessionInfo);
        }
    }

    private JsonObject changed(String prefix, JsonObject values) {
        JsonObject result = new JsonObject();
        values.entrySet().forEach(entry -> {
            JsonElement previous = lastValues.put(prefix + entry.getKey(), entry.getValue());
            if (!entry.getValue().equals(previous)) {
                result.add(entry.getKey(), entry.getValue());
            }
        });
        return result;
    }

    private static JsonElement toJson(Object value) {
        if (value instanceof Boolean b) {
            return new JsonPrimitive(b);
        }
        if (value instanceof Number n) {
            return new JsonPrimitive(n);
        }
        return new JsonPrimitive(String.valueOf(value));
    }

    private static Throwable unwrap(Throwable error) {
        Throwable current = error;
        while ((current instanceof CompletionException || current instanceof ExecutionException) && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

}
