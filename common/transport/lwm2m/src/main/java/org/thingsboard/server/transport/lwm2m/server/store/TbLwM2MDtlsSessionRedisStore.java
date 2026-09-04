// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
                c.stringCommands().set(getKey(endpoint), serializedMsg);
            } else {
                throw new RuntimeException("Problem with serialization of message: " + msg);
            }
        }
    }

    @Override
    public TbX509DtlsSessionInfo get(String endpoint) {
        try (var c = connectionFactory.getConnection()) {
            var data = c.stringCommands().get(getKey(endpoint));
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
            c.keyCommands().del(getKey(endpoint));
        }
    }

    private byte[] getKey(String endpoint) {
        return (SESSION_EP + endpoint).getBytes();
    }
}
