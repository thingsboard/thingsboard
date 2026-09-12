// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
