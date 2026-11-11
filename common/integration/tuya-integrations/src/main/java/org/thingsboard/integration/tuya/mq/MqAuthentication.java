// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.tuya.mq;

import lombok.RequiredArgsConstructor;
import org.apache.pulsar.client.api.Authentication;
import org.apache.pulsar.client.api.PulsarClientException;

import java.io.IOException;
import java.util.Map;

@RequiredArgsConstructor
public class MqAuthentication implements Authentication {

    private final String accessId;
    private final String accessKey;

    @Override
    public String getAuthMethodName() {
        return "auth1";
    }

    @Override
    public org.apache.pulsar.client.api.AuthenticationDataProvider getAuthData() throws PulsarClientException {
        return new MqAuthenticationDataProvider(this.accessId, this.accessKey);
    }

    @Override
    public void configure(Map<String, String> map) {
    }

    @Override
    public void start() throws PulsarClientException {
    }

    @Override
    public void close() throws IOException {
    }
}
