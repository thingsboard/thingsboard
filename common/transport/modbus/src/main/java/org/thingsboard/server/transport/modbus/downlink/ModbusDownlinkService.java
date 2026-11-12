// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus.downlink;

import com.digitalpetri.modbus.ExceptionCode;
import com.digitalpetri.modbus.exceptions.ModbusResponseException;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.data.transport.modbus.ModbusFunctionCode;
import org.thingsboard.server.common.data.transport.modbus.ModbusMapping;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeUpdateNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcResponseMsg;
import org.thingsboard.server.queue.util.TbModbusTransportComponent;
import org.thingsboard.server.transport.modbus.connection.ModbusClientAdapter;
import org.thingsboard.server.transport.modbus.connection.ModbusConnection;
import org.thingsboard.server.transport.modbus.poll.PollItem;
import org.thingsboard.server.transport.modbus.poll.ReadBlock;
import org.thingsboard.server.transport.modbus.poll.RegisterCodec;
import org.thingsboard.server.transport.modbus.session.ModbusSessionContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Writes shared attribute updates and executes RPCs against Modbus devices, through the same per-connection queue as polling.
 * <ul>
 *     <li>Shared attributes: every updated key with a mapping in {@code attributeUpdates} is written; failures become error events,
 *     a successful write counts as device activity. Only the latest value of an attribute waits in the queue: a newer one
 *     replaces it, and the replaced value is dropped silently.</li>
 *     <li>RPC: the method name selects a mapping in {@code rpc} by tag. A read function code answers {@code {"value": …}},
 *     a write answers {@code {"result": "ok"}}. Errors go to the response's {@code error} field, so core marks the RPC failed.
 *     An RPC that expires while waiting in the queue is never sent.</li>
 * </ul>
 */
@Slf4j
@TbModbusTransportComponent
@Service
@RequiredArgsConstructor
public class ModbusDownlinkService {

    static final String ATTRIBUTE_UPDATE_ERROR = "modbusAttributeUpdate";
    private static final byte[] NO_DATA = new byte[0];

    private final TransportService transportService;

    public void onAttributeUpdate(ModbusSessionContext session, AttributeUpdateNotificationMsg msg) {
        var profileConfig = session.getProfileConfig();
        if (!session.isActive() || profileConfig == null || profileConfig.getAttributeUpdates() == null || msg.getSharedUpdatedCount() == 0) {
            return;
        }
        // only updated keys: toJson also adds a "deleted" array, which must not be mistaken for an attribute
        JsonObject values = JsonConverter.toJson(AttributeUpdateNotificationMsg.newBuilder().addAllSharedUpdated(msg.getSharedUpdatedList()).build());
        for (ModbusMapping mapping : profileConfig.getAttributeUpdates()) {
            JsonElement value = values.get(mapping.getTag());
            if (value == null) {
                continue;
            }
            var coalesceKey = new AttributeWriteKey(session.getDeviceId(), mapping.getTag());
            write(session, mapping, value, ModbusConnection.NO_DEADLINE, coalesceKey).whenComplete((r, e) -> {
                if (e != null && unwrap(e) instanceof ModbusConnection.SupersededException) {
                    log.trace("[{}] Write of attribute '{}' was replaced by a newer value", session.getDeviceId(), mapping.getTag());
                } else if (e != null) {
                    log.debug("[{}] Failed to write attribute '{}'", session.getDeviceId(), mapping.getTag(), e);
                    transportService.errorEvent(session.getTenantId(), session.getDeviceId(), ATTRIBUTE_UPDATE_ERROR,
                            new RuntimeException("Failed to write '" + mapping.getTag() + "': " + describe(e), unwrap(e)));
                } else {
                    // the device acknowledged the write: it is alive, even if nothing is polled from it
                    transportService.recordActivity(session.getSessionInfo());
                }
            });
        }
    }

    public void onToDeviceRpcRequest(ModbusSessionContext session, ToDeviceRpcRequestMsg msg) {
        if (!session.isActive()) {
            respondError(session, msg, "Device session is not active");
            return;
        }
        Optional<ModbusMapping> mapping = Optional.ofNullable(session.getProfileConfig().getRpc()).orElse(List.of()).stream()
                .filter(m -> m.getTag().equals(msg.getMethodName()))
                .findFirst();
        if (mapping.isEmpty()) {
            respondError(session, msg, "Unknown RPC method '" + msg.getMethodName() + "'");
            return;
        }
        CompletableFuture<String> result;
        try {
            result = mapping.get().getFunctionCode().isWrite()
                    ? write(session, mapping.get(), valueOf(msg.getParams()), deadlineOf(msg), null).thenApply(r -> "{\"result\":\"ok\"}")
                    : read(session, mapping.get(), deadlineOf(msg));
        } catch (Exception e) {
            result = CompletableFuture.failedFuture(e);
        }

        result.whenComplete((payload, error) -> {
            if (error != null) {
                respondError(session, msg, describe(error));
            } else {
                respondSuccess(session, msg, payload);
            }
        });
    }

