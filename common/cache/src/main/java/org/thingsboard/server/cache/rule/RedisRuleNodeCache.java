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

import java.util.Set;

@Service
@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@RequiredArgsConstructor
public class RedisRuleNodeCache implements RuleNodeCache {

    private final RedisConnectionFactory redisConnectionFactory;

    @Override
    public void add(String key, byte[]... values) {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.setCommands().sAdd(key.getBytes(), values);
        }
    }

    @Override
    public void remove(String key, byte[]... values) {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.setCommands().sRem(key.getBytes(), values);
        }
    }

    @Override
    public Set<byte[]> get(String key) {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            return connection.setCommands().sMembers(key.getBytes());
        }
    }

    @Override
    public void evict(String key) {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.del(key.getBytes());
        }
    }

}
