/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.cache;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.thingsboard.server.common.data.HasVersion;

import java.io.Serializable;

@Slf4j
public abstract class VersionedRedisTbCache<K extends VersionedCacheKey, V extends Serializable & HasVersion> extends RedisTbTransactionalCache<K, V> implements VersionedTbCache<K, V> {

    private static final int VERSION_SIZE = 8;
    private static final int VALUE_END_OFFSET = -1;

    static final byte[] SET_VERSIONED_VALUE_LUA_SCRIPT = StringRedisSerializer.UTF_8.serialize("""
            local key = KEYS[1]
            local newValue = ARGV[1]
            local newVersion = tonumber(ARGV[2])
            local expiration = tonumber(ARGV[3])

            local function setNewValue()
                local newValueWithVersion = struct.pack(">I8", newVersion) .. newValue
                redis.call('SET', key, newValueWithVersion, 'EX', expiration)
            end

            -- Get the current version (first 8 bytes) of the current value
            local currentVersionBytes = redis.call('GETRANGE', key, 0, 7)

            if currentVersionBytes and #currentVersionBytes == 8 then
                local currentVersion = struct.unpack(">I8", currentVersionBytes)
                if newVersion > currentVersion then
                    setNewValue()
                end
            else
                -- If the current value is absent or the current version is not found, set the new value
                setNewValue()
            end
            """);
    static final byte[] SET_VERSIONED_VALUE_SHA = StringRedisSerializer.UTF_8.serialize("0453cb1814135b706b4198b09a09f43c9f67bbfe");

    public VersionedRedisTbCache(String cacheName, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory, TBRedisCacheConfiguration configuration, TbRedisSerializer<K, V> valueSerializer) {
        super(cacheName, cacheSpecsMap, connectionFactory, configuration, valueSerializer);
    }

    @Override
    protected byte[] doGet(K key, RedisConnection connection) {
        if (!key.isVersioned()) {
            return super.doGet(key, connection);
        }
        byte[] rawKey = getRawKey(key);
        return connection.stringCommands().getRange(rawKey, VERSION_SIZE, VALUE_END_OFFSET);
    }

    @Override
    public void put(K key, V value) {
        if (!key.isVersioned()) {
            super.put(key, value);
            return;
        }
        Long version = getVersion(value);
        if (version == null) {
            return;
        }
        doPut(key, value, version, cacheTtl);
    }

    @Override
    public void put(K key, V value, RedisConnection connection) {
        if (!key.isVersioned()) {
            super.put(key, value, connection); // because scripting commands are not supported in transaction mode
            return;
        }
        Long version = getVersion(value);
        if (version == null) {
            return;
        }
        byte[] rawKey = getRawKey(key);
        doPut(rawKey, value, version, cacheTtl, connection);
    }

    private void doPut(K key, V value, Long version, Expiration expiration) {
        if (!cacheEnabled) {
            return;
        }
        log.trace("put [{}][{}][{}]", key, value, version);
        final byte[] rawKey = getRawKey(key);
        try (var connection = getConnection(rawKey)) {
            doPut(rawKey, value, version, expiration, connection);
        }
    }

    private void doPut(byte[] rawKey, V value, Long version, Expiration expiration, RedisConnection connection) {
        byte[] rawValue = getRawValue(value);
        byte[] rawVersion = StringRedisSerializer.UTF_8.serialize(String.valueOf(version));
        byte[] rawExpiration = StringRedisSerializer.UTF_8.serialize(String.valueOf(expiration.getExpirationTimeInSeconds()));
        executeScript(connection, SET_VERSIONED_VALUE_SHA, SET_VERSIONED_VALUE_LUA_SCRIPT, ReturnType.VALUE, 1, rawKey, rawValue, rawVersion, rawExpiration);
    }

    @Override
    public void evict(K key, Long version) {
        log.trace("evict [{}][{}]", key, version);
        if (version != null) {
            doPut(key, null, version, evictExpiration);
        }
    }

    @Override
    public void putIfAbsent(K key, V value) {
        throw new NotImplementedException("putIfAbsent is not supported by versioned cache");
    }

    @Override
    public void evictOrPut(K key, V value) {
        throw new NotImplementedException("evictOrPut is not supported by versioned cache");
    }

}
