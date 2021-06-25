/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import org.nustaq.serialization.FSTConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;

public class TbRedisLwM2MClientStore implements TbLwM2MClientStore {

    private static final String CLIENT_EP = "CLIENT#EP#";
    private final RedisConnectionFactory connectionFactory;
    private final FSTConfiguration serializer;

    public TbRedisLwM2MClientStore(RedisConnectionFactory redisConnectionFactory) {
        this.connectionFactory = redisConnectionFactory;
        this.serializer = FSTConfiguration.createDefaultConfiguration();
    }

    @Override
    public LwM2mClient get(String endpoint) {
        try (var connection = connectionFactory.getConnection()) {
            byte[] data = connection.get(getKey(endpoint));
            if (data == null) {
                return null;
            } else {
                return (LwM2mClient) serializer.asObject(data);
            }
        }
    }

    @Override
    public void put(LwM2mClient client) {
        byte[] clientSerialized = serializer.asByteArray(client);
        try (var connection = connectionFactory.getConnection()) {
            connection.getSet(getKey(client.getEndpoint()), clientSerialized);
        }
    }

    @Override
    public void remove(String endpoint) {
        try (var connection = connectionFactory.getConnection()) {
            connection.del(getKey(endpoint));
        }
    }

    private byte[] getKey(String endpoint) {
        return (CLIENT_EP + endpoint).getBytes();
    }
}
