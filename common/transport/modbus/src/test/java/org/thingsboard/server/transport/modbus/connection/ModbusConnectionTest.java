// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus.connection;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.transport.modbus.ModbusFunctionCode;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class ModbusConnectionTest {

    @Test
    void keepsAtMostMaxInFlightRequestsOutstanding() {
        FakeClient client = new FakeClient();
        ModbusConnection connection = new ModbusConnection("tcp://h:502", "framing=MBAP", 2, 100, Duration.ofSeconds(1), t -> client);

        var r1 = read(connection);
        var r2 = read(connection);
        var r3 = read(connection);
        assertThat(client.pending).hasSize(2);

        client.pending.get(0).complete(new byte[]{1, 2});
        assertThat(r1.join()).containsExactly(1, 2);
        assertThat(client.pending).hasSize(3); // third request dispatched once a slot was free
        assertThat(r3).isNotDone();
        assertThat(r2).isNotDone();
    }

    @Test
    void singleInFlightQueuesTheRestUntilCompletion() {
        FakeClient client = new FakeClient();
        ModbusConnection connection = new ModbusConnection("tcp://h:502", "framing=RTU", 1, 100, Duration.ofSeconds(1), t -> client);

        read(connection);
        read(connection);
        assertThat(client.pending).hasSize(1);
        client.pending.get(0).completeExceptionally(new RuntimeException("boom"));
        assertThat(client.pending).hasSize(2); // a failure frees the slot too
    }

    @Test
    void closeFailsQueuedRequestsAndDisconnects() {
        FakeClient client = new FakeClient();
        ModbusConnection connection = new ModbusConnection("tcp://h:502", "framing=MBAP", 1, 100, Duration.ofSeconds(1), t -> client);
        read(connection);
        var queued = read(connection);

        connection.close();

        assertThat(queued).isCompletedExceptionally();
        assertThat(client.disconnected).isTrue();
        assertThat(read(connection)).isCompletedExceptionally();
    }

    @Test
    void growingTimeoutRebuildsClient() {
        List<Duration> created = new ArrayList<>();
        ModbusConnection connection = new ModbusConnection("tcp://h:502", "framing=MBAP", 1, 100, Duration.ofSeconds(1), t -> {
            created.add(t);
            return new FakeClient();
        });

        connection.ensureTimeout(Duration.ofMillis(500));
        connection.ensureTimeout(Duration.ofSeconds(3));

        assertThat(created).containsExactly(Duration.ofSeconds(1), Duration.ofSeconds(3));
        assertThat(connection.getTimeout()).isEqualTo(Duration.ofSeconds(3));
    }

    @Test
    void detachReportsWhenUnused() {
        ModbusConnection connection = new ModbusConnection("tcp://h:502", "framing=MBAP", 1, 100, Duration.ofSeconds(1), t -> new FakeClient());
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        connection.attach(a);
        connection.attach(b);

        assertThat(connection.detach(a)).isFalse();
        assertThat(connection.detach(b)).isTrue();
    }

    @Test
    void writesWaitBehindQueuedReads() {
        FakeClient client = new FakeClient();
        ModbusConnection connection = new ModbusConnection("tcp://h:502", "framing=MBAP", 1, 100, Duration.ofSeconds(1), t -> client);
        read(connection);
        var write = connection.execute(c -> c.writeRegisters(1, ModbusFunctionCode.WRITE_SINGLE_REGISTER, 20, new byte[]{0, 1})
                .thenApply(v -> new byte[0]), ModbusConnection.NO_DEADLINE);

        assertThat(client.writes).isEmpty();
        client.pending.get(0).complete(new byte[]{0, 0});
        assertThat(client.writes).containsExactly("register@20");
        assertThat(write).isCompleted();
    }

    @Test
    void expiredRequestIsDroppedWithoutReachingTheDevice() {
        FakeClient client = new FakeClient();
        ModbusConnection connection = new ModbusConnection("tcp://h:502", "framing=MBAP", 1, 100, Duration.ofSeconds(1), t -> client);
        read(connection);
        var expired = connection.execute(c -> c.writeCoil(1, ModbusFunctionCode.WRITE_SINGLE_COIL, 1, true)
                .thenApply(v -> new byte[0]), System.currentTimeMillis() - 1);

        client.pending.get(0).complete(new byte[]{0, 0});

        assertThat(expired).isCompletedExceptionally();
        assertThat(client.writes).isEmpty();
    }

    @Test
    void requestsBeyondQueueLimitFailAtOnce() {
        FakeClient client = new FakeClient();
        ModbusConnection connection = new ModbusConnection("tcp://h:502", "framing=MBAP", 1, 2, Duration.ofSeconds(1), t -> client);
        read(connection); // in flight, not counted as waiting
        var second = read(connection);
        var third = read(connection);

        var rejected = read(connection);

        assertThat(rejected).failsWithin(Duration.ZERO).withThrowableOfType(ExecutionException.class)
                .withMessageContaining("is overloaded: 2 requests are already waiting");
        assertThat(second).isNotDone();
        assertThat(third).isNotDone();

        client.pending.get(0).complete(new byte[]{0, 0}); // frees a place in the queue
        assertThat(read(connection)).isNotCompletedExceptionally();
    }

    @Test
    void longRunOfExpiredRequestsIsDrainedWithoutNesting() {
        assertQueueOfImmediatelyFailingRequestsIsDrained(c -> c.read(1, ModbusFunctionCode.READ_HOLDING_REGISTERS, 0, 1), System.currentTimeMillis() - 1);
    }

    @Test
    void longRunOfThrowingOperationsIsDrainedWithoutNesting() {
        assertQueueOfImmediatelyFailingRequestsIsDrained(c -> {
            throw new IllegalArgumentException("bad request");
        }, ModbusConnection.NO_DEADLINE);
    }

    /** Each of these requests completes inside the dispatch that picked it; handling them recursively would overflow the stack. */
    private static void assertQueueOfImmediatelyFailingRequestsIsDrained(
            Function<ModbusClientAdapter, CompletionStage<byte[]>> operation, long deadlineMs) {
        int count = 50_000;
        FakeClient client = new FakeClient();
        ModbusConnection connection = new ModbusConnection("tcp://h:502", "framing=MBAP", 1, count, Duration.ofSeconds(1), t -> client);
        read(connection); // holds the only slot while the rest queue up
        List<CompletableFuture<byte[]>> failing = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            failing.add(connection.execute(operation, deadlineMs));
        }

        client.pending.get(0).complete(new byte[]{0, 0});

        assertThat(failing).allMatch(CompletableFuture::isCompletedExceptionally);
        read(connection);
        assertThat(client.pending).hasSize(2); // the slot is free again: the next request reaches the device
    }

    private static CompletableFuture<byte[]> read(ModbusConnection connection) {
        return connection.read(1, ModbusFunctionCode.READ_HOLDING_REGISTERS, 0, 1);
    }

    private static class FakeClient implements ModbusClientAdapter {

        final List<CompletableFuture<byte[]>> pending = new ArrayList<>();
        final List<String> writes = new ArrayList<>();
        boolean disconnected;

        @Override
        public CompletionStage<Void> connect() {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<Void> disconnect() {
            disconnected = true;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<byte[]> read(int unitId, ModbusFunctionCode functionCode, int address, int count) {
            CompletableFuture<byte[]> future = new CompletableFuture<>();
            pending.add(future);
            return future;
        }

        @Override
        public CompletionStage<Void> writeCoil(int unitId, ModbusFunctionCode functionCode, int address, boolean value) {
            writes.add("coil@" + address);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<Void> writeRegisters(int unitId, ModbusFunctionCode functionCode, int address, byte[] registerBytes) {
            writes.add("register@" + address);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<Void> maskWriteRegister(int unitId, int address, int andMask, int orMask) {
            writes.add("mask@" + address);
            return CompletableFuture.completedFuture(null);
        }

    }

}
