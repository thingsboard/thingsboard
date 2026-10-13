// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.latest.telemetry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.thingsboard.common.util.DirectListeningExecutor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.analytics.incoming.MathFunction;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TbPeContext;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.util.concurrent.Futures.immediateFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TbAggLatestTelemetryNodeV2Test extends AbstractRuleNodeUpgradeTest {

    static final String FILTER_FUNCTION = "return Number(attributes['temperature']) > 42;";

    @Mock
    TbContext ctx;
    @Mock
    TbPeContext peCtx;
    @Mock
    RelationService relationService;
    @Mock
    TimeseriesService timeseriesService;
    @Mock
    AttributesService attributesService;
    @Mock
    ScriptEngine scriptEngine;

    ListeningExecutor executor = DirectListeningExecutor.INSTANCE;
    TbAggLatestTelemetryNodeV2 node;
    TenantId tenantId;
    AssetId assetId;

    @BeforeEach
    void before() {
        tenantId = TenantId.fromUUID(UUID.randomUUID());
        assetId = new AssetId(UUID.randomUUID());
        node = spy(new TbAggLatestTelemetryNodeV2());

        lenient().when(ctx.getTenantId()).thenReturn(tenantId);
        lenient().when(ctx.getPeContext()).thenReturn(peCtx);
        lenient().when(ctx.getDbCallbackExecutor()).thenReturn(executor);
        lenient().when(ctx.getRelationService()).thenReturn(relationService);
        lenient().when(ctx.getTimeseriesService()).thenReturn(timeseriesService);
        lenient().when(ctx.getAttributesService()).thenReturn(attributesService);

        initMocks();
    }

    @AfterEach
    void after() {
        node.destroy();
    }

    TbAggLatestTelemetryNodeV2Configuration getConfigNode() {
        TbAggLatestTelemetryNodeV2Configuration config = new TbAggLatestTelemetryNodeV2Configuration();

        List<AggLatestMapping> aggMappings = new ArrayList<>();

        AggLatestMapping avgTempMapping = new AggLatestMapping();
        avgTempMapping.setSource("temperature");
        avgTempMapping.setSourceScope("LATEST_TELEMETRY");
        avgTempMapping.setAggFunction(MathFunction.AVG);
        avgTempMapping.setDefaultValue(0);
        avgTempMapping.setTarget("latestAvgTemperature");
        aggMappings.add(avgTempMapping);

        AggLatestMapping countMapping = new AggLatestMapping();
        countMapping.setSource("temperature");
        countMapping.setSourceScope("LATEST_TELEMETRY");
        countMapping.setAggFunction(MathFunction.COUNT);
        countMapping.setDefaultValue(0);
        countMapping.setTarget("deviceCount");
        aggMappings.add(countMapping);

        config.setAggMappings(aggMappings);

        config.setDirection(EntitySearchDirection.FROM);
        config.setRelationType(EntityRelation.CONTAINS_TYPE);
        config.setOutMsgType(TbMsgType.POST_TELEMETRY_REQUEST.name());

        return config;
    }

    void initMocks() {
        var deviceA = new DeviceId(UUID.randomUUID());
        var deviceB = new DeviceId(UUID.randomUUID());

        var relationA = new EntityRelation(assetId, deviceA, EntityRelation.CONTAINS_TYPE);
        var relationB = new EntityRelation(assetId, deviceB, EntityRelation.CONTAINS_TYPE);

        var deviceATemperature = new BasicTsKvEntry(System.currentTimeMillis(), new DoubleDataEntry("temperature", 42.0));
        var deviceBTemperature = new BasicTsKvEntry(System.currentTimeMillis(), new DoubleDataEntry("temperature", 44.0));

        lenient().when(relationService.findByFromAndTypeAsync(tenantId, assetId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON))
                .thenReturn(immediateFuture(List.of(relationA, relationB)));

        lenient().when(timeseriesService.findLatest(tenantId, deviceA, Set.of("temperature"))).thenReturn(immediateFuture(List.of(deviceATemperature)));
        lenient().when(timeseriesService.findLatest(tenantId, deviceB, Set.of("temperature"))).thenReturn(immediateFuture(List.of(deviceBTemperature)));
    }

    void checkMsg(TbMsg msg, boolean checkSum) {
        Assertions.assertNotNull(msg);
        Assertions.assertNotNull(msg.getData());
        ObjectNode objectNode = (ObjectNode) JacksonUtil.toJsonNode(msg.getData());
        Assertions.assertEquals(43.0, objectNode.get("latestAvgTemperature").asDouble(), 0.0);
        Assertions.assertEquals(2, objectNode.get("deviceCount").asInt());

        if (checkSum) {
            //check filtered
            Assertions.assertTrue(objectNode.has("sumTemperature"));
            Assertions.assertEquals(44, objectNode.get("sumTemperature").asInt());
        }
    }

    @Test
    void testSimpleAggregationWithoutFilter() throws TbNodeException {
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(getConfigNode())));

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(assetId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> captor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, Mockito.timeout(5000).times(1)).enqueueForTellNext(captor.capture(), eq(TbNodeConnectionType.SUCCESS));

        checkMsg(captor.getValue(), false);
    }

    @Test
    void testSimpleAggregationWithFilter() throws TbNodeException {
        TbAggLatestTelemetryNodeV2Configuration config = getConfigNode();
        List<AggLatestMapping> aggMappings = config.getAggMappings();

        AggLatestMapping sumMapping = new AggLatestMapping();
        sumMapping.setSource("temperature");
        sumMapping.setSourceScope("LATEST_TELEMETRY");
        sumMapping.setAggFunction(MathFunction.SUM);
        sumMapping.setDefaultValue(0);
        sumMapping.setTarget("sumTemperature");

        AggLatestMappingFilter filter = new AggLatestMappingFilter();
        filter.setLatestTsKeyNames(Collections.singletonList("temperature"));
        filter.setScriptLang(ScriptLanguage.TBEL);
        filter.setTbelFilterFunction(FILTER_FUNCTION);
        sumMapping.setFilter(filter);

        aggMappings.add(sumMapping);
        config.setAggMappings(aggMappings);

        //Mock for filter
        when(peCtx.createAttributesScriptEngine(ScriptLanguage.TBEL, FILTER_FUNCTION)).thenReturn(scriptEngine);
        when(scriptEngine.executeAttributesFilterAsync(anyMap())).then(
                (Answer<ListenableFuture<Boolean>>) invocation -> {
                    Map<String, BasicTsKvEntry> attributes = (Map<String, BasicTsKvEntry>) (invocation.getArguments())[0];
                    if (attributes.containsKey("temperature")) {
                        try {
                            double temperature = attributes.get("temperature").getDoubleValue().get();
                            return immediateFuture(temperature > 42);
                        } catch (NumberFormatException e) {
                            return immediateFuture(false);
                        }
                    }
                    return immediateFuture(false);
                }
        );

        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(assetId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> captor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, Mockito.timeout(5000).times(1)).enqueueForTellNext(captor.capture(), eq(TbNodeConnectionType.SUCCESS));

        checkMsg(captor.getValue(), true);
    }

    @Test
    void testSimpleAggregationWithDeduplication() throws TbNodeException {
        int countMsg = 5;
        long deduplicationInSec = 60;
        TbAggLatestTelemetryNodeV2Configuration config = getConfigNode();
        config.setDeduplicationInSec(deduplicationInSec); //delay 60 sec
        when(ctx.newMsg(any(), eq(TbMsgType.TB_AGG_LATEST_CLEAR_INACTIVE_ENTITIES_SELF_MSG), any(), any(), any(), any()))
                .thenReturn(TbMsg.newMsg()
                        .type(TbMsgType.TB_AGG_LATEST_CLEAR_INACTIVE_ENTITIES_SELF_MSG)
                        .originator(null)
                        .copyMetaData(TbMsgMetaData.EMPTY)
                        .data(null)
                        .build());
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        //Mock for tellSelf
        doAnswer((Answer<TbMsg>) invocationOnMock -> {
            TbMsgType type = (TbMsgType) (invocationOnMock.getArguments())[1];
            EntityId originator = (EntityId) (invocationOnMock.getArguments())[2];
            TbMsgMetaData metaData = (TbMsgMetaData) (invocationOnMock.getArguments())[4];
            String data = (String) (invocationOnMock.getArguments())[5];
            return TbMsg.newMsg()
                    .type(type)
                    .originator(originator)
                    .copyMetaData(metaData)
                    .data(data)
                    .build();
        }).when(ctx).newMsg(isNull(), eq(TbMsgType.TB_AGG_LATEST_SELF_MSG), nullable(EntityId.class),
                any(), any(TbMsgMetaData.class), anyString());

        doAnswer((Answer<Void>) invocation -> {
            TbMsg msg = (TbMsg) (invocation.getArguments())[0];
            node.onMsg(ctx, msg);
            return null;
        }).when(ctx).tellSelf(any(TbMsg.class), anyLong());

        //create Msg
        for (int count = 0; count < countMsg; count++) {
            TbMsg msg = TbMsg.newMsg()
                    .type(TbMsgType.POST_TELEMETRY_REQUEST)
                    .originator(assetId)
                    .copyMetaData(TbMsgMetaData.EMPTY)
                    .data(TbMsg.EMPTY_JSON_OBJECT)
                    .build();
            node.onMsg(ctx, msg);
        }

        ArgumentCaptor<TbMsg> captor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<TbMsg> captorForDelayedMsg = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, Mockito.timeout(5000).times(countMsg)).enqueueForTellNext(captor.capture(), eq(TbNodeConnectionType.SUCCESS));
        // four TB_AGG_LATEST_NODE_MSG tbMsgs during node.onMsg() and one TB_CLEAR_LAST_MSG_MAP_NODE_MSG tbMsg during node.init()
        verify(ctx, Mockito.timeout(5000).times(countMsg)).tellSelf(captorForDelayedMsg.capture(), anyLong());

        List<TbMsg> resultMsg = captor.getAllValues();
        List<TbMsg> delayedMsg = captorForDelayedMsg.getAllValues()
                .stream()
                .filter(tbMsg -> tbMsg.isTypeOf(TbMsgType.TB_AGG_LATEST_SELF_MSG))
                .collect(Collectors.toList());

        Assertions.assertNotNull(resultMsg);
        Assertions.assertEquals(countMsg, resultMsg.size());

        Assertions.assertNotNull(delayedMsg);
        Assertions.assertEquals(countMsg - 1, delayedMsg.size());

        resultMsg.forEach(tbMsg -> checkMsg(tbMsg, false));

        //check delayed Msg
        delayedMsg.forEach(tbMsg -> {
            Assertions.assertNotNull(tbMsg);
            Assertions.assertTrue(tbMsg.isTypeOf(TbMsgType.TB_AGG_LATEST_SELF_MSG));
        });
    }

    @Test
    void testAggregationWithNoTargetTelemetry() throws Exception {
        BasicTsKvEntry emptyKvEntry = new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry("temperature", null));
        when(timeseriesService.findLatest(eq(tenantId), any(), eq(Set.of("temperature"))))
                .thenReturn(immediateFuture(List.of(emptyKvEntry)));

        TbNodeConfiguration config = new TbNodeConfiguration(JacksonUtil.valueToTree(getConfigNode()));
        node.init(ctx, config);

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(assetId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> out = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, Mockito.timeout(5000)).enqueueForTellNext(out.capture(), eq(TbNodeConnectionType.SUCCESS));

        JsonNode data = JacksonUtil.toJsonNode(out.getValue().getData());
        assertThat(data.get("latestAvgTemperature").asDouble()).isEqualTo(0.0);
    }

    @Test
    void testAggregationWithNoTargetAttributes() throws Exception {
        TbAggLatestTelemetryNodeV2Configuration config = new TbAggLatestTelemetryNodeV2Configuration();
        AggLatestMapping avgAttrMapping = new AggLatestMapping();
        avgAttrMapping.setSource("attr");
        avgAttrMapping.setSourceScope(DataConstants.SERVER_SCOPE);
        avgAttrMapping.setAggFunction(MathFunction.AVG);
        avgAttrMapping.setDefaultValue(0);
        avgAttrMapping.setTarget("avgAttr");
        config.setAggMappings(List.of(avgAttrMapping));
        config.setDirection(EntitySearchDirection.FROM);
        config.setRelationType(EntityRelation.CONTAINS_TYPE);
        config.setOutMsgType(TbMsgType.POST_TELEMETRY_REQUEST.name());

        when(attributesService.find(eq(tenantId), any(), eq(AttributeScope.SERVER_SCOPE), anyCollection()))
                .thenReturn(immediateFuture(Collections.emptyList()));

        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(assetId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> out = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, Mockito.timeout(5000)).enqueueForTellNext(out.capture(), eq(TbNodeConnectionType.SUCCESS));

        JsonNode data = JacksonUtil.toJsonNode(out.getValue().getData());
        assertThat(data.get("avgAttr").asDouble()).isEqualTo(0.0);
    }

    @Test
    void testCountAggregation_resultIsZeroWhenAllChildrenFilteredOut() throws Exception {
        // GIVEN
        var config = new TbAggLatestTelemetryNodeV2Configuration();

        config.setDirection(EntitySearchDirection.FROM);
        config.setRelationType(EntityRelation.CONTAINS_TYPE);
        config.setDeduplicationInSec(Duration.ofMinutes(10L).toSeconds());
        config.setOutMsgType(TbMsgType.POST_TELEMETRY_REQUEST.name());

        List<AggLatestMapping> mappings = new ArrayList<>();

        var filter = new AggLatestMappingFilter();
        filter.setClientAttributeNames(Collections.emptyList());
        filter.setSharedAttributeNames(Collections.emptyList());
        filter.setServerAttributeNames(List.of("occupied"));
        filter.setLatestTsKeyNames(Collections.emptyList());
        filter.setScriptLang(ScriptLanguage.TBEL);
        filter.setTbelFilterFunction("return attributes['occupied'] == 'false';");

        var countMapping = new AggLatestMapping();
        countMapping.setSource("occupied");
        countMapping.setSourceScope("SERVER_SCOPE");
        countMapping.setDefaultValue(0);
        countMapping.setAggFunction(MathFunction.COUNT);
        countMapping.setTarget("freeParkingLots");
        countMapping.setFilter(filter);
        mappings.add(countMapping);

        config.setAggMappings(mappings);

        var device = new Device(new DeviceId(UUID.randomUUID()));

        given(relationService.findByFromAndTypeAsync(tenantId, assetId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON)).willReturn(
                immediateFuture(List.of(new EntityRelation(assetId, device.getId(), EntityRelation.CONTAINS_TYPE)))
        );

        given(attributesService.find(tenantId, device.getId(), AttributeScope.SERVER_SCOPE, Set.of("occupied"))).willReturn(
                immediateFuture(List.of(new BaseAttributeKvEntry(123L, new BooleanDataEntry("occupied", true))))
        );

        given(peCtx.createAttributesScriptEngine(ScriptLanguage.TBEL, filter.getTbelFilterFunction())).willReturn(scriptEngine);
        given(scriptEngine.executeAttributesFilterAsync(anyMap())).willAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Map<String, KvEntry> keyValues = (Map<String, KvEntry>) (invocation.getArguments())[0];
            return immediateFuture(keyValues.get("occupied").getValueAsString().equals("false"));
        });

        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(assetId)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .metaData(TbMsgMetaData.EMPTY)
                .build();

        node.onMsg(ctx, msg);

        // THEN
        var out = ArgumentCaptor.forClass(TbMsg.class);

        then(ctx).should().enqueueForTellNext(out.capture(), eq(TbNodeConnectionType.SUCCESS));

        var actualData = (ObjectNode) JacksonUtil.toJsonNode(out.getValue().getData());
        assertThat(actualData.hasNonNull("freeParkingLots")).as("should have count result in the outgoing message").isTrue();
        assertThat(actualData.get("freeParkingLots").intValue()).isZero();
    }

    @Test
    void testCountUniqueAggregation_resultIsZeroWhenAllChildrenFilteredOut() throws Exception {
        // GIVEN
        var config = new TbAggLatestTelemetryNodeV2Configuration();

        config.setDirection(EntitySearchDirection.FROM);
        config.setRelationType(EntityRelation.CONTAINS_TYPE);
        config.setDeduplicationInSec(Duration.ofMinutes(10L).toSeconds());
        config.setOutMsgType(TbMsgType.POST_TELEMETRY_REQUEST.name());

        List<AggLatestMapping> mappings = new ArrayList<>();

        var filter = new AggLatestMappingFilter();
        filter.setClientAttributeNames(Collections.emptyList());
        filter.setSharedAttributeNames(Collections.emptyList());
        filter.setServerAttributeNames(List.of("occupied"));
        filter.setLatestTsKeyNames(Collections.emptyList());
        filter.setScriptLang(ScriptLanguage.TBEL);
        filter.setTbelFilterFunction("return attributes['occupied'] == 'true';");

        var countMapping = new AggLatestMapping();
        countMapping.setSource("plateNumber");
        countMapping.setSourceScope("SERVER_SCOPE");
        countMapping.setDefaultValue(0);
        countMapping.setAggFunction(MathFunction.COUNT_UNIQUE);
        countMapping.setTarget("uniqueVehiclesPresent");
        countMapping.setFilter(filter);
        mappings.add(countMapping);

        config.setAggMappings(mappings);

        var device1 = new Device(new DeviceId(UUID.randomUUID()));
        var device2 = new Device(new DeviceId(UUID.randomUUID()));

        given(relationService.findByFromAndTypeAsync(tenantId, assetId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON)).willReturn(
                immediateFuture(List.of(
                        new EntityRelation(assetId, device1.getId(), EntityRelation.CONTAINS_TYPE),
                        new EntityRelation(assetId, device2.getId(), EntityRelation.CONTAINS_TYPE))
                )
        );

        given(attributesService.find(tenantId, device1.getId(), AttributeScope.SERVER_SCOPE, Set.of("occupied", "plateNumber"))).willReturn(
                immediateFuture(List.of(
                        new BaseAttributeKvEntry(123L, new BooleanDataEntry("occupied", false)),
                        new BaseAttributeKvEntry(123L, new StringDataEntry("plateNumber", "ABC123"))
                ))
        );
        given(attributesService.find(tenantId, device2.getId(), AttributeScope.SERVER_SCOPE, Set.of("occupied", "plateNumber"))).willReturn(
                immediateFuture(List.of(
                        new BaseAttributeKvEntry(123L, new BooleanDataEntry("occupied", false)),
                        new BaseAttributeKvEntry(123L, new StringDataEntry("plateNumber", "ABC123"))
                ))
        );

        given(peCtx.createAttributesScriptEngine(ScriptLanguage.TBEL, filter.getTbelFilterFunction())).willReturn(scriptEngine);
        given(scriptEngine.executeAttributesFilterAsync(anyMap())).willAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Map<String, KvEntry> keyValues = (Map<String, KvEntry>) (invocation.getArguments())[0];
            return immediateFuture(keyValues.get("occupied").getValueAsString().equals("true"));
        });

        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(assetId)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .metaData(TbMsgMetaData.EMPTY)
                .build();

        node.onMsg(ctx, msg);

        // THEN
        var out = ArgumentCaptor.forClass(TbMsg.class);

        then(ctx).should().enqueueForTellNext(out.capture(), eq(TbNodeConnectionType.SUCCESS));

        var actualData = (ObjectNode) JacksonUtil.toJsonNode(out.getValue().getData());
        assertThat(actualData.hasNonNull("uniqueVehiclesPresent")).as("should have count unique result in the outgoing message").isTrue();
        assertThat(actualData.get("uniqueVehiclesPresent").intValue()).isZero();
    }

    // Rule nodes upgrade
    static final String EXPECTED_CONFIG = "{\"outMsgType\":null,\"direction\":\"FROM\",\"relationType\":null,\"deduplicationInSec\":10," +
            "\"aggMappings\":[{\"source\":\"temperature\",\"sourceScope\":\"LATEST_TELEMETRY\",\"defaultValue\":0.0," +
            "\"target\":\"latestAvgTemperature\",\"aggFunction\":\"AVG\",\"filter\":null}]}";

    static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                // default config for version 0
                Arguments.of(0,
                        "{\"outMsgType\":null,\"direction\":\"FROM\",\"relationType\":null,\"deduplicationInSec\":10," +
                                "\"aggMappings\":[{\"source\":\"temperature\",\"sourceScope\":\"LATEST_TELEMETRY\",\"defaultValue\":0.0," +
                                "\"target\":\"latestAvgTemperature\",\"aggFunction\":\"AVG\",\"filter\":null}], \"queueName\":null}",
                        true,
                        EXPECTED_CONFIG),
                // default config for version 0 with queueName
                Arguments.of(0,
                        "{\"outMsgType\":null,\"direction\":\"FROM\",\"relationType\":null,\"deduplicationInSec\":10," +
                                "\"aggMappings\":[{\"source\":\"temperature\",\"sourceScope\":\"LATEST_TELEMETRY\",\"defaultValue\":0.0," +
                                "\"target\":\"latestAvgTemperature\",\"aggFunction\":\"AVG\",\"filter\":null}], \"queueName\":\"Main\"}",
                        true,
                        EXPECTED_CONFIG),
                // default config for version 1 with upgrade from version 0
                Arguments.of(0, EXPECTED_CONFIG, false, EXPECTED_CONFIG)
        );
    }

    @Override
    protected TbNode getTestNode() {
        return node;
    }

}
