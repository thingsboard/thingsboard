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
package org.thingsboard.server.cache;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.thingsboard.server.common.data.HasVersion;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Slf4j
public abstract class VersionedRedisTbTransactionalCache<K extends Serializable, V extends Serializable & HasVersion> extends RedisTbTransactionalCache<K, V> implements VersionedTbTransactionalCache<K, V> {

    private static final int VERSION_SIZE = 8;
    private static final int VALUE_END_OFFSET = -1;

    static final byte[] SET_VERSIONED_VALUE_LUA_SCRIPT = StringRedisSerializer.UTF_8.serialize("""
            -- KEYS[1] is the key
            -- ARGV[1] is the new value
            -- ARGV[2] is the new version

            local key = KEYS[1]
            local newValue = ARGV[1]
            local newVersion = tonumber(ARGV[2])

            -- Function to set the new value with the version
            local function setNewValue()
                local newValueWithVersion = struct.pack(">I8", newVersion) .. newValue:sub(9)
                redis.call('SET', key, newValueWithVersion)
            end

            -- Get the current version (first 8 bytes) of the current value
            local currentVersionBytes = redis.call('GETRANGE', key, 0, 7)

            if currentVersionBytes and #currentVersionBytes == 8 then
                -- Extract the current version from the first 8 bytes
                local currentVersion = tonumber(struct.unpack(">I8", currentVersionBytes))

                if newVersion >= currentVersion then
                    setNewValue()
                end
            else
                -- If the current value is absent or the current version is not found, set the new value
                setNewValue()
            end
            """);
    static final byte[] SET_VERSIONED_VALUE_SHA = StringRedisSerializer.UTF_8.serialize("041b109dd56f6c8afb55090076e754727a5d3da0");

    public VersionedRedisTbTransactionalCache(String cacheName, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory, TBRedisCacheConfiguration configuration, TbRedisSerializer<K, V> valueSerializer) {
        super(cacheName, cacheSpecsMap, connectionFactory, configuration, valueSerializer);
    }

    @PostConstruct
    public void init() {
        try (var connection = getConnection(SET_VERSIONED_VALUE_SHA)) {
            log.debug("Loading LUA with expected SHA[{}], connection [{}]", new String(SET_VERSIONED_VALUE_SHA), connection.getNativeConnection());
            String sha = connection.scriptingCommands().scriptLoad(SET_VERSIONED_VALUE_LUA_SCRIPT);
            if (!Arrays.equals(SET_VERSIONED_VALUE_SHA, StringRedisSerializer.UTF_8.serialize(sha))) {
                log.error("SHA for SET_VERSIONED_VALUE_LUA_SCRIPT wrong! Expected [{}], but actual [{}], connection [{}]", new String(SET_VERSIONED_VALUE_SHA), sha, connection.getNativeConnection());
            }
        } catch (Throwable t) {
            log.error("Error on Redis versioned cache init", t);
        }
    }

    @Override
    protected byte[] doGet(RedisConnection connection, byte[] rawKey) {
        return connection.stringCommands().getRange(rawKey, VERSION_SIZE, VALUE_END_OFFSET);
    }

    @Override
    public void put(K key, V value) {
        Long version = value!= null ? value.getVersion() : 0;
        put(key, value, version);
    }

    @Override
    public void put(K key, V value, Long version) {
        //TODO: use expiration
        log.trace("put [{}][{}][{}]", key, value, version);
        if (version == null) {
            return;
        }
        final byte[] rawKey = getRawKey(key);
        try (var connection = getConnection(rawKey)) {
            byte[] rawValue = getRawValue(value);
            byte[] rawVersion = StringRedisSerializer.UTF_8.serialize(String.valueOf(version));
            try {
                connection.scriptingCommands().evalSha(SET_VERSIONED_VALUE_SHA, ReturnType.VALUE, 1, rawKey, rawValue, rawVersion);
            } catch (InvalidDataAccessApiUsageException e) {
                log.debug("loading LUA [{}]", connection.getNativeConnection());
                String sha = connection.scriptingCommands().scriptLoad(SET_VERSIONED_VALUE_LUA_SCRIPT);
                if (!Arrays.equals(SET_VERSIONED_VALUE_SHA, StringRedisSerializer.UTF_8.serialize(sha))) {
                    log.error("SHA for SET_VERSIONED_VALUE_LUA_SCRIPT wrong! Expected [{}], but actual [{}]", new String(SET_VERSIONED_VALUE_SHA), sha);
                }
                try {
                    connection.scriptingCommands().evalSha(SET_VERSIONED_VALUE_SHA, ReturnType.VALUE, 1, rawKey, rawValue, rawVersion);
                } catch (InvalidDataAccessApiUsageException ignored) {
                    log.debug("Slowly executing eval instead of fast evalsha");
                    connection.scriptingCommands().eval(SET_VERSIONED_VALUE_LUA_SCRIPT, ReturnType.VALUE, 1, rawKey, rawValue, rawVersion);
                }
            }
        }
    }

    @Override
    public void evict(K key, Long version) {
        log.trace("evict [{}][{}]", key, version);
        if (version != null) {
            //TODO: use evict expiration
            put(key, null, version);
        }
    }

    @Override
    public void putIfAbsent(K key, V value) {
        log.trace("putIfAbsent [{}][{}]", key, value);
        throw new NotImplementedException("putIfAbsent is not supported by versioned cache");
    }

    @Override
    public void evict(Collection<K> keys) {
        throw new NotImplementedException("evict by many keys is not supported by versioned cache");
    }

    @Override
    public void evictOrPut(K key, V value) {
        throw new NotImplementedException("evictOrPut is not supported by versioned cache");
    }

    @Override
    public TbCacheTransaction<K, V> newTransactionForKey(K key) {
        throw new NotImplementedException("newTransactionForKey is not supported by versioned cache");
    }

    @Override
    public TbCacheTransaction<K, V> newTransactionForKeys(List<K> keys) {
        throw new NotImplementedException("newTransactionForKeys is not supported by versioned cache");
    }

}
