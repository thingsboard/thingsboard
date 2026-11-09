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

import static org.assertj.core.api.Assertions.assertThat;

class ModbusConnectionTest {

    @Test
    void keepsAtMostMaxInFlightRequestsOutstanding() {
        FakeClient client = new FakeClient();
        ModbusConnection connection = new ModbusConnection("tcp://h:502", "framing=MBAP", 2, Duration.ofSeconds(1), t -> client);

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
        ModbusConnection connection = new ModbusConnection("tcp://h:502", "framing=RTU", 1, Duration.ofSeconds(1), t -> client);

        read(connection);
        read(connection);
        assertThat(client.pending).hasSize(1);
        client.pending.get(0).completeExceptionally(new RuntimeException("boom"));
        assertThat(client.pending).hasSize(2); // a failure frees the slot too
    }

    @Test
    void closeFailsQueuedRequestsAndDisconnects() {
        FakeClient client = new FakeClient();
        ModbusConnection connection = new ModbusConnection("tcp://h:502", "framing=MBAP", 1, Duration.ofSeconds(1), t -> client);
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
        ModbusConnection connection = new ModbusConnection("tcp://h:502", "framing=MBAP", 1, Duration.ofSeconds(1), t -> {
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
        ModbusConnection connection = new ModbusConnection("tcp://h:502", "framing=MBAP", 1, Duration.ofSeconds(1), t -> new FakeClient());
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        connection.attach(a);
        connection.attach(b);

        assertThat(connection.detach(a)).isFalse();
        assertThat(connection.detach(b)).isTrue();
    }

    private static CompletableFuture<byte[]> read(ModbusConnection connection) {
        return connection.read(1, ModbusFunctionCode.READ_HOLDING_REGISTERS, 0, 1);
    }

    private static class FakeClient implements ModbusClientAdapter {

        final List<CompletableFuture<byte[]>> pending = new ArrayList<>();
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

    }

}
