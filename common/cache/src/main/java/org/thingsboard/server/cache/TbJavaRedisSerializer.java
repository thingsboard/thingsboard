// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.cache;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

public class TbJavaRedisSerializer<K, V> implements TbRedisSerializer<K, V> {

    final RedisSerializer<Object> serializer = RedisSerializer.java();

    @Override
    public byte[] serialize(V value) throws SerializationException {
        return serializer.serialize(value);
    }

    @Override
    public V deserialize(K key, byte[] bytes) throws SerializationException {
        return (V) serializer.deserialize(bytes);
    }

}
