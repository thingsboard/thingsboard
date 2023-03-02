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
package org.thingsboard.server.service.alarm.rule.store;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cache.RedisUtil;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.alarm.rule.state.PersistedEntityState;

import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
public class RedisAlarmRuleEntityStateStore implements AlarmRuleEntityStateStore {

    private final PartitionService partitionService;
    private final RedisConnectionFactory redisConnectionFactory;

    @Override
    public void put(PersistedEntityState entityState) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_ALARM_RULES_EXECUTOR, entityState.getTenantId(), entityState.getEntityId());
        try (var connection = redisConnectionFactory.getConnection()) {
            connection.getSet(getKey(tpi, entityState.getEntityId()), JacksonUtil.writeValueAsBytes(entityState));
        }
    }

    @Override
    public void remove(TenantId tenantId, EntityId entityId) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_ALARM_RULES_EXECUTOR, tenantId, entityId);
        try (var connection = redisConnectionFactory.getConnection()) {
            connection.del(getKey(tpi, entityId));
        }
    }

    @Override
    public List<PersistedEntityState> getAll(TopicPartitionInfo tpi) {
        try (var connection = redisConnectionFactory.getConnection()) {
            return RedisUtil.getAll(connection, tpi.getFullTopicName(), bytes -> JacksonUtil.fromBytes(bytes, PersistedEntityState.class));
        }
    }

    private byte[] getKey(TopicPartitionInfo tpi, EntityId entityId) {
        return String.format("%s::%s", tpi.getFullTopicName(), entityId).getBytes();
    }
}
