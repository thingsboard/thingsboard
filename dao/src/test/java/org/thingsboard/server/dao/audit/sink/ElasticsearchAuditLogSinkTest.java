// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.audit.sink;

import com.google.common.util.concurrent.MoreExecutors;
import org.apache.http.HttpHost;
import org.apache.http.ProtocolVersion;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicRequestLine;
import org.apache.http.message.BasicStatusLine;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.audit.ActionStatus;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.id.AuditLogId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;

import java.io.IOException;
import java.net.ConnectException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.dao.audit.sink.ElasticsearchAuditLogSink.DISCOVERY_RETRY_INTERVAL_MS;
import static org.thingsboard.server.dao.audit.sink.ElasticsearchAuditLogSink.LEGACY_DOCUMENT_TYPE;
import static org.thingsboard.server.dao.audit.sink.ElasticsearchAuditLogSink.TYPELESS_DOCUMENT_TYPE;

public class ElasticsearchAuditLogSinkTest {

    private static final String INDEX = "audit_index";

    private ElasticsearchAuditLogSink sink;
    private RestClient restClient;

    @AfterEach
    public void tearDown() {
        if (sink != null) {
            ReflectionTestUtils.invokeMethod(sink, "destroy");
        }
    }

    @ParameterizedTest(name = "{0} {1} -> {2}")
    @CsvSource({
            "elasticsearch, 6.8.23,       audit_log",
            "elasticsearch, 7.17.25,      audit_log",
            "elasticsearch, 8.0.0-SNAPSHOT, _doc",
            "elasticsearch, 8.18.8,       _doc",
            "elasticsearch, 9.1.0,        _doc",
            "opensearch,    1.3.20,       audit_log",
            "opensearch,    2.19.1,       _doc",
            "opensearch,    3.0.0,        _doc",
            "opensearch-compat, 7.10.2,   _doc",
            "elasticsearch, unknown,      _doc"
    })
    public void givenAutoMode_whenLogAction_thenDocumentTypeDependsOnServerVersion(String distribution, String version, String expectedType) throws IOException {
        initSink("auto");
        mockServerVersion(distribution, version);

        sink.logAction(newAuditLog());
        sink.logAction(newAuditLog());

        assertThat(capturePostEndpoints()).containsExactly("/" + INDEX + "/" + expectedType, "/" + INDEX + "/" + expectedType);
        verify(restClient, times(1)).performRequest(any());
    }

    @Test
    public void givenDiscoveryFails_whenLogAction_thenUseTypelessAndRetryAfterInterval() throws IOException {
        initSink("auto");
        doThrow(new ConnectException("Connection refused")).when(restClient).performRequest(any());

        sink.logAction(newAuditLog());
        sink.logAction(newAuditLog());
        verify(restClient, times(1)).performRequest(any());

        mockServerVersion("elasticsearch", "7.17.25");
        expireDiscoveryInterval();
        sink.logAction(newAuditLog());

        assertThat(capturePostEndpoints()).containsExactly(
                "/" + INDEX + "/" + TYPELESS_DOCUMENT_TYPE,
                "/" + INDEX + "/" + TYPELESS_DOCUMENT_TYPE,
                "/" + INDEX + "/" + LEGACY_DOCUMENT_TYPE);
    }

    @Test
    public void givenServerErrorOnWrite_whenLogAction_thenRediscoverVersion() throws IOException {
        initSink("auto");
        mockServerVersion("elasticsearch", "7.17.25");
        sink.logAction(newAuditLog());

        // cluster upgraded to 8.x, typed endpoint is rejected
        mockServerVersion("elasticsearch", "8.18.8");
        failLastWrite();
        expireDiscoveryInterval();
        sink.logAction(newAuditLog());

        assertThat(capturePostEndpoints()).containsExactly(
                "/" + INDEX + "/" + LEGACY_DOCUMENT_TYPE,
                "/" + INDEX + "/" + TYPELESS_DOCUMENT_TYPE);
    }

    @ParameterizedTest
    @CsvSource({"typed, audit_log", "TYPELESS, _doc"})
    public void givenExplicitMode_whenLogAction_thenSkipDiscovery(String indexApi, String expectedType) throws IOException {
        initSink(indexApi);

        sink.logAction(newAuditLog());
        failLastWrite();
        sink.logAction(newAuditLog());

        assertThat(capturePostEndpoints()).containsOnly("/" + INDEX + "/" + expectedType);
        verify(restClient, never()).performRequest(any());
    }

    @Test
    public void givenRealRestClient_whenDestroy_thenIoThreadsStop() {
        Set<Thread> existing = restClientThreads();
        ElasticsearchAuditLogSink realSink = newSink("auto");
        Set<Thread> started = restClientThreads();
        started.removeAll(existing);
        assertThat(started).isNotEmpty().noneMatch(Thread::isDaemon);

        ReflectionTestUtils.invokeMethod(realSink, "destroy");

        await().atMost(10, TimeUnit.SECONDS).until(() -> started.stream().noneMatch(Thread::isAlive));
    }

