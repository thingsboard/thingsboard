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
package org.thingsboard.monitoring.service.transport.impl;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.monitoring.config.transport.MqttTransportMonitoringConfig;
import org.thingsboard.monitoring.config.transport.TransportMonitoringTarget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MqttTransportHealthCheckerTest {

    @Test
    public void sendAcceptedTestPayload_alwaysUsesQos1_regardlessOfConfiguredQos() throws Exception {
        MqttTransportMonitoringConfig config = new MqttTransportMonitoringConfig();
        config.setQos(0);
        MqttTransportHealthChecker checker = new MqttTransportHealthChecker(config, new TransportMonitoringTarget());
        MqttClient mqttClient = mock(MqttClient.class);
        ReflectionTestUtils.setField(checker, "mqttClient", mqttClient);

        checker.sendAcceptedTestPayload("{}");

        ArgumentCaptor<MqttMessage> captor = ArgumentCaptor.forClass(MqttMessage.class);
        verify(mqttClient).publish(anyString(), captor.capture());
        assertThat(captor.getValue().getQos()).isEqualTo(1);
    }

    @Test
    public void sendTestPayload_stillUsesConfiguredQos() throws Exception {
        MqttTransportMonitoringConfig config = new MqttTransportMonitoringConfig();
        config.setQos(0);
        MqttTransportHealthChecker checker = new MqttTransportHealthChecker(config, new TransportMonitoringTarget());
        MqttClient mqttClient = mock(MqttClient.class);
        ReflectionTestUtils.setField(checker, "mqttClient", mqttClient);

        checker.sendTestPayload("{}");

        ArgumentCaptor<MqttMessage> captor = ArgumentCaptor.forClass(MqttMessage.class);
        verify(mqttClient).publish(anyString(), captor.capture());
        assertThat(captor.getValue().getQos()).isEqualTo(0);
    }

}
