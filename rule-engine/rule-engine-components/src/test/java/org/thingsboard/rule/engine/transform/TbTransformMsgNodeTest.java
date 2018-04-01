/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.rule.engine.transform;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.thingsboard.rule.engine.api.ListeningExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TbTransformMsgNodeTest {

    private TbTransformMsgNode node;

    @Mock
    private TbContext ctx;
    @Mock
    private ListeningExecutor executor;

    @Test
    public void metadataCanBeUpdated() throws TbNodeException {
        initWithScript("return metadata.temp = metadata.temp * 10;");
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("temp", "7");
        metaData.putValue("humidity", "99");
        String rawJson = "{\"name\": \"Vit\", \"passed\": 5, \"bigObj\": {\"prop\":42}}";

        TbMsg msg = new TbMsg(UUIDs.timeBased(), "USER", null, metaData, rawJson.getBytes());
        mockJsExecutor();

        node.onMsg(ctx, msg);
        verify(ctx).getJsExecutor();
        ArgumentCaptor<TbMsg> captor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx).tellNext(captor.capture());
        TbMsg actualMsg = captor.getValue();
        assertEquals("70.0", actualMsg.getMetaData().getValue("temp"));
    }

    @Test
    public void metadataCanBeAdded() throws TbNodeException {
        initWithScript("return metadata.newAttr = metadata.humidity - msg.passed;");
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("temp", "7");
        metaData.putValue("humidity", "99");
        String rawJson = "{\"name\": \"Vit\", \"passed\": 5, \"bigObj\": {\"prop\":42}}";

        TbMsg msg = new TbMsg(UUIDs.timeBased(), "USER", null, metaData, rawJson.getBytes());
        mockJsExecutor();

        node.onMsg(ctx, msg);
        verify(ctx).getJsExecutor();
        ArgumentCaptor<TbMsg> captor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx).tellNext(captor.capture());
        TbMsg actualMsg = captor.getValue();
        assertEquals("94.0", actualMsg.getMetaData().getValue("newAttr"));
    }

    @Test
    public void payloadCanBeUpdated() throws TbNodeException {
        initWithScript("msg.passed = msg.passed * metadata.temp; return msg.bigObj.newProp = 'Ukraine' ");
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("temp", "7");
        metaData.putValue("humidity", "99");
        String rawJson = "{\"name\":\"Vit\",\"passed\": 5,\"bigObj\":{\"prop\":42}}";

        TbMsg msg = new TbMsg(UUIDs.timeBased(), "USER", null, metaData, rawJson.getBytes());
        mockJsExecutor();

        node.onMsg(ctx, msg);
        verify(ctx).getJsExecutor();
        ArgumentCaptor<TbMsg> captor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx).tellNext(captor.capture());
        TbMsg actualMsg = captor.getValue();
        String expectedJson = "{\"name\":\"Vit\",\"passed\":35.0,\"bigObj\":{\"prop\":42,\"newProp\":\"Ukraine\"}}";
        assertEquals(expectedJson, new String(actualMsg.getData()));
    }

    private void initWithScript(String script) throws TbNodeException {
        TbTransformMsgNodeConfiguration config = new TbTransformMsgNodeConfiguration();
        config.setJsScript(script);
        ObjectMapper mapper = new ObjectMapper();
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));

        node = new TbTransformMsgNode();
        node.init(null, nodeConfiguration);
    }

    private void mockJsExecutor() {
        when(ctx.getJsExecutor()).thenReturn(executor);
        doAnswer((Answer<ListenableFuture<TbMsg>>) invocationOnMock -> {
            try {
                Callable task = (Callable) (invocationOnMock.getArguments())[0];
                return Futures.immediateFuture((TbMsg) task.call());
            } catch (Throwable th) {
                return Futures.immediateFailedFuture(th);
            }
        }).when(executor).executeAsync(Matchers.any(Callable.class));
    }

    private void verifyError(TbMsg msg, String message, Class expectedClass) {
        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctx).tellError(same(msg), captor.capture());

        Throwable value = captor.getValue();
        assertEquals(expectedClass, value.getClass());
        assertEquals(message, value.getMessage());
    }
}