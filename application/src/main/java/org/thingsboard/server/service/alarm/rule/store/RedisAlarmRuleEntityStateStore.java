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
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.alarm.rule.state.PersistedEntityState;

import java.util.List;

import static org.thingsboard.server.common.data.DataConstants.INTERNAL_QUEUE_NAME;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
public class RedisAlarmRuleEntityStateStore implements AlarmRuleEntityStateStore {

    private static final String KEY_PREFIX = "AR_ENTITY_STATE";

    private final PartitionService partitionService;
    private final RedisConnectionFactory redisConnectionFactory;

    @Override
    public void put(PersistedEntityState entityState) {
        try (var connection = redisConnectionFactory.getConnection()) {
            connection.getSet(getKey(entityState.getTenantId(), entityState.getEntityId()), JacksonUtil.writeValueAsBytes(entityState));
        }
    }

    @Override
    public void remove(TenantId tenantId, EntityId entityId) {
        try (var connection = redisConnectionFactory.getConnection()) {
            connection.del(getKey(tenantId, entityId));
        }
    }

    @Override
    public List<PersistedEntityState> getAll(TenantId tenantId) {
        try (var connection = redisConnectionFactory.getConnection()) {
            return RedisUtil.getAll(connection, getKeyPattern(tenantId), bytes -> JacksonUtil.fromBytes(bytes, PersistedEntityState.class), state -> isLocalEntity(tenantId, state.getEntityId()));
        }
    }

    private String getKeyPattern(TenantId tenantId) {
        return tenantId.isSysTenantId() ? KEY_PREFIX : String.format("%s::%s", KEY_PREFIX, tenantId);
    }

    private byte[] getKey(TenantId tenantId, EntityId entityId) {
        return String.format("%s::%s::%s", KEY_PREFIX, tenantId, entityId).getBytes();
    }

    private boolean isLocalEntity(TenantId tenantId, EntityId entityId) {
        return partitionService.resolve(ServiceType.TB_RULE_ENGINE, INTERNAL_QUEUE_NAME, tenantId, entityId).isMyPartition();
    }
}
