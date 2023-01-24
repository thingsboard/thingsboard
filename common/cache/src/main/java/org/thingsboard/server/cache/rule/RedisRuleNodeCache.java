/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.cache.rule;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.SerializationUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@RequiredArgsConstructor
public class RedisRuleNodeCache implements RuleNodeCache {

    private final RedisConnectionFactory redisConnectionFactory;

    @Override
    public void add(String key, String value) {
        processAdd(key, value.getBytes());
    }

    @Override
    public void add(String key, EntityId value) {
        processAdd(key, SerializationUtils.serialize(value));
    }

    @Override
    public void add(String key, TbMsg value) {
        processAdd(key, TbMsg.toByteArray(value));
    }

    @Override
    public void removeStringList(String key, List<String> values) {
        processRemove(key, stringListToBytes(values));
    }

    @Override
    public void removeEntityIdList(String key, List<EntityId> values) {
        processRemove(key, entityIdListToBytes(values));
    }

    @Override
    public void removeTbMsgList(String key, List<TbMsg> values) {
        processRemove(key, tbMsgListToBytes(values));
    }

    @Override
    public Set<String> getStringSetByKey(String key) {
        return toStringSet(processGetMembers(key));
    }

    @Override
    public Set<EntityId> getEntityIdSetByKey(String key) {
        return toEntityIdSet(processGetMembers(key));
    }

    @Override
    public Set<TbMsg> getTbMsgSetByKey(String key, String queueName) {
        return toTbMsgSet(processGetMembers(key), queueName);
    }

    @Override
    public void evict(String key) {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.del(key.getBytes());
        }
    }

    private void processAdd(String key, byte[] value) {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.setCommands().sAdd(key.getBytes(), value);
        }
    }

    private void processRemove(String key, byte[][] values) {
        if (values.length == 0) {
            return;
        }
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.setCommands().sRem(key.getBytes(), values);
        }
    }

    private Set<byte[]> processGetMembers(String key) {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            Set<byte[]> bytes = connection.setCommands().sMembers(key.getBytes());
            if (bytes == null) {
                return Collections.emptySet();
            }
            return bytes;
        }
    }

    private byte[][] stringListToBytes(List<String> values) {
        return values.stream()
                .map(String::getBytes)
                .toArray(byte[][]::new);
    }

    private byte[][] entityIdListToBytes(List<EntityId> values) {
        return values.stream()
                .map(SerializationUtils::serialize)
                .toArray(byte[][]::new);
    }

    private byte[][] tbMsgListToBytes(List<TbMsg> values) {
        return values.stream()
                .map(TbMsg::toByteArray)
                .toArray(byte[][]::new);
    }

    private Set<String> toStringSet(Set<byte[]> values) {
        return values.stream()
                .map(String::new)
                .collect(Collectors.toSet());
    }

    private Set<EntityId> toEntityIdSet(Set<byte[]> values) {
        return values.stream()
                .map(bytes -> (EntityId) SerializationUtils.deserialize(bytes))
                .collect(Collectors.toSet());
    }

    private Set<TbMsg> toTbMsgSet(Set<byte[]> values, String queueName) {
        return values.stream()
                .map(bytes -> TbMsg.fromBytes(queueName, bytes, TbMsgCallback.EMPTY))
                .collect(Collectors.toSet());
    }

}
