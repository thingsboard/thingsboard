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
package org.thingsboard.server.cache.rule;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@RequiredArgsConstructor
public class RedisRuleNodeCache implements RuleNodeCache {

    private static final int SCAN_COUNT = 1000;
    private static final ScanOptions OPTIONS = ScanOptions.scanOptions().count(SCAN_COUNT).build();

    private final RedisConnectionFactory redisConnectionFactory;

    @Override
    public void add(byte[] key, byte[] value) {
        if (value == null || value.length == 0) {
            return;
        }
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.setCommands().sAdd(key, value);
        }
    }

    @Override
    public void remove(byte[] key, byte[][] values) {
        if (values == null || values.length == 0) {
            return;
        }
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.setCommands().sRem(key, values);
        }
    }

    @Override
    public Set<byte[]> get(byte[] key) {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            Set<byte[]> bytes = new HashSet<>();
            Cursor<byte[]> cursor = connection.setCommands().sScan(key, OPTIONS);
            while (cursor.hasNext()) {
                bytes.add(cursor.next());
            }
            return bytes;
        }
    }

    @Override
    public void evict(byte[] key) {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.del(key);
        }
    }

}
