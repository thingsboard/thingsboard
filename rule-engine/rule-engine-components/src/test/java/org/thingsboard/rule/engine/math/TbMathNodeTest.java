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
package org.thingsboard.rule.engine.math;

import com.google.common.util.concurrent.Futures;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.assertj.core.api.SoftAssertions;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.verification.Timeout;
import org.thingsboard.common.util.AbstractListeningExecutor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleEngineTelemetryService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@Slf4j
public class TbMathNodeTest {

    static final int RULE_DISPATCHER_POOL_SIZE = 2;
    static final int DB_CALLBACK_POOL_SIZE = 3;
    private final EntityId originator = DeviceId.fromString("ccd71696-0586-422d-940e-755a41ec3b0d");
    private final TenantId tenantId = TenantId.fromUUID(UUID.fromString("e7f46b23-0c7d-42f5-9b06-fc35ab17af8a"));

    @Mock
    private TbContext ctx;
    @Mock
    private AttributesService attributesService;
    @Mock
    private TimeseriesService tsService;
    @Mock
    private RuleEngineTelemetryService telemetryService;
    private AbstractListeningExecutor dbCallbackExecutor;
    private AbstractListeningExecutor ruleEngineDispatcherExecutor;

    @BeforeEach
    public void before() {
        dbCallbackExecutor = new DBCallbackExecutor();
        dbCallbackExecutor.init();
        ruleEngineDispatcherExecutor = new RuleDispatcherExecutor();
        ruleEngineDispatcherExecutor.init();

        lenient().when(ctx.getAttributesService()).thenReturn(attributesService);
        lenient().when(ctx.getTelemetryService()).thenReturn(telemetryService);
        lenient().when(ctx.getTimeseriesService()).thenReturn(tsService);
        lenient().when(ctx.getTenantId()).thenReturn(tenantId);
        lenient().when(ctx.getDbCallbackExecutor()).thenReturn(dbCallbackExecutor);
    }

    @AfterEach
    public void after() {
        ruleEngineDispatcherExecutor.executor().shutdownNow();
        dbCallbackExecutor.executor().shutdownNow();
    }

    private void initMocks() {
        Mockito.clearInvocations(ctx, attributesService, tsService, telemetryService);
    }

    private TbMathNode initNode(TbRuleNodeMathFunctionType operation, TbMathResult result, TbMathArgument... arguments) {
        return initNode(operation, null, result, arguments);
    }

    private TbMathNode initNodeWithCustomFunction(String expression, TbMathResult result, TbMathArgument... arguments) {
        return initNode(TbRuleNodeMathFunctionType.CUSTOM, expression, result, arguments);
    }

    private TbMathNode initNodeWithCustomFunction(TbContext ctx, String expression, TbMathResult result, TbMathArgument... arguments) {
        return initNode(ctx, TbRuleNodeMathFunctionType.CUSTOM, expression, result, arguments);
    }

    private TbMathNode initNode(TbRuleNodeMathFunctionType operation, String expression, TbMathResult result, TbMathArgument... arguments) {
        return initNode(this.ctx, operation, expression, result, arguments);
    }

