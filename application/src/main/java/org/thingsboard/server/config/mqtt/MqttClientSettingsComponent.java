// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.config.mqtt;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.context.annotation.Configuration;
import org.thingsboard.rule.engine.api.MqttClientSettings;

@ToString
@EqualsAndHashCode
@Configuration
@RequiredArgsConstructor
public class MqttClientSettingsComponent implements MqttClientSettings {

    private final MqttClientRetransmissionSettingsComponent retransmissionSettingsComponent;

    @Override
    public int getRetransmissionMaxAttempts() {
        return retransmissionSettingsComponent.getMaxAttempts();
    }

    @Override
    public long getRetransmissionInitialDelayMillis() {
        return retransmissionSettingsComponent.getInitialDelayMillis();
    }

    @Override
    public double getRetransmissionJitterFactor() {
        return retransmissionSettingsComponent.getJitterFactor();
    }

}