    @Test
    public void givenInvalidMode_whenInit_thenFail() {
        sink = new ElasticsearchAuditLogSink();
        ReflectionTestUtils.setField(sink, "indexApi", "invalid");
        assertThatThrownBy(sink::init).isInstanceOf(IllegalArgumentException.class);
    }

    private static ElasticsearchAuditLogSink newSink(String indexApi) {
        ElasticsearchAuditLogSink sink = new ElasticsearchAuditLogSink();
        ReflectionTestUtils.setField(sink, "indexPattern", INDEX);
        ReflectionTestUtils.setField(sink, "dateFormat", "yyyy.MM.dd");
        ReflectionTestUtils.setField(sink, "schemeName", "http");
        ReflectionTestUtils.setField(sink, "host", "localhost");
        ReflectionTestUtils.setField(sink, "port", 9200);
        ReflectionTestUtils.setField(sink, "indexApi", indexApi);
        sink.init();
        return sink;
    }

    private void initSink(String indexApi) {
        sink = newSink(indexApi);
        try {
            ((RestClient) ReflectionTestUtils.getField(sink, "restClient")).close();
        } catch (IOException ignored) {
        }
        restClient = mock(RestClient.class);
        ReflectionTestUtils.setField(sink, "restClient", restClient);
        ReflectionTestUtils.setField(sink, "executor", MoreExecutors.newDirectExecutorService());
    }

    private static Set<Thread> restClientThreads() {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(t -> t.getName().startsWith("elasticsearch-rest-client"))
                .collect(Collectors.toCollection(HashSet::new));
    }

    private void mockServerVersion(String distribution, String version) throws IOException {
        // Real response shapes; "opensearch-compat" is OpenSearch with compatibility.override_main_response_version=true
        String body = switch (distribution) {
            case "elasticsearch" -> String.format("{\"version\":{\"number\":\"%s\",\"build_flavor\":\"default\"},\"tagline\":\"You Know, for Search\"}", version);
            case "opensearch" -> String.format("{\"version\":{\"distribution\":\"opensearch\",\"number\":\"%s\"},\"tagline\":\"The OpenSearch Project: https://opensearch.org/\"}", version);
            case "opensearch-compat" -> String.format("{\"version\":{\"number\":\"%s\"},\"tagline\":\"The OpenSearch Project: https://opensearch.org/\"}", version);
            default -> throw new IllegalArgumentException(distribution);
        };
        Response response = newResponse(200, body);
        doReturn(response).when(restClient).performRequest(argThat(r -> "GET".equals(r.getMethod()) && "/".equals(r.getEndpoint())));
    }

    private void failLastWrite() throws IOException {
        ArgumentCaptor<ResponseListener> listener = ArgumentCaptor.forClass(ResponseListener.class);
        verify(restClient, atLeastOnce()).performRequestAsync(any(), listener.capture());
        listener.getValue().onFailure(new ResponseException(newResponse(400, "{}")));
    }

    private void expireDiscoveryInterval() {
        long lastDiscoveryTs = (long) ReflectionTestUtils.getField(sink, "lastDiscoveryTs");
        ReflectionTestUtils.setField(sink, "lastDiscoveryTs", lastDiscoveryTs - DISCOVERY_RETRY_INTERVAL_MS);
    }

    private List<String> capturePostEndpoints() {
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        verify(restClient, atLeastOnce()).performRequestAsync(captor.capture(), any());
        return captor.getAllValues().stream().map(Request::getEndpoint).toList();
    }

    private static Response newResponse(int status, String body) {
        ProtocolVersion protocol = new ProtocolVersion("HTTP", 1, 1);
        BasicHttpResponse httpResponse = new BasicHttpResponse(new BasicStatusLine(protocol, status, ""));
        httpResponse.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
        Response response = mock(Response.class);
        when(response.getEntity()).thenReturn(httpResponse.getEntity());
        when(response.getStatusLine()).thenReturn(httpResponse.getStatusLine());
        when(response.getRequestLine()).thenReturn(new BasicRequestLine("POST", "/", protocol));
        when(response.getHost()).thenReturn(new HttpHost("localhost", 9200));
        return response;
    }

    private static AuditLog newAuditLog() {
        AuditLog auditLog = new AuditLog(new AuditLogId(UUID.randomUUID()));
        auditLog.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        auditLog.setEntityId(new DeviceId(UUID.randomUUID()));
        auditLog.setUserId(new UserId(UUID.randomUUID()));
        auditLog.setActionType(ActionType.ADDED);
        auditLog.setActionStatus(ActionStatus.SUCCESS);
        return auditLog;
    }

}
