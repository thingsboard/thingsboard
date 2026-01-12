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
package org.thingsboard.server.msa;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestProperties {

    private static final String HTTPS_URL = "https://localhost";

    private static final String WSS_URL = "wss://localhost";

    private static final ContainerTestSuite instance = ContainerTestSuite.getInstance();

    public static String getBaseUrl() {
        if (instance.isActive()) {
            return HTTPS_URL;
        }
        return System.getProperty("tb.baseUrl", "http://localhost:8080");
    }

    public static String getBaseUiUrl() {
        if (instance.isActive()) {
            //return "https://host.docker.internal"; // this alternative requires docker-selenium.yml extra_hosts: - "host.docker.internal:host-gateway"
            //return "https://" + DockerClientFactory.instance().dockerHostIpAddress(); //this alternative will get Docker IP from testcontainers
            return "https://haproxy"; //communicate inside current docker-compose network to the load balancer container
        }
        return System.getProperty("tb.baseUiUrl", "http://localhost:8080");
    }

    public static String getWebSocketUrl() {
        if (instance.isActive()) {
            return WSS_URL;
        }
        return System.getProperty("tb.wsUrl", "ws://localhost:8080");
    }

    public static String getMqttBrokerUrl() {
        if (instance.isActive()) {
            String host = instance.getTestContainer().getServiceHost("broker", 1883);
            Integer port = instance.getTestContainer().getServicePort("broker", 1883);
            return "tcp://" + host + ":" + port;
        }
        return System.getProperty("mqtt.broker", "tcp://localhost:1883");
    }
}
