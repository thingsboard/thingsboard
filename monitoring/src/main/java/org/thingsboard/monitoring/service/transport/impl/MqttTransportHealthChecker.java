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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.monitoring.config.transport.MqttTransportMonitoringConfig;
import org.thingsboard.monitoring.config.transport.RpcInfo;
import org.thingsboard.monitoring.config.transport.TransportMonitoringTarget;
import org.thingsboard.monitoring.config.transport.TransportType;
import org.thingsboard.monitoring.data.Latencies;
import org.thingsboard.monitoring.data.ServiceFailureException;
import org.thingsboard.monitoring.service.transport.TransportHealthChecker;
import org.thingsboard.server.common.data.id.DeviceId;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class MqttTransportHealthChecker extends TransportHealthChecker<MqttTransportMonitoringConfig> {

    MqttClient mqttClient;
    private boolean rpcSubscribed;

    private static final String DEVICE_TELEMETRY_TOPIC = "v1/devices/me/telemetry";
    private static final String DEVICE_RPC_REQUEST_SUB_TOPIC = "v1/devices/me/rpc/request/+";
    private static final String DEVICE_RPC_RESPONSE_TOPIC_PREFIX = "v1/devices/me/rpc/response/";

    protected MqttTransportHealthChecker(MqttTransportMonitoringConfig config, TransportMonitoringTarget target) {
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
            log.debug("Initialized MQTT client for URI {}", mqttClient.getServerURI());
            rpcSubscribed = false;
        }
        if (target.isRpcEnabled() && !rpcSubscribed) {
            mqttClient.subscribe(DEVICE_RPC_REQUEST_SUB_TOPIC, config.getQos(), this::echoRpcRequest);
            rpcSubscribed = true;
            log.debug("Subscribed for RPC requests on {}", DEVICE_RPC_REQUEST_SUB_TOPIC);
        }
    }

    void echoRpcRequest(String topic, MqttMessage request) throws Exception {
        String requestId = StringUtils.substringAfterLast(topic, "/");
        JsonNode body = JacksonUtil.toJsonNode(new String(request.getPayload(), StandardCharsets.UTF_8));
        JsonNode params = body == null ? null : body.get("params");
        byte[] responsePayload = (params == null ? "{}" : JacksonUtil.toString(params))
                .getBytes(StandardCharsets.UTF_8);
        MqttMessage response = new MqttMessage(responsePayload);
        response.setQos(config.getQos());
        mqttClient.publish(DEVICE_RPC_RESPONSE_TOPIC_PREFIX + requestId, response);
    }

    @Override
    protected void sendTestPayload(String payload) throws Exception {
        MqttMessage message = new MqttMessage();
        message.setPayload(payload.getBytes());
        message.setQos(config.getQos());
        mqttClient.publish(DEVICE_TELEMETRY_TOPIC, message);
    }

    @Override
    protected void doRpcCheck() throws Exception {
        if (!target.isRpcEnabled()) {
            return;
        }
        RpcInfo rpcInfo = getRpcInfo();
        String testValue = UUID.randomUUID().toString();
        ObjectNode body = JacksonUtil.newObjectNode();
        body.put("method", "monitoringCheck");
        body.set("params", JacksonUtil.newObjectNode().put("value", testValue));
        body.put("timeout", getRpcTimeoutMs());

        long start = System.nanoTime();
        JsonNode response;
        try {
            response = tbClient.handleTwoWayDeviceRPCRequest(new DeviceId(target.getDeviceId()), body);
        } catch (Throwable e) {
            throw new ServiceFailureException(rpcInfo, e);
        }
        String actual = response == null ? null : response.path("value").asText(null);
        if (!testValue.equals(actual)) {
            throw new ServiceFailureException(rpcInfo,
                    "RPC echo mismatch: expected " + testValue + " but got " + actual);
        }
        reportRpcLatency(System.nanoTime() - start);
        log.trace("RPC round-trip latency reported under {}", Latencies.rpcRoundTrip(getKey()));
    }

    @Override
    protected void destroyClient() throws Exception {
        if (mqttClient != null) {
            mqttClient.disconnect();
            mqttClient = null;
            rpcSubscribed = false;
            log.info("Disconnected MQTT client");
        }
    }

    @Override
    protected TransportType getTransportType() {
        return TransportType.MQTT;
    }

}
