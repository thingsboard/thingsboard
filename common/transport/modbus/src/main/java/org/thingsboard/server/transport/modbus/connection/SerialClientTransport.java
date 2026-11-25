// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus.connection;

import com.digitalpetri.modbus.ModbusRtuFrame;
import com.digitalpetri.modbus.ModbusRtuResponseFrameParser;
import com.digitalpetri.modbus.ModbusRtuResponseFrameParser.Accumulated;
import com.digitalpetri.modbus.ModbusRtuResponseFrameParser.ParseError;
import com.digitalpetri.modbus.client.ModbusRtuClientTransport;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.ThingsBoardThreadFactory;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * RTU frames over a serial port. Written instead of the library's serial transport, which repeats a write for as long
 * as the port accepts no bytes and so never returns once the port is gone. Here a frame is written in one call, on the
 * executor and never on the caller's thread: the native write has no time limit of its own and repeats for as long as
 * the port is open but takes no bytes. A write that is not over after {@code writeTimeout} (at most
 * {@link #MAX_WRITE_TIMEOUT}), a frame that was not
 * written completely, or a port reported as disconnected closes the port and fails the request, and
 * {@link ModbusClientRetryDecorator} opens the port again. Closing the port is also what ends a write that is stuck.
 */
@Slf4j
class SerialClientTransport implements ModbusRtuClientTransport {

    /**
     * Upper limit for writing one frame. A frame is at most 256 bytes, about 270 ms at 9600 baud. The limit stays below
     * the usual request timeout on purpose: a stuck write must fail as a write, before the request times out, because
     * a timeout is not treated as a lost connection.
     */
    static final Duration MAX_WRITE_TIMEOUT = Duration.ofSeconds(1);

    private final ModbusRtuResponseFrameParser frameParser = new ModbusRtuResponseFrameParser();

    private final String portName;
    private final int baudRate;
    private final int dataBits;
    private final int stopBits;
    private final int parity;
    private final boolean rs485Mode;
    private final boolean rs485RtsActiveHigh;
    private final boolean rs485Termination;
    private final boolean rs485RxDuringTx;
    private final int rs485DelayBefore;
    private final int rs485DelayAfter;
    private final Duration writeTimeout;
    private final Executor executor;
    private final ScheduledExecutorService scheduler;

    private volatile SerialPort port;
    private volatile Consumer<ModbusRtuFrame> frameReceiver;

    @Builder
    SerialClientTransport(String portName, int baudRate, int dataBits, int stopBits, int parity, boolean rs485Mode,
                          boolean rs485RtsActiveHigh, boolean rs485Termination, boolean rs485RxDuringTx,
                          int rs485DelayBefore, int rs485DelayAfter, Duration writeTimeout, Executor executor,
                          ScheduledExecutorService scheduler) {
        this.portName = portName;
        this.baudRate = baudRate;
        this.dataBits = dataBits;
        this.stopBits = stopBits;
        this.parity = parity;
        this.rs485Mode = rs485Mode;
        this.rs485RtsActiveHigh = rs485RtsActiveHigh;
        this.rs485Termination = rs485Termination;
        this.rs485RxDuringTx = rs485RxDuringTx;
        this.rs485DelayBefore = rs485DelayBefore;
        this.rs485DelayAfter = rs485DelayAfter;
        this.writeTimeout = writeTimeout.compareTo(MAX_WRITE_TIMEOUT) < 0 ? writeTimeout : MAX_WRITE_TIMEOUT;
        this.executor = executor;
        this.scheduler = scheduler;
    }

    @Override
    public synchronized CompletionStage<Void> connect() {
        if (isConnected()) {
            return CompletableFuture.completedFuture(null);
        }
        try {
            SerialPort serialPort = SerialPort.getCommPort(portName);
            serialPort.setComPortParameters(baudRate, dataBits, stopBits, parity, rs485Mode);
            if (rs485Mode) {
                serialPort.setRs485ModeParameters(true, rs485RtsActiveHigh, rs485Termination, rs485RxDuringTx, rs485DelayBefore, rs485DelayAfter);
            }
            // limits a write on Windows only; elsewhere the limit is the deadline in send()
            serialPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, (int) writeTimeout.toMillis());
            if (!serialPort.openPort()) {
                return CompletableFuture.failedFuture(new IllegalStateException(
                        "Failed to open serial port " + portName + ", error code " + serialPort.getLastErrorCode()));
            }
            frameParser.reset();
            serialPort.addDataListener(new PortListener(serialPort));
            port = serialPort;
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(new IllegalStateException("Failed to open serial port " + portName + ": " + e.getMessage(), e));
        }
    }

    @Override
    public CompletionStage<Void> disconnect() {
        close(port);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean isConnected() {
        SerialPort serialPort = port;
        return serialPort != null && serialPort.isOpen();
    }

    @Override
    public CompletionStage<Void> send(ModbusRtuFrame frame) {
        SerialPort serialPort = port;
        if (serialPort == null || !serialPort.isOpen()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Not connected to serial port " + portName));
        }
        ByteBuffer buffer = ByteBuffer.allocate(1 + frame.pdu().remaining() + frame.crc().remaining());
        buffer.put((byte) frame.unitId());
        buffer.put(frame.pdu());
        buffer.put(frame.crc());
        byte[] data = buffer.array();

        CompletableFuture<Void> result = new CompletableFuture<>();
        long timeoutMs = writeTimeout.toMillis();
        ScheduledFuture<?> deadline;
        try {
            // not on the executor: the write it has to end may hold the last free thread there
            deadline = scheduler.schedule(() -> {
                if (result.completeExceptionally(new IllegalStateException(
                        "Write to serial port " + portName + " did not complete in " + timeoutMs + " ms"))) {
                    closeInBackground(serialPort);
                }
            }, timeoutMs, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            result.completeExceptionally(stopped(e));
            return result;
        }
        try {
            executor.execute(() -> {
                try {
                    write(serialPort, data, result);
                } catch (Throwable e) {
                    result.completeExceptionally(e);
                } finally {
                    deadline.cancel(false);
                }
            });
        } catch (RejectedExecutionException e) {
            deadline.cancel(false);
            result.completeExceptionally(stopped(e));
        }
        return result;
    }

    private IllegalStateException stopped(RejectedExecutionException e) {
        return new IllegalStateException("Failed to write to serial port " + portName + ": transport is stopped", e);
    }

    private void write(SerialPort serialPort, byte[] data, CompletableFuture<Void> result) {
        int written = serialPort.writeBytes(data, data.length);
        if (written == data.length) {
            result.complete(null);
            return;
        }
        int errorCode = serialPort.getLastErrorCode();
        // already failed if the deadline closed the port, which is what made this write return
        if (result.completeExceptionally(new IllegalStateException("Failed to write to serial port " + portName
                + ": " + Math.max(written, 0) + " of " + data.length + " bytes written, error code " + errorCode))) {
            close(serialPort);
        }
    }

    @Override
    public void receive(Consumer<ModbusRtuFrame> frameReceiver) {
        this.frameReceiver = frameReceiver;
    }

    @Override
    public void resetFrameParser() {
        frameParser.reset();
    }

    /** Closes the given port; it stops being the current one unless a newer port has already replaced it. */
    private void close(SerialPort serialPort) {
        if (serialPort == null) {
            return;
        }
        synchronized (this) {
            if (port == serialPort) {
                port = null;
            }
            frameParser.reset();
        }
        // outside the lock: closing a port whose write is stuck can take time and must not hold up connect()
        serialPort.removeDataListener();
        if (serialPort.isOpen() && !serialPort.closePort()) {
            log.debug("[{}] Failed to close serial port, error code {}", portName, serialPort.getLastErrorCode());
        }
    }

    /**
     * On a thread of its own, for the two callers that must not wait for the close: the scheduler, which all
     * deadlines share, and the port's event thread, which the close itself waits for. Both cases are rare.
     */
    private void closeInBackground(SerialPort serialPort) {
        ThingsBoardThreadFactory.forName("modbus-serial-close").newThread(() -> close(serialPort)).start();
    }

    private class PortListener implements SerialPortDataListener {

        private final SerialPort serialPort;

        PortListener(SerialPort serialPort) {
            this.serialPort = serialPort;
        }

        @Override
        public int getListeningEvents() {
            return SerialPort.LISTENING_EVENT_DATA_RECEIVED | SerialPort.LISTENING_EVENT_PORT_DISCONNECTED;
        }

        @Override
        public void serialEvent(SerialPortEvent event) {
            if ((event.getEventType() & SerialPort.LISTENING_EVENT_PORT_DISCONNECTED) != 0) {
                log.debug("[{}] Serial port is disconnected", portName);
                closeInBackground(serialPort);
                return;
            }
            byte[] data = event.getReceivedData();
            if (data == null || data.length == 0) {
                return;
            }
            var state = frameParser.parse(data);
            if (state instanceof Accumulated accumulated) {
                frameParser.reset();
                Consumer<ModbusRtuFrame> receiver = frameReceiver;
                if (receiver != null) {
                    executor.execute(() -> receiver.accept(accumulated.frame()));
                }
            } else if (state instanceof ParseError error) {
                log.debug("[{}] Invalid RTU response: {}", portName, error.error());
                frameParser.reset();
            }
        }
    }

}
