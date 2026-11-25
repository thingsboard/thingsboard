// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.modbus.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.TbTransportService;
import org.thingsboard.server.common.data.transport.modbus.ModbusMapping;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.queue.util.TbModbusTransportComponent;
import org.thingsboard.server.transport.modbus.connection.ModbusConnection;
import org.thingsboard.server.transport.modbus.connection.ModbusConnectionPool;
import org.thingsboard.server.transport.modbus.poll.ModbusPollLoop;
import org.thingsboard.server.transport.modbus.poll.PollItem;
import org.thingsboard.server.transport.modbus.poll.ReadBlock;
import org.thingsboard.server.transport.modbus.poll.ReadPlanner;
import org.thingsboard.server.transport.modbus.session.ModbusSessionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Network side of the Modbus transport. Being a {@link TbTransportService} named {@code MODBUS} is what advertises
 * this node as a Modbus node in service discovery, so the balancer can split devices between Modbus nodes.
 */
@Slf4j
@TbModbusTransportComponent
@Service
@RequiredArgsConstructor
public class ModbusTransportService implements TbTransportService {

    private final ModbusConnectionPool connectionPool;
    private final TransportService transportService;

    @Value("${transport.modbus.scheduler_thread_pool_size:4}")
    private int schedulerThreadPoolSize;
    @Value("${transport.modbus.block_merge_max_gap:8}")
    private int blockMergeMaxGap;
    @Value("${transport.modbus.min_poll_period_ms:100}")
    private long minPollPeriodMs;

    private ScheduledExecutorService scheduler;

    @PostConstruct
    public void init() {
        scheduler = ThingsBoardExecutors.newScheduledThreadPool(schedulerThreadPoolSize, "modbus-polling");
    }

    @PreDestroy
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    @Override
    public String getName() {
        return DataConstants.MODBUS_TRANSPORT_NAME;
    }

    /**
     * Attaches the session to its connection and starts its poll loops: one loop when timeseries and attributes share a
     * period (their reads are merged), two loops otherwise.
     */
    public void startPolling(ModbusSessionContext session) {
        var profileConfig = session.getProfileConfig();
        if (profileConfig == null) {
            throw new IllegalStateException("Modbus profile configuration is missing");
        }
        ModbusConnection connection = connectionPool.acquire(session);
        try {
            session.setConnection(connection);
            List<PollItem> timeseries = items(profileConfig.getTimeseries(), PollItem.Target.TELEMETRY);
            List<PollItem> attributes = items(profileConfig.getAttributes(), PollItem.Target.ATTRIBUTE);
            long timeseriesPeriod = clamp(session.getEffectiveTimeseriesPeriodMs());
            long attributesPeriod = clamp(session.getEffectiveAttributesPeriodMs());

            if (timeseriesPeriod == attributesPeriod) {
                List<PollItem> all = new ArrayList<>(timeseries);
                all.addAll(attributes);
                startLoop(session, connection, all, timeseriesPeriod);
            } else {
                startLoop(session, connection, timeseries, timeseriesPeriod);
                startLoop(session, connection, attributes, attributesPeriod);
            }
            log.debug("[{}] Started {} poll loop(s) via {}", session.getDeviceId(), session.getPollLoops().size(), connection.getKey());
        } catch (RuntimeException e) {
            stopPolling(session);
            throw e;
        }
    }

    public void stopPolling(ModbusSessionContext session) {
        session.getPollLoops().forEach(ModbusPollLoop::stop);
        session.getPollLoops().clear();
        ModbusConnection connection = session.getConnection();
        if (connection != null) {
            session.setConnection(null);
            connectionPool.release(session, connection);
        }
    }

    private void startLoop(ModbusSessionContext session, ModbusConnection connection, List<PollItem> items, long periodMs) {
        if (items.isEmpty()) {
            return;
        }
        List<ReadBlock> blocks = ReadPlanner.plan(items, blockMergeMaxGap);
        int retries = session.getProfileConfig().getRetries() != null ? session.getProfileConfig().getRetries() : 0;
        ModbusPollLoop loop = new ModbusPollLoop(session, connection, blocks, periodMs, retries, scheduler, transportService);
        session.getPollLoops().add(loop);
        loop.start();
    }

    private long clamp(long periodMs) {
        return Math.max(periodMs, minPollPeriodMs);
    }

    private static List<PollItem> items(List<ModbusMapping> mappings, PollItem.Target target) {
        if (mappings == null) {
            return List.of();
        }
        return mappings.stream().map(mapping -> new PollItem(mapping, target)).toList();
    }

}
