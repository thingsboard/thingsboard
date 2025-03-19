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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.edqs.EdqsEventType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;
import org.thingsboard.server.queue.edqs.EdqsComponent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@EdqsComponent
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "queue.edqs.stats.enabled", havingValue = "true", matchIfMissing = true)
public class EdqsStatsService {

    private final ConcurrentHashMap<TenantId, EdqsStats> statsMap = new ConcurrentHashMap<>();
    private final StatsFactory statsFactory;

    public void reportEvent(TenantId tenantId, ObjectType objectType, EdqsEventType eventType) {
        statsMap.computeIfAbsent(tenantId, id -> new EdqsStats(tenantId, statsFactory))
                .reportEvent(objectType, eventType);
    }

    @Getter
    @AllArgsConstructor
    static class EdqsStats {

        private final TenantId tenantId;
        private final ConcurrentHashMap<ObjectType, AtomicInteger> entityCounters = new ConcurrentHashMap<>();
        private final StatsFactory statsFactory;

        private AtomicInteger getOrCreateObjectCounter(ObjectType objectType) {
            return entityCounters.computeIfAbsent(objectType,
                    type -> statsFactory.createGauge(StatsType.EDQS.getName() + "_object_count", new AtomicInteger(),
                            "tenantId", tenantId.toString(), "objectType", type.name()));
        }

        @Override
        public String toString() {
            return entityCounters.entrySet().stream()
                    .map(counters -> counters.getKey().name()+ " total = [" + counters.getValue() + "]")
                    .collect(Collectors.joining(", "));
        }

        public void reportEvent(ObjectType objectType, EdqsEventType eventType) {
            AtomicInteger objectCounter = getOrCreateObjectCounter(objectType);
            if (eventType == EdqsEventType.UPDATED){
                objectCounter.incrementAndGet();
            } else if (eventType == EdqsEventType.DELETED) {
                objectCounter.decrementAndGet();
            }
        }
    }

}
