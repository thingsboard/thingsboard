// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus.connection;

import com.fazecast.jSerialComm.SerialPort;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.transport.modbus.config.ModbusEndpoint;
import org.thingsboard.server.common.data.transport.modbus.config.ModbusRs485Settings;
import org.thingsboard.server.common.data.transport.modbus.config.ModbusSerialEndpoint;
import org.thingsboard.server.common.data.transport.modbus.config.ModbusSerialParity;
import org.thingsboard.server.common.data.transport.modbus.config.ModbusSerialStopBits;
import org.thingsboard.server.common.data.transport.modbus.config.ModbusTcpEndpoint;
import org.thingsboard.server.common.data.transport.modbus.config.ModbusTcpFraming;
import org.thingsboard.server.queue.util.TbModbusTransportComponent;
import org.thingsboard.server.transport.modbus.session.ModbusSessionContext;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

/**
 * Shares one {@link ModbusConnection} per endpoint key ({@code tcp://host:port}) between sessions.
 */
@Slf4j
@TbModbusTransportComponent
@Component
public class ModbusConnectionPool {

    @Value("${transport.modbus.netty.event_loop_threads:2}")
    private int eventLoopThreads;
    @Value("${transport.modbus.response_processing.parallelism_level:4}")
    private int responseProcessingParallelism;
    @Value("${transport.modbus.max_in_flight_per_endpoint:1}")
    private int maxInFlightPerEndpoint;
    @Value("${transport.modbus.max_queue_size_per_endpoint:1000}")
    private int maxQueueSizePerEndpoint;
    @Value("${transport.modbus.connect_timeout_ms:5000}")
    private long connectTimeoutMs;
    @Value("${transport.modbus.serial.max_reconnect_delay_ms:5000}")
    private long serialMaxReconnectDelayMs;

    private final Map<String, ModbusConnection> connections = new ConcurrentHashMap<>();

    private EventLoopGroup eventLoopGroup;
    private ExecutorService executor;
    private ScheduledExecutorService reconnectScheduler;

    @PostConstruct
    public void init() {
        eventLoopGroup = new NioEventLoopGroup(eventLoopThreads, ThingsBoardThreadFactory.forName("modbus-netty"));
        executor = ThingsBoardExecutors.newWorkStealingPool(responseProcessingParallelism, "modbus-response-processing");
        reconnectScheduler = ThingsBoardExecutors.newSingleThreadScheduledExecutor("modbus-reconnect");
    }

    @PreDestroy
    public void destroy() {
        connections.values().forEach(ModbusConnection::close);
        connections.clear();
        if (eventLoopGroup != null) {
            eventLoopGroup.shutdownGracefully();
        }
        if (executor != null) {
            executor.shutdownNow();
        }
        if (reconnectScheduler != null) {
            reconnectScheduler.shutdownNow();
        }
    }

    /**
     * Attaches the session to the connection for its device's endpoint, creating and connecting it on first use.
     *
     * @throws IllegalStateException if a connection with the same key is already open with different settings
     */
    public ModbusConnection acquire(ModbusSessionContext session) {
        ConnectionSpec spec = connectionSpec(session.getDeviceConfig().getEndpoint());
        Duration timeout = Duration.ofMillis(session.getEffectiveTimeoutMs());

        return connections.compute(spec.key(), (key, existing) -> {
            if (existing != null && !existing.getSettings().equals(spec.settings())) {
                throw new IllegalStateException(
                        "Modbus connection " + key + " is already used with " + existing.getSettings() + "; device requests " + spec.settings());
            }
            var connection = existing != null
                    ? existing
                    : create(spec, timeout);

            connection.attach(session.getSessionId());
            connection.ensureTimeout(timeout);
            return connection;
        });
    }

    public void release(ModbusSessionContext session, ModbusConnection connection) {
        connections.computeIfPresent(connection.getKey(), (key, current) -> {
            if (current != connection) {
                return current;
            }
            if (current.detach(session.getSessionId())) {
                current.close();
                return null;
            }
            return current;
        });
    }

