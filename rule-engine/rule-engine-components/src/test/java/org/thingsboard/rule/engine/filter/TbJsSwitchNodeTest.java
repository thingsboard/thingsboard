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

import java.util.Set;
import java.util.concurrent.Callable;

import static org.junit.Assert.*;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TbJsSwitchNodeTest {

    private TbJsSwitchNode node;

    @Mock
    private TbContext ctx;
    @Mock
    private ListeningExecutor executor;

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
        initWithScript(jsCode, Sets.newHashSet("one", "two"));
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("temp", "10");
        metaData.putValue("humidity", "99");
        String rawJson = "{\"name\": \"Vit\", \"passed\": 5}";

        TbMsg msg = new TbMsg(UUIDs.timeBased(), "USER", null, metaData, rawJson.getBytes());
        mockJsExecutor();

        node.onMsg(ctx, msg);
        verify(ctx).getJsExecutor();
        verify(ctx).tellNext(msg, "one");
    }

    @Test
    public void unknownRelationThrowsException() throws TbNodeException {
        String jsCode = "function nextRelation(meta, msg) {\n" +
                "    return 'nine';" +
                "};\n" +
                "\n" +
                "nextRelation(meta, msg);";
        initWithScript(jsCode, Sets.newHashSet("one", "two"));
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("temp", "10");
        metaData.putValue("humidity", "99");
        String rawJson = "{\"name\": \"Vit\", \"passed\": 5}";

        TbMsg msg = new TbMsg(UUIDs.timeBased(), "USER", null, metaData, rawJson.getBytes());
        mockJsExecutor();

        node.onMsg(ctx, msg);
        verify(ctx).getJsExecutor();
        verifyError(msg, "Unsupported relation for switch nine", IllegalStateException.class);
    }

    private void initWithScript(String script, Set<String> relations) throws TbNodeException {
        TbJsSwitchNodeConfiguration config = new TbJsSwitchNodeConfiguration();
        config.setJsScript(script);
        config.setAllowedRelations(relations);
        ObjectMapper mapper = new ObjectMapper();
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration();
        nodeConfiguration.setData(mapper.valueToTree(config));

        node = new TbJsSwitchNode();
        node.init(nodeConfiguration, null);
    }

    private void mockJsExecutor() {
        when(ctx.getJsExecutor()).thenReturn(executor);
        doAnswer((Answer<ListenableFuture<String>>) invocationOnMock -> {
            try {
                Callable task = (Callable) (invocationOnMock.getArguments())[0];
                return Futures.immediateFuture((String) task.call());
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