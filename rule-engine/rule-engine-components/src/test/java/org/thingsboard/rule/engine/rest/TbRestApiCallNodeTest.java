/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbEmail;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import com.datastax.oss.driver.api.core.uuid.Uuids;

@RunWith(MockitoJUnitRunner.class)
public class TbRestApiCallNodeTest {
	
    private TbRestApiCallNode restNode;

    @Mock
    private TbContext ctx;

    private EntityId originator = new DeviceId(Uuids.timeBased());
    private TbMsgMetaData metaData = new TbMsgMetaData();

    private RuleChainId ruleChainId = new RuleChainId(Uuids.timeBased());
    private RuleNodeId ruleNodeId = new RuleNodeId(Uuids.timeBased());

	private HttpServer server;

    public void setupServer(String pattern, HttpRequestHandler handler) throws IOException {
        SocketConfig config  = SocketConfig.custom().setSoReuseAddress(true).setTcpNoDelay(true).build();
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

    @After
    public void teardown() {
        server.stop();
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
                    assertEquals("Request path matches", request.getRequestLine().getUri(), path);
                    assertFalse("Content-Type not included", request.containsHeader("Content-Type"));
                    assertTrue("Custom header included", request.containsHeader("Foo"));
                    assertEquals("Custom header value", "Bar", request.getFirstHeader("Foo").getValue());
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
                } catch ( Exception e ) {
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

        TbMsg msg = TbMsg.newMsg( "USER", originator, metaData, TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);
        restNode.onMsg(ctx, msg);

        assertTrue("Server handled request", latch.await(10, TimeUnit.SECONDS));

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());


        assertEquals("USER", typeCaptor.getValue());
        assertEquals(originator, originatorCaptor.getValue());
        assertNotSame(metaData, metadataCaptor.getValue());
        assertEquals("{}", dataCaptor.getValue());
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
                    assertEquals("Request path matches", path, request.getRequestLine().getUri());
                    assertTrue("Content-Type included", request.containsHeader("Content-Type"));
                    assertEquals("Content-Type value", "text/plain;charset=ISO-8859-1",
                            request.getFirstHeader("Content-Type").getValue());
                    assertTrue("Content-Length included", request.containsHeader("Content-Length"));
                    assertEquals("Content-Length value", "2",
                            request.getFirstHeader("Content-Length").getValue());
                    assertTrue("Custom header included", request.containsHeader("Foo"));
                    assertEquals("Custom header value", "Bar", request.getFirstHeader("Foo").getValue());
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
                } catch ( Exception e ) {
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

        TbMsg msg = TbMsg.newMsg( "USER", originator, metaData, TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);
        restNode.onMsg(ctx, msg);

        assertTrue("Server handled request", latch.await(10, TimeUnit.SECONDS));

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());
        
        assertEquals("USER", typeCaptor.getValue());
        assertEquals(originator, originatorCaptor.getValue());
        assertNotSame(metaData, metadataCaptor.getValue());
        assertEquals("{}", dataCaptor.getValue());
    }

}
