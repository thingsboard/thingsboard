// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.mqtt.credentials;

import io.netty.handler.ssl.SslContext;
import org.thingsboard.mqtt.MqttClientConfig;

import java.util.Optional;

public class AnonymousCredentials implements MqttClientCredentials {

    @Override
    public Optional<SslContext> initSslContext() {
        return Optional.empty();
    }

    @Override
    public void configure(MqttClientConfig config) {

    }
}
