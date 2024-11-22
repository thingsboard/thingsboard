/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.ttl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.kafka.TbKafkaAdmin;
import org.thingsboard.server.queue.kafka.TbKafkaSettings;
import org.thingsboard.server.queue.kafka.TbKafkaTopicConfigs;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.state.DefaultDeviceStateService;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
@ConditionalOnExpression("'${queue.type:null}'=='kafka' && ${edges.enabled:true}")
public class KafkaEdgeTopicsCleanUpService {

    private final EdgeService edgeService;
    private final TenantService tenantService;
    private final AttributesService attributesService;

    private final TopicService topicService;
    private final PartitionService partitionService;

    private final TbKafkaSettings kafkaSettings;
    private final TbKafkaTopicConfigs kafkaTopicConfigs;

    @Value("${sql.ttl.edge_events.edge_events_ttl:2628000}")
    private long ttlSeconds;

    @Scheduled(initialDelayString = "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${sql.ttl.edge_events.execution_interval_ms})}", fixedDelayString = "${sql.ttl.edge_events.execution_interval_ms}")
    public void cleanUp() {
        PageDataIterable<TenantId> tenants = new PageDataIterable<>(tenantService::findTenantsIds, 10_000);
        for (TenantId tenantId : tenants) {
            try {
                cleanUp(tenantId);
            } catch (Exception e) {
                log.warn("Failed to drop kafka topics for tenant {}", tenantId, e);
            }
        }
    }

    private void cleanUp(TenantId tenantId) throws Exception {
        if (!partitionService.resolve(ServiceType.TB_CORE, tenantId, tenantId).isMyPartition()) {
            return;
        }

        PageDataIterable<EdgeId> edgeIds = new PageDataIterable<>(link -> edgeService.findEdgeIdsByTenantId(tenantId, link), 1024);
        long currentTimeMillis = System.currentTimeMillis();
        long ttlMillis = TimeUnit.SECONDS.toChronoUnit().getDuration().multipliedBy(ttlSeconds).toMillis();

        for (EdgeId edgeId : edgeIds) {
            TbKafkaAdmin kafkaAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getEdgeEventConfigs());
            attributesService.find(tenantId, edgeId, AttributeScope.SERVER_SCOPE, DefaultDeviceStateService.LAST_CONNECT_TIME).get()
                    .flatMap(AttributeKvEntry::getLongValue)
                    .filter(lastConnectTime -> isTopicExpired(lastConnectTime, ttlMillis, currentTimeMillis))
                    .ifPresent(lastConnectTime -> {
                        String topic = topicService.buildEdgeEventNotificationsTopicPartitionInfo(tenantId, edgeId).getTopic();
                        if (kafkaAdmin.isTopicEmpty(topic)) {
                            kafkaAdmin.deleteTopic(topic);
                            log.info("Removed outdated topic for tenant {} and edge with id {} older than {}",
                                    tenantId, edgeId, Date.from(Instant.ofEpochMilli(currentTimeMillis - ttlMillis)));
                        }
                    });
        }
    }

    private boolean isTopicExpired(long lastConnectTime, long ttlMillis, long currentTimeMillis) {
        return lastConnectTime + ttlMillis < currentTimeMillis;
    }

}
