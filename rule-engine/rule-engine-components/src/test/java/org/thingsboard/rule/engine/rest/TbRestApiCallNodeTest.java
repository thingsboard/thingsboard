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
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.DirectListeningExecutor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.api.TbHttpClientSettings;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@ResourceLock("SsrfProtectionValidator") // to avoid race conditions when modifying SsrfProtectionValidator's static configuration
public class TbRestApiCallNodeTest extends AbstractRuleNodeUpgradeTest {

    static final long TIMEOUT = TimeUnit.SECONDS.toMillis(30);

    @Spy
    private TbRestApiCallNode restNode;

    @Mock
    private TbContext ctx;

    private EntityId originator = new DeviceId(Uuids.timeBased());
    private TbMsgMetaData metaData = new TbMsgMetaData();

    private RuleChainId ruleChainId = new RuleChainId(Uuids.timeBased());
    private RuleNodeId ruleNodeId = new RuleNodeId(Uuids.timeBased());

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

    @AfterEach
    public void teardown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void deleteRequestWithoutBody() throws IOException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final String path = "/path/to/delete";
        setupServer("*", new HttpRequestHandler() {

            @Override
            public void handle(HttpRequest request, HttpResponse response, HttpContext context)
                    throws HttpException, IOException {
                try {
                    assertEquals(request.getRequestLine().getUri(), path, "Request path matches");
                    assertTrue(request.containsHeader("Foo"), "Custom header included");
                    assertEquals("Bar", request.getFirstHeader("Foo").getValue(), "Custom header value");
                    response.setStatusCode(200);
                    latch.countDown();
                } catch (Exception e) {
                    System.out.println("Exception handling request: " + e.toString());
                    e.printStackTrace();
                    latch.countDown();
                }
            }
        });

        given(ctx.getExternalCallExecutor()).willReturn(DirectListeningExecutor.INSTANCE);

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
        verify(ctx, timeout(TIMEOUT)).transformMsg(msgCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertNotSame(metaData, metadataCaptor.getValue());
        assertEquals(TbMsg.EMPTY_JSON_OBJECT, dataCaptor.getValue());
    }

