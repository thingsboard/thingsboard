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
package org.thingsboard.server.service.stats;

import com.google.common.util.concurrent.FutureCallback;
import jakarta.annotation.Nullable;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.server.common.data.id.QueueStatsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.queue.QueueStats;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.queue.QueueStatsService;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;
import org.thingsboard.server.service.queue.TbRuleEngineConsumerStats;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@TbRuleEngineComponent
@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultRuleEngineStatisticsService implements RuleEngineStatisticsService {

    public static final String RULE_ENGINE_EXCEPTION = "ruleEngineException";
    public static final FutureCallback<Void> CALLBACK = new FutureCallback<Void>() {
        @Override
        public void onSuccess(@Nullable Void result) {

        }

        @Override
        public void onFailure(Throwable t) {
            log.warn("Failed to persist statistics", t);
        }
    };

    private final TbServiceInfoProvider serviceInfoProvider;
    private final TelemetrySubscriptionService tsService;
    private final QueueStatsService queueStatsService;
    private final ApiLimitService apiLimitService;
    private final Lock lock = new ReentrantLock();
    private final ConcurrentMap<TenantQueueKey, QueueStatsId> tenantQueueStats = new ConcurrentHashMap<>();

    @Value("${queue.rule-engine.stats.max-error-message-length:4096}")
    private int maxErrorMessageLength;

    @Override
    public void reportQueueStats(long ts, TbRuleEngineConsumerStats ruleEngineStats) {
        String queueName = ruleEngineStats.getQueueName();
        ruleEngineStats.getTenantStats().forEach((id, stats) -> {
            try {
                TenantId tenantId = TenantId.fromUUID(id);
                QueueStatsId queueStatsId = getQueueStatsId(tenantId, queueName);
                if (stats.getTotalMsgCounter().get() > 0) {
                    List<TsKvEntry> tsList = stats.getCounters().entrySet().stream()
                            .map(kv -> new BasicTsKvEntry(ts, new LongDataEntry(kv.getKey(), (long) kv.getValue().get())))
                            .collect(Collectors.toList());
                    if (!tsList.isEmpty()) {
                        long ttl = apiLimitService.getLimit(tenantId, DefaultTenantProfileConfiguration::getQueueStatsTtlDays);
                        ttl = TimeUnit.DAYS.toSeconds(ttl);
                        tsService.saveTimeseriesInternal(TimeseriesSaveRequest.builder()
                                .tenantId(tenantId)
                                .entityId(queueStatsId)
                                .entries(tsList)
                                .ttl(ttl)
                                .callback(CALLBACK)
                                .build());
                    }
                }
            } catch (Exception e) {
                if (!"Asset is referencing to non-existent tenant!".equalsIgnoreCase(e.getMessage())) {
                    log.debug("[{}] Failed to store the statistics", id, e);
                }
            }
        });
        ruleEngineStats.getTenantExceptions().forEach((tenantId, e) -> {
            try {
                TsKvEntry tsKv = new BasicTsKvEntry(e.getTs(), new JsonDataEntry(RULE_ENGINE_EXCEPTION, e.toJsonString(maxErrorMessageLength)));
                long ttl = apiLimitService.getLimit(tenantId, DefaultTenantProfileConfiguration::getRuleEngineExceptionsTtlDays);
                ttl = TimeUnit.DAYS.toSeconds(ttl);
                tsService.saveTimeseriesInternal(TimeseriesSaveRequest.builder()
                        .tenantId(tenantId)
                        .entityId(getQueueStatsId(tenantId, queueName))
                        .entry(tsKv)
                        .ttl(ttl)
                        .callback(CALLBACK)
                        .build());
            } catch (Exception e2) {
                if (!"Asset is referencing to non-existent tenant!".equalsIgnoreCase(e2.getMessage())) {
                    log.debug("[{}] Failed to store the statistics", tenantId, e2);
                }
            }
        });
    }

    private QueueStatsId getQueueStatsId(TenantId tenantId, String queueName) {
        TenantQueueKey key = new TenantQueueKey(tenantId, queueName);
        QueueStatsId queueStatsId = tenantQueueStats.get(key);
        if (queueStatsId == null) {
            lock.lock();
            try {
                queueStatsId = tenantQueueStats.get(key);
                if (queueStatsId == null) {
                    QueueStats queueStats = queueStatsService.findByTenantIdAndNameAndServiceId(tenantId, queueName , serviceInfoProvider.getServiceId());
                    if (queueStats == null) {
                        queueStats = new QueueStats();
                        queueStats.setTenantId(tenantId);
                        queueStats.setQueueName(queueName);
                        queueStats.setServiceId(serviceInfoProvider.getServiceId());
                        queueStats = queueStatsService.save(tenantId, queueStats);
                    }
                    queueStatsId = queueStats.getId();
                    tenantQueueStats.put(key, queueStatsId);
                }
            } finally {
                lock.unlock();
            }
        }
        return queueStatsId;
    }

    @Data
    private static class TenantQueueKey {
        private final TenantId tenantId;
        private final String queueName;
    }
}
