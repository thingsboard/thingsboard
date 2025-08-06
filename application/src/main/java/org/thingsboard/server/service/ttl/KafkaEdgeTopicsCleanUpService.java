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
package org.thingsboard.server.service.ttl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.kafka.KafkaAdmin;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.service.state.DefaultDeviceStateService.LAST_CONNECT_TIME;

@Slf4j
@Service
@TbCoreComponent
@ConditionalOnExpression("'${queue.type:null}'=='kafka' && ${edges.enabled:true} && ${sql.ttl.edge_events.edge_events_ttl:0} > 0")
public class KafkaEdgeTopicsCleanUpService extends AbstractCleanUpService {

    private final TopicService topicService;
    private final TenantService tenantService;
    private final EdgeService edgeService;
    private final AttributesService attributesService;
    private final KafkaAdmin kafkaAdmin;

    @Value("${sql.ttl.edge_events.edge_events_ttl:2628000}")
    private long ttlSeconds;

    @Value("${queue.edge.event-notifications-topic:tb_edge_event.notifications}")
    private String tbEdgeEventNotificationsTopic;

    public KafkaEdgeTopicsCleanUpService(PartitionService partitionService, EdgeService edgeService,
                                         TenantService tenantService, AttributesService attributesService,
                                         TopicService topicService, KafkaAdmin kafkaAdmin) {
        super(partitionService);
        this.topicService = topicService;
        this.tenantService = tenantService;
        this.edgeService = edgeService;
        this.attributesService = attributesService;
        this.kafkaAdmin = kafkaAdmin;
    }

    @Scheduled(initialDelayString = "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${sql.ttl.edge_events.execution_interval_ms})}", fixedDelayString = "${sql.ttl.edge_events.execution_interval_ms}")
    public void cleanUp() {
        if (!isSystemTenantPartitionMine()) {
            return;
        }

        Set<String> topics = kafkaAdmin.listTopics();
        if (topics.isEmpty()) {
            return;
        }

        String edgeTopicPrefix = topicService.buildTopicName(tbEdgeEventNotificationsTopic);
        List<String> matchingTopics = topics.stream().filter(topic -> topic.startsWith(edgeTopicPrefix)).toList();
        if (matchingTopics.isEmpty()) {
            log.debug("No matching topics found with prefix [{}]. Skipping cleanup.", edgeTopicPrefix);
            return;
        }

        Map<TenantId, List<EdgeId>> tenantEdgeMap = extractTenantAndEdgeIds(matchingTopics, edgeTopicPrefix);

        long currentTimeMillis = System.currentTimeMillis();
        long ttlMillis = TimeUnit.SECONDS.toMillis(ttlSeconds);

        tenantEdgeMap.forEach((tenantId, edgeIds) -> processTenantCleanUp(tenantId, edgeIds, ttlMillis, currentTimeMillis));
    }

    private void processTenantCleanUp(TenantId tenantId, List<EdgeId> edgeIds, long ttlMillis, long currentTimeMillis) {
        boolean tenantExists = tenantService.tenantExists(tenantId);
        if (tenantExists) {
            for (EdgeId edgeId : edgeIds) {
                try {
                    attributesService.find(tenantId, edgeId, AttributeScope.SERVER_SCOPE, LAST_CONNECT_TIME).get()
                            .flatMap(AttributeKvEntry::getLongValue)
                            .filter(lastConnectTime -> isTopicExpired(lastConnectTime, ttlMillis, currentTimeMillis))
                            .ifPresentOrElse(lastConnectTime -> {
                                String topic = topicService.buildEdgeEventNotificationsTopicPartitionInfo(tenantId, edgeId).getTopic();
                                if (kafkaAdmin.isTopicEmpty(topic)) {
                                    deleteTopicAndConsumerGroup(topic);
                                    log.info("[{}] Removed outdated topic {} for edge {} older than {}",
                                            tenantId, topic, edgeId, Date.from(Instant.ofEpochMilli(currentTimeMillis - ttlMillis)));
                                }
                            }, () -> {
                                Edge edge = edgeService.findEdgeById(tenantId, edgeId);
                                if (edge == null) {
                                    String topic = topicService.buildEdgeEventNotificationsTopicPartitionInfo(tenantId, edgeId).getTopic();
                                    deleteTopicAndConsumerGroup(topic);
                                    log.info("[{}] Removed topic {} for deleted edge {}", tenantId, topic, edgeId);
                                }
                            });
                } catch (Exception e) {
                    log.error("[{}] Failed to delete topic for edge {}", tenantId, edgeId, e);
                }
            }
        } else {
            for (EdgeId edgeId : edgeIds) {
                String topic = topicService.buildEdgeEventNotificationsTopicPartitionInfo(tenantId, edgeId).getTopic();
                deleteTopicAndConsumerGroup(topic);
            }
            log.info("[{}] Removed topics for not existing tenant and edges {}", tenantId, edgeIds);
        }
    }

    private void deleteTopicAndConsumerGroup(String topic) {
        kafkaAdmin.deleteTopic(topic);
        kafkaAdmin.deleteConsumerGroup(topic);
    }

    private boolean isTopicExpired(long lastConnectTime, long ttlMillis, long currentTimeMillis) {
        return lastConnectTime + ttlMillis < currentTimeMillis;
    }

    private Map<TenantId, List<EdgeId>> extractTenantAndEdgeIds(List<String> topics, String prefix) {
        Map<TenantId, List<EdgeId>> tenantEdgeMap = new HashMap<>();
        for (String topic : topics) {
            try {
                String remaining = topic.substring(prefix.length() + 1);
                String[] parts = remaining.split("\\.");
                TenantId tenantId = TenantId.fromUUID(UUID.fromString(parts[0]));
                EdgeId edgeId = new EdgeId(UUID.fromString(parts[1]));
                tenantEdgeMap.computeIfAbsent(tenantId, id -> new ArrayList<>()).add(edgeId);
            } catch (Exception e) {
                log.warn("Failed to extract TenantId and EdgeId from topic [{}]", topic, e);
            }
        }
        return tenantEdgeMap;
    }

}
