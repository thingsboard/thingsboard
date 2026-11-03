// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.attributes;

import com.google.protobuf.InvalidProtocolBufferException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.cache.TbRedisSerializer;
import org.thingsboard.server.cache.VersionedRedisTbCache;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeValueProto;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("AttributeCache")
public class AttributeRedisCache extends VersionedRedisTbCache<AttributeCacheKey, AttributeKvEntry> {

    public AttributeRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.ATTRIBUTES_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbRedisSerializer<>() {
            @Override
            public byte[] serialize(AttributeKvEntry attributeKvEntry) throws SerializationException {
                return ProtoUtils.toProto(attributeKvEntry).toByteArray();
            }

            @Override
            public AttributeKvEntry deserialize(AttributeCacheKey key, byte[] bytes) throws SerializationException {
                try {
                    return ProtoUtils.fromProto(AttributeValueProto.parseFrom(bytes));
                } catch (InvalidProtocolBufferException e) {
                    throw new SerializationException(e.getMessage());
                }
            }
        });
    }

}
