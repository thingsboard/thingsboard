/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

package org.thingsboard.server.dao.audit.sink;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.AuditLogUtil;
import org.thingsboard.server.common.data.audit.AuditLog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;

/*
 * A component that implements the {@link AuditLogSink} interface to forward audit log entries to a configured WebHook endpoint.
  * This implementation supports authentication via API key and secret, which are included in the request headers.
 * The endpoint URL and authentication details are configured through application properties.
 * As optional parameter it can compress the logs via gzip before sending
 *
 * The component initializes an HTTP client upon startup and performs cleanup during shutdown to release resources.
 */
@Component
@ConditionalOnProperty(prefix = "audit-log.sink", value = "type", havingValue = "webhook")
@Slf4j
public class WebHookAuditLogSink implements AuditLogSink {

    private HttpClient client;
    @Value("${audit-log.sink.webhook_url}")
    private String url;
    @Value("${audit-log.sink.webhook_api_key}")
    private String apiKey;
    @Value("${audit-log.sink.webhook_api_secret}")
    private String apiSecret;

    @Value("${audit-log.sink.webhook_gzip_enabled:false}")
    private boolean gzipEnabled;

    @PostConstruct
    private void init() {
        log.info("Initializing WebHookAuditLogSink");
      this.client = HttpClient.newBuilder().build();
        log.info("WebHookAuditLogSink initialized");

    }

    @Override
    public void logAction(AuditLog auditLogEntry) {
        String payload = AuditLogUtil.createJsonRecord(auditLogEntry);
        HttpRequest.BodyPublisher bodyPublisher;
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();

        try {
            if (gzipEnabled) {
                byte[] compressedPayload = gzipCompress(payload);
                bodyPublisher = HttpRequest.BodyPublishers.ofByteArray(compressedPayload);
                requestBuilder.header("Content-Encoding", "gzip");
                log.debug("Using gzip compression for payload.");
            } else {
                bodyPublisher = HttpRequest.BodyPublishers.ofString(payload);
            }
        } catch (IOException e) {
            log.error("Error with compressing the auditLogData {}", e.getMessage(), e);
            bodyPublisher = HttpRequest.BodyPublishers.ofString(payload);
        }

        HttpRequest request = requestBuilder
                .POST(bodyPublisher)
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("X-API-KEY", apiKey)
                .header("X-API-SECRET", Base64.getEncoder().encodeToString(apiSecret.getBytes()))
                .header("Content-Type", "application/json")
                .build();
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> log.debug("Audit log entry sent to WebHook: {} with response status code: {}",
                        AuditLogUtil.format(auditLogEntry), response.statusCode()))
                .exceptionally(e -> {
                    log.error("Error while sending : {}", e.getMessage(), e);
                    return null;
                });
    }


     private byte[] gzipCompress(String payload) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
            gzipOutputStream.write(payload.getBytes());
        }
        return byteArrayOutputStream.toByteArray();
    }

    @PreDestroy
    private void destroy() {
        //Set Client to Null  as it doesnt implement any AutoClosable @TODO change in J21 to shutdown()
        client = null;
    }
}