    public int getConnectionsCount() {
        return connections.size();
    }

    private ModbusConnection create(ConnectionSpec spec, Duration timeout) {
        log.info("[{}] Opening Modbus connection ({})", spec.key(), spec.settings());
        return ModbusConnection.builder()
                .key(spec.key())
                .settings(spec.settings())
                .maxInFlight(spec.maxInFlight())
                .maxQueueSize(maxQueueSizePerEndpoint)
                .timeout(timeout)
                .clientFactory(spec.clientFactory())
                .build();
    }

    /**
     * Everything that depends on the endpoint type. A serial endpoint is added here as another branch.
     */
    private ConnectionSpec connectionSpec(ModbusEndpoint endpoint) {
        int singleInFlight = 1;

        if (endpoint instanceof ModbusTcpEndpoint tcp) {
            // RTU frames carry no transaction id, so a response can only be matched to the single outstanding request
            int maxInFlight = tcp.getFraming() == ModbusTcpFraming.RTU ? singleInFlight : maxInFlightPerEndpoint;
            Duration connectTimeout = Duration.ofMillis(connectTimeoutMs);
            return new ConnectionSpec(tcp.getKey(), tcp.getConnectionSettings(), maxInFlight,
                    requestTimeout -> DigitalPetriModbusClientAdapter.tcp()
                            .host(tcp.getHost().trim())
                            .port(tcp.getPort())
                            .framing(tcp.getFraming())
                            .requestTimeout(requestTimeout)
                            .connectTimeout(connectTimeout)
                            .eventLoopGroup(eventLoopGroup)
                            .executor(executor)
                            .build());
        } else if (endpoint instanceof ModbusSerialEndpoint serial) {
            return new ConnectionSpec(serial.getKey(), serial.getConnectionSettings(), singleInFlight, modbusClientAdapterProvider(serial));
        }
        throw new IllegalArgumentException("Unsupported Modbus endpoint type: " + endpoint.getType());
    }

    private Function<Duration, ModbusClientAdapter> modbusClientAdapterProvider(ModbusSerialEndpoint serial) {
        return requestTimeout -> {
            var builder = DigitalPetriModbusClientAdapter.serial()
                    .serialPort(serial.getPort())
                    .baudRate(serial.getBaudRate())
                    .parity(getSerialParity(serial.getParity()))
                    .dataBits(ModbusSerialEndpoint.DATA_BITS)
                    .stopBits(getSerialStopBits(serial.getStopBits()))
                    .requestTimeout(requestTimeout)
                    .executor(executor)
                    .scheduler(reconnectScheduler);

            ModbusRs485Settings rs485 = serial.getRs485();
            if (rs485 != null) {
                builder.rs485Mode(true)
                        .rs485RtsActiveHigh(rs485.isRtsActiveHigh())
                        .rs485Termination(rs485.isTermination())
                        .rs485DuringTx(rs485.isRxDuringTx())
                        // the serial library takes microseconds
                        .rs485DelayBefore(rs485.getDelayBeforeMs() * 1000)
                        .rs485DelayAfter(rs485.getDelayAfterMs() * 1000);
            }
            // the library's serial transport opens the port once and never again, unlike our TCP transports
            return new ModbusClientRetryDecorator(serial.getKey(), builder.build(), reconnectScheduler, serialMaxReconnectDelayMs);
        };
    }

    private int getSerialParity(ModbusSerialParity parity) {
        return switch (parity) {
            case NONE -> SerialPort.NO_PARITY;
            case ODD  -> SerialPort.ODD_PARITY;
            case EVEN -> SerialPort.EVEN_PARITY;
        };
    }

    private int getSerialStopBits(ModbusSerialStopBits stopBits) {
        return switch (stopBits) {
            case ONE -> SerialPort.ONE_STOP_BIT;
            case TWO -> SerialPort.TWO_STOP_BITS;
        };
    }

    private record ConnectionSpec(String key, String settings, int maxInFlight,
                                  Function<Duration, ModbusClientAdapter> clientFactory) {}

}
