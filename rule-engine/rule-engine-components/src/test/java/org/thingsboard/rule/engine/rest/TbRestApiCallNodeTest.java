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
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TbRestApiCallNodeTest extends AbstractRuleNodeUpgradeTest {

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
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(1000L);
                            } catch (InterruptedException e) {
                                // ignore
                            } finally {
                                latch.countDown();
                            }
                        }
                    }).start();
                } catch (Exception e) {
                    System.out.println("Exception handling request: " + e.toString());
                    e.printStackTrace();
                    latch.countDown();
                }
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
        verify(ctx).transformMsg(msgCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

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
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(1000L);
                            } catch (InterruptedException e) {
                                // ignore
                            } finally {
                                latch.countDown();
                            }
                        }
                    }).start();
                } catch (Exception e) {
                    System.out.println("Exception handling request: " + e.toString());
                    e.printStackTrace();
                    latch.countDown();
                }
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
        verify(ctx).transformMsg(msgCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertNotSame(metaData, metadataCaptor.getValue());
        assertEquals(TbMsg.EMPTY_JSON_OBJECT, dataCaptor.getValue());
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
