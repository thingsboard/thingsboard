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
package org.thingsboard.rule.engine.filter;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TbJsSwitchNodeTest {

    private TbJsSwitchNode node;

    @Mock
    private TbContext ctx;
    @Mock
    private ListeningExecutor executor;

    @Test
    public void routeToAllDoNotEvaluatesJs() throws TbNodeException {
        HashSet<String> relations = Sets.newHashSet("one", "two");
        initWithScript("test qwerty", relations, true);
        TbMsg msg = new TbMsg(UUIDs.timeBased(), "USER", null, new TbMsgMetaData(), "{}".getBytes());

        node.onMsg(ctx, msg);
        verify(ctx).tellNext(msg, relations);
        verifyNoMoreInteractions(ctx, executor);
    }

    @Test
    public void multipleRoutesAreAllowed() throws TbNodeException {
        String jsCode = "function nextRelation(meta, msg) {\n" +
                "    if(msg.passed == 5 && meta.temp == 10)\n" +
                "        return ['three', 'one']\n" +
                "    else\n" +
                "        return 'two';\n" +
                "};\n" +
                "\n" +
                "nextRelation(meta, msg);";
        initWithScript(jsCode, Sets.newHashSet("one", "two", "three"), false);
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("temp", "10");
        metaData.putValue("humidity", "99");
        String rawJson = "{\"name\": \"Vit\", \"passed\": 5}";

        TbMsg msg = new TbMsg(UUIDs.timeBased(), "USER", null, metaData, rawJson.getBytes());
        mockJsExecutor();

        node.onMsg(ctx, msg);
        verify(ctx).getJsExecutor();
        verify(ctx).tellNext(msg, Sets.newHashSet("one", "three"));
    }

    @Test
    public void allowedRelationPassed() throws TbNodeException {
        String jsCode = "function nextRelation(meta, msg) {\n" +
                "    if(msg.passed == 5 && meta.temp == 10)\n" +
                "        return 'one'\n" +
                "    else\n" +
                "        return 'two';\n" +
                "};\n" +
                "\n" +
                "nextRelation(meta, msg);";
        initWithScript(jsCode, Sets.newHashSet("one", "two"), false);
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("temp", "10");
        metaData.putValue("humidity", "99");
        String rawJson = "{\"name\": \"Vit\", \"passed\": 5}";

        TbMsg msg = new TbMsg(UUIDs.timeBased(), "USER", null, metaData, rawJson.getBytes());
        mockJsExecutor();

        node.onMsg(ctx, msg);
        verify(ctx).getJsExecutor();
        verify(ctx).tellNext(msg, Sets.newHashSet("one"));
    }

    @Test
    public void unknownRelationThrowsException() throws TbNodeException {
        String jsCode = "function nextRelation(meta, msg) {\n" +
                "    return ['one','nine'];" +
                "};\n" +
                "\n" +
                "nextRelation(meta, msg);";
        initWithScript(jsCode, Sets.newHashSet("one", "two"), false);
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("temp", "10");
        metaData.putValue("humidity", "99");
        String rawJson = "{\"name\": \"Vit\", \"passed\": 5}";

        TbMsg msg = new TbMsg(UUIDs.timeBased(), "USER", null, metaData, rawJson.getBytes());
        mockJsExecutor();

        node.onMsg(ctx, msg);
        verify(ctx).getJsExecutor();
        verifyError(msg, "Unsupported relation for switch [nine, one]", IllegalStateException.class);
    }

    private void initWithScript(String script, Set<String> relations, boolean routeToAll) throws TbNodeException {
        TbJsSwitchNodeConfiguration config = new TbJsSwitchNodeConfiguration();
        config.setJsScript(script);
        config.setAllowedRelations(relations);
        config.setRouteToAllWithNoCheck(routeToAll);
        ObjectMapper mapper = new ObjectMapper();
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration();
        nodeConfiguration.setData(mapper.valueToTree(config));

        node = new TbJsSwitchNode();
        node.init(nodeConfiguration, null);
    }

    private void mockJsExecutor() {
        when(ctx.getJsExecutor()).thenReturn(executor);
        doAnswer((Answer<ListenableFuture<Set<String>>>) invocationOnMock -> {
            try {
                Callable task = (Callable) (invocationOnMock.getArguments())[0];
                return Futures.immediateFuture((Set<String>) task.call());
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