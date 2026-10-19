// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.tuya.mq;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.pulsar.client.api.AuthenticationDataProvider;

public class MqAuthenticationDataProvider implements AuthenticationDataProvider {

    private final String commandData;

    public MqAuthenticationDataProvider(String accessId, String accessKey) {
        this.commandData = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", accessId,
                DigestUtils.md5Hex(accessId + DigestUtils.md5Hex(accessKey)).substring(8, 24));
    }

    @Override
    public String getCommandData() {
        return commandData;
    }

    @Override
    public boolean hasDataForHttp() {
        return false;
    }

    @Override
    public boolean hasDataFromCommand() {
        return true;
    }

}
