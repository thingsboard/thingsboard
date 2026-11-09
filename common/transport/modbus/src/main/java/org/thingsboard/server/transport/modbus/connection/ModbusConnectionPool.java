// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus.connection;

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
import org.thingsboard.server.common.data.transport.modbus.config.ModbusTcpEndpoint;
import org.thingsboard.server.common.data.transport.modbus.config.ModbusTcpFraming;
import org.thingsboard.server.queue.util.TbModbusTransportComponent;
import org.thingsboard.server.transport.modbus.session.ModbusSessionContext;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
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
    @Value("${transport.modbus.connect_timeout_ms:5000}")
    private long connectTimeoutMs;

    private final Map<String, ModbusConnection> connections = new ConcurrentHashMap<>();

    private EventLoopGroup eventLoopGroup;
    private ExecutorService executor;

    @PostConstruct
    public void init() {
        eventLoopGroup = new NioEventLoopGroup(eventLoopThreads, ThingsBoardThreadFactory.forName("modbus-netty"));
        executor = ThingsBoardExecutors.newWorkStealingPool(responseProcessingParallelism, "modbus-response-processing");
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
                throw new IllegalStateException("Modbus connection " + key + " is already used with " + existing.getSettings()
                        + "; device requests " + spec.settings());
            }
            var connection = existing != null ? existing : create(spec, timeout);
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
                .timeout(timeout)
                .clientFactory(spec.clientFactory())
                .build();
    }

    /**
     * Everything that depends on the endpoint type. A serial endpoint is added here as another branch.
     */
    private ConnectionSpec connectionSpec(ModbusEndpoint endpoint) {
        if (endpoint instanceof ModbusTcpEndpoint tcp) {
            // RTU frames carry no transaction id, so a response can only be matched to the single outstanding request
            int maxInFlight = tcp.getFraming() == ModbusTcpFraming.RTU ? 1 : maxInFlightPerEndpoint;
            Duration connectTimeout = Duration.ofMillis(connectTimeoutMs);
            return new ConnectionSpec(tcp.getKey(), "framing=" + tcp.getFraming(), maxInFlight,
                    requestTimeout -> DigitalPetriModbusClientAdapter.builder()
                            .host(tcp.getHost().trim())
                            .port(tcp.getPort())
                            .framing(tcp.getFraming())
                            .requestTimeout(requestTimeout)
                            .connectTimeout(connectTimeout)
                            .eventLoopGroup(eventLoopGroup)
                            .executor(executor)
                            .build());
        }
        throw new IllegalArgumentException("Unsupported Modbus endpoint type: " + endpoint.getType());
    }

    private record ConnectionSpec(String key, String settings, int maxInFlight,
                                  Function<Duration, ModbusClientAdapter> clientFactory) {}

}
