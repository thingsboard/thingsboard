// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.util;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class IntegrationMqttClientSettingsComponent {

    @Value("${mqtt.client.retransmission.max_attempts:3}")
    private int retransmissionMaxAttempts;

    @Value("${mqtt.client.retransmission.initial_delay_millis:5000}")
    private long retransmissionInitialDelayMillis;

    @Value("${mqtt.client.retransmission.jitter_factor:0.15}")
    private double retransmissionJitterFactor;

    @Value("${integrations.init.connection_timeout_sec:10}")
    private int integrationConnectTimeoutSec;

    @Value("${integrations.back_pressure.high_watermark:450}")
    private int backPressureHighWatermark;

    @Value("${integrations.back_pressure.low_watermark:200}")
    private int backPressureLowWatermark;

}
