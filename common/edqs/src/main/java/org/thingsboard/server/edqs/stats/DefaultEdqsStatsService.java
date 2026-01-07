/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.TbBytePool;
import org.thingsboard.common.util.TbStringPool;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.stats.EdqsStatsService;
import org.thingsboard.server.common.stats.StatsCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsTimer;
import org.thingsboard.server.edqs.repo.DefaultEdqsRepository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnExpression("'${queue.edqs.api.supported:true}' == 'true' && '${queue.edqs.stats.enabled:true}' == 'true'")
public class DefaultEdqsStatsService implements EdqsStatsService {

    private final StatsFactory statsFactory;

    @Value("${queue.edqs.stats.slow_query_threshold}")
    private int slowQueryThreshold;

    private final ConcurrentMap<ObjectType, AtomicInteger> objectCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, StatsTimer> timers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, StatsCounter> counters = new ConcurrentHashMap<>();

    @PostConstruct
    private void init() {
        statsFactory.createGauge("edqsMapGauges", "stringPoolSize", TbStringPool.getPool(), Map::size);
        statsFactory.createGauge("edqsMapGauges", "bytePoolSize", TbBytePool.getPool(), Map::size);
        statsFactory.createGauge("edqsMapGauges", "tenantReposSize", DefaultEdqsRepository.getRepos(), Map::size);
    }

    @Override
    public void reportAdded(ObjectType objectType) {
        getObjectGauge(objectType).incrementAndGet();
    }

    @Override
    public void reportRemoved(ObjectType objectType) {
        getObjectGauge(objectType).decrementAndGet();
    }

    @Override
    public void reportEntityDataQuery(TenantId tenantId, EntityDataQuery query, long timingNanos) {
        checkTiming(tenantId, query, timingNanos);
        getTimer("entityDataQueryTimer").record(timingNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void reportEntityCountQuery(TenantId tenantId, EntityCountQuery query, long timingNanos) {
        checkTiming(tenantId, query, timingNanos);
        getTimer("entityCountQueryTimer").record(timingNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void reportEdqsDataQuery(TenantId tenantId, EntityDataQuery query, long timingNanos) {
        checkTiming(tenantId, query, timingNanos);
        getTimer("edqsDataQueryTimer").record(timingNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void reportEdqsCountQuery(TenantId tenantId, EntityCountQuery query, long timingNanos) {
        checkTiming(tenantId, query, timingNanos);
        getTimer("edqsCountQueryTimer").record(timingNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void reportStringCompressed() {
        getCounter("stringsCompressed").increment();
    }

    @Override
    public void reportStringUncompressed() {
        getCounter("stringsUncompressed").increment();
    }

    private void checkTiming(TenantId tenantId, EntityCountQuery query, long timingNanos) {
        double timingMs = timingNanos / 1000_000.0;
        String queryType = query instanceof EntityDataQuery ? "data" : "count";
        if (timingMs < slowQueryThreshold) {
            log.debug("[{}] Executed " + queryType + " query in {} ms: {}", tenantId, timingMs, query);
        } else {
            log.warn("[{}] Executed slow " + queryType + " query in {} ms: {}", tenantId, timingMs, query);
        }
    }

    private StatsTimer getTimer(String name) {
        return timers.computeIfAbsent(name, __ -> statsFactory.createStatsTimer("edqsTimers", name));
    }

    private StatsCounter getCounter(String name) {
        return counters.computeIfAbsent(name, __ -> statsFactory.createStatsCounter("edqsCounters", name));
    }

    private AtomicInteger getObjectGauge(ObjectType objectType) {
        return objectCounters.computeIfAbsent(objectType, type ->
                statsFactory.createGauge("edqsGauges", "objectsCount", new AtomicInteger(), "objectType", type.name()));
    }

}
