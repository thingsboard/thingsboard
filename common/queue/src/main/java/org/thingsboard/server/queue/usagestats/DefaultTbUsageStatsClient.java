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
package org.thingsboard.server.queue.usagestats;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.UsageRecordKey;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos.ToUsageStatsServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.UsageStatsKVProto;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.scheduler.SchedulerComponent;

import javax.annotation.PostConstruct;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class DefaultTbUsageStatsClient implements TbUsageStatsClient {

    @Value("${usage.stats.report.enabled:true}")
    private boolean enabled;
    @Value("${usage.stats.report.interval:600}")
    private int interval;

    private final ConcurrentMap<TenantId, AtomicLong>[] values = new ConcurrentMap[UsageRecordKey.values().length];
    private final PartitionService partitionService;
    private final SchedulerComponent scheduler;
    private final TbQueueProducerProvider producerProvider;
    private TbQueueProducer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> msgProducer;

    public DefaultTbUsageStatsClient(PartitionService partitionService, SchedulerComponent scheduler, TbQueueProducerProvider producerProvider) {
        this.partitionService = partitionService;
        this.scheduler = scheduler;
        this.producerProvider = producerProvider;
    }

    @PostConstruct
    private void init() {
        if (enabled) {
            msgProducer = this.producerProvider.getTbUsageStatsMsgProducer();
            for (UsageRecordKey key : UsageRecordKey.values()) {
                values[key.ordinal()] = new ConcurrentHashMap<>();
            }
            scheduler.scheduleWithFixedDelay(this::reportStats, new Random().nextInt(interval), interval, TimeUnit.SECONDS);
        }
    }

    private void reportStats() {
        ConcurrentMap<TenantId, ToUsageStatsServiceMsg.Builder> report = new ConcurrentHashMap<>();

        for (UsageRecordKey key : UsageRecordKey.values()) {
            values[key.ordinal()].forEach(((tenantId, atomicLong) -> {
                long value = atomicLong.getAndSet(0);
                if (value > 0) {
                    ToUsageStatsServiceMsg.Builder msgBuilder = report.computeIfAbsent(tenantId, id -> {
                        ToUsageStatsServiceMsg.Builder msg = ToUsageStatsServiceMsg.newBuilder();
                        msg.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
                        msg.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());
                        return msg;
                    });
                    msgBuilder.addValues(UsageStatsKVProto.newBuilder().setKey(key.name()).setValue(value).build());
                }
            }));
        }

        report.forEach(((tenantId, builder) -> {
            //TODO: figure out how to minimize messages into the queue. Maybe group by 100s of messages?
            TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, tenantId);
            msgProducer.send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), builder.build()), null);
        }));
    }

    @Override
    public void report(TenantId tenantId, UsageRecordKey key, long value) {
        if (enabled) {
            ConcurrentMap<TenantId, AtomicLong> map = values[key.ordinal()];
            AtomicLong atomicValue = map.computeIfAbsent(tenantId, id -> new AtomicLong());
            atomicValue.addAndGet(value);
        }
    }

}