    @Test
    public void deleteRequestWithBody() throws IOException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final String path = "/path/to/delete";
        setupServer("*", new HttpRequestHandler() {

            @Override
            public void handle(HttpRequest request, HttpResponse response, HttpContext context)
                    throws HttpException, IOException {
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
                    System.out.println("Exception handling request: " + e.toString());
                    e.printStackTrace();
                    latch.countDown();
                }
            }
        });

        given(ctx.getExternalCallExecutor()).willReturn(DirectListeningExecutor.INSTANCE);

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
        verify(ctx, timeout(TIMEOUT)).transformMsg(msgCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertNotSame(metaData, metadataCaptor.getValue());
        assertEquals(TbMsg.EMPTY_JSON_OBJECT, dataCaptor.getValue());
    }

    @Test
    public void givenForceAckTrue_whenOnMsgAndServerReturns200_thenAckedImmediatelyAndEnqueuedForTellNext() throws IOException {
        final String path = "/path/to/get";
        setupServer("*", new HttpRequestHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response, HttpContext context)
                    throws HttpException, IOException {
                response.setStatusCode(200);
            }
        });

        TbMsg transformedMsg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(originator)
                .copyMetaData(metaData)
                .dataType(TbMsgDataType.JSON)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .ruleChainId(ruleChainId)
                .ruleNodeId(ruleNodeId)
                .build();

        given(ctx.isExternalNodeForceAck()).willReturn(true);
        given(ctx.getExternalCallExecutor()).willReturn(DirectListeningExecutor.INSTANCE);
        given(ctx.transformMsg(any(), any(), any())).willReturn(transformedMsg);

        TbRestApiCallNodeConfiguration config = new TbRestApiCallNodeConfiguration().defaultConfiguration();
        config.setRequestMethod("GET");
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

        verify(ctx).ack(msg);
        verify(ctx, timeout(TIMEOUT)).enqueueForTellNext(any(), eq(TbNodeConnectionType.SUCCESS));
        verify(ctx, never()).tellSuccess(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void givenMaxParallelRequestsCountAndBadUrl_whenOnMsg_thenSemaphoreIsReleasedAndFailureReported(boolean forceAck) throws IOException {
        given(ctx.isExternalNodeForceAck()).willReturn(forceAck);

        TbRestApiCallNodeConfiguration config = new TbRestApiCallNodeConfiguration().defaultConfiguration();
        config.setMaxParallelRequestsCount(1);
        config.setRestEndpointUrlPattern("");
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

        assertThat(restNode.httpClient.getSemaphore().availablePermits()).isEqualTo(1);
        if (forceAck) {
            verify(ctx).enqueueForTellFailure(any(), any(Throwable.class));
        } else {
            verify(ctx).tellFailure(any(), any());
        }
    }

    @Test
    public void givenMaxPendingRequestsExceeded_whenOnMsg_thenFailsImmediatelyAndQueuedRequestFiresAfterSlotOpens() throws IOException, InterruptedException {
        CountDownLatch releaseResponse = new CountDownLatch(1);
        setupServer("*", (request, response, context) -> {
            try {
                releaseResponse.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            response.setStatusCode(200);
        });

        given(ctx.isExternalNodeForceAck()).willReturn(false);
        given(ctx.getExternalCallExecutor()).willReturn(DirectListeningExecutor.INSTANCE);
        // Simulate server-level cap: maxPendingRequests=1 via TbHttpClientSettings
        given(ctx.getTbHttpClientSettings()).willReturn(new TbHttpClientSettings() {
            @Override public int getMaxParallelRequests() { return 0; }
            @Override public int getMaxPendingRequests() { return 1; }
            @Override public int getPoolMaxConnections() { return 0; }
        });
        TbMsg transformedMsg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(originator)
                .copyMetaData(metaData)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        given(ctx.transformMsg(any(), any(), any())).willReturn(transformedMsg);

        TbRestApiCallNodeConfiguration config = new TbRestApiCallNodeConfiguration().defaultConfiguration();
        config.setMaxParallelRequestsCount(1);
        config.setRequestMethod("GET");
        config.setIgnoreRequestBody(true);
        config.setRestEndpointUrlPattern(String.format("http://localhost:%d/path", server.getLocalPort()));
        initWithConfig(config);

        TbMsg msg1 = TbMsg.newMsg().type(TbMsgType.POST_TELEMETRY_REQUEST).originator(originator)
                .copyMetaData(metaData).dataType(TbMsgDataType.JSON).data(TbMsg.EMPTY_JSON_OBJECT)
                .ruleChainId(ruleChainId).ruleNodeId(ruleNodeId).build();
        TbMsg msg2 = TbMsg.newMsg().type(TbMsgType.POST_TELEMETRY_REQUEST).originator(originator)
                .copyMetaData(metaData).dataType(TbMsgDataType.JSON).data(TbMsg.EMPTY_JSON_OBJECT)
                .ruleChainId(ruleChainId).ruleNodeId(ruleNodeId).build();
        TbMsg msg3 = TbMsg.newMsg().type(TbMsgType.POST_TELEMETRY_REQUEST).originator(originator)
                .copyMetaData(metaData).dataType(TbMsgDataType.JSON).data(TbMsg.EMPTY_JSON_OBJECT)
                .ruleChainId(ruleChainId).ruleNodeId(ruleNodeId).build();

        restNode.onMsg(ctx, msg1);  // fires immediately (semaphore acquired)
        restNode.onMsg(ctx, msg2);  // queues (semaphore exhausted, queue has room)
        restNode.onMsg(ctx, msg3);  // fails immediately (queue full — server-level maxPendingRequests=1)

        verify(ctx, timeout(TIMEOUT)).tellFailure(any(), any());

        releaseResponse.countDown();
        verify(ctx, timeout(TIMEOUT).times(2)).tellSuccess(any());
    }

    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                // config for version 2 with upgrade from version 0
                Arguments.of(0,
                        "{\"restEndpointUrlPattern\":\"http://localhost/api\",\"requestMethod\":\"POST\"," +
                                "\"useSimpleClientHttpFactory\":false,\"ignoreRequestBody\":false,\"enableProxy\":false," +
                                "\"useSystemProxyProperties\":false,\"proxyScheme\":null,\"proxyHost\":null,\"proxyPort\":0," +
                                "\"proxyUser\":null,\"proxyPassword\":null,\"readTimeoutMs\":0,\"maxParallelRequestsCount\":0," +
                                "\"headers\":{\"Content-Type\":\"application/json\"},\"useRedisQueueForMsgPersistence\":false," +
                                "\"trimQueue\":null,\"maxQueueSize\":null,\"credentials\":{\"type\":\"anonymous\"},\"trimDoubleQuotes\":false}",
                        true,
                        "{\"restEndpointUrlPattern\":\"http://localhost/api\",\"requestMethod\": \"POST\"," +
                                "\"useSimpleClientHttpFactory\": false,\"parseToPlainText\": false,\"ignoreRequestBody\": false," +
                                "\"enableProxy\": false,\"useSystemProxyProperties\": false,\"proxyScheme\": null,\"proxyHost\": null," +
                                "\"proxyPort\": 0,\"proxyUser\": null,\"proxyPassword\": null,\"readTimeoutMs\": 0," +
                                "\"maxParallelRequestsCount\": 0,\"headers\": {\"Content-Type\": \"application/json\"}," +
                                "\"credentials\": {\"type\": \"anonymous\"}," +
                                "\"maxInMemoryBufferSizeInKb\": 256}"),
                // config for version 2 with upgrade from version 1
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
                                "\"useSimpleClientHttpFactory\": false,\"parseToPlainText\": false,\"ignoreRequestBody\": false," +
                                "\"enableProxy\": false,\"useSystemProxyProperties\": false,\"proxyScheme\": null,\"proxyHost\": null," +
                                "\"proxyPort\": 0,\"proxyUser\": null,\"proxyPassword\": null,\"readTimeoutMs\": 0," +
                                "\"maxParallelRequestsCount\": 0,\"headers\": {\"Content-Type\": \"application/json\"}," +
                                "\"credentials\": {\"type\": \"anonymous\"}," +
                                "\"maxInMemoryBufferSizeInKb\": 256}"),
                // config for version 3 with upgrade from version 2
                Arguments.of(2,
                        "{\"restEndpointUrlPattern\":\"http://localhost/api\",\"requestMethod\": \"POST\"," +
                                "\"useSimpleClientHttpFactory\": false,\"parseToPlainText\": false,\"ignoreRequestBody\": false," +
                                "\"enableProxy\": false,\"useSystemProxyProperties\": false,\"proxyScheme\": null,\"proxyHost\": null," +
                                "\"proxyPort\": 0,\"proxyUser\": null,\"proxyPassword\": null,\"readTimeoutMs\": 0," +
                                "\"maxParallelRequestsCount\": 0,\"headers\": {\"Content-Type\": \"application/json\"}," +
                                "\"credentials\": {\"type\": \"anonymous\"}}",
                        true,
                        "{\"restEndpointUrlPattern\":\"http://localhost/api\",\"requestMethod\": \"POST\"," +
                                "\"useSimpleClientHttpFactory\": false,\"parseToPlainText\": false,\"ignoreRequestBody\": false," +
                                "\"enableProxy\": false,\"useSystemProxyProperties\": false,\"proxyScheme\": null,\"proxyHost\": null," +
                                "\"proxyPort\": 0,\"proxyUser\": null,\"proxyPassword\": null,\"readTimeoutMs\": 0," +
                                "\"maxParallelRequestsCount\": 0,\"headers\": {\"Content-Type\": \"application/json\"}," +
                                "\"credentials\": {\"type\": \"anonymous\"}," +
                                "\"maxInMemoryBufferSizeInKb\": 256}")
        );
    }

    @Override
    protected TbNode getTestNode() {
        return restNode;
    }

}
