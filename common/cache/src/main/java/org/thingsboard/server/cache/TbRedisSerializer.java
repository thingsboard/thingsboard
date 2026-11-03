// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.cache;

import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.lang.Nullable;

public interface TbRedisSerializer<K, T> {

    @Nullable
    byte[] serialize(@Nullable T t) throws SerializationException;

    @Nullable
    T deserialize(K key, @Nullable byte[] bytes) throws SerializationException;

}
