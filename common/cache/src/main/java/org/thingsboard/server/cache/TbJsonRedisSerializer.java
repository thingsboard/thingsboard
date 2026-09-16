// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.cache;

import org.springframework.data.redis.serializer.SerializationException;
import org.thingsboard.common.util.JacksonUtil;

import java.io.IOException;

public class TbJsonRedisSerializer<K, V> implements TbRedisSerializer<K, V> {

    private final Class<V> clazz;

    public TbJsonRedisSerializer(Class<V> clazz) {
        this.clazz = clazz;
    }

    @Override
    public byte[] serialize(V v) throws SerializationException {
        return JacksonUtil.writeValueAsBytes(v);
    }

    @Override
    public V deserialize(K key, byte[] bytes) throws SerializationException {
        if (bytes == null) {
            return null;
        }
        try {
            return JacksonUtil.IGNORE_UNKNOWN_PROPERTIES_JSON_MAPPER.readValue(bytes, clazz);
        } catch (IOException e) {
            throw new SerializationException("Failed to deserialize cached value", e);
        }
    }
}
