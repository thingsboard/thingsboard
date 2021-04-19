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
package org.thingsboard.server.queue.usagestats;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos.ToUsageStatsServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.UsageStatsKVProto;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.scheduler.SchedulerComponent;

import javax.annotation.PostConstruct;
import java.util.EnumMap;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class DefaultTbApiUsageClient implements TbApiUsageClient {

    @Value("${usage.stats.report.enabled:true}")
    private boolean enabled;
    @Value("${usage.stats.report.interval:10}")
    private int interval;

    private final EnumMap<ApiUsageRecordKey, ConcurrentMap<EntityId, AtomicLong>> stats = new EnumMap<>(ApiUsageRecordKey.class);
    private final ConcurrentMap<EntityId, TenantId> tenants = new ConcurrentHashMap<>();

    private final PartitionService partitionService;
    private final SchedulerComponent scheduler;
    private final TbQueueProducerProvider producerProvider;
    private TbQueueProducer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> msgProducer;

    public DefaultTbApiUsageClient(PartitionService partitionService, SchedulerComponent scheduler, TbQueueProducerProvider producerProvider) {
        this.partitionService = partitionService;
        this.scheduler = scheduler;
        this.producerProvider = producerProvider;
    }

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
        ConcurrentMap<EntityId, ToUsageStatsServiceMsg.Builder> report = new ConcurrentHashMap<>();

        for (ApiUsageRecordKey key : ApiUsageRecordKey.values()) {
            ConcurrentMap<EntityId, AtomicLong> statsForKey = stats.get(key);
            statsForKey.forEach((initiatorId, statsValue) -> {
                long value = statsValue.get();
                if (value == 0) return;

                ToUsageStatsServiceMsg.Builder statsMsgBuilder = report.computeIfAbsent(initiatorId, id -> {
                    ToUsageStatsServiceMsg.Builder newStatsMsgBuilder = ToUsageStatsServiceMsg.newBuilder();

                    TenantId tenantId;
                    if (initiatorId.getEntityType() == EntityType.TENANT) {
                        tenantId = (TenantId) initiatorId;
                    } else {
                        tenantId = tenants.get(initiatorId);
                    }

                    newStatsMsgBuilder.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
                    newStatsMsgBuilder.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());

                    if (initiatorId.getEntityType() == EntityType.CUSTOMER) {
                        newStatsMsgBuilder.setCustomerIdMSB(initiatorId.getId().getMostSignificantBits());
                        newStatsMsgBuilder.setCustomerIdLSB(initiatorId.getId().getLeastSignificantBits());
                    }

                    return newStatsMsgBuilder;
                });

                statsMsgBuilder.addValues(UsageStatsKVProto.newBuilder().setKey(key.name()).setValue(value).build());
            });
            statsForKey.clear();
        }

        report.forEach(((initiatorId, builder) -> {
            //TODO: figure out how to minimize messages into the queue. Maybe group by 100s of messages?

            TenantId tenantId;
            if (initiatorId.getEntityType() == EntityType.TENANT) {
                tenantId = (TenantId) initiatorId;
            } else {
                tenantId = tenants.get(initiatorId);
            }

            TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, initiatorId).newByTopic(msgProducer.getDefaultTopic());
            msgProducer.send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), builder.build()), null);
        }));

        if (!report.isEmpty()) {
            log.info("Reporting API usage statistics for {} tenants and customers", report.size());
        }
    }

    @Override
    public void report(TenantId tenantId, CustomerId customerId, ApiUsageRecordKey key, long value) {
        if (enabled) {
            ConcurrentMap<EntityId, AtomicLong> statsForKey = stats.get(key);

            statsForKey.computeIfAbsent(tenantId, id -> new AtomicLong()).addAndGet(value);
            statsForKey.computeIfAbsent(TenantId.SYS_TENANT_ID, id -> new AtomicLong()).addAndGet(value);

            if (customerId != null && !customerId.isNullUid()) {
                statsForKey.computeIfAbsent(customerId, id -> new AtomicLong()).addAndGet(value);
                tenants.putIfAbsent(customerId, tenantId);
            }
        }
    }

    @Override
    public void report(TenantId tenantId, CustomerId customerId, ApiUsageRecordKey key) {
        report(tenantId, customerId, key, 1);
    }
}
