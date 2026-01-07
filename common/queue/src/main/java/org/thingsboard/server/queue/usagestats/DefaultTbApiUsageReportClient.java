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
package org.thingsboard.server.queue.usagestats;

import com.google.common.collect.Lists;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.exception.TenantNotFoundException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.UsageStatsServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToUsageStatsServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.UsageStatsKVProto;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.scheduler.SchedulerComponent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
@RequiredArgsConstructor
public class DefaultTbApiUsageReportClient implements TbApiUsageReportClient {

    @Value("${usage.stats.report.enabled:true}")
    private boolean enabled;
    @Value("${usage.stats.report.enabled_per_customer:false}")
    private boolean enabledPerCustomer;
    @Value("${usage.stats.report.interval:10}")
    private int interval;
    @Value("${usage.stats.report.pack_size:1024}")
    private int packSize;

    private final EnumMap<ApiUsageRecordKey, ConcurrentMap<ReportLevel, AtomicLong>> stats = new EnumMap<>(ApiUsageRecordKey.class);

    private final PartitionService partitionService;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final SchedulerComponent scheduler;
    private final TbQueueProducerProvider producerProvider;
    private TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToUsageStatsServiceMsg>> msgProducer;

    @PostConstruct
    private void init() {
        if (enabled) {
            msgProducer = this.producerProvider.getTbUsageStatsMsgProducer();
            for (ApiUsageRecordKey key : ApiUsageRecordKey.values()) {
                stats.put(key, new ConcurrentHashMap<>());
            }
            scheduler.scheduleWithFixedDelay(() -> {
                try {
                    reportStats();
                } catch (Exception e) {
                    log.warn("Failed to report statistics: ", e);
                }
            }, new Random().nextInt(interval), interval, TimeUnit.SECONDS);
        }
    }

    private void reportStats() {
        ConcurrentMap<ParentEntity, UsageStatsServiceMsg.Builder> report = new ConcurrentHashMap<>();

        for (ApiUsageRecordKey key : ApiUsageRecordKey.values()) {
            ConcurrentMap<ReportLevel, AtomicLong> statsForKey = stats.get(key);
            statsForKey.forEach((reportLevel, statsValue) -> {
                long value = statsValue.get();
                if (value == 0 && key.isCounter()) return;

                UsageStatsServiceMsg.Builder statsMsg = report.computeIfAbsent(reportLevel.getParentEntity(), parent -> {
                    UsageStatsServiceMsg.Builder newStatsMsg = UsageStatsServiceMsg.newBuilder();

                    TenantId tenantId = parent.getTenantId();
                    newStatsMsg.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
                    newStatsMsg.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());

                    CustomerId customerId = parent.getCustomerId();
                    if (customerId != null) {
                        newStatsMsg.setCustomerIdMSB(customerId.getId().getMostSignificantBits());
                        newStatsMsg.setCustomerIdLSB(customerId.getId().getLeastSignificantBits());
                    }

                    return newStatsMsg;
                });

                UsageStatsKVProto.Builder statsItem = UsageStatsKVProto.newBuilder()
                        .setRecordKey(ProtoUtils.toProto(key))
                        .setValue(value);
                statsMsg.addValues(statsItem.build());
            });
            statsForKey.clear();
        }

        Map<TopicPartitionInfo, List<UsageStatsServiceMsg>> reportStatsPerTpi = new HashMap<>();

        report.forEach((parent, statsMsg) -> {
            try {
                TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, parent.getTenantId(), parent.getId())
                        .withTopic(msgProducer.getDefaultTopic());
                reportStatsPerTpi.computeIfAbsent(tpi, k -> new ArrayList<>()).add(statsMsg.build());
            } catch (TenantNotFoundException e) {
                log.debug("Couldn't report usage stats for non-existing tenant: {}", e.getTenantId());
            }
        });

        reportStatsPerTpi.forEach((tpi, statsList) -> {
            toMsgPack(statsList).forEach(pack -> {
                try {
                    msgProducer.send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), pack), null);
                } catch (Exception e) {
                    log.warn("Failed to report usage stats pack to TPI {}", tpi, e);
                }
            });
        });

        if (!report.isEmpty()) {
            log.debug("Reporting API usage statistics for {} tenants and customers", report.size());
        }
    }

    private List<ToUsageStatsServiceMsg> toMsgPack(List<UsageStatsServiceMsg> list) {
        return Lists.partition(list, packSize)
                .stream()
                .map(partition ->
                        ToUsageStatsServiceMsg.newBuilder()
                                .addAllMsgs(partition)
                                .setServiceId(serviceInfoProvider.getServiceId())
                                .build())
                .toList();
    }

    @Override
    public void report(TenantId tenantId, CustomerId customerId, ApiUsageRecordKey key, long value) {
        if (!enabled) return;

        ReportLevel[] reportLevels = new ReportLevel[3];
        reportLevels[0] = ReportLevel.of(tenantId);
        if (key.isCounter()) {
            reportLevels[1] = ReportLevel.of(TenantId.SYS_TENANT_ID);
        }
        if (enabledPerCustomer && customerId != null && !customerId.isNullUid()) {
            reportLevels[2] = ReportLevel.of(tenantId, customerId);
        }
        report(key, value, reportLevels);
    }

    @Override
    public void report(TenantId tenantId, CustomerId customerId, ApiUsageRecordKey key) {
        report(tenantId, customerId, key, 1);
    }

    private void report(ApiUsageRecordKey key, long value, ReportLevel... levels) {
        ConcurrentMap<ReportLevel, AtomicLong> statsForKey = stats.get(key);
        for (ReportLevel level : levels) {
            if (level == null) continue;

            AtomicLong n = statsForKey.computeIfAbsent(level, k -> new AtomicLong());
            if (key.isCounter()) {
                n.addAndGet(value);
            } else {
                n.set(value);
            }
        }
    }

    @Data
    private static class ReportLevel {
        private final TenantId tenantId;
        private final CustomerId customerId;

        public static ReportLevel of(TenantId tenantId) {
            return new ReportLevel(tenantId, null);
        }

        public static ReportLevel of(TenantId tenantId, CustomerId customerId) {
            return new ReportLevel(tenantId, customerId);
        }

        public ParentEntity getParentEntity() {
            return new ParentEntity(tenantId, customerId);
        }

    }

    @Data
    private static class ParentEntity {
        private final TenantId tenantId;
        private final CustomerId customerId;

        public EntityId getId() {
            return customerId != null ? customerId : tenantId;
        }
    }

}
