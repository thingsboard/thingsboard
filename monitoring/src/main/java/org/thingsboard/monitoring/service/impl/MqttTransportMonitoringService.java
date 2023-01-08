/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.monitoring.service.impl;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.thingsboard.monitoring.config.MonitoringTargetConfig;
import org.thingsboard.monitoring.config.TransportType;
import org.thingsboard.monitoring.config.service.MqttTransportMonitoringServiceConfig;
import org.thingsboard.monitoring.service.TransportMonitoringService;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MqttTransportMonitoringService extends TransportMonitoringService<MqttTransportMonitoringServiceConfig> {

    private MqttClient mqttClient;

    private static final String DEVICE_TELEMETRY_TOPIC = "v1/devices/me/telemetry";

    protected MqttTransportMonitoringService(MqttTransportMonitoringServiceConfig config, MonitoringTargetConfig target) {
        super(config, target);
    }

    @Override
    protected void initClient() throws Exception {
        if (mqttClient == null || !mqttClient.isConnected()) {
            String clientId = MqttAsyncClient.generateClientId();
            String accessToken = target.getDevice().getCredentials().getCredentialsId();
            mqttClient = new MqttClient(target.getBaseUrl(), clientId, new MemoryPersistence());
            mqttClient.setTimeToWait(config.getRequestTimeoutMs());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(accessToken);
            options.setConnectionTimeout(config.getRequestTimeoutMs() / 1000);
            IMqttToken result = mqttClient.connectWithResult(options);
            if (result.getException() != null) {
                throw result.getException();
            }
        }
    }

    @Override
    protected void sendTestPayload(String payload) throws Exception {
        MqttMessage message = new MqttMessage();
        message.setPayload(payload.getBytes());
        message.setQos(config.getQos());
        mqttClient.publish(DEVICE_TELEMETRY_TOPIC, message);
    }

    @Override
    protected void destroyClient() throws Exception {
        if (mqttClient != null) {
            mqttClient.disconnect();
            mqttClient = null;
        }
    }

    @Override
    protected TransportType getTransportType() {
        return TransportType.MQTT;
    }

}
