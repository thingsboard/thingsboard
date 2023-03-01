/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.gen.transport.TransportProtos.ToUsageStatsServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.UsageStatsKVProto;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.scheduler.SchedulerComponent;

import javax.annotation.PostConstruct;
import java.util.EnumMap;
import java.util.Optional;
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

    private final EnumMap<ApiUsageRecordKey, ConcurrentMap<ReportLevel, AtomicLong>> stats = new EnumMap<>(ApiUsageRecordKey.class);

    private final PartitionService partitionService;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final SchedulerComponent scheduler;
    private final TbQueueProducerProvider producerProvider;
    private TbQueueProducer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> msgProducer;

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
        ConcurrentMap<ReportLevel, ToUsageStatsServiceMsg.Builder> report = new ConcurrentHashMap<>();

        for (ApiUsageRecordKey key : ApiUsageRecordKey.values()) {
            ConcurrentMap<ReportLevel, AtomicLong> statsForKey = stats.get(key);
            statsForKey.forEach((reportLevel, statsValue) -> {
                long value = statsValue.get();
                if (value == 0 && key.isCounter()) return;

                ToUsageStatsServiceMsg.Builder statsMsgBuilder = report.computeIfAbsent(reportLevel, id -> {
                    ToUsageStatsServiceMsg.Builder newStatsMsgBuilder = ToUsageStatsServiceMsg.newBuilder();

                    TenantId tenantId = reportLevel.getTenantId();
                    newStatsMsgBuilder.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
                    newStatsMsgBuilder.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());

                    CustomerId customerId = reportLevel.getCustomerId();
                    if (customerId != null) {
                        newStatsMsgBuilder.setCustomerIdMSB(customerId.getId().getMostSignificantBits());
                        newStatsMsgBuilder.setCustomerIdLSB(customerId.getId().getLeastSignificantBits());
                    }

                    newStatsMsgBuilder.setServiceId(serviceInfoProvider.getServiceId());
                    return newStatsMsgBuilder;
                });

                statsMsgBuilder.addValues(UsageStatsKVProto.newBuilder().setKey(key.name()).setValue(value).build());
            });
            statsForKey.clear();
        }

        report.forEach(((reportLevel, statsMsg) -> {
            //TODO: figure out how to minimize messages into the queue. Maybe group by 100s of messages?

            TenantId tenantId = reportLevel.getTenantId();
            EntityId entityId = Optional.ofNullable((EntityId) reportLevel.getCustomerId()).orElse(tenantId);
            TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, entityId).newByTopic(msgProducer.getDefaultTopic());
            msgProducer.send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), statsMsg.build()), null);
        }));

        if (!report.isEmpty()) {
            log.debug("Reporting API usage statistics for {} tenants and customers", report.size());
        }
    }

    @Override
    public void report(TenantId tenantId, CustomerId customerId, ApiUsageRecordKey key, long value) {
        if (enabled) {
            ReportLevel[] levels = new ReportLevel[3];
            levels[0] = ReportLevel.of(tenantId);
            if (key.isCounter()) {
                levels[1] = ReportLevel.of(TenantId.SYS_TENANT_ID);
            }
            if (enabledPerCustomer && customerId != null && !customerId.isNullUid()) {
                levels[2] = ReportLevel.of(tenantId, customerId);
            }
            report(key, value, levels);
        }
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

    @RequiredArgsConstructor(staticName = "of")
    @AllArgsConstructor(staticName = "of")
    @Getter
    private static class ReportLevel {
        private final TenantId tenantId;
        private CustomerId customerId;
    }

}
