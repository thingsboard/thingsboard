// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.timeseries;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.cache.TbRedisSerializer;
import org.thingsboard.server.cache.VersionedRedisTbCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.util.KvProtoUtil;
import org.thingsboard.server.gen.transport.TransportProtos;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("TsLatestCache")
@Slf4j
public class TsLatestRedisCache extends VersionedRedisTbCache<TsLatestCacheKey, TsKvEntry> {

    public TsLatestRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.TS_LATEST_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbRedisSerializer<>() {
            @Override
            public byte[] serialize(TsKvEntry tsKvEntry) throws SerializationException {
                return KvProtoUtil.toTsKvProto(tsKvEntry.getTs(), tsKvEntry, tsKvEntry.getVersion()).toByteArray();
            }

            @Override
            public TsKvEntry deserialize(TsLatestCacheKey key, byte[] bytes) throws SerializationException {
                try {
                    return KvProtoUtil.fromTsKvProto(TransportProtos.TsKvProto.parseFrom(bytes));
                } catch (InvalidProtocolBufferException e) {
                    throw new SerializationException(e.getMessage());
                }
            }
        });
    }
}
