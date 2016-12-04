package org.thingsboard.client.tools; /**
 * Copyright © 2016 The Thingsboard Authors
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
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.security.DeviceCredentials;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Andrew Shvayka
 */
@Slf4j
public class MqttStressTestTool {

    private static final long TEST_DURATION = TimeUnit.MINUTES.toMillis(1);
    private static final long TEST_ITERATION = TimeUnit.MILLISECONDS.toMillis(100);
    private static final long TEST_SUB_ITERATION = TimeUnit.MILLISECONDS.toMillis(2);
    private static final int DEVICE_COUNT = 100;
    private static final String BASE_URL = "http://localhost:8080";
    private static final String[] MQTT_URLS = {"tcp://localhost:1883"};
//    private static final String[] MQTT_URLS = {"tcp://localhost:1883", "tcp://localhost:1884", "tcp://localhost:1885"};
    private static final String USERNAME = "tenant@thingsboard.org";
    private static final String PASSWORD = "tenant";


    public static void main(String[] args) throws Exception {
        ResultAccumulator results = new ResultAccumulator();

        AtomicLong value = new AtomicLong(Long.MAX_VALUE);
        log.info("value: {} ", value.incrementAndGet());

        RestClient restClient = new RestClient(BASE_URL);
        restClient.login(USERNAME, PASSWORD);

        List<MqttStressTestClient> clients = new ArrayList<>();
        for (int i = 0; i < DEVICE_COUNT; i++) {
            Device device = restClient.createDevice("Device " + i);
            DeviceCredentials credentials = restClient.getCredentials(device.getId());
            String mqttURL = MQTT_URLS[i % MQTT_URLS.length];
            MqttStressTestClient client = new MqttStressTestClient(results, mqttURL, credentials.getCredentialsId());
            client.connect();
            clients.add(client);
        }
        Thread.sleep(1000);


        byte[] data = "{\"longKey\":73}".getBytes(StandardCharsets.UTF_8);
        long startTime = System.currentTimeMillis();
        int iterationsCount = (int) (TEST_DURATION / TEST_ITERATION);
        int subIterationsCount = (int) (TEST_ITERATION / TEST_SUB_ITERATION);
        if (clients.size() % subIterationsCount != 0) {
            throw new IllegalArgumentException("Invalid parameter exception!");
        }
        for (int i = 0; i < iterationsCount; i++) {
            for (int j = 0; j < subIterationsCount; j++) {
                int packSize = clients.size() / subIterationsCount;
                for (int k = 0; k < packSize; k++) {
                    int clientIndex = packSize * j + k;
                    clients.get(clientIndex).publishTelemetry(data);
                }
                Thread.sleep(TEST_SUB_ITERATION);
            }
        }
        Thread.sleep(1000);
        for (MqttStressTestClient client : clients) {
            client.disconnect();
        }
        log.info("Results: {} took {}ms", results, System.currentTimeMillis() - startTime);
    }

}
