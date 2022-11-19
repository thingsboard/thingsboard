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
package org.thingsboard.aba;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.security.DeviceCredentials;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class SaasApi {

    private static final long wsResponseWaitSec = 300;


    @Value("${saas.host}")
    private String host;

    private AtomicLong counter = new AtomicLong();
    private SampleMqttClient mqttClient;
    private AtomicLong mqttErrors = new AtomicLong();
    private AtomicLong mqttReconnects = new AtomicLong();

    public void checkMqtt(Device device, DeviceCredentials deviceCredentials, RestClient restClient, LatencyMsg latency) {
        try {
            WsClient wsClient = subscribeToWebSocket(device.getId(), "tsSubCmds", restClient, latency);

            long msgTs = System.currentTimeMillis();
            long submittedValue = counter.incrementAndGet();
            sendMqtt(deviceCredentials, submittedValue, msgTs, latency);

            WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage(wsResponseWaitSec);
            if(actualLatestTelemetry == null) {
                latency.setMqttTotalLatency(-1*wsResponseWaitSec);
            } else {
                validateWsResponse(actualLatestTelemetry, msgTs, submittedValue);
                long responseReadyTs = System.currentTimeMillis();
                latency.setMqttTotalLatency(responseReadyTs - msgTs);

            }
            wsClient.closeBlocking();
            latency.setMqttErrors(mqttErrors.getAndSet(0L));
            latency.setMqttReconnects(mqttReconnects.getAndSet(0L));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not check mqtt: " + ex.getMessage(), ex);
        }
    }

    public void checkHttp(Device device, DeviceCredentials deviceCredentials, RestClient restClient, LatencyMsg latency) {
        try {
            WsClient wsClient = subscribeToWebSocket(device.getId(), "tsSubCmds", restClient, latency);
            long msgTs = System.currentTimeMillis();
            long submittedValue = counter.incrementAndGet();
            sendHttp(restClient, deviceCredentials, submittedValue, msgTs, latency);
            WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage(wsResponseWaitSec);
            if(actualLatestTelemetry == null) {
                latency.setHttpTotalLatency(-1*wsResponseWaitSec);
            } else {
                validateWsResponse(actualLatestTelemetry, msgTs, submittedValue);
                long responseReadyTs = System.currentTimeMillis();
                latency.setHttpTotalLatency(responseReadyTs - msgTs);
            }
            wsClient.closeBlocking();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not check http: " + ex.getMessage(), ex);
        }
    }


    private void validateWsResponse(WsTelemetryResponse response, long expectedTs, long expectedVal) {
        try {
            List<Object> values = response.getDataValuesByKey("checkKey");
            if (CollectionUtils.isEmpty(values)) {
                throw new IllegalStateException("Ws response - no data");
            }
            long actualTs = Long.parseLong(values.get(0).toString());
            long actualVal = Long.parseLong(values.get(1).toString());

            if (actualTs != expectedTs) {
                throw new IllegalStateException("Ws response - Ts not matched. Actual: " + actualTs + "  Expected: " + expectedTs + " Delta: " + (expectedTs - actualTs));
            }

            if (actualVal != expectedVal) {
                throw new IllegalStateException("Ws response - Value not matched. Actual: " + actualVal + "  Expected: " + expectedVal + " Delta: " + (expectedVal - actualVal));
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Could not validate WS response: " + response, ex);
        }
    }

    private void sendHttp(RestClient restClient, DeviceCredentials deviceCredentials, long value, long msgTs, LatencyMsg latency) {
        try {
            long start = System.currentTimeMillis();

            RestTemplate restTemplate = new RestTemplate();
            restTemplate.setInterceptors(Collections.singletonList((httpRequest, bytes, clientHttpRequestExecution) -> {
                HttpRequest wrapper = new HttpRequestWrapper(httpRequest);
                wrapper.getHeaders().set("X-Authorization", "Bearer " + restClient.getToken());
                return clientHttpRequestExecution.execute(wrapper, bytes);
            }));

            String payload = createPayload(msgTs, value).toString();

            restTemplate.postForEntity("https://" + host + "/api/v1/" + deviceCredentials.getCredentialsId() + "/telemetry", payload, String.class);

            latency.setHttpSendLatency(System.currentTimeMillis() - start);
            log.info("HTTP msg submitted");
        } catch (Exception ex) {
            throw new IllegalStateException("Could not send http: " + ex.getMessage(), ex);
        }
    }

    private void sendMqtt(DeviceCredentials deviceCredentials, long value, long msgTs, LatencyMsg latency) {
        try {
            long start = System.currentTimeMillis();
            SampleMqttClient mqttClient = getMqttClient(deviceCredentials, latency);

            JsonObject payload = createPayload(msgTs, value);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.readTree(payload.toString());
            mqttClient.publishTelemetry(objectMapper.readTree(payload.toString()));
            latency.setMqttSendLatency(System.currentTimeMillis() - start);
            log.info("Mqtt msg submitted");
        } catch (Exception ex) {
            throw new IllegalStateException("Could not send mqtt: " + ex.getMessage(), ex);
        }
    }

    private WsClient subscribeToWebSocket(DeviceId deviceId, String property, RestClient restClient, LatencyMsg latency) {
        try {
            long start = System.currentTimeMillis();
            WsClient wsClient = new WsClient(new URI("wss://" + host + "/api/ws/plugins/telemetry?token=" + restClient.getToken()));
            SSLContextBuilder builder = SSLContexts.custom();
            builder.loadTrustMaterial(null, (TrustStrategy) (chain, authType) -> true);
            wsClient.setSocketFactory(builder.build().getSocketFactory());
            wsClient.connectBlocking();

            JsonObject cmdsObject = new JsonObject();
            cmdsObject.addProperty("entityType", EntityType.DEVICE.name());
            cmdsObject.addProperty("entityId", deviceId.toString());
            cmdsObject.addProperty("scope", "LATEST_TELEMETRY");
            cmdsObject.addProperty("cmdId", new Random().nextInt(100));

            JsonArray cmd = new JsonArray();
            cmd.add(cmdsObject);
            JsonObject wsRequest = new JsonObject();
            wsRequest.add(property, cmd);
            wsClient.send(wsRequest.toString());
            wsClient.waitForFirstReply();
            latency.setWsSubInitLatency(System.currentTimeMillis() - start);
            log.info("Ws subscription created");
            return wsClient;
        } catch (Exception ex) {
            throw new IllegalStateException("Could not subscribe to WS: " + ex.getMessage(), ex);
        }
    }

    private SampleMqttClient getMqttClient(DeviceCredentials deviceCredentials, LatencyMsg latency) {
        try {
            long start = System.currentTimeMillis();
            if (mqttClient == null || !mqttClient.nativeClient.isConnected()) {
                if (mqttClient != null) {
                    mqttReconnects.incrementAndGet();
                    try {
                        mqttClient.disconnect();
                    } catch (Exception ex) {
                        log.error("fail disconnect mqtt", ex);
                    }
                }
                String uri = "tcp://" + host + ":1883";
                String deviceTmpName = "health check device";
                String token = deviceCredentials.getCredentialsId();
                mqttClient = new SampleMqttClient(uri, deviceTmpName, token, mqttErrors);
                boolean connected = mqttClient.connect();
                if (!connected) {
                    throw new IllegalStateException("Could not connect mqtt nativeClient");
                }
            }
            latency.setMqttConnectLatency(System.currentTimeMillis() - start);
            return mqttClient;
        } catch (Exception ex) {
            throw new IllegalStateException("Could not create mqtt nativeClient: " + ex.getMessage(), ex);
        }
    }

    private static JsonObject createPayload(long ts, long val) {
        JsonObject values = createPayload(val);
        JsonObject payload = new JsonObject();
        payload.addProperty("ts", ts);
        payload.add("values", values);
        return payload;
    }

    private static JsonObject createPayload(long val) {
        JsonObject values = new JsonObject();
        values.addProperty("checkKey", val);

        return values;
    }
}
