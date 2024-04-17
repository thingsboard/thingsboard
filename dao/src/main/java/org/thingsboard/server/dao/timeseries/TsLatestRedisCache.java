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
package org.thingsboard.server.dao.timeseries;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.RedisTbTransactionalCache;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.cache.TbCacheTransaction;
import org.thingsboard.server.cache.TbCacheValueWrapper;
import org.thingsboard.server.cache.TbJavaRedisSerializer;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("TsLatestCache")
@Slf4j
public class TsLatestRedisCache<K extends Serializable, V extends Serializable> extends RedisTbTransactionalCache<TsLatestCacheKey, TsKvEntry> {

    static final byte[] UPSERT_TS_LATEST_LUA_SCRIPT = StringRedisSerializer.UTF_8.serialize("" +
            "redis.call('ZREMRANGEBYSCORE', KEYS[1], ARGV[1], ARGV[1]); " +
            "redis.call('ZADD', KEYS[1], ARGV[1], ARGV[2]); " +
            "local current_size = redis.call('ZCARD', KEYS[1]); " +
            "if current_size > 1 then" +
            "  redis.call('ZREMRANGEBYRANK', KEYS[1], 0, -2) " +
            "end;");
    static final byte[] UPSERT_TS_LATEST_SHA = StringRedisSerializer.UTF_8.serialize("24e226c3ea34e3e850113e8eb1f3cd2b88171988");

    public TsLatestRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.TS_LATEST_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbJavaRedisSerializer<>());
    }

    @PostConstruct
    public void init() {
        try (var connection = getConnection(UPSERT_TS_LATEST_SHA)) {
            log.debug("Loading LUA with expected SHA[{}], connection [{}]", new String(UPSERT_TS_LATEST_SHA), connection.getNativeConnection());
            String sha = connection.scriptingCommands().scriptLoad(UPSERT_TS_LATEST_LUA_SCRIPT);
            if (!Arrays.equals(UPSERT_TS_LATEST_SHA, StringRedisSerializer.UTF_8.serialize(sha))) {
                log.error("SHA for UPSERT_TS_LATEST_LUA_SCRIPT wrong! Expected [{}], but actual [{}], connection [{}]", new String(UPSERT_TS_LATEST_SHA), sha, connection.getNativeConnection());
            }
        } catch (Throwable t) {
            log.error("Error on Redis TS Latest cache init", t);
        }
    }

    @Override
    public TbCacheValueWrapper<TsKvEntry> get(TsLatestCacheKey key) {
        log.debug("get [{}]", key);
        return super.get(key);
    }

    @Override
    protected byte[] doGet(RedisConnection connection, byte[] rawKey) {
        log.trace("doGet [{}][{}]", connection, rawKey);
        Set<byte[]> values = connection.commands().zRange(rawKey, -1, -1);
        return values == null ? null : values.stream().findFirst().orElse(null);
    }

    @Override
    public void put(TsLatestCacheKey key, TsKvEntry value) {
        log.trace("put [{}][{}]", key, value);
        final byte[] rawKey = getRawKey(key);
        try (var connection = getConnection(rawKey)) {
            byte[] rawValue = getRawValue(value);
            byte[] ts = StringRedisSerializer.UTF_8.serialize(String.valueOf(value.toTsValue().getTs()));
            try {
                connection.scriptingCommands().evalSha(UPSERT_TS_LATEST_SHA, ReturnType.VALUE, 1, rawKey, ts, rawValue);
            } catch (InvalidDataAccessApiUsageException e) {
                log.debug("loading LUA [{}]", connection.getNativeConnection());
                String sha = connection.scriptingCommands().scriptLoad(UPSERT_TS_LATEST_LUA_SCRIPT);
                if (!Arrays.equals(UPSERT_TS_LATEST_SHA, StringRedisSerializer.UTF_8.serialize(sha))) {
                    log.error("SHA for UPSERT_TS_LATEST_LUA_SCRIPT wrong! Expected [{}], but actual [{}]", new String(UPSERT_TS_LATEST_SHA), sha);
                }
                try {
                    connection.scriptingCommands().evalSha(UPSERT_TS_LATEST_SHA, ReturnType.VALUE, 1, rawKey, ts, rawValue);
                } catch (InvalidDataAccessApiUsageException ignored) {
                    log.debug("Slowly executing eval instead of fast evalsha");
                    connection.scriptingCommands().eval(UPSERT_TS_LATEST_LUA_SCRIPT, ReturnType.VALUE, 1, rawKey, ts, rawValue);
                }

            }
        }
    }

    @Override
    public void evict(TsLatestCacheKey key) {
        log.trace("evict [{}]", key);
        final byte[] rawKey = getRawKey(key);
        try (var connection = getConnection(rawKey)) {
            connection.keyCommands().del(rawKey);
        }
    }

    @Override
    public void putIfAbsent(TsLatestCacheKey key, TsKvEntry value) {
        log.trace("putIfAbsent [{}][{}]", key, value);
        throw new NotImplementedException("putIfAbsent is not supported by TsLatestRedisCache");
    }

    @Override
    public void evict(Collection<TsLatestCacheKey> keys) {
        throw new NotImplementedException("evict by many keys is not supported by TsLatestRedisCache");
    }

    @Override
    public void evictOrPut(TsLatestCacheKey key, TsKvEntry value) {
        throw new NotImplementedException("evictOrPut is not supported by TsLatestRedisCache");
    }

    @Override
    public TbCacheTransaction<TsLatestCacheKey, TsKvEntry> newTransactionForKey(TsLatestCacheKey key) {
        throw new NotImplementedException("newTransactionForKey is not supported by TsLatestRedisCache");
    }

    @Override
    public TbCacheTransaction<TsLatestCacheKey, TsKvEntry> newTransactionForKeys(List<TsLatestCacheKey> keys) {
        throw new NotImplementedException("newTransactionForKeys is not supported by TsLatestRedisCache");
    }

}
