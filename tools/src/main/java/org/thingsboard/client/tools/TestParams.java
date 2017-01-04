/**
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
package org.thingsboard.client.tools;

import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TestParams {
    static final String TEST_PROPERTIES = "test.properties";
    static final long DEFAULT_TEST_DURATION = TimeUnit.MINUTES.toMillis(1);
    static final long DEFAULT_TEST_INTERVAL = TimeUnit.MILLISECONDS.toMillis(100);
    static final int DEFAULT_DEVICE_COUNT = 25;
    static final String DEFAULT_REST_URL = "http://localhost:8080";
    static final String DEFAULT_MQTT_URLS = "tcp://localhost:1883";
    static final String DEFAULT_USERNAME = "tenant@thingsboard.org";
    static final String DEFAULT_PASSWORD = "tenant";

    private Properties params = new Properties();

    public TestParams() throws IOException {
        try {
            params.load(new FileInputStream(TEST_PROPERTIES));
        } catch (Exception e) {
            log.warn("Failed to read " + TEST_PROPERTIES);
        }
    }

    public long getDuration() {
        return Long.valueOf(params.getProperty("durationMs", Long.toString(DEFAULT_TEST_DURATION)));
    }

    public long getIterationInterval() {
        return Long.valueOf(params.getProperty("iterationIntervalMs", Long.toString(DEFAULT_TEST_INTERVAL)));
    }

    public int getDeviceCount() {
        return Integer.valueOf(params.getProperty("deviceCount", Integer.toString(DEFAULT_DEVICE_COUNT)));
    }

    public String getRestApiUrl() {
        return params.getProperty("restUrl", DEFAULT_REST_URL);
    }

    public String[] getMqttUrls() {
        return params.getProperty("mqttUrls", DEFAULT_MQTT_URLS).split(",");
    }

    public String getUsername() {
        return params.getProperty("username", DEFAULT_USERNAME);
    }

    public String getPassword() {
        return params.getProperty("password", DEFAULT_PASSWORD);
    }
}
