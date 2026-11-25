// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus.connection;

import com.digitalpetri.modbus.ModbusRtuFrame;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Timeout(30)
class SerialClientTransportTest {

    private static final String PORT_NAME = "ttyTest";
    private static final long WRITE_TIMEOUT_MS = 200;

    private MockedStatic<SerialPort> serialPorts;
    private ExecutorService executor;
    private ScheduledExecutorService scheduler;
    private SerialClientTransport transport;

    @BeforeEach
    void setUp() {
        serialPorts = mockStatic(SerialPort.class);
        executor = Executors.newFixedThreadPool(2);
        scheduler = Executors.newSingleThreadScheduledExecutor();
        transport = SerialClientTransport.builder()
                .portName(PORT_NAME).baudRate(9600).dataBits(8).stopBits(SerialPort.ONE_STOP_BIT).parity(SerialPort.NO_PARITY)
                .writeTimeout(Duration.ofMillis(WRITE_TIMEOUT_MS)).executor(executor).scheduler(scheduler).build();
    }

    @AfterEach
    void tearDown() {
        transport.disconnect();
        executor.shutdownNow();
        scheduler.shutdownNow();
        serialPorts.close();
    }

    @Test
    void writesTheFrameOffTheCallersThread() throws Exception {
        Port port = connect(new Port());

        send().get(5, TimeUnit.SECONDS);

        assertThat(port.writer.get()).isNotNull().isNotSameAs(Thread.currentThread());
        verify(port.mock).writeBytes(new byte[]{1, 4, 0, 0, 0, 2, 0x71, (byte) 0xCB}, 8);
        assertThat(transport.isConnected()).isTrue();
        verify(port.mock, never()).closePort();
    }

    @Test
    void failsTheRequestAndClosesThePortWhenTheFrameIsNotWrittenCompletely() {
        Port port = connect(new Port());
        when(port.mock.writeBytes(any(), anyInt())).thenReturn(0);
        when(port.mock.getLastErrorCode()).thenReturn(5);

        assertThatThrownBy(() -> send().get(5, TimeUnit.SECONDS))
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("0 of 8 bytes written, error code 5");

        assertThat(transport.isConnected()).isFalse();
        verify(port.mock, times(1)).closePort();
    }

    @Test
    void failsTheRequestAtTheDeadlineWhenTheWriteDoesNotReturn() {
        connect(new Port().writeBlocksUntilClosed());

        long started = System.nanoTime();
        assertThatThrownBy(() -> send().get(5, TimeUnit.SECONDS))
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("did not complete in " + WRITE_TIMEOUT_MS + " ms");

        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)).isBetween(WRITE_TIMEOUT_MS - 20, 3000L);
    }

    @Test
    void theDeadlineClosesThePortOnceAndThatEndsTheWrite() throws Exception {
        Port port = connect(new Port().writeBlocksUntilClosed());

        assertThatThrownBy(() -> send().get(5, TimeUnit.SECONDS)).hasCauseInstanceOf(IllegalStateException.class);

        // the write returns only because the port was closed, and it must not close the port again
        assertThat(port.writeReturned.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(transport.isConnected()).isFalse();
        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        verify(port.mock, times(1)).closePort();
    }

    @Test
    void capsTheWriteTimeout() {
        transport = SerialClientTransport.builder().portName(PORT_NAME)
                .writeTimeout(Duration.ofSeconds(30)).executor(executor).scheduler(scheduler).build();
        connect(new Port().writeBlocksUntilClosed());

        assertThatThrownBy(() -> send().get(5, TimeUnit.SECONDS))
                .hasMessageContaining("did not complete in " + SerialClientTransport.MAX_WRITE_TIMEOUT.toMillis() + " ms");
    }

    @Test
    void aLateCloseOfAnOldPortDoesNotAffectANewerPort() {
        Port oldPort = connect(new Port().writeBlocksUntilClosed());
        SerialPortDataListener oldListener = oldPort.listener();
        assertThatThrownBy(() -> send().get(5, TimeUnit.SECONDS)).hasCauseInstanceOf(IllegalStateException.class);
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> verify(oldPort.mock, times(1)).removeDataListener());

        Port newPort = connect(new Port());
        // the old port is reported as disconnected only now: a second close of it
        oldListener.serialEvent(new SerialPortEvent(oldPort.mock, SerialPort.LISTENING_EVENT_PORT_DISCONNECTED));
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> verify(oldPort.mock, times(2)).removeDataListener());

        assertThat(transport.isConnected()).isTrue();
        verify(newPort.mock, never()).closePort();
        verify(newPort.mock, never()).removeDataListener();
    }

    @Test
    void closesThePortWhenItIsReportedAsDisconnected() {
        Port port = connect(new Port());

        port.listener().serialEvent(new SerialPortEvent(port.mock, SerialPort.LISTENING_EVENT_PORT_DISCONNECTED));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> verify(port.mock, times(1)).closePort());
        assertThat(transport.isConnected()).isFalse();
        assertThatThrownBy(() -> send().get(5, TimeUnit.SECONDS))
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Not connected to serial port " + PORT_NAME);
        verify(port.mock, never()).writeBytes(any(), anyInt());
    }

    private Port connect(Port port) {
        serialPorts.when(() -> SerialPort.getCommPort(PORT_NAME)).thenReturn(port.mock);
        transport.connect().toCompletableFuture().join();
        assertThat(transport.isConnected()).isTrue();
        return port;
    }

    private CompletableFuture<Void> send() {
        return transport.send(new ModbusRtuFrame(1, ByteBuffer.wrap(new byte[]{4, 0, 0, 0, 2}),
                ByteBuffer.wrap(new byte[]{0x71, (byte) 0xCB}))).toCompletableFuture();
    }

    /** A mocked jSerialComm port that keeps track of being open, as the transport relies on {@code isOpen()}. */
    private static class Port {

        final SerialPort mock = mock(SerialPort.class);
        final AtomicBoolean open = new AtomicBoolean();
        final CountDownLatch closed = new CountDownLatch(1);
        final CountDownLatch writeReturned = new CountDownLatch(1);
        final AtomicReference<Thread> writer = new AtomicReference<>();
        volatile boolean writeBlocksUntilClosed;

        Port() {
            when(mock.openPort()).thenAnswer(invocation -> {
                open.set(true);
                return true;
            });
            when(mock.isOpen()).thenAnswer(invocation -> open.get());
            when(mock.closePort()).thenAnswer(invocation -> {
                open.set(false);
                closed.countDown();
                return true;
            });
            when(mock.writeBytes(any(), anyInt())).thenAnswer(invocation -> {
                writer.set(Thread.currentThread());
                try {
                    if (writeBlocksUntilClosed) {
                        closed.await();
                        // what jSerialComm returns for a port that is closed
                        return -1;
                    }
                    return invocation.<Integer>getArgument(1);
                } finally {
                    writeReturned.countDown();
                }
            });
        }

        Port writeBlocksUntilClosed() {
            writeBlocksUntilClosed = true;
            return this;
        }

        SerialPortDataListener listener() {
            ArgumentCaptor<SerialPortDataListener> listener = ArgumentCaptor.forClass(SerialPortDataListener.class);
            verify(mock).addDataListener(listener.capture());
            return listener.getValue();
        }

    }

}
