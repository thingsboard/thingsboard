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
package org.thingsboard.rule.engine.rest;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpRequestHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.util.KeyValueEntry;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.exception.DataValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TbRestApiCallNodeTest extends AbstractRuleNodeUpgradeTest {

    @Spy
    private TbRestApiCallNode restNode;

    @Mock
    private TbContext ctx;

    private final EntityId originator = new DeviceId(Uuids.timeBased());
    private final TbMsgMetaData metaData = new TbMsgMetaData();

    private final RuleChainId ruleChainId = new RuleChainId(Uuids.timeBased());
    private final RuleNodeId ruleNodeId = new RuleNodeId(Uuids.timeBased());

    private HttpServer server;

    public void setupServer(String pattern, HttpRequestHandler handler) throws IOException {
        SocketConfig config = SocketConfig.custom().setSoReuseAddress(true).setTcpNoDelay(true).build();
        server = ServerBootstrap.bootstrap()
                .setSocketConfig(config)
                .registerHandler(pattern, handler)
                .create();
        server.start();
    }

    private void initWithConfig(TbRestApiCallNodeConfiguration config) {
        try {
            TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
            restNode = new TbRestApiCallNode();
            restNode.init(ctx, nodeConfiguration);
        } catch (TbNodeException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @BeforeEach
    public void setup() {
        RuleNode ruleNode = new RuleNode();
        ruleNode.setId(ruleNodeId);
        ruleNode.setName("Test REST API call node");
        lenient().when(ctx.getSelf()).thenReturn(ruleNode);
    }

    @AfterEach
    public void teardown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void shouldNotAllowNullQueryParamNames() {
        // GIVEN
        var config = new TbRestApiCallNodeConfiguration().defaultConfiguration();
        config.setQueryParams(List.of(new KeyValueEntry<>(null, "value")));

        // WHEN-THEN
        assertThatThrownBy(() -> new TbRestApiCallNode().init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(TbNodeException.class)
                .matches(e -> ((TbNodeException) e).isUnrecoverable())
                .rootCause()
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("query parameter names and values must be non-null");
    }

    @Test
    public void shouldNotAllowNullQueryParamValues() {
        // GIVEN
        var config = new TbRestApiCallNodeConfiguration().defaultConfiguration();
        config.setQueryParams(List.of(new KeyValueEntry<>("key", null)));

        // WHEN-THEN
        assertThatThrownBy(() -> new TbRestApiCallNode().init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(TbNodeException.class)
                .matches(e -> ((TbNodeException) e).isUnrecoverable())
                .rootCause()
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("query parameter names and values must be non-null");
    }

    @Test
    public void shouldNotAllowNullQueryParamEntries() {
        // GIVEN
        var config = new TbRestApiCallNodeConfiguration().defaultConfiguration();

        var queryParams = new ArrayList<KeyValueEntry<String, String>>();
        queryParams.add(new KeyValueEntry<>("key", "value"));
        queryParams.add(null);
        config.setQueryParams(queryParams);

        // WHEN-THEN
        assertThatThrownBy(() -> new TbRestApiCallNode().init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(TbNodeException.class)
                .matches(e -> ((TbNodeException) e).isUnrecoverable())
                .rootCause()
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    public void shouldUseNewEncodingForNewNodesByDefault() {
        // GIVEN-WHEN
        var defaultConfig = new TbRestApiCallNodeConfiguration().defaultConfiguration();

        // THEN
        assertEquals(Collections.emptyList(), defaultConfig.getQueryParams());
    }

    @Test
    public void shouldDefaultToOldEncodingForLegacyConfigs() {
        // GIVEN
        String configJson = """
                {
                    "restEndpointUrlPattern": "http://url?param=value",
                    "requestMethod": "GET",
                    "parseToPlainText": false,
                    "ignoreRequestBody": false,
                    "enableProxy": false,
                    "useSystemProxyProperties": false,
                    "proxyHost": null,
                    "proxyPort": 0,
                    "proxyUser": null,
                    "proxyPassword": null,
                    "readTimeoutMs": 0,
                    "maxParallelRequestsCount": 0,
                    "headers": {
                        "Content-Type": "application/json",
                        "X-Authorization": "Bearer eyJhbGciOi...."
                    },
                    "credentials": {
                        "type": "anonymous"
                    },
                    "maxInMemoryBufferSizeInKb": 256
                }""";

        // WHEN
        var config = JacksonUtil.fromString(configJson, TbRestApiCallNodeConfiguration.class);

        // THEN
        assertNull(config.getQueryParams());
    }

    @Test
    public void deleteRequestWithoutBody() throws IOException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final String path = "/path/to/delete";
        setupServer("*", (request, response, _) -> {
            try {
                assertEquals(path, request.getRequestLine().getUri(), "Request path matches");
                assertTrue(request.containsHeader("Foo"), "Custom header included");
                assertEquals("Bar", request.getFirstHeader("Foo").getValue(), "Custom header value");
                response.setStatusCode(200);
                latch.countDown();
            } catch (Exception e) {
                System.out.println("Exception handling request: " + e);
                e.printStackTrace();
                latch.countDown();
            }
        });

        TbRestApiCallNodeConfiguration config = new TbRestApiCallNodeConfiguration().defaultConfiguration();
        config.setRequestMethod("DELETE");
        config.setHeaders(Collections.singletonMap("Foo", "Bar"));
        config.setIgnoreRequestBody(true);
        config.setRestEndpointUrlPattern(String.format("http://localhost:%d%s", server.getLocalPort(), path));
        initWithConfig(config);

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(originator)
                .copyMetaData(metaData)
                .dataType(TbMsgDataType.JSON)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .ruleChainId(ruleChainId)
                .ruleNodeId(ruleNodeId)
                .build();
        restNode.onMsg(ctx, msg);

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Server handled request");

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx, timeout(10_000)).transformMsg(msgCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertNotSame(metaData, metadataCaptor.getValue());
        assertEquals(TbMsg.EMPTY_JSON_OBJECT, dataCaptor.getValue());
    }

    @Test
    public void deleteRequestWithBody() throws IOException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final String path = "/path/to/delete";
        setupServer("*", (request, response, _) -> {
            try {
                assertEquals(path, request.getRequestLine().getUri(), "Request path matches");
                assertTrue(request.containsHeader("Content-Type"), "Content-Type included");
                assertEquals("application/json",
                        request.getFirstHeader("Content-Type").getValue(), "Content-Type value");
                assertTrue(request.containsHeader("Content-Length"), "Content-Length included");
                assertEquals("2",
                        request.getFirstHeader("Content-Length").getValue(), "Content-Length value");
                assertTrue(request.containsHeader("Foo"), "Custom header included");
                assertEquals("Bar", request.getFirstHeader("Foo").getValue(), "Custom header value");
                response.setStatusCode(200);
                latch.countDown();
            } catch (Exception e) {
                System.out.println("Exception handling request: " + e);
                e.printStackTrace();
                latch.countDown();
            }
        });

        TbRestApiCallNodeConfiguration config = new TbRestApiCallNodeConfiguration().defaultConfiguration();
        config.setRequestMethod("DELETE");
        config.setHeaders(Collections.singletonMap("Foo", "Bar"));
        config.setIgnoreRequestBody(false);
        config.setRestEndpointUrlPattern(String.format("http://localhost:%d%s", server.getLocalPort(), path));
        initWithConfig(config);

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(originator)
                .copyMetaData(metaData)
                .dataType(TbMsgDataType.JSON)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .ruleChainId(ruleChainId)
                .ruleNodeId(ruleNodeId)
                .build();
        restNode.onMsg(ctx, msg);

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Server handled request");

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx, timeout(10_000)).transformMsg(msgCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertNotSame(metaData, metadataCaptor.getValue());
        assertEquals(TbMsg.EMPTY_JSON_OBJECT, dataCaptor.getValue());
    }

    @Test
    public void postRequestWithBodyTemplate() throws IOException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final String path = "/api/token";
        final AtomicReference<String> capturedBody = new AtomicReference<>();
        setupServerWithBodyCapture(capturedBody, latch);

        TbRestApiCallNodeConfiguration config = new TbRestApiCallNodeConfiguration().defaultConfiguration();
        config.setRequestMethod("POST");
        config.setRequestBodyTemplate("{\"grant_type\":\"client_credentials\",\"client_id\":\"${clientId}\",\"value\":\"$[token]\"}");
        config.setRestEndpointUrlPattern(String.format("http://localhost:%d%s", server.getLocalPort(), path));
        initWithConfig(config);

        metaData.putValue("clientId", "my-client-123");
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(originator)
                .copyMetaData(metaData)
                .dataType(TbMsgDataType.JSON)
                .data("{\"token\":\"abc-xyz\"}")
                .ruleChainId(ruleChainId)
                .ruleNodeId(ruleNodeId)
                .build();
        restNode.onMsg(ctx, msg);

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Server handled request");
        assertEquals("{\"grant_type\":\"client_credentials\",\"client_id\":\"my-client-123\",\"value\":\"abc-xyz\"}", capturedBody.get());
    }

    @Test
    public void postRequestWithBodyTemplateAndParseToPlainText() throws IOException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final String path = "/api/text";
        final AtomicReference<String> capturedBody = new AtomicReference<>();
        setupServerWithBodyCapture(capturedBody, latch);

        TbRestApiCallNodeConfiguration config = new TbRestApiCallNodeConfiguration().defaultConfiguration();
        config.setRequestMethod("POST");
        config.setParseToPlainText(true);
        config.setRequestBodyTemplate("Hello ${name}, your token is $[token]!");
        config.setRestEndpointUrlPattern(String.format("http://localhost:%d%s", server.getLocalPort(), path));
        initWithConfig(config);

        metaData.putValue("name", "World");
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(originator)
                .copyMetaData(metaData)
                .dataType(TbMsgDataType.JSON)
                .data("{\"token\":\"abc-xyz\"}")
                .ruleChainId(ruleChainId)
                .ruleNodeId(ruleNodeId)
                .build();
        restNode.onMsg(ctx, msg);

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Server handled request");
        assertEquals("Hello World, your token is abc-xyz!", capturedBody.get());
    }

    @Test
    public void postRequestWithBodyTemplateEscapesJsonSpecialChars() throws IOException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final String path = "/api/token";
        final AtomicReference<String> capturedBody = new AtomicReference<>();
        setupServerWithBodyCapture(capturedBody, latch);

        TbRestApiCallNodeConfiguration config = new TbRestApiCallNodeConfiguration().defaultConfiguration();
        config.setRequestMethod("POST");
        config.setRequestBodyTemplate("{\"name\":\"${userName}\",\"desc\":\"$[description]\"}");
        config.setRestEndpointUrlPattern(String.format("http://localhost:%d%s", server.getLocalPort(), path));
        initWithConfig(config);

        metaData.putValue("userName", "John \"Doe\"");
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(originator)
                .copyMetaData(metaData)
                .dataType(TbMsgDataType.JSON)
                .data("{\"description\":\"line1\\nline2\"}")
                .ruleChainId(ruleChainId)
                .ruleNodeId(ruleNodeId)
                .build();
        restNode.onMsg(ctx, msg);

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Server handled request");
        assertEquals("{\"name\":\"John \\\"Doe\\\"\",\"desc\":\"line1\\nline2\"}", capturedBody.get());
    }

    @Test
    public void postRequestWithEmptyBodyTemplateUsesMessageData() throws IOException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final String path = "/api/data";
        final AtomicReference<String> capturedBody = new AtomicReference<>();
        setupServerWithBodyCapture(capturedBody, latch);

        TbRestApiCallNodeConfiguration config = new TbRestApiCallNodeConfiguration().defaultConfiguration();
        config.setRequestMethod("POST");
        // requestBodyTemplate is null by default — should use msg.getData()
        config.setRestEndpointUrlPattern(String.format("http://localhost:%d%s", server.getLocalPort(), path));
        initWithConfig(config);

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(originator)
                .copyMetaData(metaData)
                .dataType(TbMsgDataType.JSON)
                .data("{\"temperature\":25}")
                .ruleChainId(ruleChainId)
                .ruleNodeId(ruleNodeId)
                .build();
        restNode.onMsg(ctx, msg);

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Server handled request");

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx, timeout(10_000)).transformMsg(msgCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());
        assertEquals("{\"temperature\":25}", capturedBody.get());
    }

    private void setupServerWithBodyCapture(AtomicReference<String> capturedBody, CountDownLatch latch) throws IOException {
        setupServer("*", (request, response, _) -> {
            try {
                if (request instanceof org.apache.http.HttpEntityEnclosingRequest entityRequest) {
                    InputStream is = entityRequest.getEntity().getContent();
                    capturedBody.set(new String(is.readAllBytes(), StandardCharsets.UTF_8));
                }
                response.setStatusCode(200);
                latch.countDown();
            } catch (Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        });
    }

    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                Arguments.of(0,
                        "{\"restEndpointUrlPattern\":\"http://localhost/api\",\"requestMethod\":\"POST\"," +
                                "\"useSimpleClientHttpFactory\":false,\"ignoreRequestBody\":false,\"enableProxy\":false," +
                                "\"useSystemProxyProperties\":false,\"proxyScheme\":null,\"proxyHost\":null,\"proxyPort\":0," +
                                "\"proxyUser\":null,\"proxyPassword\":null,\"readTimeoutMs\":0,\"maxParallelRequestsCount\":0," +
                                "\"headers\":{\"Content-Type\":\"application/json\"},\"useRedisQueueForMsgPersistence\":false," +
                                "\"trimQueue\":null,\"maxQueueSize\":null,\"credentials\":{\"type\":\"anonymous\"},\"trimDoubleQuotes\":false}",
                        true,
                        "{\"restEndpointUrlPattern\":\"http://localhost/api\",\"requestMethod\": \"POST\"," +
                                "\"parseToPlainText\": false,\"ignoreRequestBody\": false," +
                                "\"enableProxy\": false,\"useSystemProxyProperties\": false,\"proxyHost\": null," +
                                "\"proxyPort\": 0,\"proxyUser\": null,\"proxyPassword\": null,\"readTimeoutMs\": 0," +
                                "\"maxParallelRequestsCount\": 0,\"headers\": {\"Content-Type\": \"application/json\"}," +
                                "\"credentials\": {\"type\": \"anonymous\"}," +
                                "\"maxInMemoryBufferSizeInKb\": 256,\"requestBodyTemplate\": null}"),
                Arguments.of(1,
                        "{\"restEndpointUrlPattern\":\"http://localhost/api\",\"requestMethod\": \"POST\"," +
                                "\"useSimpleClientHttpFactory\": false,\"parseToPlainText\": false,\"ignoreRequestBody\": false," +
                                "\"enableProxy\": false,\"useSystemProxyProperties\": false,\"proxyScheme\": null,\"proxyHost\": null," +
                                "\"proxyPort\": 0,\"proxyUser\": null,\"proxyPassword\": null,\"readTimeoutMs\": 0," +
                                "\"maxParallelRequestsCount\": 0,\"headers\": {\"Content-Type\": \"application/json\"}," +
                                "\"useRedisQueueForMsgPersistence\": false,\"trimQueue\": null,\"maxQueueSize\": null," +
                                "\"credentials\": {\"type\": \"anonymous\"}}",
                        true,
                        "{\"restEndpointUrlPattern\":\"http://localhost/api\",\"requestMethod\": \"POST\"," +
                                "\"parseToPlainText\": false,\"ignoreRequestBody\": false," +
                                "\"enableProxy\": false,\"useSystemProxyProperties\": false,\"proxyHost\": null," +
                                "\"proxyPort\": 0,\"proxyUser\": null,\"proxyPassword\": null,\"readTimeoutMs\": 0," +
                                "\"maxParallelRequestsCount\": 0,\"headers\": {\"Content-Type\": \"application/json\"}," +
                                "\"credentials\": {\"type\": \"anonymous\"}," +
                                "\"maxInMemoryBufferSizeInKb\": 256,\"requestBodyTemplate\": null}"),
                Arguments.of(2,
                        "{\"restEndpointUrlPattern\":\"http://localhost/api\",\"requestMethod\": \"POST\"," +
                                "\"useSimpleClientHttpFactory\": false,\"parseToPlainText\": false,\"ignoreRequestBody\": false," +
                                "\"enableProxy\": false,\"useSystemProxyProperties\": false,\"proxyScheme\": null,\"proxyHost\": null," +
                                "\"proxyPort\": 0,\"proxyUser\": null,\"proxyPassword\": null,\"readTimeoutMs\": 0," +
                                "\"maxParallelRequestsCount\": 0,\"headers\": {\"Content-Type\": \"application/json\"}," +
                                "\"credentials\": {\"type\": \"anonymous\"}}",
                        true,
                        "{\"restEndpointUrlPattern\":\"http://localhost/api\",\"requestMethod\": \"POST\"," +
                                "\"parseToPlainText\": false,\"ignoreRequestBody\": false," +
                                "\"enableProxy\": false,\"useSystemProxyProperties\": false,\"proxyHost\": null," +
                                "\"proxyPort\": 0,\"proxyUser\": null,\"proxyPassword\": null,\"readTimeoutMs\": 0," +
                                "\"maxParallelRequestsCount\": 0,\"headers\": {\"Content-Type\": \"application/json\"}," +
                                "\"credentials\": {\"type\": \"anonymous\"}," +
                                "\"maxInMemoryBufferSizeInKb\": 256,\"requestBodyTemplate\": null}"),
                Arguments.of(3, """
                                {
                                    "restEndpointUrlPattern": "http://localhost/api",
                                    "requestMethod": "POST",
                                    "useSimpleClientHttpFactory": true,
                                    "parseToPlainText": false,
                                    "ignoreRequestBody": false,
                                    "enableProxy": false,
                                    "useSystemProxyProperties": false,
                                    "proxyScheme": null,
                                    "proxyHost": null,
                                    "proxyPort": 0,
                                    "proxyUser": null,
                                    "proxyPassword": null,
                                    "readTimeoutMs": 0,
                                    "maxParallelRequestsCount": 0,
                                    "headers": {
                                        "Content-Type": "application/json"
                                    },
                                    "credentials": {
                                        "type": "anonymous"
                                    },
                                    "maxInMemoryBufferSizeInKb": 256,
                                    "trimQueue": true,
                                    "maxQueueSize": 100,
                                    "trimDoubleQuotes": false,
                                    "useRedisQueueForMsgPersistence": false
                                }""",
                        true, """
                                {
                                    "restEndpointUrlPattern": "http://localhost/api",
                                    "requestMethod": "POST",
                                    "parseToPlainText": false,
                                    "ignoreRequestBody": false,
                                    "enableProxy": false,
                                    "useSystemProxyProperties": false,
                                    "proxyHost": null,
                                    "proxyPort": 0,
                                    "proxyUser": null,
                                    "proxyPassword": null,
                                    "readTimeoutMs": 0,
                                    "maxParallelRequestsCount": 0,
                                    "headers": {
                                        "Content-Type": "application/json"
                                    },
                                    "credentials": {
                                        "type": "anonymous"
                                    },
                                    "requestBodyTemplate": null,
                                    "maxInMemoryBufferSizeInKb": 256
                                }""")
        );
    }

    @Override
    protected TbNode getTestNode() {
        return restNode;
    }

}
