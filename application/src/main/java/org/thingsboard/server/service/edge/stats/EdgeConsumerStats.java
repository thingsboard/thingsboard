/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.edge.stats;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.stats.StatsCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdgeMsg;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class EdgeConsumerStats {
    public static final String TOTAL_MSGS = "totalMsgs";
    public static final String EDGE_NOTIFICATIONS = "edgeNfs";
    public static final String TO_CORE_NF_EDGE_EVENT_UPDATE = "coreNfEdgeUpd";
    public static final String TO_CORE_NF_EDGE_SYNC_REQUEST = "coreNfEdgeSyncReq";
    public static final String TO_CORE_NF_EDGE_SYNC_RESPONSE = "coreNfEdgeSyncResp";

    private final StatsCounter totalCounter;
    private final StatsCounter edgeNotificationsCounter;
    private final StatsCounter toCoreNfEdgeEventUpdateCounter;
    private final StatsCounter toCoreNfEdgeSyncRequestCounter;
    private final StatsCounter toCoreNfEdgeSyncResponseCounter;

    private final List<StatsCounter> counters = new ArrayList<>(2);

    public EdgeConsumerStats(StatsFactory statsFactory) {
        String statsKey = StatsType.EDGE.getName();

        this.totalCounter = register(statsFactory.createStatsCounter(statsKey, TOTAL_MSGS));
        this.edgeNotificationsCounter = register(statsFactory.createStatsCounter(statsKey, EDGE_NOTIFICATIONS));
        this.toCoreNfEdgeEventUpdateCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_EDGE_EVENT_UPDATE));
        this.toCoreNfEdgeSyncRequestCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_EDGE_SYNC_REQUEST));
        this.toCoreNfEdgeSyncResponseCounter = register(statsFactory.createStatsCounter(statsKey, TO_CORE_NF_EDGE_SYNC_RESPONSE));
    }

    private StatsCounter register(StatsCounter counter) {
        counters.add(counter);
        return counter;
    }

    public void log(ToEdgeMsg msg) {
        totalCounter.increment();
        if (msg.hasEdgeNotificationMsg()) {
            edgeNotificationsCounter.increment();
        }
    }

    public void log(TransportProtos.EdgeNotificationMsgProto msg) {
        totalCounter.increment();
        edgeNotificationsCounter.increment();
    }

    public void printStats() {
        int total = totalCounter.get();
        if (total > 0) {
            StringBuilder stats = new StringBuilder();
            counters.forEach(counter -> stats.append(counter.getName()).append(" = [").append(counter.get()).append("] "));
            log.info("Edge Stats: {}", stats);
        }
    }

    public void reset() {
        counters.forEach(StatsCounter::clear);
    }
}
