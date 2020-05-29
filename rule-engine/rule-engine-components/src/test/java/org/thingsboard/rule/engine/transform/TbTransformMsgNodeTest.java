/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import javax.script.ScriptException;
import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;

@RunWith(MockitoJUnitRunner.class)
public class TbTransformMsgNodeTest {

    private TbTransformMsgNode node;

    @Mock
    private TbContext ctx;
    @Mock
    private ListeningExecutor executor;
    @Mock
    private ScriptEngine scriptEngine;

    @Test
    public void metadataCanBeUpdated() throws TbNodeException, ScriptException {
        initWithScript();
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("temp", "7");
        String rawJson = "{\"passed\": 5}";

        RuleChainId ruleChainId = new RuleChainId(UUIDs.timeBased());
        RuleNodeId ruleNodeId = new RuleNodeId(UUIDs.timeBased());
        TbMsg msg = TbMsg.newMsg( "USER", null, metaData, TbMsgDataType.JSON,rawJson, ruleChainId, ruleNodeId);
        TbMsg transformedMsg = TbMsg.newMsg( "USER", null, metaData, TbMsgDataType.JSON, "{new}", ruleChainId, ruleNodeId);
        mockJsExecutor();
        when(scriptEngine.executeUpdateAsync(msg)).thenReturn(Futures.immediateFuture(transformedMsg));

        node.onMsg(ctx, msg);
        verify(ctx).getDbCallbackExecutor();
        ArgumentCaptor<TbMsg> captor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx).tellSuccess(captor.capture());
        TbMsg actualMsg = captor.getValue();
        assertEquals(transformedMsg, actualMsg);
    }

    @Test
    public void exceptionHandledCorrectly() throws TbNodeException, ScriptException {
        initWithScript();
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("temp", "7");
        String rawJson = "{\"passed\": 5";

        RuleChainId ruleChainId = new RuleChainId(UUIDs.timeBased());
        RuleNodeId ruleNodeId = new RuleNodeId(UUIDs.timeBased());
        TbMsg msg = TbMsg.newMsg( "USER", null, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);
        mockJsExecutor();
        when(scriptEngine.executeUpdateAsync(msg)).thenReturn(Futures.immediateFailedFuture(new IllegalStateException("error")));

        node.onMsg(ctx, msg);
        verifyError(msg, "error", IllegalStateException.class);
    }

    private void initWithScript() throws TbNodeException {
        TbTransformMsgNodeConfiguration config = new TbTransformMsgNodeConfiguration();
        config.setJsScript("scr");
        ObjectMapper mapper = new ObjectMapper();
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));

        when(ctx.createJsScriptEngine("scr")).thenReturn(scriptEngine);

        node = new TbTransformMsgNode();
        node.init(ctx, nodeConfiguration);
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
        verify(ctx).tellFailure(same(msg), captor.capture());

        Throwable value = captor.getValue();
        assertEquals(expectedClass, value.getClass());
        assertEquals(message, value.getMessage());
    }
}
