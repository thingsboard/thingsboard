// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.housekeeper.stats;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTaskType;
import org.thingsboard.server.common.stats.DefaultCounter;
import org.thingsboard.server.common.stats.StatsCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsTimer;
import org.thingsboard.server.common.stats.StatsType;
import org.thingsboard.server.gen.transport.TransportProtos.ToHousekeeperServiceMsg;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@ConditionalOnProperty(name = "queue.core.housekeeper.stats.enabled", havingValue = "true", matchIfMissing = true)
public class HousekeeperStatsService {

    private final Map<HousekeeperTaskType, HousekeeperStats> stats = new EnumMap<>(HousekeeperTaskType.class);

    public HousekeeperStatsService(StatsFactory statsFactory) {
        for (HousekeeperTaskType taskType : HousekeeperTaskType.values()) {
            stats.put(taskType, new HousekeeperStats(taskType, statsFactory));
        }
    }

    @Scheduled(initialDelayString = "${queue.core.housekeeper.stats.print-interval-ms:60000}",
            fixedDelayString = "${queue.core.housekeeper.stats.print-interval-ms:60000}")
    private void reportStats() {
        String statsStr = stats.values().stream().map(stats -> {
            String countersStr = stats.getCounters().stream()
                    .filter(counter -> counter.get() > 0)
                    .map(counter -> counter.getName() + " = [" + counter.get() + "]")
                    .collect(Collectors.joining(" "));
            if (countersStr.isEmpty()) {
                return null;
            } else {
                return stats.getTaskType() + " " + countersStr + " avgProcessingTime [" + stats.getProcessingTimer().getAvg() + " ms]";
            }
        }).filter(Objects::nonNull).collect(Collectors.joining(", "));

        if (!statsStr.isEmpty()) {
            stats.values().forEach(HousekeeperStats::reset);
            log.info("Housekeeper stats: {}", statsStr);
        }
    }

    public void reportProcessed(HousekeeperTaskType taskType, ToHousekeeperServiceMsg msg, long timing) {
        HousekeeperStats stats = this.stats.get(taskType);
        if (msg.getTask().getErrorsCount() == 0) {
            stats.getProcessedCounter().increment();
        } else {
            stats.getReprocessedCounter().increment();
        }
        stats.getProcessingTimer().record(timing);
    }

    public void reportFailure(HousekeeperTaskType taskType, ToHousekeeperServiceMsg msg) {
        HousekeeperStats stats = this.stats.get(taskType);
        if (msg.getTask().getErrorsCount() == 0) {
            stats.getFailedProcessingCounter().increment();
        } else {
            stats.getFailedReprocessingCounter().increment();
        }
    }

    @Getter
    static class HousekeeperStats {
        private final HousekeeperTaskType taskType;
        private final List<StatsCounter> counters = new ArrayList<>();

        private final StatsCounter processedCounter;
        private final StatsCounter failedProcessingCounter;
        private final StatsCounter reprocessedCounter;
        private final StatsCounter failedReprocessingCounter;

        private final StatsTimer processingTimer;

        public HousekeeperStats(HousekeeperTaskType taskType, StatsFactory statsFactory) {
            this.taskType = taskType;
            this.processedCounter = register("processed", statsFactory);
            this.failedProcessingCounter = register("failedProcessing", statsFactory);
            this.reprocessedCounter = register("reprocessed", statsFactory);
            this.failedReprocessingCounter = register("failedReprocessing", statsFactory);
            this.processingTimer = statsFactory.createStatsTimer(StatsType.HOUSEKEEPER.getName(), "processingTime", "taskType", taskType.name());
        }

        private StatsCounter register(String statsName, StatsFactory statsFactory) {
            StatsCounter counter = statsFactory.createStatsCounter(StatsType.HOUSEKEEPER.getName(), statsName, "taskType", taskType.name());
            counters.add(counter);
            return counter;
        }

        public void reset() {
            counters.forEach(DefaultCounter::clear);
            processingTimer.reset();
        }

    }

}
