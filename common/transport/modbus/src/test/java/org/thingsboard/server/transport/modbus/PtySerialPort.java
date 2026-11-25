// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus;

import com.digitalpetri.modbus.ModbusRtuFrame;
import com.digitalpetri.modbus.ModbusRtuRequestFrameParser;
import com.digitalpetri.modbus.ModbusRtuRequestFrameParser.Accumulated;
import com.digitalpetri.modbus.ModbusRtuRequestFrameParser.Idle;
import com.digitalpetri.modbus.server.ModbusRequestContext.ModbusRtuRequestContext;
import com.digitalpetri.modbus.server.ModbusRtuServerTransport;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Platform;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * A serial port for tests, without hardware: a pseudo-terminal. Its slave side is linked at the given path and is what
 * the transport opens as a serial port; its master side is the wire, served here as the transport of an RTU server.
 * {@link #bind()} plugs the port in and {@link #unbind()} unplugs it: the path disappears and a port that is open
 * reports a hang-up, as an adapter that is pulled out does. POSIX only.
 */
class PtySerialPort implements ModbusRtuServerTransport {

    private static final int O_RDWR = 2;
    private static final int O_NOCTTY = Platform.isMac() ? 0x20000 : 0x100;
    private static final short POLLIN = 1;
    /** A request that is still incomplete after this long without new bytes is dropped. */
    private static final long FRAME_GAP_MS = 100;

    private final Path path;
    private volatile FrameReceiver<ModbusRtuRequestContext, ModbusRtuFrame> frameReceiver;

    private int master = -1;
    private int slave = -1;
    private Thread reader;
    private volatile boolean running;

    PtySerialPort(Path path) {
        this.path = path;
    }

    @Override
    public synchronized CompletionStage<Void> bind() {
        if (running) {
            return CompletableFuture.completedFuture(null);
        }
        try {
            master = check(LibC.INSTANCE.posix_openpt(O_RDWR | O_NOCTTY), "posix_openpt");
            check(LibC.INSTANCE.grantpt(master), "grantpt");
            check(LibC.INSTANCE.unlockpt(master), "unlockpt");
            String slaveName = LibC.INSTANCE.ptsname(master);
            // held open, so that the master side keeps working while the transport has the port closed
            slave = check(LibC.INSTANCE.open(slaveName, O_RDWR | O_NOCTTY), "open " + slaveName);
            Files.deleteIfExists(path);
            Files.createSymbolicLink(path, Path.of(slaveName));
            running = true;
            reader = new Thread(this::serve, "pty-serial-port");
            reader.setDaemon(true);
            reader.start();
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            closeHandles();
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public synchronized CompletionStage<Void> unbind() {
        if (!running) {
            return CompletableFuture.completedFuture(null);
        }
        running = false;
        try {
            reader.join(5000);
            Files.deleteIfExists(path);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        } finally {
            closeHandles();
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void receive(FrameReceiver<ModbusRtuRequestContext, ModbusRtuFrame> frameReceiver) {
        this.frameReceiver = frameReceiver;
    }

    private void serve() {
        ModbusRtuRequestFrameParser parser = new ModbusRtuRequestFrameParser();
        byte[] buffer = new byte[512];
        Memory pollFd = new Memory(8); // struct pollfd { int fd; short events; short revents; }
        long lastDataAt = System.currentTimeMillis();
        while (running) {
            pollFd.setInt(0, master);
            pollFd.setShort(4, POLLIN);
            pollFd.setShort(6, (short) 0);
            int ready = LibC.INSTANCE.poll(pollFd, 1, 20);
            int received = ready > 0 && (pollFd.getShort(6) & POLLIN) != 0
                    ? LibC.INSTANCE.read(master, buffer, new NativeLong(buffer.length)).intValue() : 0;
            if (received <= 0) {
                if (!(parser.getState() instanceof Idle) && System.currentTimeMillis() - lastDataAt > FRAME_GAP_MS) {
                    parser.reset();
                }
                continue;
            }
            lastDataAt = System.currentTimeMillis();
            if (parser.parse(Arrays.copyOf(buffer, received)) instanceof Accumulated accumulated) {
                parser.reset();
                answer(accumulated.frame());
            }
        }
    }

    private void answer(ModbusRtuFrame request) {
        FrameReceiver<ModbusRtuRequestContext, ModbusRtuFrame> receiver = frameReceiver;
        if (receiver == null) {
            return;
        }
        ModbusRtuFrame response;
        try {
            response = receiver.receive(new ModbusRtuRequestContext() {}, request);
        } catch (Exception e) {
            // a unit that is not on this line does not answer
            return;
        }
        ByteBuffer data = ByteBuffer.allocate(1 + response.pdu().remaining() + response.crc().remaining());
        data.put((byte) response.unitId()).put(response.pdu()).put(response.crc());
        LibC.INSTANCE.write(master, data.array(), new NativeLong(data.capacity()));
    }

    private void closeHandles() {
        if (master >= 0) {
            LibC.INSTANCE.close(master);
            master = -1;
        }
        if (slave >= 0) {
            LibC.INSTANCE.close(slave);
            slave = -1;
        }
    }

    private static int check(int result, String call) {
        if (result < 0) {
            throw new IllegalStateException(call + " failed, errno " + Native.getLastError());
        }
        return result;
    }

    /** The few libc calls a pseudo-terminal needs; Java has none of them. */
    public interface LibC extends Library {

        LibC INSTANCE = Native.load("c", LibC.class);

        int posix_openpt(int flags);

        int grantpt(int fd);

        int unlockpt(int fd);

        String ptsname(int fd);

        int open(String path, int flags);

        int close(int fd);

        int poll(Memory fds, int count, int timeoutMs);

        NativeLong read(int fd, byte[] buffer, NativeLong count);

        NativeLong write(int fd, byte[] buffer, NativeLong count);

    }

}
