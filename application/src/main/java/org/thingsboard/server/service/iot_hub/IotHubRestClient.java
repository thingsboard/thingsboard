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
package org.thingsboard.server.service.iot_hub;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Component
@TbCoreComponent
@Slf4j
public class IotHubRestClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${iot-hub.base-url:https://iot-hub.thingsboard.io}")
    private String baseUrl;

    public JsonNode getVersionInfo(String versionId) {
        String url = baseUrl + "/api/versions/" + versionId;
        log.debug("Fetching IoT Hub version info: {}", url);
        return restTemplate.getForObject(url, JsonNode.class);
    }

    public byte[] getVersionFileData(String versionId) {
        String url = baseUrl + "/api/versions/" + versionId + "/fileData";
        log.debug("Fetching IoT Hub version file data: {}", url);
        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
        return response.getBody();
    }

    public void reportVersionInstalled(String versionId) {
        String url = baseUrl + "/api/versions/" + versionId + "/install";
        log.debug("Reporting IoT Hub version installed: {}", url);
        restTemplate.postForObject(url, null, Void.class);
    }
}
