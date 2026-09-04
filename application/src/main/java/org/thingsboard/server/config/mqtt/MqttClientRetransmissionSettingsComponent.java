// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.config.mqtt;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "mqtt.client.retransmission")
public class MqttClientRetransmissionSettingsComponent {

    @PositiveOrZero
    private int maxAttempts;
    @PositiveOrZero
    private long initialDelayMillis;
    @PositiveOrZero
    private double jitterFactor;

}