    private TbMathNode initNode(TbContext ctx, TbRuleNodeMathFunctionType operation, String expression, TbMathResult result, TbMathArgument... arguments) {
        try {
            TbMathNodeConfiguration configuration = new TbMathNodeConfiguration();
            configuration.setOperation(operation);
            if (TbRuleNodeMathFunctionType.CUSTOM.equals(operation)) {
                configuration.setCustomFunction(expression);
            }
            configuration.setResult(result);
            configuration.setArguments(Arrays.asList(arguments));
            TbMathNode node = new TbMathNode();
            node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(configuration)));
            return node;
        } catch (TbNodeException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Test
    public void testExp4j() {
        var node = initNodeWithCustomFunction("2a+3b",
                new TbMathResult(TbMathArgumentType.MESSAGE_BODY, "result", 2, false, false, null),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a"),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "b")
        );

        TbMsg msg = TbMsg.newMsg("TEST", originator, new TbMsgMetaData(), JacksonUtil.newObjectNode().put("a", 2).put("b", 2).toString());

        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        Mockito.verify(ctx, Mockito.timeout(5000)).tellSuccess(msgCaptor.capture());

        TbMsg resultMsg = msgCaptor.getValue();
        Assert.assertNotNull(resultMsg);
        Assert.assertNotNull(resultMsg.getData());
        var resultJson = JacksonUtil.toJsonNode(resultMsg.getData());
        Assert.assertTrue(resultJson.has("result"));
        Assert.assertEquals(10, resultJson.get("result").asInt());
    }

    @Test
    public void testSimpleFunctions() {
        testSimpleTwoArgumentFunction(TbRuleNodeMathFunctionType.ADD, 2.1, 2.2, 4.3);
        testSimpleTwoArgumentFunction(TbRuleNodeMathFunctionType.SUB, 2.1, 2.2, -0.1);
        testSimpleTwoArgumentFunction(TbRuleNodeMathFunctionType.MULT, 2.1, 2.0, 4.2);
        testSimpleTwoArgumentFunction(TbRuleNodeMathFunctionType.DIV, 4.2, 2.0, 2.1);

        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.SIN, Math.toRadians(30), 0.5);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.SIN, Math.toRadians(90), 1.0);

        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.SINH, Math.toRadians(0), 0.0);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.COSH, Math.toRadians(0), 1.0);

        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.COS, Math.toRadians(60), 0.5);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.COS, Math.toRadians(0), 1.0);

        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.TAN, Math.toRadians(45), 1);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.TAN, Math.toRadians(0), 0);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.TANH, 90, 1);

        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.ACOS, 0.5, 1.05);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.ASIN, 0.5, 0.52);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.ATAN, 0.5, 0.46);
        testSimpleTwoArgumentFunction(TbRuleNodeMathFunctionType.ATAN2, 0.5, 0.3, 1.03);

        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.EXP, 1, 2.72);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.EXPM1, 1, 1.72);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.ABS, -1, 1);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.SQRT, 4, 2);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.CBRT, 8, 2);

        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.GET_EXP, 4, 2);
        testSimpleTwoArgumentFunction(TbRuleNodeMathFunctionType.HYPOT, 4, 5, 6.4);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.LOG, 4, 1.39);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.LOG10, 4, 0.6);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.LOG1P, 4, 1.61);

        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.CEIL, 1.55, 2);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.FLOOR, 23.97, 23);
        testSimpleTwoArgumentFunction(TbRuleNodeMathFunctionType.FLOOR_DIV, 5, 3, 1);
        testSimpleTwoArgumentFunction(TbRuleNodeMathFunctionType.FLOOR_MOD, 6, 3, 0);

        testSimpleTwoArgumentFunction(TbRuleNodeMathFunctionType.MIN, 5, 3, 3);
        testSimpleTwoArgumentFunction(TbRuleNodeMathFunctionType.MAX, 5, 3, 5);
        testSimpleTwoArgumentFunction(TbRuleNodeMathFunctionType.POW, 5, 3, 125);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.SIGNUM, 0.55, 1);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.RAD, 5, 0.09);
        testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType.DEG, 5, 286.48);
    }

    private void testSimpleTwoArgumentFunction(TbRuleNodeMathFunctionType function, double arg1, double arg2, double result) {
        initMocks();

        var node = initNode(function,
                new TbMathResult(TbMathArgumentType.MESSAGE_BODY, "result", 2, false, false, null),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a"),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "b")
        );

        TbMsg msg = TbMsg.newMsg("TEST", originator, new TbMsgMetaData(), JacksonUtil.newObjectNode().put("a", arg1).put("b", arg2).toString());

        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        Mockito.verify(ctx, Mockito.timeout(5000).times(1)).tellSuccess(msgCaptor.capture());

        TbMsg resultMsg = msgCaptor.getValue();
        Assert.assertNotNull(resultMsg);
        Assert.assertNotNull(resultMsg.getData());
        var resultJson = JacksonUtil.toJsonNode(resultMsg.getData());
        Assert.assertTrue(resultJson.has("result"));
        Assert.assertEquals(result, resultJson.get("result").asDouble(), 0d);
    }

    private void testSimpleOneArgumentFunction(TbRuleNodeMathFunctionType function, double arg1, double result) {
        initMocks();

        var node = initNode(function,
                new TbMathResult(TbMathArgumentType.MESSAGE_BODY, "result", 2, false, false, null),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a")
        );

        TbMsg msg = TbMsg.newMsg("TEST", originator, new TbMsgMetaData(), JacksonUtil.newObjectNode().put("a", arg1).toString());

        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        Mockito.verify(ctx, Mockito.timeout(5000)).tellSuccess(msgCaptor.capture());

        TbMsg resultMsg = msgCaptor.getValue();
        Assert.assertNotNull(resultMsg);
        Assert.assertNotNull(resultMsg.getData());
        var resultJson = JacksonUtil.toJsonNode(resultMsg.getData());
        Assert.assertTrue(resultJson.has("result"));
        Assert.assertEquals(result, resultJson.get("result").asDouble(), 0d);
    }

    @Test
    public void test_2_plus_2_body() {
        var node = initNode(TbRuleNodeMathFunctionType.ADD,
                new TbMathResult(TbMathArgumentType.MESSAGE_BODY, "result", 2, false, false, null),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a"),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "b")
        );

        TbMsg msg = TbMsg.newMsg("TEST", originator, new TbMsgMetaData(), JacksonUtil.newObjectNode().put("a", 2).put("b", 2).toString());

        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        Mockito.verify(ctx, Mockito.timeout(5000)).tellSuccess(msgCaptor.capture());

        TbMsg resultMsg = msgCaptor.getValue();
        Assert.assertNotNull(resultMsg);
        Assert.assertNotNull(resultMsg.getData());
        var resultJson = JacksonUtil.toJsonNode(resultMsg.getData());
        Assert.assertTrue(resultJson.has("result"));
        Assert.assertEquals(4, resultJson.get("result").asInt());
    }

    @Test
    public void test_2_plus_2_meta() {
        var node = initNode(TbRuleNodeMathFunctionType.ADD,
                new TbMathResult(TbMathArgumentType.MESSAGE_METADATA, "result", 0, false, false, null),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a"),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "b")
        );

        TbMsg msg = TbMsg.newMsg("TEST", originator, new TbMsgMetaData(), JacksonUtil.newObjectNode().put("a", 2).put("b", 2).toString());

        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        Mockito.verify(ctx, Mockito.timeout(5000)).tellSuccess(msgCaptor.capture());

        TbMsg resultMsg = msgCaptor.getValue();
        Assert.assertNotNull(resultMsg);
        Assert.assertNotNull(resultMsg.getData());
        Assert.assertNotNull(resultMsg.getMetaData());
        var result = resultMsg.getMetaData().getValue("result");
        Assert.assertNotNull(result);
        Assert.assertEquals("4", result);
    }

    @Test
    public void test_2_plus_2_attr_and_ts() {
        var node = initNode(TbRuleNodeMathFunctionType.ADD,
                new TbMathResult(TbMathArgumentType.MESSAGE_BODY, "result", 2, false, false, null),
                new TbMathArgument(TbMathArgumentType.ATTRIBUTE, "a"),
                new TbMathArgument(TbMathArgumentType.TIME_SERIES, "b")
        );

        TbMsg msg = TbMsg.newMsg("TEST", originator, new TbMsgMetaData(), JacksonUtil.newObjectNode().toString());

        Mockito.when(attributesService.find(tenantId, originator, DataConstants.SERVER_SCOPE, "a"))
                .thenReturn(Futures.immediateFuture(Optional.of(new BaseAttributeKvEntry(System.currentTimeMillis(), new DoubleDataEntry("a", 2.0)))));

        Mockito.when(tsService.findLatest(tenantId, originator, "b"))
                .thenReturn(Futures.immediateFuture(Optional.of(new BasicTsKvEntry(System.currentTimeMillis(), new LongDataEntry("b", 2L)))));

        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        Mockito.verify(ctx, Mockito.timeout(5000)).tellSuccess(msgCaptor.capture());

        TbMsg resultMsg = msgCaptor.getValue();
        Assert.assertNotNull(resultMsg);
        Assert.assertNotNull(resultMsg.getData());
        var resultJson = JacksonUtil.toJsonNode(resultMsg.getData());
        Assert.assertTrue(resultJson.has("result"));
        Assert.assertEquals(4, resultJson.get("result").asInt());
    }

    @Test
    public void test_sqrt_5_body() {
        var node = initNode(TbRuleNodeMathFunctionType.SQRT,
                new TbMathResult(TbMathArgumentType.MESSAGE_BODY, "result", 3, false, false, null),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a")
        );

        TbMsg msg = TbMsg.newMsg("TEST", originator, new TbMsgMetaData(), JacksonUtil.newObjectNode().put("a", 5).toString());

        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        Mockito.verify(ctx, Mockito.timeout(5000)).tellSuccess(msgCaptor.capture());

        TbMsg resultMsg = msgCaptor.getValue();
        Assert.assertNotNull(resultMsg);
        Assert.assertNotNull(resultMsg.getData());
        var resultJson = JacksonUtil.toJsonNode(resultMsg.getData());
        Assert.assertTrue(resultJson.has("result"));
        Assert.assertEquals(2.236, resultJson.get("result").asDouble(), 0.0);
    }

    @Test
    public void test_sqrt_5_meta() {
        var node = initNode(TbRuleNodeMathFunctionType.SQRT,
                new TbMathResult(TbMathArgumentType.MESSAGE_METADATA, "result", 3, false, false, null),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a")
        );

        TbMsg msg = TbMsg.newMsg("TEST", originator, new TbMsgMetaData(), JacksonUtil.newObjectNode().put("a", 5).toString());

        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        Mockito.verify(ctx, Mockito.timeout(5000)).tellSuccess(msgCaptor.capture());

        TbMsg resultMsg = msgCaptor.getValue();
        Assert.assertNotNull(resultMsg);
        Assert.assertNotNull(resultMsg.getData());
        var result = resultMsg.getMetaData().getValue("result");
        Assert.assertNotNull(result);
        Assert.assertEquals("2.236", result);
    }

    @Test
    public void test_sqrt_5_to_attribute_and_metadata() {
        var node = initNode(TbRuleNodeMathFunctionType.SQRT,
                new TbMathResult(TbMathArgumentType.ATTRIBUTE, "result", 3, false, true, DataConstants.SERVER_SCOPE),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a")
        );

        TbMsg msg = TbMsg.newMsg("TEST", originator, new TbMsgMetaData(), JacksonUtil.newObjectNode().put("a", 5).toString());

        Mockito.when(telemetryService.saveAttrAndNotify(any(), any(), anyString(), anyString(), anyDouble()))
                .thenReturn(Futures.immediateFuture(null));

        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        Mockito.verify(ctx, Mockito.timeout(5000)).tellSuccess(msgCaptor.capture());
        Mockito.verify(telemetryService, times(1)).saveAttrAndNotify(any(), any(), anyString(), anyString(), anyDouble());

        TbMsg resultMsg = msgCaptor.getValue();
        Assert.assertNotNull(resultMsg);
        Assert.assertNotNull(resultMsg.getData());
        var result = resultMsg.getMetaData().getValue("result");
        Assert.assertNotNull(result);
        Assert.assertEquals("2.236", result);
    }

    @Test
    public void test_sqrt_5_to_timeseries_and_data() {
        var node = initNode(TbRuleNodeMathFunctionType.SQRT,
                new TbMathResult(TbMathArgumentType.TIME_SERIES, "result", 3, true, false, DataConstants.SERVER_SCOPE),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a")
        );

        TbMsg msg = TbMsg.newMsg("TEST", originator, new TbMsgMetaData(), JacksonUtil.newObjectNode().put("a", 5).toString());
        Mockito.when(telemetryService.saveAndNotify(any(), any(), any(TsKvEntry.class)))
                .thenReturn(Futures.immediateFuture(null));

        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, Mockito.timeout(5000)).tellSuccess(msgCaptor.capture());
        Mockito.verify(telemetryService, times(1)).saveAndNotify(any(), any(), any(TsKvEntry.class));

        TbMsg resultMsg = msgCaptor.getValue();
        Assert.assertNotNull(resultMsg);
        Assert.assertNotNull(resultMsg.getData());
        var resultJson = JacksonUtil.toJsonNode(resultMsg.getData());
        Assert.assertTrue(resultJson.has("result"));
        Assert.assertEquals(2.236, resultJson.get("result").asDouble(), 0.0);
    }

    @Test
    public void test_sqrt_5_to_timeseries_and_metadata_and_data() {
        var node = initNode(TbRuleNodeMathFunctionType.SQRT,
                new TbMathResult(TbMathArgumentType.TIME_SERIES, "result", 3, true, true, DataConstants.SERVER_SCOPE),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a")
        );

        TbMsg msg = TbMsg.newMsg("TEST", originator, new TbMsgMetaData(), JacksonUtil.newObjectNode().put("a", 5).toString());
        Mockito.when(telemetryService.saveAndNotify(any(), any(), any(TsKvEntry.class)))
                .thenReturn(Futures.immediateFuture(null));

        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, Mockito.timeout(5000)).tellSuccess(msgCaptor.capture());
        Mockito.verify(telemetryService, times(1)).saveAndNotify(any(), any(), any(TsKvEntry.class));

        TbMsg resultMsg = msgCaptor.getValue();
        Assert.assertNotNull(resultMsg);
        Assert.assertNotNull(resultMsg.getData());
        var resultMetadata = resultMsg.getMetaData().getValue("result");
        var resultData = JacksonUtil.toJsonNode(resultMsg.getData());

        Assert.assertTrue(resultData.has("result"));
        Assert.assertEquals(2.236, resultData.get("result").asDouble(), 0.0);

        Assert.assertNotNull(resultMetadata);
        Assert.assertEquals("2.236", resultMetadata);
    }

    @Test
    public void test_sqrt_5_default_value() {
        TbMathArgument tbMathArgument = new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "TestKey");
        tbMathArgument.setDefaultValue(5.0);
        var node = initNode(TbRuleNodeMathFunctionType.SQRT,
                new TbMathResult(TbMathArgumentType.MESSAGE_METADATA, "result", 3, false, false, null),
                tbMathArgument
        );
        TbMsg msg = TbMsg.newMsg("TEST", originator, new TbMsgMetaData(), JacksonUtil.newObjectNode().put("a", 10).toString());

        node.onMsg(ctx, msg);
        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        Mockito.verify(ctx, Mockito.timeout(5000)).tellSuccess(msgCaptor.capture());

        TbMsg resultMsg = msgCaptor.getValue();
        Assert.assertNotNull(resultMsg);
        Assert.assertNotNull(resultMsg.getData());
        var result = resultMsg.getMetaData().getValue("result");
        Assert.assertNotNull(result);
        Assert.assertEquals("2.236", result);
    }

    @Test
    public void test_sqrt_5_default_value_failure() {
        var node = initNode(TbRuleNodeMathFunctionType.SQRT,
                new TbMathResult(TbMathArgumentType.TIME_SERIES, "result", 3, true, false, DataConstants.SERVER_SCOPE),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "TestKey")
        );
        TbMsg msg = TbMsg.newMsg("TEST", originator, new TbMsgMetaData(), JacksonUtil.newObjectNode().put("a", 10).toString());
        node.onMsg(ctx, msg);

        ArgumentCaptor<Throwable> tCaptor = ArgumentCaptor.forClass(Throwable.class);
        Mockito.verify(ctx, Mockito.timeout(5000)).tellFailure(eq(msg), tCaptor.capture());
        Assert.assertNotNull(tCaptor.getValue().getMessage());
    }

    @Test
    public void testConvertMsgBodyIfRequiredFailure() {
        var node = initNode(TbRuleNodeMathFunctionType.SQRT,
                new TbMathResult(TbMathArgumentType.MESSAGE_BODY, "result", 3, true, false, DataConstants.SERVER_SCOPE),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a")
        );

        TbMsg msg = TbMsg.newMsg("TEST", originator, new TbMsgMetaData(), "[]");
        node.onMsg(ctx, msg);

        ArgumentCaptor<Throwable> tCaptor = ArgumentCaptor.forClass(Throwable.class);
        Mockito.verify(ctx, Mockito.timeout(5000)).tellFailure(eq(msg), tCaptor.capture());
        Assert.assertNotNull(tCaptor.getValue().getMessage());
    }

    @Test
    public void testExp4j_concurrent() {
        TbMathNode node = spy(initNodeWithCustomFunction("2a+3b",
                new TbMathResult(TbMathArgumentType.MESSAGE_BODY, "result", 2, false, false, null),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a"),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "b")
        ));
        EntityId originatorSlow = DeviceId.fromString("7f01170d-6bba-419c-b95c-2b4c3ba32f30");
        EntityId originatorFast = DeviceId.fromString("c45360ff-7906-4102-a2ae-3495a86168d0");
        CountDownLatch slowProcessingLatch = new CountDownLatch(1);

        List<TbMsg> slowMsgList = IntStream.range(0, 5)
                .mapToObj(x -> TbMsg.newMsg("TEST", originatorSlow, new TbMsgMetaData(), JacksonUtil.newObjectNode().put("a", 2).put("b", 2).toString()))
                .collect(Collectors.toList());
        List<TbMsg> fastMsgList = IntStream.range(0, 2)
                .mapToObj(x -> TbMsg.newMsg("TEST", originatorFast, new TbMsgMetaData(), JacksonUtil.newObjectNode().put("a", 2).put("b", 2).toString()))
                .collect(Collectors.toList());

        assertThat(slowMsgList.size()).as("slow msgs >= rule-dispatcher pool size").isGreaterThanOrEqualTo(RULE_DISPATCHER_POOL_SIZE);

        log.debug("rule-dispatcher [{}], db-callback [{}], slowMsg [{}], fastMsg [{}]", RULE_DISPATCHER_POOL_SIZE, DB_CALLBACK_POOL_SIZE, slowMsgList.size(), fastMsgList.size());

        willAnswer(invocation -> {
            TbMsg msg = invocation.getArgument(1);
            log.debug("\uD83D\uDC0C processMsgAsync slow originator [{}][{}]", msg.getOriginator(), msg);
            try {
                assertThat(slowProcessingLatch.await(30, TimeUnit.SECONDS)).as("await on slowProcessingLatch").isTrue();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return invocation.callRealMethod();
        }).given(node).processMsgAsync(eq(ctx), argThat(slowMsgList::contains));

        willAnswer(invocation -> {
            TbMsg msg = invocation.getArgument(1);
            log.debug("\u26A1\uFE0F processMsgAsync FAST originator [{}][{}]", msg.getOriginator(), msg);
            return invocation.callRealMethod();
        }).given(node).processMsgAsync(eq(ctx), argThat(fastMsgList::contains));

        willAnswer(invocation -> {
            TbMsg msg = invocation.getArgument(1);
            log.debug("submit slow originator onMsg [{}][{}]", msg.getOriginator(), msg);
            return invocation.callRealMethod();
        }).given(node).onMsg(eq(ctx), argThat(slowMsgList::contains));

        willAnswer(invocation -> {
            TbMsg msg = invocation.getArgument(1);
            log.debug("submit FAST originator onMsg [{}][{}]", msg.getOriginator(), msg);
            return invocation.callRealMethod();
        }).given(node).onMsg(eq(ctx), argThat(fastMsgList::contains));

        // submit slow msg may block all rule engine dispatcher threads
        slowMsgList.forEach(msg -> ruleEngineDispatcherExecutor.executeAsync(() -> node.onMsg(ctx, msg)));
        // wait until dispatcher threads started with all slowMsg
        verify(node, new Timeout(TimeUnit.SECONDS.toMillis(5), times(slowMsgList.size()))).onMsg(eq(ctx), argThat(slowMsgList::contains));

        // submit fast have to return immediately
        fastMsgList.forEach(msg -> ruleEngineDispatcherExecutor.executeAsync(() -> node.onMsg(ctx, msg)));
        // wait until all fast messages processed
        verify(ctx, new Timeout(TimeUnit.SECONDS.toMillis(5), times(fastMsgList.size()))).tellSuccess(any());

        slowProcessingLatch.countDown();

        verify(ctx, new Timeout(TimeUnit.SECONDS.toMillis(5), times(fastMsgList.size() + slowMsgList.size()))).tellSuccess(any());

        verify(ctx, never()).tellFailure(any(), any());
    }

    @Test
    public void testExp4j_concurrentBySingleOriginator_processMsgAsyncException() {
        TbMathNode node = spy(initNodeWithCustomFunction("2a+3b",
                new TbMathResult(TbMathArgumentType.MESSAGE_BODY, "result", 2, false, false, null),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a"),
                new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "b")
        ));

        willThrow(new RuntimeException("Message body has no 'delta'")).given(node).resolveArguments(any(), any(), any(), any());

        EntityId originatorSlow = DeviceId.fromString("7f01170d-6bba-419c-b95c-2b4c3ba32f30");
        CountDownLatch slowProcessingLatch = new CountDownLatch(1);

        List<TbMsg> slowMsgList = IntStream.range(0, 5)
                .mapToObj(x -> TbMsg.newMsg("TEST", originatorSlow, new TbMsgMetaData(), JacksonUtil.newObjectNode().put("a", 2).put("b", 2).toString()))
                .collect(Collectors.toList());

        assertThat(slowMsgList.size()).as("slow msgs >= rule-dispatcher pool size").isGreaterThanOrEqualTo(RULE_DISPATCHER_POOL_SIZE);

        log.debug("rule-dispatcher [{}], db-callback [{}], slowMsg [{}]", RULE_DISPATCHER_POOL_SIZE, DB_CALLBACK_POOL_SIZE, slowMsgList.size());

        willAnswer(invocation -> {
            TbMsg msg = invocation.getArgument(1);
            if (slowProcessingLatch.getCount() > 0) {
                log.debug("Await on slowProcessingLatch before processMsgAsync");
                try {
                    assertThat(slowProcessingLatch.await(30, TimeUnit.SECONDS)).as("await on slowProcessingLatch").isTrue();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            log.debug("\uD83D\uDC0C processMsgAsync with exception [{}][{}]", msg.getOriginator(), msg);
            return invocation.callRealMethod();
        }).given(node).processMsgAsync(eq(ctx), argThat(slowMsgList::contains));

        willAnswer(invocation -> {
            TbMsg msg = invocation.getArgument(1);
            log.debug("submit slow originator onMsg [{}][{}]", msg.getOriginator(), msg);
            return invocation.callRealMethod();
        }).given(node).onMsg(eq(ctx), argThat(slowMsgList::contains));

        // submit slow msg may block all rule engine dispatcher threads
        slowMsgList.forEach(msg -> ruleEngineDispatcherExecutor.executeAsync(() -> node.onMsg(ctx, msg)));
        // wait until dispatcher threads started with all slowMsg
        verify(node, new Timeout(TimeUnit.SECONDS.toMillis(5), times(slowMsgList.size()))).onMsg(eq(ctx), argThat(slowMsgList::contains));

        slowProcessingLatch.countDown();

        verify(ctx, new Timeout(TimeUnit.SECONDS.toMillis(5), times(slowMsgList.size()))).tellFailure(any(), any());
        verify(ctx, never()).tellSuccess(any());

    }

    @Test
    public void testExp4j_concurrentBySingleOriginator_SingleMsg_manyNodesWithDifferentOutput() {
        assertThat(RULE_DISPATCHER_POOL_SIZE).as("dispatcher pool size have to be > 1").isGreaterThan(1);
        CountDownLatch processingLatch = new CountDownLatch(1);
        List<Triple<TbContext, String, TbMathNode>> ctxNodes = IntStream.range(0, RULE_DISPATCHER_POOL_SIZE * 2)
                .mapToObj(x -> {
                    final TbContext ctx = mock(TbContext.class); // many rule nodes - many contexts
                    willReturn(dbCallbackExecutor).given(ctx).getDbCallbackExecutor();
                    final String resultKey = "result" + x;
                    final TbMathNode node = spy(initNodeWithCustomFunction(ctx, "2a+3b",
                            new TbMathResult(TbMathArgumentType.MESSAGE_METADATA, resultKey, 1, false, true, null),
                            new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "a"),
                            new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "b")));
                    willAnswer(invocation -> {
                        if (processingLatch.getCount() > 0) {
                            log.debug("Await on processingLatch before processMsgAsync");
                            try {
                                assertThat(processingLatch.await(30, TimeUnit.SECONDS)).as("await on processingLatch").isTrue();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        log.debug("\uD83D\uDC0C processMsgAsync on node with expected resultKey [{}]", resultKey);
                        return invocation.callRealMethod();
                    }).given(node).processMsgAsync(any(), any());
                    willAnswer(invocation -> {
                        TbMsg msg = invocation.getArgument(1);
                        log.debug("submit originator onMsg [{}][{}]", msg.getOriginator(), msg);
                        return invocation.callRealMethod();
                    }).given(node).onMsg(any(), any());
                    return Triple.of(ctx, resultKey, node);
                })
                .collect(Collectors.toList());

        final TbMsg msg = TbMsg.newMsg("POST_TELEMETRY_REQUEST", originator, new TbMsgMetaData(), JacksonUtil.newObjectNode().put("a", 2).put("b", 2).toString());

        ctxNodes.forEach(ctxNode -> ruleEngineDispatcherExecutor.executeAsync(() -> ctxNode.getRight().onMsg(ctxNode.getLeft(), msg)));
        ctxNodes.forEach(ctxNode -> verify(ctxNode.getRight(), timeout(5000)).onMsg(eq(ctxNode.getLeft()), any()));
        processingLatch.countDown();

        SoftAssertions softly = new SoftAssertions();
        ctxNodes.forEach(ctxNode -> {
            final TbContext ctx = ctxNode.getLeft();
            final String resultKey = ctxNode.getMiddle();
            ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
            verify(ctx, timeout(5000)).tellSuccess(msgCaptor.capture());

            TbMsg resultMsg = msgCaptor.getValue();
            assertThat(resultMsg).as("result msg non null for result key " + resultKey).isNotNull();
            log.debug("asserting result key [{}] in metadata [{}]", resultKey, resultMsg.getMetaData().getData());
            softly.assertThat(resultMsg.getMetaData().getValue(resultKey)).as("asserting result key " + resultKey)
                    .isEqualTo("10.0");
        });

        softly.assertAll();
        verify(ctx, never()).tellFailure(any(), any());
    }

    static class RuleDispatcherExecutor extends AbstractListeningExecutor {
        @Override
        protected int getThreadPollSize() {
            return RULE_DISPATCHER_POOL_SIZE;
        }
    }

    static class DBCallbackExecutor extends AbstractListeningExecutor {
        @Override
        protected int getThreadPollSize() {
            return DB_CALLBACK_POOL_SIZE;
        }
    }

}
