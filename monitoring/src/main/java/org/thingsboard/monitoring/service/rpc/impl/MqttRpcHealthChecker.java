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
package org.thingsboard.monitoring.service.rpc.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.monitoring.config.rpc.MqttRpcMonitoringConfig;
import org.thingsboard.monitoring.config.rpc.RpcMonitoringTarget;
import org.thingsboard.monitoring.config.rpc.RpcTransportType;
import org.thingsboard.monitoring.service.rpc.BaseRpcHealthChecker;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class MqttRpcHealthChecker extends BaseRpcHealthChecker<MqttRpcMonitoringConfig> {

    private static final String RPC_REQUEST_TOPIC = "v1/devices/me/rpc/request/+";
    private static final String RPC_REQUEST_TOPIC_PREFIX = "v1/devices/me/rpc/request/";
    private static final String RPC_RESPONSE_TOPIC_PREFIX = "v1/devices/me/rpc/response/";

    private MqttClient mqttClient;

    protected MqttRpcHealthChecker(MqttRpcMonitoringConfig config, RpcMonitoringTarget target) {
        super(config, target);
    }

    @Override
    protected void initClient() throws Exception {
        if (mqttClient != null && mqttClient.isConnected()) {
            return;
        }
        String clientId = MqttAsyncClient.generateClientId();
        mqttClient = createMqttClient(target.getBaseUrl(), clientId);
        mqttClient.setTimeToWait(config.getRequestTimeoutMs());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(target.getAccessToken());
        options.setConnectionTimeout(config.getRequestTimeoutMs() / 1000);

        IMqttToken result = mqttClient.connectWithResult(options);
        if (result.getException() != null) {
            throw result.getException();
        }

        // Subscribe to server-side RPC requests and echo params back as the response.
        // The callback runs on Paho's internal thread while the two-way RPC REST call
        // blocks the checker thread, so TB receives the reply and unblocks the caller.
        mqttClient.subscribe(RPC_REQUEST_TOPIC, config.getQos(), (topic, message) -> {
            String requestId = topic.substring(RPC_REQUEST_TOPIC_PREFIX.length());
            JsonNode request = JacksonUtil.toJsonNode(new String(message.getPayload()));
            JsonNode params = request.path("params");

            MqttMessage response = new MqttMessage(params.toString().getBytes());
            response.setQos(config.getQos());
            mqttClient.publish(RPC_RESPONSE_TOPIC_PREFIX + requestId, response);
            log.trace("[{}] Replied to RPC request {}", getInfo(), requestId);
        });

        log.info("[{}] Connected and subscribed for RPC", getInfo());
    }

    @Override
    protected void destroyClient() throws Exception {
        if (mqttClient != null) {
            mqttClient.disconnect();
            mqttClient = null;
            log.info("[{}] Disconnected MQTT RPC client", getInfo());
        }
    }

    @Override
    protected RpcTransportType getTransportType() {
        return RpcTransportType.MQTT;
    }

    protected MqttClient createMqttClient(String brokerUrl, String clientId) throws MqttException {
        return new MqttClient(brokerUrl, clientId, new MemoryPersistence());
    }

}
