/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.edqs.stats;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.stats.EdqsStatsService;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsTimer;
import org.thingsboard.server.common.stats.StatsType;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@ConditionalOnExpression("'${queue.edqs.api.supported:true}' == 'true' && '${queue.edqs.stats.enabled:true}' == 'true'")
public class DefaultEdqsStatsService implements EdqsStatsService {

    private final StatsFactory statsFactory;

    @Value("${queue.edqs.stats.slow_query_threshold:3000}")
    private int slowQueryThreshold;

    private final ConcurrentHashMap<ObjectType, AtomicInteger> objectCounters = new ConcurrentHashMap<>();
    private final StatsTimer dataQueryTimer;
    private final StatsTimer countQueryTimer;

    private DefaultEdqsStatsService(StatsFactory statsFactory) {
        this.statsFactory = statsFactory;
        dataQueryTimer = statsFactory.createTimer(StatsType.EDQS, "entityDataQueryTimer");
        countQueryTimer = statsFactory.createTimer(StatsType.EDQS, "entityCountQueryTimer");
    }

    @Override
    public void reportAdded(ObjectType objectType) {
        getObjectCounter(objectType).incrementAndGet();
    }

    @Override
    public void reportRemoved(ObjectType objectType) {
        getObjectCounter(objectType).decrementAndGet();
    }

    @Override
    public void reportDataQuery(TenantId tenantId, EntityDataQuery query, long timingNanos) {
        double timingMs = timingNanos / 1000_000.0;
        if (timingMs < slowQueryThreshold) {
            log.debug("[{}] Executed data query in {} ms: {}", tenantId, timingMs, query);
        } else {
            log.warn("[{}] Executed slow data query in {} ms: {}", tenantId, timingMs, query);
        }
        dataQueryTimer.record(timingNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void reportCountQuery(TenantId tenantId, EntityCountQuery query, long timingNanos) {
        double timingMs = timingNanos / 1000_000.0;
        if (timingMs < slowQueryThreshold) {
            log.debug("[{}] Executed count query in {} ms: {}", tenantId, timingMs, query);
        } else {
            log.warn("[{}] Executed slow count query in {} ms: {}", tenantId, timingMs, query);
        }
        countQueryTimer.record(timingNanos, TimeUnit.NANOSECONDS);
    }

    private AtomicInteger getObjectCounter(ObjectType objectType) {
        return objectCounters.computeIfAbsent(objectType, type ->
                statsFactory.createGauge("edqsObjectsCount", new AtomicInteger(), "objectType", type.name()));
    }

}