    /**
     * Never throws: encoding errors and a missing connection come back as a failed future.
     *
     * @param coalesceKey if not {@code null}, only the latest write with this key waits in the queue (see
     *                    {@link ModbusConnection#executeLatest}); {@code deadlineMs} is not used then
     */
    private CompletableFuture<byte[]> write(ModbusSessionContext session, ModbusMapping mapping, JsonElement value, long deadlineMs,
                                            AttributeWriteKey coalesceKey) {
        try {
            return encodeAndQueueWrite(session, mapping, value, deadlineMs, coalesceKey);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<byte[]> encodeAndQueueWrite(ModbusSessionContext session, ModbusMapping mapping, JsonElement value, long deadlineMs,
                                                          AttributeWriteKey coalesceKey) {
        var profileConfig = session.getProfileConfig();
        ModbusFunctionCode functionCode = mapping.getFunctionCode();
        int address = mapping.getAddress();
        int unitId = session.getDeviceConfig().getUnitId();

        // encode before queuing, so a bad value fails right away and never reaches the device
        Function<ModbusClientAdapter, CompletionStage<Void>> operation = switch (functionCode) {
            case WRITE_SINGLE_COIL, WRITE_MULTIPLE_COILS -> {
                boolean coil = RegisterCodec.encodeCoil(value);
                yield client -> client.writeCoil(unitId, functionCode, address, coil);
            }
            case WRITE_SINGLE_REGISTER, WRITE_MULTIPLE_REGISTERS -> {
                byte[] registers = RegisterCodec.encodeRegisters(value, mapping, profileConfig.getByteOrder(), profileConfig.getWordOrder());
                yield client -> client.writeRegisters(unitId, functionCode, address, registers);
            }
            case MASK_WRITE_REGISTER -> {
                int[] masks = RegisterCodec.maskFor(RegisterCodec.encodeCoil(value), mapping.getBit());
                yield client -> client.maskWriteRegister(unitId, address, masks[0], masks[1]);
            }
            default -> throw new IllegalArgumentException("Function code " + functionCode.getCode() + " is not a write function code");
        };
        Function<ModbusClientAdapter, CompletionStage<byte[]>> queued = client -> operation.apply(client).thenApply(v -> NO_DATA);
        // a retry carries an older value: it must not replace a newer one that is already waiting
        return withRetries(session,
                (connection, isRetryRun) -> coalesceKey == null
                        ? connection.execute(queued, deadlineMs)
                        : connection.executeLatest(coalesceKey, queued, !isRetryRun),
                retries(session));
    }

    private CompletableFuture<String> read(ModbusSessionContext session, ModbusMapping mapping, long deadlineMs) {
        var profileConfig = session.getProfileConfig();
        int unitId = session.getDeviceConfig().getUnitId();

        ReadBlock block = new ReadBlock(mapping.getFunctionCode(), mapping.getAddress(), mapping.getEffectiveObjectsCount(),
                List.of(new PollItem(mapping, PollItem.Target.TELEMETRY)));

        BiFunction<ModbusConnection, Boolean, CompletableFuture<byte[]>> readOp = (connection, isRetryRun) ->
                connection.execute(client -> client.read(unitId, block.functionCode(), block.address(), block.count()), deadlineMs);

        return withRetries(session, readOp, retries(session))
                .thenApply(data -> {
                    Object value = RegisterCodec.decode(data, block, mapping, profileConfig.getByteOrder(), profileConfig.getWordOrder());
                    JsonObject response = new JsonObject();
                    response.add("value", toJson(value));
                    return response.toString();
                });
    }

    /** Each attempt takes the session's connection at that moment: a restart of polling may replace or clear it in between. */
    private CompletableFuture<byte[]> withRetries(
            ModbusSessionContext session, BiFunction<ModbusConnection, Boolean, CompletableFuture<byte[]>> attempt, int attemptsLeft) {
        boolean isFirstRunRetry = false;
        return withRetries(session, attempt, attemptsLeft, isFirstRunRetry);
    }

    private CompletableFuture<byte[]> withRetries(
            ModbusSessionContext session, BiFunction<ModbusConnection, Boolean, CompletableFuture<byte[]>> attemptOp, int attemptsLeft, boolean isRetryRun) {

        ModbusConnection connection = session.getConnection();
        if (connection == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Device is not connected"));
        }
        CompletableFuture<byte[]> result;
        try {
            result = attemptOp.apply(connection, isRetryRun);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }

        boolean isNextRetry = true;
        return result.exceptionallyCompose(
                error -> attemptsLeft > 0 && unwrap(error) instanceof TimeoutException
                        ? withRetries(session, attemptOp, attemptsLeft - 1, isNextRetry)
                        : CompletableFuture.failedFuture(error)
        );
    }

    private void respondSuccess(ModbusSessionContext session, ToDeviceRpcRequestMsg msg, String payload) {
        if (msg.getOneway()) {
            reportOneWayStatus(session, msg, RpcStatus.DELIVERED);
        } else {
            transportService.process(session.getSessionInfo(), ToDeviceRpcResponseMsg.newBuilder()
                    .setRequestId(msg.getRequestId())
                    .setPayload(payload)
                    .build(), TransportServiceCallback.EMPTY);
        }
    }

    /**
     * Two-way errors go out as {@code {"error": "…"}}: core hands the error text to the REST caller only if it is JSON
     * (plain text ends as an empty 406), and stores it as the response of a persistent RPC marked FAILED.
     */
    private void respondError(ModbusSessionContext session, ToDeviceRpcRequestMsg msg, String error) {
        log.debug("[{}] RPC '{}' failed: {}", session.getDeviceId(), msg.getMethodName(), error);
        if (msg.getOneway()) {
            reportOneWayStatus(session, msg, RpcStatus.FAILED);
        } else {
            transportService.process(session.getSessionInfo(), ToDeviceRpcResponseMsg.newBuilder()
                    .setRequestId(msg.getRequestId())
                    .setError(errorJson(error))
                    .build(), TransportServiceCallback.EMPTY);
        }
    }

    /**
     * A one-way RPC has no response, only a status, and core keeps track of it only when the RPC is persistent:
     * a non-persistent one is forgotten as soon as it is sent, so a status for it would just log a warning there.
     * A delivered write still counts as device activity (a two-way response records activity on its own).
     */
    private void reportOneWayStatus(ModbusSessionContext session, ToDeviceRpcRequestMsg msg, RpcStatus status) {
        boolean delivered = status == RpcStatus.DELIVERED;
        if (msg.getPersisted()) {
            transportService.process(session.getSessionInfo(), msg, status, delivered, TransportServiceCallback.EMPTY);
        } else if (delivered) {
            transportService.recordActivity(session.getSessionInfo());
        }
    }

    static String errorJson(String error) {
        JsonObject body = new JsonObject();
        body.addProperty("error", error);
        return body.toString();
    }

    /** Accepts a bare value ({@code 50.0}, {@code true}) or {@code {"value": …}}, which dashboard widgets send. */
    static JsonElement valueOf(String params) {
        if (StringUtils.isBlank(params)) {
            throw new IllegalArgumentException("Value is required");
        }
        JsonElement parsed = JsonParser.parseString(params);
        if (parsed.isJsonObject()) {
            JsonElement value = parsed.getAsJsonObject().get("value");
            if (value == null || value.isJsonNull()) {
                throw new IllegalArgumentException("Value is required: send it as params or as {\"value\": …}");
            }
            return value;
        }
        if (parsed.isJsonNull()) {
            throw new IllegalArgumentException("Value is required");
        }
        return parsed;
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

    private static long deadlineOf(ToDeviceRpcRequestMsg msg) {
        return msg.getExpirationTime() > 0 ? msg.getExpirationTime() : ModbusConnection.NO_DEADLINE;
    }

    private static int retries(ModbusSessionContext session) {
        Integer retries = session.getProfileConfig().getRetries();
        return retries != null ? retries : 0;
    }

    static String describe(Throwable error) {
        Throwable cause = unwrap(error);
        if (cause instanceof ModbusResponseException mre) {
            String name = ExceptionCode.from(mre.getExceptionCode()).map(Enum::name).orElse("UNKNOWN");
            return String.format("Modbus exception %02d (%s)", mre.getExceptionCode(), name);
        }
        if (cause instanceof TimeoutException) {
            return "Device did not respond in time";
        }
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
    }

    /** One shared attribute of one device: the connection is shared by all devices of its endpoint, so the tag alone is not enough. */
    private record AttributeWriteKey(DeviceId deviceId, String tag) {}

    private static Throwable unwrap(Throwable error) {
        Throwable current = error;
        while ((current instanceof CompletionException || current instanceof ExecutionException) && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

}
