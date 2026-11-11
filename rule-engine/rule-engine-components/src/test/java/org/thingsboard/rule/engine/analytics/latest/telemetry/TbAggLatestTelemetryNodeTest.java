// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.latest.telemetry;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.internal.verification.Times;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubber;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.analytics.incoming.MathFunction;
import org.thingsboard.rule.engine.analytics.latest.ParentEntitiesGroup;
import org.thingsboard.rule.engine.analytics.latest.ParentEntitiesQuery;
import org.thingsboard.rule.engine.analytics.latest.ParentEntitiesRelationsQuery;
import org.thingsboard.rule.engine.analytics.latest.ParentEntitiesSingleEntity;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TbPeContext;
import org.thingsboard.rule.engine.data.RelationsQuery;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.google.common.util.concurrent.Futures.immediateFailedFuture;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TbAggLatestTelemetryNodeTest extends AbstractRuleNodeUpgradeTest {

    private final Gson gson = new Gson();

    @Mock
    private TbContext ctx;
    @Mock
    private TbPeContext peCtx;
    @Mock
    private ListeningExecutor executor;
    @Mock
    private RelationService relationService;
    @Mock
    private TimeseriesService timeseriesService;
    @Mock
    private ScriptEngine scriptEngine;

    private TbAggLatestTelemetryNode node;
    private TbNodeConfiguration nodeConfiguration;

    private RelationsQuery relationsQuery;
    private EntityId rootEntityId;

    private Map<EntityId, Double> expectedAvgTempMap;
    private Map<EntityId, Integer> expectedDeviceCountMap;

    private int scheduleCount = 0;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void init() {
        TbAggLatestTelemetryNodeConfiguration config = new TbAggLatestTelemetryNodeConfiguration();
        node = spy(new TbAggLatestTelemetryNode());

        lenient().doAnswer((Answer<TbMsg>) invocationOnMock -> {
            TbMsgType type = (TbMsgType) (invocationOnMock.getArguments())[1];
            EntityId originator = (EntityId) (invocationOnMock.getArguments())[2];
            TbMsgMetaData metaData = (TbMsgMetaData) (invocationOnMock.getArguments())[3];
            String data = (String) (invocationOnMock.getArguments())[4];
            return TbMsg.newMsg()
                    .type(type)
                    .originator(originator)
                    .copyMetaData(metaData)
                    .data(data)
                    .build();
        }).when(ctx).newMsg(isNull(), any(TbMsgType.class), nullable(EntityId.class),
                any(TbMsgMetaData.class), any(String.class));

        scheduleCount = 0;

        lenient().doAnswer((Answer<Void>) invocationOnMock -> {
            scheduleCount++;
            if (scheduleCount == 1) {
                TbMsg msg = (TbMsg) (invocationOnMock.getArguments())[0];
                node.onMsg(ctx, msg);
            }
            return null;
        }).when(ctx).tellSelf(any(TbMsg.class), anyLong());

        lenient().when(ctx.getPeContext()).thenReturn(peCtx);

        lenient().when(ctx.isLocalEntity(any(EntityId.class))).thenReturn(true);
        lenient().when(peCtx.isLocalEntity(any(EntityId.class))).thenReturn(true);

        lenient().when(ctx.getDbCallbackExecutor()).thenReturn(executor);

        Stubber executorAnswer = lenient().doAnswer(invocationOnMock -> {
            try {
                Object arg = (invocationOnMock.getArguments())[0];
                Object result = null;
                if (arg instanceof Callable) {
                    Callable task = (Callable) arg;
                    result = task.call();
                } else if (arg instanceof Runnable) {
                    Runnable task = (Runnable) arg;
                    task.run();
                }
                return immediateFuture(result);
            } catch (Throwable th) {
                return immediateFailedFuture(th);
            }
        });

        executorAnswer.when(executor).execute(any(Runnable.class));

        lenient().when(ctx.getRelationService()).thenReturn(relationService);
        lenient().when(ctx.getTimeseriesService()).thenReturn(timeseriesService);

        String attributesFilterScript = "return Number(attributes['temperature']) > 21;";

        lenient().when(peCtx.createAttributesScriptEngine(ScriptLanguage.JS, attributesFilterScript)).thenReturn(scriptEngine);

        lenient().when(scriptEngine.executeAttributesFilterAsync(anyMap())).then(
                (Answer<ListenableFuture<Boolean>>) invocation -> {
                    Map<String, KvEntry> attributes = (Map<String, KvEntry>) (invocation.getArguments())[0];
                    if (attributes.containsKey("temperature")) {
                        String temperature = attributes.get("temperature").getValueAsString();
                        try {
                            return immediateFuture(Double.parseDouble(temperature) > 21);
                        } catch (NumberFormatException e) {
                            return immediateFuture(false);
                        }
                    }
                    return immediateFuture(false);
                }
        );

        relationsQuery = new RelationsQuery();
        relationsQuery.setDirection(EntitySearchDirection.FROM);
        relationsQuery.setMaxLevel(1);
        RelationEntityTypeFilter entityTypeFilter = new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.emptyList());
        relationsQuery.setFilters(Collections.singletonList(entityTypeFilter));

        rootEntityId = new TenantId(Uuids.timeBased());

        ParentEntitiesRelationsQuery parentEntitiesRelationsQuery = new ParentEntitiesRelationsQuery();
        parentEntitiesRelationsQuery.setRootEntityId(rootEntityId);

        parentEntitiesRelationsQuery.setRelationsQuery(relationsQuery);
        parentEntitiesRelationsQuery.setChildRelationsQuery(relationsQuery);

        config.setParentEntitiesQuery(parentEntitiesRelationsQuery);

        List<AggLatestMapping> aggMappings = new ArrayList<>();

        AggLatestMapping avgTempMapping = new AggLatestMapping();
        avgTempMapping.setSource("temperature");
        avgTempMapping.setSourceScope("LATEST_TELEMETRY");
        avgTempMapping.setAggFunction(MathFunction.AVG);
        avgTempMapping.setDefaultValue(0);
        avgTempMapping.setTarget("latestAvgTemperature");
        aggMappings.add(avgTempMapping);

        AggLatestMapping countMapping = new AggLatestMapping();
        countMapping.setAggFunction(MathFunction.COUNT);
        countMapping.setTarget("deviceCount");

        AggLatestMappingFilter filter = new AggLatestMappingFilter();
        filter.setLatestTsKeyNames(Collections.singletonList("temperature"));
        filter.setScriptLang(ScriptLanguage.JS);
        filter.setFilterFunction("return Number(attributes['temperature']) > 21;");

        countMapping.setFilter(filter);

        aggMappings.add(countMapping);

        config.setAggMappings(aggMappings);

        config.setPeriodTimeUnit(TimeUnit.MILLISECONDS);
        config.setPeriodValue(0);
        config.setOutMsgType(TbMsgType.POST_TELEMETRY_REQUEST.name());

        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        expectedAvgTempMap = new HashMap<>();
        expectedDeviceCountMap = new HashMap<>();
    }

    @Test
    public void parentEntitiesByRelationQueryAttributesAggregated() throws TbNodeException {
        List<EntityRelation> parentEntityRelations = new ArrayList<>();

        int parentCount = 10 + (int) (Math.random() * 20);

        for (int i = 0; i < parentCount; i++) {
            EntityId parentEntityId = new AssetId(Uuids.timeBased());
            parentEntityRelations.add(createEntityRelation(rootEntityId, parentEntityId));

            List<EntityRelation> childRelations = new ArrayList<>();
            int childCount = 10 + (int) (Math.random() * 20);

            BigDecimal sum = BigDecimal.ZERO;

            int expectedDeviceCount = 0;

            for (int c = 0; c < childCount; c++) {
                EntityId childEntityId = new DeviceId(Uuids.timeBased());
                childRelations.add(createEntityRelation(parentEntityId, childEntityId));

                TsKvEntry kvEntry = null;
                if (Math.random() > 0.5) {
                    double temperature = 17 + Math.random() * 10;
                    sum = sum.add(BigDecimal.valueOf(temperature));
                    kvEntry = new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry("temperature", "" + temperature));
                    if (temperature > 21) {
                        expectedDeviceCount++;
                    }
                }
                when(timeseriesService.findLatest(any(), eq(childEntityId), eq(Collections.singletonList("temperature")))).thenReturn(
                        immediateFuture(kvEntry != null ? Collections.singletonList(kvEntry) : Collections.emptyList())
                );

                Map<String, String> attributes = new HashMap<>();
                if (kvEntry != null) {
                    attributes.put("temperature", kvEntry.getValueAsString());
                }

            }

            expectedDeviceCountMap.put(parentEntityId, expectedDeviceCount);

            expectedAvgTempMap.put(parentEntityId,
                    sum.divide(BigDecimal.valueOf(childCount), 2, RoundingMode.HALF_UP).doubleValue());

            when(relationService.findByQuery(any(), eq(buildQuery(parentEntityId, relationsQuery)))).thenReturn(immediateFuture(childRelations));
        }

        when(relationService.findByQuery(any(), eq(buildQuery(rootEntityId, relationsQuery)))).thenReturn(immediateFuture(parentEntityRelations));

        node.init(ctx, nodeConfiguration);

        ArgumentCaptor<TbMsg> captor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, new Times(parentCount * 2)).enqueueForTellNext(captor.capture(), eq(TbNodeConnectionType.SUCCESS));

        List<TbMsg> messages = captor.getAllValues();
        for (TbMsg msg : messages) {
            verifyMessage(msg);
        }
    }

    @Test
    public void someFailedOtherAggregated() throws TbNodeException {
        List<EntityRelation> parentEntityRelations = new ArrayList<>();

        int parentCount = 10 + (int) (Math.random() * 20);

        int successAvgTempCount = 0;

        Map<EntityId, String> invalidValueMap = new HashMap<>();

        for (int i = 0; i < parentCount; i++) {
            EntityId parentEntityId = new AssetId(Uuids.timeBased());
            parentEntityRelations.add(createEntityRelation(rootEntityId, parentEntityId));

            List<EntityRelation> childRelations = new ArrayList<>();
            int childCount = 10 + (int) (Math.random() * 20);

            BigDecimal sum = BigDecimal.ZERO;

            int expectedDeviceCount = 0;

            int failedChildIndex = -1;
            boolean shouldFail = Math.random() > 0.5;
            if (!shouldFail) {
                successAvgTempCount++;
            } else {
                failedChildIndex = (int) Math.floor(Math.random() * childCount);
            }

            for (int c = 0; c < childCount; c++) {
                EntityId childEntityId = new DeviceId(Uuids.timeBased());
                childRelations.add(createEntityRelation(parentEntityId, childEntityId));
                double temperature = 17 + Math.random() * 10;

                sum = sum.add(BigDecimal.valueOf(temperature));

                boolean setInvalidTemperature = failedChildIndex == c;

                String temperatureString = (setInvalidTemperature ? "invalid" : "") + temperature;
                if (setInvalidTemperature) {
                    invalidValueMap.put(parentEntityId, temperatureString);
                } else if (temperature > 21) {
                    expectedDeviceCount++;
                }

                TsKvEntry kvEntry = new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry("temperature", temperatureString));
                when(timeseriesService.findLatest(any(), eq(childEntityId), eq(Collections.singletonList("temperature")))).thenReturn(
                        immediateFuture(Collections.singletonList(kvEntry))
                );

            }

            expectedDeviceCountMap.put(parentEntityId, expectedDeviceCount);

            expectedAvgTempMap.put(parentEntityId,
                    sum.divide(BigDecimal.valueOf(childCount), 2, RoundingMode.HALF_UP).doubleValue());

            when(relationService.findByQuery(any(), eq(buildQuery(parentEntityId, relationsQuery)))).thenReturn(immediateFuture(childRelations));
        }

        when(relationService.findByQuery(any(), eq(buildQuery(rootEntityId, relationsQuery)))).thenReturn(immediateFuture(parentEntityRelations));

        node.init(ctx, nodeConfiguration);

        int successMsgCount = parentCount + successAvgTempCount;

        ArgumentCaptor<TbMsg> captor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, new Times(successMsgCount)).enqueueForTellNext(captor.capture(), eq(TbNodeConnectionType.SUCCESS));

        List<TbMsg> messages = captor.getAllValues();
        for (TbMsg msg : messages) {
            verifyMessage(msg);
        }

        int failedMsgCount = parentCount - successAvgTempCount;

        if (failedMsgCount > 0) {
            ArgumentCaptor<TbMsg> failureMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
            ArgumentCaptor<String> throwableCaptor = ArgumentCaptor.forClass(String.class);

            verify(ctx, new Times(failedMsgCount)).enqueueForTellFailure(failureMsgCaptor.capture(), throwableCaptor.capture());

            List<TbMsg> failedMessages = failureMsgCaptor.getAllValues();
            List<String> throwables = throwableCaptor.getAllValues();
            for (int i = 0; i < failedMessages.size(); i++) {
                TbMsg failedMsg = failedMessages.get(i);
                String t = throwables.get(i);
                Assertions.assertTrue(t.startsWith("Aggregation failed. Unable to parse value"));
                String invalidValue = invalidValueMap.get(failedMsg.getOriginator());
                Assertions.assertNotNull(invalidValue);
                Assertions.assertTrue(t.contains(invalidValue));
            }
        }
    }

    @ParameterizedTest
    @MethodSource
    public void shouldNotStartWhenParentEntitiesQueryRootIsNotLocalEntity(EntityId queryRoot, ParentEntitiesQuery query) throws Exception {
        // GIVEN
        var node = new TbAggLatestTelemetryNode();

        var config = new TbAggLatestTelemetryNodeConfiguration().defaultConfiguration();
        config.setParentEntitiesQuery(query);

        given(ctx.isLocalEntity(queryRoot)).willReturn(false);

        // WHEN
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // THEN
        // verify a tick message was not scheduled
        then(ctx).should(never()).tellSelf(any(), anyLong());
    }

    private static Stream<Arguments> shouldNotStartWhenParentEntitiesQueryRootIsNotLocalEntity() {
        var relationsQuery = new RelationsQuery();
        relationsQuery.setDirection(EntitySearchDirection.FROM);
        relationsQuery.setFilters(List.of(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.emptyList())));

        var singleEntity = new ParentEntitiesSingleEntity();
        singleEntity.setEntityId(new AssetId(Uuids.timeBased()));
        singleEntity.setChildRelationsQuery(relationsQuery);

        var entitiesGroup = new ParentEntitiesGroup();
        entitiesGroup.setEntityGroupId(new EntityGroupId(Uuids.timeBased()));

        var parentEntitiesRelationsQuery = new ParentEntitiesRelationsQuery();
        parentEntitiesRelationsQuery.setRootEntityId(new AssetId(Uuids.timeBased()));
        parentEntitiesRelationsQuery.setRelationsQuery(relationsQuery);
        parentEntitiesRelationsQuery.setChildRelationsQuery(relationsQuery);
        parentEntitiesRelationsQuery.setIncludeRootEntity(true);

        return Stream.of(
                Arguments.of(singleEntity.getEntityId(), singleEntity),
                Arguments.of(entitiesGroup.getEntityGroupId(), entitiesGroup),
                Arguments.of(parentEntitiesRelationsQuery.getRootEntityId(), parentEntitiesRelationsQuery)
        );
    }

    // Rule nodes upgrade
    public static final String EXPECTED_CONFIG = "{\"parentEntitiesQuery\":{\"type\":\"group\",\"entityGroupId\":null}," +
            "\"periodTimeUnit\":\"MINUTES\",\"periodValue\":5," +
            "\"aggMappings\":[{\"source\":\"temperature\",\"sourceScope\":\"LATEST_TELEMETRY\"," +
            "\"defaultValue\":0.0,\"target\":\"latestAvgTemperature\",\"aggFunction\":\"AVG\",\"filter\":null}],\"outMsgType\":\"POST_TELEMETRY_REQUEST\"}";

    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                // default config for version 0
                Arguments.of(0,
                        "{\"parentEntitiesQuery\":{\"type\":\"group\",\"entityGroupId\":null}," +
                                "\"periodTimeUnit\":\"MINUTES\",\"periodValue\":5," +
                                "\"aggMappings\":[{\"source\":\"temperature\",\"sourceScope\":\"LATEST_TELEMETRY\"," +
                                "\"defaultValue\":0.0,\"target\":\"latestAvgTemperature\",\"aggFunction\":\"AVG\",\"filter\":null}],\"queueName\":null}",
                        true,
                        EXPECTED_CONFIG),
                // default config for version 0 with queueName
                Arguments.of(0,
                        "{\"parentEntitiesQuery\":{\"type\":\"group\",\"entityGroupId\":null}," +
                                "\"periodTimeUnit\":\"MINUTES\",\"periodValue\":5," +
                                "\"aggMappings\":[{\"source\":\"temperature\",\"sourceScope\":\"LATEST_TELEMETRY\"," +
                                "\"defaultValue\":0.0,\"target\":\"latestAvgTemperature\",\"aggFunction\":\"AVG\",\"filter\":null}],\"queueName\":\"Main\"}",
                        true,
                        EXPECTED_CONFIG),
                // default config for version 1 with upgrade from version 1
                Arguments.of(1,
                        "{\"parentEntitiesQuery\":{\"type\":\"group\",\"entityGroupId\":null}," +
                                "\"periodTimeUnit\":\"MINUTES\",\"periodValue\":5," +
                                "\"aggMappings\":[{\"source\":\"temperature\",\"sourceScope\":\"LATEST_TELEMETRY\"," +
                                "\"defaultValue\":0.0,\"target\":\"latestAvgTemperature\",\"aggFunction\":\"AVG\",\"filter\":null}],\"queueName\":\"Main\",\"outMsgType\":\"POST_TELEMETRY_REQUEST\"}",
                        true,
                        EXPECTED_CONFIG),
                // default config for version 2 with upgrade from version 0
                Arguments.of(0, EXPECTED_CONFIG, false, EXPECTED_CONFIG)
        );
    }

    private void verifyMessage(TbMsg msg) {
        Assertions.assertTrue(msg.isTypeOf(TbMsgType.POST_TELEMETRY_REQUEST));
        EntityId entityId = msg.getOriginator();
        Assertions.assertNotNull(entityId);
        String data = msg.getData();
        Assertions.assertNotNull(data);
        JsonObject dataJson = gson.fromJson(data, JsonObject.class);

        Assertions.assertTrue(dataJson.has("latestAvgTemperature") || dataJson.has("deviceCount"));
        if (dataJson.has("latestAvgTemperature")) {
            JsonElement elem = dataJson.get("latestAvgTemperature");
            Assertions.assertTrue(elem.isJsonPrimitive());
            double doubleVal = elem.getAsDouble();
            Assertions.assertEquals(expectedAvgTempMap.get(entityId).doubleValue(), doubleVal, 0.0);
        }
        if (dataJson.has("deviceCount")) {
            JsonElement elem = dataJson.get("deviceCount");
            Assertions.assertTrue(elem.isJsonPrimitive());
            long longVal = elem.getAsLong();
            Assertions.assertEquals(expectedDeviceCountMap.get(entityId).longValue(), longVal);
        }
    }

    private static EntityRelation createEntityRelation(EntityId from, EntityId to) {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(from);
        relation.setTo(to);
        relation.setType(EntityRelation.CONTAINS_TYPE);
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        return relation;
    }

    private static EntityRelationsQuery buildQuery(EntityId originator, RelationsQuery relationsQuery) {
        EntityRelationsQuery query = new EntityRelationsQuery();
        RelationsSearchParameters parameters = new RelationsSearchParameters(originator,
                relationsQuery.getDirection(), relationsQuery.getMaxLevel(), false);
        query.setParameters(parameters);
        query.setFilters(relationsQuery.getFilters());
        return query;
    }

    @Override
    protected TbNode getTestNode() {
        return node;
    }

}
