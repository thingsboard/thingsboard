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

import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.nustaq.serialization.FSTConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MSecurityInfo;
import org.thingsboard.server.transport.lwm2m.server.ota.LwM2MClientOtaInfo;

import java.util.concurrent.locks.Lock;

public class TbLwM2mRedisClientOtaInfoStore implements TbLwM2MClientOtaInfoStore {
    private static final String OTA_EP = "OTA#EP#";

    private final RedisConnectionFactory connectionFactory;

    public TbLwM2mRedisClientOtaInfoStore(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public LwM2MClientOtaInfo get(OtaPackageType type, String endpoint) {
        try (var connection = connectionFactory.getConnection()) {
            byte[] data = connection.get((OTA_EP + type + endpoint).getBytes());
            return JacksonUtil.fromBytes(data, LwM2MClientOtaInfo.class);
        }
    }

    @Override
    public void put(LwM2MClientOtaInfo info) {
        try (var connection = connectionFactory.getConnection()) {
            connection.set((OTA_EP + info.getType() + info.getEndpoint()).getBytes(), JacksonUtil.toString(info).getBytes());
        }
    }

}
