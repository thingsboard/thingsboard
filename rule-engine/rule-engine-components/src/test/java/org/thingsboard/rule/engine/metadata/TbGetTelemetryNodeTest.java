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
package org.thingsboard.rule.engine.metadata;

import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvQuery;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.page.SortOrder.Direction;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.spy;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;

@ExtendWith(MockitoExtension.class)
public class TbGetTelemetryNodeTest extends AbstractRuleNodeUpgradeTest {

    private final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("5738401b-9dba-422b-b656-a62fe7431917"));
    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("8a8fd749-b2ec-488b-a6c6-fc66614d8686"));

    private final ListeningExecutor executor = new TestDbCallbackExecutor();

    private TbGetTelemetryNode node;
    private TbGetTelemetryNodeConfiguration config;

    @Mock
    private TbContext ctxMock;
    @Mock
    private TimeseriesService timeseriesServiceMock;

    @BeforeEach
    public void setUp() {
        node = spy(new TbGetTelemetryNode());
        config = new TbGetTelemetryNodeConfiguration().defaultConfiguration();
        config.setLatestTsKeyNames(List.of("temperature"));
    }

    @Test
    public void verifyDefaultConfig() {
        config = new TbGetTelemetryNodeConfiguration().defaultConfiguration();
        assertThat(config.getStartInterval()).isEqualTo(2);
        assertThat(config.getEndInterval()).isEqualTo(1);
        assertThat(config.getStartIntervalPattern()).isEqualTo("");
        assertThat(config.getEndIntervalPattern()).isEqualTo("");
        assertThat(config.isUseMetadataIntervalPatterns()).isFalse();
        assertThat(config.getStartIntervalTimeUnit()).isEqualTo(TimeUnit.MINUTES.name());
        assertThat(config.getEndIntervalTimeUnit()).isEqualTo(TimeUnit.MINUTES.name());
        assertThat(config.getFetchMode()).isEqualTo(FetchMode.FIRST);
        assertThat(config.getOrderBy()).isEqualTo(Direction.ASC);
        assertThat(config.getAggregation()).isEqualTo(Aggregation.NONE);
        assertThat(config.getLimit()).isEqualTo(1000);
        assertThat(config.getLatestTsKeyNames()).isEmpty();
    }

    @Test
    public void givenEmptyTsKeyNames_whenInit_thenThrowsException() {
        // GIVEN
        config.setLatestTsKeyNames(Collections.emptyList());

        // WHEN-THEN
        assertThatThrownBy(() -> node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("Telemetry should be specified!")
                .extracting(e -> ((TbNodeException) e).isUnrecoverable())
                .isEqualTo(true);
    }

    @Test
    public void givenFetchModeIsNull_whenInit_thenThrowsException() {
        // GIVEN
        config.setFetchMode(null);

        // WHEN-THEN
        assertThatThrownBy(() -> node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("FetchMode should be specified!")
                .extracting(e -> ((TbNodeException) e).isUnrecoverable())
                .isEqualTo(true);
    }

    @Test
    public void givenFetchModeAllAndOrderByIsNull_whenInit_thenThrowsException() {
        // GIVEN
        config.setFetchMode(FetchMode.ALL);
        config.setOrderBy(null);

        // WHEN-THEN
        assertThatThrownBy(() -> node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("OrderBy should be specified!")
                .extracting(e -> ((TbNodeException) e).isUnrecoverable())
                .isEqualTo(true);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1, 1001, 2000})
    public void givenFetchModeAllAndLimitIsOutOfRange_whenInit_thenThrowsException(int limit) {
        // GIVEN
        config.setFetchMode(FetchMode.ALL);
        config.setLimit(limit);

        // WHEN-THEN
        assertThatThrownBy(() -> node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("Limit should be in a range from 2 to 1000.")
                .extracting(e -> ((TbNodeException) e).isUnrecoverable())
                .isEqualTo(true);
    }

    @Test
    public void givenFetchModeIsAllAndAggregationIsNull_whenInit_thenThrowsException() {
        // GIVEN
        config.setFetchMode(FetchMode.ALL);
        config.setAggregation(null);
        // WHEN-THEN
        assertThatThrownBy(() -> node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config))))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("Aggregation should be specified!")
                .extracting(e -> ((TbNodeException) e).isUnrecoverable())
                .isEqualTo(true);
    }

    @Test
    public void givenIntervalStartIsGreaterThanIntervalEnd_whenOnMsg_thenThrowsException() throws TbNodeException {
        // GIVEN
        config.setStartInterval(1);
        config.setEndInterval(2);

        // WHEN-THEN
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        assertThatThrownBy(() -> node.onMsg(ctxMock, msg))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Interval start should be less than Interval end");
    }

    @Test
    public void givenUseMetadataIntervalPatternsIsTrue_whenOnMsg_thenVerifyStartAndEndTsInQuery() throws TbNodeException {
        // GIVEN
        config.setUseMetadataIntervalPatterns(true);
        config.setStartIntervalPattern("${mdStartInterval}");
        config.setEndIntervalPattern("$[msgEndInterval]");

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        mockTimeseriesService();
        given(timeseriesServiceMock.findAll(any(TenantId.class), any(EntityId.class), anyList())).willReturn(Futures.immediateFuture(Collections.emptyList()));

        // WHEN
        long startTs = 1719220350000L;
        long endTs = 1719220353000L;
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("mdStartInterval", String.valueOf(startTs));
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(metaData)
                .data("{\"msgEndInterval\":\"" + endTs + "\"}")
                .build();
        node.onMsg(ctxMock, msg);

        // THEN
        ArgumentCaptor<List<ReadTsKvQuery>> actualReadTsKvQueryList = ArgumentCaptor.forClass(List.class);
        then(timeseriesServiceMock).should().findAll(eq(TENANT_ID), eq(DEVICE_ID), actualReadTsKvQueryList.capture());
        ReadTsKvQuery actualReadTsKvQuery = actualReadTsKvQueryList.getValue().get(0);
        assertThat(actualReadTsKvQuery.getStartTs()).isEqualTo(startTs);
        assertThat(actualReadTsKvQuery.getEndTs()).isEqualTo(endTs);
    }

    @Test
    public void givenUseMetadataIntervalPatternsIsFalse_whenOnMsg_thenVerifyStartAndEndTsInQuery() throws TbNodeException {
        // GIVEN
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        long ts = System.currentTimeMillis();
        willReturn(ts).given(node).getCurrentTimeMillis();
        mockTimeseriesService();
        given(timeseriesServiceMock.findAll(any(TenantId.class), any(EntityId.class), anyList())).willReturn(Futures.immediateFuture(Collections.emptyList()));

        // WHEN
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        // THEN
        ArgumentCaptor<List<ReadTsKvQuery>> actualReadTsKvQueryList = ArgumentCaptor.forClass(List.class);
        then(timeseriesServiceMock).should().findAll(eq(TENANT_ID), eq(DEVICE_ID), actualReadTsKvQueryList.capture());
        ReadTsKvQuery actualReadTsKvQuery = actualReadTsKvQueryList.getValue().get(0);
        assertThat(actualReadTsKvQuery.getStartTs()).isEqualTo(ts - TimeUnit.MINUTES.toMillis(config.getStartInterval()));
        assertThat(actualReadTsKvQuery.getEndTs()).isEqualTo(ts - TimeUnit.MINUTES.toMillis(config.getEndInterval()));
    }

    @Test
    public void givenTsKeyNamesPatterns_whenOnMsg_thenVerifyTsKeyNamesInQuery() throws TbNodeException {
        // GIVEN
        config.setLatestTsKeyNames(List.of("temperature", "${mdTsKey}", "$[msgTsKey]"));

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        mockTimeseriesService();
        given(timeseriesServiceMock.findAll(any(TenantId.class), any(EntityId.class), anyList())).willReturn(Futures.immediateFuture(Collections.emptyList()));

        // WHEN
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("mdTsKey", "humidity");
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(metaData)
                .data("{\"msgTsKey\":\"pressure\"}")
                .build();
        node.onMsg(ctxMock, msg);

        // THEN
        ArgumentCaptor<List<ReadTsKvQuery>> actualReadTsKvQueryList = ArgumentCaptor.forClass(List.class);
        then(timeseriesServiceMock).should().findAll(eq(TENANT_ID), eq(DEVICE_ID), actualReadTsKvQueryList.capture());
        List<String> actualKeys = actualReadTsKvQueryList.getValue().stream().map(TsKvQuery::getKey).toList();
        assertThat(actualKeys).containsExactlyInAnyOrder("temperature", "humidity", "pressure");
    }

    @ParameterizedTest
    @MethodSource
    public void givenAggregation_whenOnMsg_thenVerifyAggregationStepInQuery(Aggregation aggregation, Consumer<ReadTsKvQuery> aggregationStepVerifier) throws TbNodeException {
        // GIVEN
        config.setStartInterval(5);
        config.setEndInterval(1);
        config.setFetchMode(FetchMode.ALL);
        config.setAggregation(aggregation);

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        mockTimeseriesService();
        given(timeseriesServiceMock.findAll(any(TenantId.class), any(EntityId.class), anyList())).willReturn(Futures.immediateFuture(Collections.emptyList()));

        // WHEN
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        // THEN
        ArgumentCaptor<List<ReadTsKvQuery>> actualReadTsKvQueryList = ArgumentCaptor.forClass(List.class);
        then(timeseriesServiceMock).should().findAll(eq(TENANT_ID), eq(DEVICE_ID), actualReadTsKvQueryList.capture());
        ReadTsKvQuery actualReadTsKvQuery = actualReadTsKvQueryList.getValue().get(0);
        aggregationStepVerifier.accept(actualReadTsKvQuery);
    }

    private static Stream<Arguments> givenAggregation_whenOnMsg_thenVerifyAggregationStepInQuery() {
        return Stream.of(
                Arguments.of(Aggregation.NONE, (Consumer<ReadTsKvQuery>) query -> assertThat(query.getInterval()).isEqualTo(1)),
                Arguments.of(Aggregation.AVG, (Consumer<ReadTsKvQuery>) query -> assertThat(query.getInterval()).isEqualTo(query.getEndTs() - query.getStartTs()))
        );
    }

    @ParameterizedTest
    @MethodSource
    public void givenFetchModeAndLimit_whenOnMsg_thenVerifyLimitInQuery(FetchMode fetchMode, int limit, Consumer<ReadTsKvQuery> limitInQueryVerifier) throws TbNodeException {
        // GIVEN
        config.setFetchMode(fetchMode);
        config.setLimit(limit);

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        mockTimeseriesService();
        given(timeseriesServiceMock.findAll(any(TenantId.class), any(EntityId.class), anyList())).willReturn(Futures.immediateFuture(Collections.emptyList()));

        // WHEN
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        // THEN
        ArgumentCaptor<List<ReadTsKvQuery>> actualReadTsKvQueryList = ArgumentCaptor.forClass(List.class);
        then(timeseriesServiceMock).should().findAll(eq(TENANT_ID), eq(DEVICE_ID), actualReadTsKvQueryList.capture());
        ReadTsKvQuery actualReadTsKvQuery = actualReadTsKvQueryList.getValue().get(0);
        limitInQueryVerifier.accept(actualReadTsKvQuery);
    }

    private static Stream<Arguments> givenFetchModeAndLimit_whenOnMsg_thenVerifyLimitInQuery() {
        return Stream.of(
                Arguments.of(
                        FetchMode.ALL,
                        5,
                        (Consumer<ReadTsKvQuery>) query -> assertThat(query.getLimit()).isEqualTo(5)),
                Arguments.of(
                        FetchMode.FIRST,
                        TbGetTelemetryNodeConfiguration.MAX_FETCH_SIZE,
                        (Consumer<ReadTsKvQuery>) query -> assertThat(query.getLimit()).isEqualTo(1)),
                Arguments.of(
                        FetchMode.LAST,
                        10,
                        (Consumer<ReadTsKvQuery>) query -> assertThat(query.getLimit()).isEqualTo(1))
        );
    }

    @ParameterizedTest
    @MethodSource
    public void givenFetchModeAndOrder_whenOnMsg_thenVerifyOrderInQuery(FetchMode fetchMode, Direction orderBy, Consumer<ReadTsKvQuery> orderInQueryVerifier) throws TbNodeException {
        // GIVEN
        config.setFetchMode(fetchMode);
        config.setOrderBy(orderBy);

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        mockTimeseriesService();
        given(timeseriesServiceMock.findAll(any(TenantId.class), any(EntityId.class), anyList())).willReturn(Futures.immediateFuture(Collections.emptyList()));

        // WHEN
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        // THEN
        ArgumentCaptor<List<ReadTsKvQuery>> actualReadTsKvQueryList = ArgumentCaptor.forClass(List.class);
        then(timeseriesServiceMock).should().findAll(eq(TENANT_ID), eq(DEVICE_ID), actualReadTsKvQueryList.capture());
        ReadTsKvQuery actualReadTsKvQuery = actualReadTsKvQueryList.getValue().get(0);
        orderInQueryVerifier.accept(actualReadTsKvQuery);
    }

    private static Stream<Arguments> givenFetchModeAndOrder_whenOnMsg_thenVerifyOrderInQuery() {
        return Stream.of(
                Arguments.of(
                        FetchMode.ALL,
                        Direction.DESC,
                        (Consumer<ReadTsKvQuery>) query -> assertThat(query.getOrder()).isEqualTo("DESC")),
                Arguments.of(
                        FetchMode.FIRST,
                        Direction.ASC,
                        (Consumer<ReadTsKvQuery>) query -> assertThat(query.getOrder()).isEqualTo("ASC")),
                Arguments.of(
                        FetchMode.LAST,
                        Direction.ASC,
                        (Consumer<ReadTsKvQuery>) query -> assertThat(query.getOrder()).isEqualTo("DESC"))
        );
    }

    @ParameterizedTest
    @MethodSource
    public void givenInvalidIntervalPatterns_whenOnMsg_thenThrowsException(String startIntervalPattern, String errorMsg) throws TbNodeException {
        // GIVEN
        config.setUseMetadataIntervalPatterns(true);
        config.setStartIntervalPattern(startIntervalPattern);
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN-THEN
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data("{\"msgStartInterval\":\"start\"}")
                .build();
        assertThatThrownBy(() -> node.onMsg(ctxMock, msg)).isInstanceOf(IllegalArgumentException.class).hasMessage(errorMsg);
    }

    private static Stream<Arguments> givenInvalidIntervalPatterns_whenOnMsg_thenThrowsException() {
        return Stream.of(
                Arguments.of("${mdStartInterval}", "Message value: 'mdStartInterval' is undefined"),
                Arguments.of("$[msgStartInterval]", "Message value: 'msgStartInterval' has invalid format")
        );
    }

    @Test
    public void givenFetchModeAll_whenOnMsg_thenTellSuccessAndVerifyMsg() throws TbNodeException {
        // GIVEN
        config.setLatestTsKeyNames(List.of("temperature", "humidity"));
        config.setFetchMode(FetchMode.ALL);

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        mockTimeseriesService();
        long ts = System.currentTimeMillis();
        List<TsKvEntry> tsKvEntries = List.of(
                new BasicTsKvEntry(ts - 5, new DoubleDataEntry("temperature", 23.1)),
                new BasicTsKvEntry(ts - 4, new DoubleDataEntry("temperature", 22.4)),
                new BasicTsKvEntry(ts - 4, new DoubleDataEntry("humidity", 55.5))
        );
        given(timeseriesServiceMock.findAll(any(TenantId.class), any(EntityId.class), anyList())).willReturn(Futures.immediateFuture(tsKvEntries));

        // WHEN
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        // THEN
        ArgumentCaptor<TbMsg> actualMsg = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().tellSuccess(actualMsg.capture());
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("temperature", "[{\"ts\":" + (ts - 5) + ",\"value\":23.1},{\"ts\":" + (ts - 4) + ",\"value\":22.4}]");
        metaData.putValue("humidity", "[{\"ts\":" + (ts - 4) + ",\"value\":55.5}]");
        TbMsg expectedMsg = msg.transform()
                .metaData(metaData)
                .build();
        assertThat(actualMsg.getValue()).usingRecursiveComparison().ignoringFields("ctx").isEqualTo(expectedMsg);
    }

    @ParameterizedTest
    @ValueSource(strings = {"FIRST", "LAST"})
    public void givenFetchMode_whenOnMsg_thenTellSuccessAndVerifyMsg(String fetchMode) throws TbNodeException {
        // GIVEN
        config.setFetchMode(FetchMode.valueOf(fetchMode));
        config.setLatestTsKeyNames(List.of("temperature", "humidity"));

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        mockTimeseriesService();
        long ts = System.currentTimeMillis();
        List<TsKvEntry> tsKvEntries = List.of(
                new BasicTsKvEntry(ts - 4, new DoubleDataEntry("temperature", 22.4)),
                new BasicTsKvEntry(ts - 4, new DoubleDataEntry("humidity", 55.5))
        );
        given(timeseriesServiceMock.findAll(any(TenantId.class), any(EntityId.class), anyList())).willReturn(Futures.immediateFuture(tsKvEntries));

        // WHEN
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        // THEN
        ArgumentCaptor<TbMsg> actualMsg = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().tellSuccess(actualMsg.capture());
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("temperature", "\"22.4\"");
        metaData.putValue("humidity", "\"55.5\"");
        TbMsg expectedMsg = msg.transform()
                .metaData(metaData)
                .build();
        assertThat(actualMsg.getValue()).usingRecursiveComparison().ignoringFields("ctx").isEqualTo(expectedMsg);
    }

    private void mockTimeseriesService() {
        given(ctxMock.getTimeseriesService()).willReturn(timeseriesServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);
        given(ctxMock.getDbCallbackExecutor()).willReturn(executor);
    }

    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                // config for version 0 (fetchMode is 'FIRST' and orderBy is 'INVALID_ORDER_BY' and aggregation is 'SUM')
                Arguments.of(0,
                        """
                                        {
                                                  "latestTsKeyNames": [],
                                                  "aggregation": "SUM",
                                                  "fetchMode": "FIRST",
                                                  "orderBy": "INVALID_ORDER_BY",
                                                  "limit": 1000,
                                                  "useMetadataIntervalPatterns": false,
                                                  "startIntervalPattern": "",
                                                  "endIntervalPattern": "",
                                                  "startInterval": 2,
                                                  "startIntervalTimeUnit": "MINUTES",
                                                  "endInterval": 1,
                                                  "endIntervalTimeUnit": "MINUTES"
                                        }
                                """,
                        true,
                        """
                                        {
                                                  "latestTsKeyNames": [],
                                                  "aggregation": "NONE",
                                                  "fetchMode": "FIRST",
                                                  "orderBy": "ASC",
                                                  "limit": 1000,
                                                  "useMetadataIntervalPatterns": false,
                                                  "startIntervalPattern": "",
                                                  "endIntervalPattern": "",
                                                  "startInterval": 2,
                                                  "startIntervalTimeUnit": "MINUTES",
                                                  "endInterval": 1,
                                                  "endIntervalTimeUnit": "MINUTES"
                                        }
                                """),
                // config for version 0 (fetchMode is 'LAST' and orderBy is 'ASC' and aggregation is 'AVG')
                Arguments.of(0,
                        """
                                        {
                                                  "latestTsKeyNames": [],
                                                  "aggregation": "AVG",
                                                  "fetchMode": "LAST",
                                                  "orderBy": "ASC",
                                                  "limit": 1000,
                                                  "useMetadataIntervalPatterns": false,
                                                  "startIntervalPattern": "",
                                                  "endIntervalPattern": "",
                                                  "startInterval": 2,
                                                  "startIntervalTimeUnit": "MINUTES",
                                                  "endInterval": 1,
                                                  "endIntervalTimeUnit": "MINUTES"
                                        }
                                """,
                        true,
                        """
                                        {
                                                  "latestTsKeyNames": [],
                                                  "aggregation": "NONE",
                                                  "fetchMode": "LAST",
                                                  "orderBy": "DESC",
                                                  "limit": 1000,
                                                  "useMetadataIntervalPatterns": false,
                                                  "startIntervalPattern": "",
                                                  "endIntervalPattern": "",
                                                  "startInterval": 2,
                                                  "startIntervalTimeUnit": "MINUTES",
                                                  "endInterval": 1,
                                                  "endIntervalTimeUnit": "MINUTES"
                                        }
                                """),
                // config for version 0 (fetchMode is 'ALL' and orderBy is empty and aggregation is null)
                Arguments.of(0,
                        """
                                        {
                                                  "latestTsKeyNames": [],
                                                  "aggregation": null,
                                                  "fetchMode": "ALL",
                                                  "orderBy": "",
                                                  "limit": 1000,
                                                  "useMetadataIntervalPatterns": false,
                                                  "startIntervalPattern": "",
                                                  "endIntervalPattern": "",
                                                  "startInterval": 2,
                                                  "startIntervalTimeUnit": "MINUTES",
                                                  "endInterval": 1,
                                                  "endIntervalTimeUnit": "MINUTES"
                                        }
                                """,
                        true,
                        """
                                        {
                                                  "latestTsKeyNames": [],
                                                  "aggregation": "NONE",
                                                  "fetchMode": "ALL",
                                                  "orderBy": "ASC",
                                                  "limit": 1000,
                                                  "useMetadataIntervalPatterns": false,
                                                  "startIntervalPattern": "",
                                                  "endIntervalPattern": "",
                                                  "startInterval": 2,
                                                  "startIntervalTimeUnit": "MINUTES",
                                                  "endInterval": 1,
                                                  "endIntervalTimeUnit": "MINUTES"
                                        }
                                """),
                // config for version 0 (fetchMode is 'ALL' and orderBy is 'DESC' and aggregation is 'SUM')
                Arguments.of(0,
                        """
                                        {
                                                  "latestTsKeyNames": [],
                                                  "aggregation": "SUM",
                                                  "fetchMode": "ALL",
                                                  "orderBy": "DESC",
                                                  "limit": 1000,
                                                  "useMetadataIntervalPatterns": false,
                                                  "startIntervalPattern": "",
                                                  "endIntervalPattern": "",
                                                  "startInterval": 2,
                                                  "startIntervalTimeUnit": "MINUTES",
                                                  "endInterval": 1,
                                                  "endIntervalTimeUnit": "MINUTES"
                                        }
                                """,
                        false,
                        """
                                        {
                                                  "latestTsKeyNames": [],
                                                  "aggregation": "SUM",
                                                  "fetchMode": "ALL",
                                                  "orderBy": "DESC",
                                                  "limit": 1000,
                                                  "useMetadataIntervalPatterns": false,
                                                  "startIntervalPattern": "",
                                                  "endIntervalPattern": "",
                                                  "startInterval": 2,
                                                  "startIntervalTimeUnit": "MINUTES",
                                                  "endInterval": 1,
                                                  "endIntervalTimeUnit": "MINUTES"
                                        }
                                """),
                // config for version 0 (fetchMode is 'INVALID_MODE' and orderBy is 'INVALID_ORDER_BY' and aggregation is 'INVALID_AGGREGATION')
                Arguments.of(0,
                        """
                                        {
                                                  "latestTsKeyNames": [],
                                                  "aggregation": "INVALID_AGGREGATION",
                                                  "fetchMode": "INVALID_MODE",
                                                  "orderBy": "INVALID_ORDER_BY",
                                                  "limit": 1000,
                                                  "useMetadataIntervalPatterns": false,
                                                  "startIntervalPattern": "",
                                                  "endIntervalPattern": "",
                                                  "startInterval": 2,
                                                  "startIntervalTimeUnit": "MINUTES",
                                                  "endInterval": 1,
                                                  "endIntervalTimeUnit": "MINUTES"
                                        }
                                """,
                        true,
                        """
                                        {
                                                  "latestTsKeyNames": [],
                                                  "aggregation": "NONE",
                                                  "fetchMode": "LAST",
                                                  "orderBy": "DESC",
                                                  "limit": 1000,
                                                  "useMetadataIntervalPatterns": false,
                                                  "startIntervalPattern": "",
                                                  "endIntervalPattern": "",
                                                  "startInterval": 2,
                                                  "startIntervalTimeUnit": "MINUTES",
                                                  "endInterval": 1,
                                                  "endIntervalTimeUnit": "MINUTES"
                                        }
                                """),
                // config for version 0 (fetchMode is 'ALL' and limit, aggregation and orderBy do not exist)
                Arguments.of(0,
                        """
                                        {
                                                   "latestTsKeyNames": ["key"],
                                                   "fetchMode": "ALL",
                                                   "useMetadataIntervalPatterns": false,
                                                   "startIntervalPattern": "",
                                                   "endIntervalPattern": "",
                                                   "startInterval": 2,
                                                   "startIntervalTimeUnit": "MINUTES",
                                                   "endInterval": 1,
                                                   "endIntervalTimeUnit": "MINUTES"
                                        }
                                """,
                        true,
                        """
                                        {
                                                   "latestTsKeyNames": ["key"],
                                                   "aggregation": "NONE",
                                                   "fetchMode": "ALL",
                                                   "orderBy": "ASC",
                                                   "limit": 1000,
                                                   "useMetadataIntervalPatterns": false,
                                                   "startIntervalPattern": "",
                                                   "endIntervalPattern": "",
                                                   "startInterval": 2,
                                                   "startIntervalTimeUnit": "MINUTES",
                                                   "endInterval": 1,
                                                   "endIntervalTimeUnit": "MINUTES"
                                        }
                                """)
        );
    }

    @Override
    protected TbNode getTestNode() {
        return node;
    }
}
