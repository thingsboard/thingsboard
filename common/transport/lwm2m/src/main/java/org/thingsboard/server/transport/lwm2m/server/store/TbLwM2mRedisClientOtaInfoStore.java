// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.store;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.transport.lwm2m.server.ota.LwM2MClientOtaInfo;
import org.thingsboard.server.transport.lwm2m.server.ota.firmware.LwM2MClientFwOtaInfo;
import org.thingsboard.server.transport.lwm2m.server.ota.software.LwM2MClientSwOtaInfo;

public class TbLwM2mRedisClientOtaInfoStore implements TbLwM2MClientOtaInfoStore {
    private static final String OTA_EP = "OTA#EP#";

    private final RedisConnectionFactory connectionFactory;

    public TbLwM2mRedisClientOtaInfoStore(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    private void put(OtaPackageType type, LwM2MClientOtaInfo<?, ?, ?> info) {
        try (var connection = connectionFactory.getConnection()) {
            connection.stringCommands().set((OTA_EP + type + info.getEndpoint()).getBytes(), JacksonUtil.toString(info).getBytes());
        }
    }

    @Override
    public LwM2MClientFwOtaInfo getFw(String endpoint) {
        return getLwM2MClientOtaInfo(OtaPackageType.FIRMWARE, endpoint, LwM2MClientFwOtaInfo.class);
    }

    @Override
    public void putFw(LwM2MClientFwOtaInfo info) {
        put(OtaPackageType.FIRMWARE, info);
    }

    @Override
    public LwM2MClientSwOtaInfo getSw(String endpoint) {
        return getLwM2MClientOtaInfo(OtaPackageType.SOFTWARE, endpoint, LwM2MClientSwOtaInfo.class);
    }

    @Override
    public void putSw(LwM2MClientSwOtaInfo info) {
        put(OtaPackageType.SOFTWARE, info);
    }

    private <T extends LwM2MClientOtaInfo<?, ?, ?>> T getLwM2MClientOtaInfo(OtaPackageType type, String endpoint, Class<T> clazz) {
        try (var connection = connectionFactory.getConnection()) {
            byte[] data = connection.stringCommands().get((OTA_EP + type + endpoint).getBytes());
            return JacksonUtil.fromBytes(data, clazz);
        }
    }
}
