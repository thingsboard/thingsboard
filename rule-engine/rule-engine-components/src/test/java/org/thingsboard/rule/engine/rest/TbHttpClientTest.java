/**
 * Copyright © 2016-2022 The Thingsboard Authors
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


import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.credentials.AnonymousCredentials;
import org.thingsboard.rule.engine.credentials.ClientCredentials;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TbHttpClientTest {

    EventLoopGroup eventLoop;
    TbHttpClient client;

    @Before
    public void setUp() throws Exception {
        client = mock(TbHttpClient.class);
        willCallRealMethod().given(client).getSharedOrCreateEventLoopGroup(any());
    }

    @After
    public void tearDown() throws Exception {
        if (eventLoop != null) {
            eventLoop.shutdownGracefully();
        }
    }

    @Test
    public void givenSharedEventLoop_whenGetEventLoop_ThenReturnShared() {
        eventLoop = mock(EventLoopGroup.class);
        assertThat(client.getSharedOrCreateEventLoopGroup(eventLoop), is(eventLoop));
    }

    @Test
    public void givenNull_whenGetEventLoop_ThenReturnShared() {
        eventLoop = client.getSharedOrCreateEventLoopGroup(null);
        assertThat(eventLoop, instanceOf(NioEventLoopGroup.class));
    }

    @Test
    public void testProcessMessageWithJsonInUrlVariable() throws Exception{
        var config = mock(TbRestApiCallNodeConfiguration.class);
        when(config.getCredentials()).thenReturn(new AnonymousCredentials());
        when(config.getRequestMethod()).thenReturn("GET");
        when(config.getRestEndpointUrlPattern())
                .thenReturn("http://localhost/api?data=[{\\\"test\\\":\\\"test\\\"}]");

        var httpClient = new TbHttpClient(config, eventLoop);
        var ctx = mock(TbContext.class);
        var msg = TbMsg.newMsg(
                "Main", "GET", new DeviceId(EntityId.NULL_UUID),
                TbMsgMetaData.EMPTY, ""
        );


        httpClient.processMessage(ctx, msg);
    }
}