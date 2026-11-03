// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.cache.device;

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
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.transport.TransportProtos;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("DeviceCache")
public class DeviceRedisCache extends VersionedRedisTbCache<DeviceCacheKey, Device> {

    public DeviceRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory) {
        super(CacheConstants.DEVICE_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbRedisSerializer<>() {

            @Override
            public byte[] serialize(Device device) throws SerializationException {
                return ProtoUtils.toProto(device).toByteArray();
            }

            @Override
            public Device deserialize(DeviceCacheKey key, byte[] bytes) throws SerializationException {
                try {
                    return ProtoUtils.fromProto(TransportProtos.DeviceProto.parseFrom(bytes));
                } catch (InvalidProtocolBufferException e) {
                    throw new SerializationException(e.getMessage());
                }
            }
        });
    }
}
