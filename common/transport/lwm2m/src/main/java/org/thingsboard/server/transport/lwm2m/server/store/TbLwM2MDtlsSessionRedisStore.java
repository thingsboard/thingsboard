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
package org.thingsboard.server.transport.lwm2m.server.store;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.thingsboard.server.common.data.JavaSerDesUtil;
import org.thingsboard.server.transport.lwm2m.secure.TbX509DtlsSessionInfo;

public class TbLwM2MDtlsSessionRedisStore implements TbLwM2MDtlsSessionStore {

    private static final String SESSION_EP = "SESSION#EP#";
    private final RedisConnectionFactory connectionFactory;

    public TbLwM2MDtlsSessionRedisStore(RedisConnectionFactory redisConnectionFactory) {
        this.connectionFactory = redisConnectionFactory;
    }

    @Override
    public void put(String endpoint, TbX509DtlsSessionInfo msg) {
        try (var c = connectionFactory.getConnection()) {
            var serializedMsg = JavaSerDesUtil.encode(msg);
            if (serializedMsg != null) {
                c.set(getKey(endpoint), serializedMsg);
            } else {
                throw new RuntimeException("Problem with serialization of message: " + msg);
            }
        }
    }

    @Override
    public TbX509DtlsSessionInfo get(String endpoint) {
        try (var c = connectionFactory.getConnection()) {
            var data = c.get(getKey(endpoint));
            if (data != null) {
                return JavaSerDesUtil.decode(data);
            } else {
                return null;
            }
        }
    }

    @Override
    public void remove(String endpoint) {
        try (var c = connectionFactory.getConnection()) {
            c.del(getKey(endpoint));
        }
    }

    private byte[] getKey(String endpoint) {
        return (SESSION_EP + endpoint).getBytes();
    }
}
