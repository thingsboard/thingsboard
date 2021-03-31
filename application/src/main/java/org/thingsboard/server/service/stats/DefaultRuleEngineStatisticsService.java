/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.QueueStatsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.queue.QueueStats;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.queue.QueueStatsService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;
import org.thingsboard.server.service.queue.TbRuleEngineConsumerStats;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@TbRuleEngineComponent
@Service
@Slf4j
public class DefaultRuleEngineStatisticsService implements RuleEngineStatisticsService {

    public static final String TB_SERVICE_QUEUE = "TbServiceQueue";
    public static final FutureCallback<Integer> CALLBACK = new FutureCallback<Integer>() {
        @Override
        public void onSuccess(@Nullable Integer result) {

        }

        @Override
        public void onFailure(Throwable t) {
            log.warn("Failed to persist statistics", t);
        }
    };

    private final TbServiceInfoProvider serviceInfoProvider;
    private final TelemetrySubscriptionService tsService;
    private final Lock lock = new ReentrantLock();
    private final QueueStatsService queueStatsService;
    private final QueueService queueService;
    private final ConcurrentMap<TenantQueueKey, QueueStatsId> tenantQueueStats;

    public DefaultRuleEngineStatisticsService(TelemetrySubscriptionService tsService,
                                              TbServiceInfoProvider serviceInfoProvider,
                                              QueueStatsService queueStatsService,
                                              QueueService queueService) {
        this.tsService = tsService;
        this.serviceInfoProvider = serviceInfoProvider;
        this.queueStatsService = queueStatsService;
        this.queueService = queueService;
        this.tenantQueueStats = new ConcurrentHashMap<>();
    }

    @Override
    public void reportQueueStats(long ts, TbRuleEngineConsumerStats ruleEngineStats) {
        String queueName = ruleEngineStats.getQueueName();
        ruleEngineStats.getTenantStats().forEach((id, stats) -> {
            TenantId tenantId = new TenantId(id);
            try {
                QueueStatsId queueStatsId = getQueueStatsId(tenantId, queueName);
                if (stats.getTotalMsgCounter().get() > 0) {
                    List<TsKvEntry> tsList = stats.getCounters().entrySet().stream()
                            .map(kv -> new BasicTsKvEntry(ts, new LongDataEntry(kv.getKey(), (long) kv.getValue().get())))
                            .collect(Collectors.toList());
                    if (!tsList.isEmpty()) {
                        tsService.saveAndNotifyInternal(tenantId, queueStatsId, tsList, CALLBACK);
                    }
                }
            } catch (DataValidationException e) {
                if (!e.getMessage().equalsIgnoreCase("Queue Stats is referencing to non-existent tenant!")) {
                    throw e;
                }
            }
        });
        ruleEngineStats.getTenantExceptions().forEach((tenantId, e) -> {
            TsKvEntry tsKv = new BasicTsKvEntry(ts, new JsonDataEntry("ruleEngineException", e.toJsonString()));
            try {
                tsService.saveAndNotifyInternal(tenantId, getQueueStatsId(tenantId, queueName), Collections.singletonList(tsKv), CALLBACK);
            } catch (DataValidationException e2) {
                if (!e2.getMessage().equalsIgnoreCase("Queue stats is referencing to non-existent tenant!")) {
                    throw e2;
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
                    QueueStats queueStats = queueStatsService.findByTenantIdAndName(tenantId, queueName + "_" + serviceInfoProvider.getServiceId());
                    if (queueStats == null) {
                        Queue queue = queueService.findQueueByTenantIdAndName(tenantId, queueName);
                        if (queue == null) {
                            throw new RuntimeException("Queue with name " + queueName + " is not exist.");
                        }
                        queueStats = new QueueStats();
                        queueStats.setTenantId(tenantId);
                        queueStats.setName(queueName + "_" + serviceInfoProvider.getServiceId());
                        queueStats.setQueueId(queue.getId());
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
