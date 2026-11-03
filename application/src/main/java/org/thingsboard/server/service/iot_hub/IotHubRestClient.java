// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.iot_hub;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

@Component
@TbCoreComponent
@Slf4j
public class IotHubRestClient {

    @Value("${iot-hub.base-url:https://iot-hub.thingsboard.io}")
    private String baseUrl;

    @Value("${iot-hub.connect-timeout-sec:5}")
    private int connectTimeoutSec;

    @Value("${iot-hub.read-timeout-sec:10}")
    private int readTimeoutSec;

    @Value("${iot-hub.max-file-data-size-bytes:104857600}")
    private long maxFileDataSizeBytes;

    private RestTemplate restTemplate;

    @PostConstruct
    public void initRestClient() {
        restTemplate = new RestTemplateBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSec))
                .readTimeout(Duration.ofSeconds(readTimeoutSec))
                .build();
    }

    public JsonNode getVersionInfo(String versionId) {
        String url = baseUrl + "/api/versions/" + versionId;
        log.debug("Fetching IoT Hub version info: {}", url);
        return restTemplate.getForObject(url, JsonNode.class);
    }

    /**
     * Returns the latest published version of an item, or {@code null} if the marketplace
     * responds with 404 (item missing or never published). Other 4xx/5xx still propagate.
     */
    public JsonNode getPublishedVersionByItemId(String itemId) {
        String url = baseUrl + "/api/items/" + itemId + "/published";
        log.debug("Fetching IoT Hub published version for item: {}", url);
        try {
            return restTemplate.getForObject(url, JsonNode.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }

    public byte[] getVersionFileData(String versionId) {
        String url = baseUrl + "/api/versions/" + versionId + "/fileData";
        log.debug("Fetching IoT Hub version file data: {}", url);
        // Stream the response so we can reject oversized payloads before
        // they get loaded fully into memory:
        //   1) refuse early if the server advertises a Content-Length
        //      larger than the configured cap;
        //   2) refuse mid-stream once we have already buffered more than
        //      the cap (for chunked transfers without Content-Length).
        final long limit = maxFileDataSizeBytes;
        return restTemplate.execute(url, HttpMethod.GET, null, response -> {
            long contentLength = response.getHeaders().getContentLength();
            if (contentLength > limit) {
                throw new IllegalStateException("IoT Hub file data size " + contentLength
                        + " bytes exceeds the configured limit of " + limit + " bytes");
            }
            int initialCapacity = contentLength > 0 && contentLength <= Integer.MAX_VALUE
                    ? (int) contentLength : 8192;
            try (InputStream in = response.getBody();
                 ByteArrayOutputStream out = new ByteArrayOutputStream(initialCapacity)) {
                byte[] buffer = new byte[8192];
                long total = 0;
                int read;
                while ((read = in.read(buffer)) != -1) {
                    total += read;
                    if (total > limit) {
                        throw new IllegalStateException("IoT Hub file data stream exceeded the configured limit of "
                                + limit + " bytes");
                    }
                    out.write(buffer, 0, read);
                }
                return out.toByteArray();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read IoT Hub file data", e);
            }
        });
    }

    public void reportVersionInstalled(String versionId, InstallReport report) {
        String url = baseUrl + "/api/versions/" + versionId + "/install";
        log.debug("Reporting IoT Hub version installed: {}", url);
        restTemplate.postForObject(url, report, Void.class);
    }
}
