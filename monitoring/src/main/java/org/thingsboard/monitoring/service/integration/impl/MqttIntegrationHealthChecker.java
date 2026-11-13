// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.monitoring.service.integration.impl;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.thingsboard.monitoring.config.integration.IntegrationMonitoringTarget;
import org.thingsboard.monitoring.config.integration.IntegrationType;
import org.thingsboard.monitoring.config.integration.MqttIntegrationMonitoringConfig;
import org.thingsboard.monitoring.service.integration.IntegrationHealthChecker;

@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class MqttIntegrationHealthChecker extends IntegrationHealthChecker<MqttIntegrationMonitoringConfig> {

    private MqttClient mqttClient;

    private String topic;

    public MqttIntegrationHealthChecker(MqttIntegrationMonitoringConfig config, IntegrationMonitoringTarget target) {
        super(config, target);
    }

    @Override
    protected void initClient() throws Exception {
        if (mqttClient == null || !mqttClient.isConnected()) {
            String clientId = domain + "-integration-monitoring-" + MqttAsyncClient.generateClientId();
            String userName = target.getIntegration().getConfiguration().get("clientConfiguration").get("credentials").get("username").asText();
            mqttClient = new MqttClient(target.getBaseUrl(), clientId, new MemoryPersistence());
            mqttClient.setTimeToWait(config.getRequestTimeoutMs());
            topic = "monitoring/" + target.getIntegration().getRoutingKey();

            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(userName);
            options.setConnectionTimeout(config.getRequestTimeoutMs() / 1000);
            IMqttToken result = mqttClient.connectWithResult(options);
            if (result.getException() != null) {
                throw result.getException();
            }
            log.debug("Initialized MQTT client for URI {}", mqttClient.getServerURI());
        }
    }

    @Override
    protected void sendTestPayload(String payload) throws Exception {
        MqttMessage message = new MqttMessage();
        message.setPayload(payload.getBytes());
        message.setQos(1);
        mqttClient.publish(topic, message);
    }

    @Override
    protected void destroyClient() throws Exception {
        if (mqttClient != null) {
            mqttClient.disconnect();
            mqttClient = null;
            log.info("Disconnected MQTT client");
        }
    }

    @Override
    protected IntegrationType getIntegrationType() {
        return IntegrationType.MQTT;
    }

}
