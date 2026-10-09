// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.device;

import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.msg.EncryptionUtil;

/**
 * Single source of truth for the {@code credentialsId} derived from MQTT_BASIC client id / user name.
 * Written by {@link DeviceCredentialsServiceImpl} and used as a lookup key by everything that resolves a
 * device from a gateway configuration, so both sides must agree byte-for-byte.
 */
public final class BasicMqttCredentialsIds {

    private BasicMqttCredentialsIds() {
    }

    /**
     * @return the credentials id for the given pair, or {@code null} when both parts are empty.
     */
    public static String toCredentialsId(String clientId, String userName) {
        if (StringUtils.isEmpty(clientId) && StringUtils.isEmpty(userName)) {
            return null;
        }
        if (StringUtils.isEmpty(clientId)) {
            return userName;
        }
        if (StringUtils.isEmpty(userName)) {
            return EncryptionUtil.getSha3Hash(clientId);
        }
        return EncryptionUtil.getSha3Hash("|", clientId, userName);
    }
}
