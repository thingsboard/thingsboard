// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.auth.cache;

import com.google.protobuf.InvalidProtocolBufferException;
import org.springframework.data.redis.serializer.SerializationException;
import org.thingsboard.server.cache.TbRedisSerializer;
import org.thingsboard.server.gen.transport.TransportProtos.TokenValidationCacheEntryProto;

public class TokenValidationCacheEntrySerializer implements TbRedisSerializer<String, TokenValidationCacheEntryProto> {

    @Override
    public byte[] serialize(TokenValidationCacheEntryProto entry) throws SerializationException {
        return entry.toByteArray();
    }

    @Override
    public TokenValidationCacheEntryProto deserialize(String key, byte[] bytes) throws SerializationException {
        try {
            return TokenValidationCacheEntryProto.parseFrom(bytes);
        } catch (InvalidProtocolBufferException e) {
            throw new SerializationException("Failed to deserialize token validation cache entry", e);
        }
    }

}
