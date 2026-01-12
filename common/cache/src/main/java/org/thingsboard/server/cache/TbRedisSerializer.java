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

import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.lang.Nullable;

public interface TbRedisSerializer<K, T> {

    @Nullable
    byte[] serialize(@Nullable T t) throws SerializationException;

    @Nullable
    T deserialize(K key, @Nullable byte[] bytes) throws SerializationException;

}
