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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.audit.ActionStatus;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.id.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WebHookAuditLogSinkTest {

    private WebHookAuditLogSink webHookAuditLogSink;

    @Mock
    private HttpClient httpClient;

    @Mock
    private CompletableFuture<HttpResponse<String>> completableFuture;

    @Mock
    private CompletableFuture<Void> voidCompletableFuture;

    private final String TEST_URL = "http://test-webhook.com";
    private final String TEST_API_KEY = "test-api-key";
    private final String TEST_API_SECRET = "test-api-secret";

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        webHookAuditLogSink = new WebHookAuditLogSink();
        ReflectionTestUtils.setField(webHookAuditLogSink, "url", TEST_URL);
        ReflectionTestUtils.setField(webHookAuditLogSink, "apiKey", TEST_API_KEY);
        ReflectionTestUtils.setField(webHookAuditLogSink, "apiSecret", TEST_API_SECRET);
        ReflectionTestUtils.setField(webHookAuditLogSink, "gzipEnabled", false);
        ReflectionTestUtils.setField(webHookAuditLogSink, "client", httpClient);
    }

    @Test
    public void testLogAction() {
        // Create a sample AuditLog
        AuditLog auditLog = createSampleAuditLog();

        // Mock HTTP client response
        when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(completableFuture);
        when(completableFuture.thenAccept(any(Consumer.class))).thenReturn(voidCompletableFuture);
        when(voidCompletableFuture.exceptionally(any())).thenReturn(voidCompletableFuture);

        // Call the method under test
        webHookAuditLogSink.logAction(auditLog);

        // Capture the HTTP request
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).sendAsync(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));

        // Verify the request
        HttpRequest capturedRequest = requestCaptor.getValue();
        assertEquals(URI.create(TEST_URL), capturedRequest.uri());
        assertEquals("POST", capturedRequest.method());

        // Verify headers
        assertTrue(capturedRequest.headers().map().containsKey("X-API-KEY"));
        assertEquals(TEST_API_KEY, capturedRequest.headers().firstValue("X-API-KEY").orElse(null));

        assertTrue(capturedRequest.headers().map().containsKey("X-API-SECRET"));
        String expectedSecret = Base64.getEncoder().encodeToString(TEST_API_SECRET.getBytes());
        assertEquals(expectedSecret, capturedRequest.headers().firstValue("X-API-SECRET").orElse(null));

        assertTrue(capturedRequest.headers().map().containsKey("Content-Type"));
        assertEquals("application/json", capturedRequest.headers().firstValue("Content-Type").orElse(null));

        // Verify that Content-Encoding header is not present when gzip is disabled
        assertFalse(capturedRequest.headers().map().containsKey("Content-Encoding"));
    }

    @Test
    public void testInit() {
        // Create a new instance to test init method
        WebHookAuditLogSink sink = new WebHookAuditLogSink();
        ReflectionTestUtils.setField(sink, "url", TEST_URL);
        ReflectionTestUtils.setField(sink, "apiKey", TEST_API_KEY);
        ReflectionTestUtils.setField(sink, "apiSecret", TEST_API_SECRET);
        ReflectionTestUtils.setField(sink, "gzipEnabled", false);

        // Call init method
        ReflectionTestUtils.invokeMethod(sink, "init");

        // Verify that client is initialized
        HttpClient client = (HttpClient) ReflectionTestUtils.getField(sink, "client");
        assertNotNull(client);
    }

    @Test
    public void testLogActionWithGzipEnabled() {
        // Set gzipEnabled to true
        ReflectionTestUtils.setField(webHookAuditLogSink, "gzipEnabled", true);

        // Create a sample AuditLog
        AuditLog auditLog = createSampleAuditLog();

        // Mock HTTP client response
        when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(completableFuture);
        when(completableFuture.thenAccept(any(Consumer.class))).thenReturn(voidCompletableFuture);
        when(voidCompletableFuture.exceptionally(any())).thenReturn(voidCompletableFuture);

        // Call the method under test
        webHookAuditLogSink.logAction(auditLog);

        // Capture the HTTP request
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).sendAsync(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));

        // Verify the request
        HttpRequest capturedRequest = requestCaptor.getValue();
        assertEquals(URI.create(TEST_URL), capturedRequest.uri());
        assertEquals("POST", capturedRequest.method());

        // Verify headers
        assertTrue(capturedRequest.headers().map().containsKey("X-API-KEY"));
        assertEquals(TEST_API_KEY, capturedRequest.headers().firstValue("X-API-KEY").orElse(null));

        assertTrue(capturedRequest.headers().map().containsKey("X-API-SECRET"));
        String expectedSecret = Base64.getEncoder().encodeToString(TEST_API_SECRET.getBytes());
        assertEquals(expectedSecret, capturedRequest.headers().firstValue("X-API-SECRET").orElse(null));

        assertTrue(capturedRequest.headers().map().containsKey("Content-Type"));
        assertEquals("application/json", capturedRequest.headers().firstValue("Content-Type").orElse(null));

        // Verify gzip-specific headers
        assertTrue(capturedRequest.headers().map().containsKey("Content-Encoding"));
        assertEquals("gzip", capturedRequest.headers().firstValue("Content-Encoding").orElse(null));
    }

    @Test
    public void testDestroy() {
        // Call destroy method
        ReflectionTestUtils.invokeMethod(webHookAuditLogSink, "destroy");

        // Verify that client is set to null
        HttpClient client = (HttpClient) ReflectionTestUtils.getField(webHookAuditLogSink, "client");
        assertNull(client);
    }

    private AuditLog createSampleAuditLog() {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(new AuditLogId(UUID.randomUUID()));
        auditLog.setTenantId(new TenantId(UUID.randomUUID()));
        auditLog.setCustomerId(new CustomerId(UUID.randomUUID()));
        auditLog.setEntityId(new DeviceId(UUID.randomUUID()));
        auditLog.setEntityName("Test Device");
        auditLog.setUserId(new UserId(UUID.randomUUID()));
        auditLog.setUserName("test@example.com");
        auditLog.setActionType(ActionType.ADDED);
        auditLog.setActionStatus(ActionStatus.SUCCESS);
        auditLog.setActionData(JacksonUtil.newObjectNode().put("test", "value"));
        auditLog.setActionFailureDetails("");
        return auditLog;
    }
}
