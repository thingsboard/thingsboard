/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.dao.util;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.stats.DefaultCounter;
import org.thingsboard.server.common.stats.StatsCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Getter
public class BufferedRateExecutorStats {
    private static final String TENANT_ID_TAG = "tenantId";


    private static final String TOTAL_ADDED = "totalAdded";
    private static final String TOTAL_LAUNCHED = "totalLaunched";
    private static final String TOTAL_RELEASED = "totalReleased";
    private static final String TOTAL_FAILED = "totalFailed";
    private static final String TOTAL_EXPIRED = "totalExpired";
    private static final String TOTAL_REJECTED = "totalRejected";
    private static final String TOTAL_RATE_LIMITED = "totalRateLimited";

    private final StatsFactory statsFactory;

    private final ConcurrentMap<TenantId, DefaultCounter> rateLimitedTenants = new ConcurrentHashMap<>();

    private final List<StatsCounter> statsCounters = new ArrayList<>();

    private final StatsCounter totalAdded;
    private final StatsCounter totalLaunched;
    private final StatsCounter totalReleased;
    private final StatsCounter totalFailed;
    private final StatsCounter totalExpired;
    private final StatsCounter totalRejected;
    private final StatsCounter totalRateLimited;

    public BufferedRateExecutorStats(StatsFactory statsFactory) {
        this.statsFactory = statsFactory;

        String key = StatsType.RATE_EXECUTOR.getName();

        this.totalAdded = statsFactory.createStatsCounter(key, TOTAL_ADDED);
        this.totalLaunched = statsFactory.createStatsCounter(key, TOTAL_LAUNCHED);
        this.totalReleased = statsFactory.createStatsCounter(key, TOTAL_RELEASED);
        this.totalFailed = statsFactory.createStatsCounter(key, TOTAL_FAILED);
        this.totalExpired = statsFactory.createStatsCounter(key, TOTAL_EXPIRED);
        this.totalRejected = statsFactory.createStatsCounter(key, TOTAL_REJECTED);
        this.totalRateLimited = statsFactory.createStatsCounter(key, TOTAL_RATE_LIMITED);

        this.statsCounters.add(totalAdded);
        this.statsCounters.add(totalLaunched);
        this.statsCounters.add(totalReleased);
        this.statsCounters.add(totalFailed);
        this.statsCounters.add(totalExpired);
        this.statsCounters.add(totalRejected);
        this.statsCounters.add(totalRateLimited);
    }

    public void incrementRateLimitedTenant(TenantId tenantId){
        rateLimitedTenants.computeIfAbsent(tenantId,
                tId -> {
                    String key = StatsType.RATE_EXECUTOR.getName() + ".tenant";
                    return statsFactory.createDefaultCounter(key, TENANT_ID_TAG, tId.toString());
                }
        )
                .increment();
    }
}
