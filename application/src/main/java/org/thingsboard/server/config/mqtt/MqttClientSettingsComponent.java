/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
