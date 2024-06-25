/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
public class TbGetTelemetryNodeTest {

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
    public void setUp() throws Exception {
        node = new TbGetTelemetryNode();
        config = new TbGetTelemetryNodeConfiguration().defaultConfiguration();
    }

    @Test
    public void givenAggregationAsString_whenParseAggregation_thenReturnEnum() throws TbNodeException {
        config.setFetchMode("ALL");
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        //compatibility with old configs without "aggregation" parameter
        assertThat(node.parseAggregationConfig(null)).isEqualTo(Aggregation.NONE);
        assertThat(node.parseAggregationConfig("")).isEqualTo(Aggregation.NONE);

        //common values
        assertThat(node.parseAggregationConfig("MIN")).isEqualTo(Aggregation.MIN);
        assertThat(node.parseAggregationConfig("MAX")).isEqualTo(Aggregation.MAX);
        assertThat(node.parseAggregationConfig("AVG")).isEqualTo(Aggregation.AVG);
        assertThat(node.parseAggregationConfig("SUM")).isEqualTo(Aggregation.SUM);
        assertThat(node.parseAggregationConfig("COUNT")).isEqualTo(Aggregation.COUNT);
        assertThat(node.parseAggregationConfig("NONE")).isEqualTo(Aggregation.NONE);

        //all possible values in future
        for (Aggregation aggEnum : Aggregation.values()) {
            assertThat(node.parseAggregationConfig(aggEnum.name())).isEqualTo(aggEnum);
        }
    }

    @Test
    public void givenAggregationWhiteSpace_whenParseAggregation_thenException() throws TbNodeException {
        config.setFetchMode("ALL");
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        assertThatThrownBy(() -> node.parseAggregationConfig(" ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void givenAggregationIncorrect_whenParseAggregation_thenException() throws TbNodeException {
        config.setFetchMode("ALL");
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        assertThatThrownBy(() -> node.parseAggregationConfig("TOP")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void verifyDefaultConfig() {
        assertThat(config.getStartInterval()).isEqualTo(2);
        assertThat(config.getEndInterval()).isEqualTo(1);
        assertThat(config.getStartIntervalPattern()).isEqualTo("");
        assertThat(config.getEndIntervalPattern()).isEqualTo("");
        assertThat(config.isUseMetadataIntervalPatterns()).isFalse();
        assertThat(config.getStartIntervalTimeUnit()).isEqualTo(TimeUnit.MINUTES.name());
        assertThat(config.getEndIntervalTimeUnit()).isEqualTo(TimeUnit.MINUTES.name());
        assertThat(config.getFetchMode()).isEqualTo("FIRST");
        assertThat(config.getOrderBy()).isEqualTo("ASC");
        assertThat(config.getAggregation()).isEqualTo(Aggregation.NONE.name());
        assertThat(config.getLimit()).isEqualTo(1000);
        assertThat(config.getLatestTsKeyNames()).isEmpty();
    }

    @ParameterizedTest
    @MethodSource
    public void givenFetchModeAndLimit_whenInit_thenVerifyLimit(String fetchMode, int limit, int expectedLimit) throws TbNodeException {
        config.setFetchMode(fetchMode);
        config.setLimit(limit);

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var actualLimit = ReflectionTestUtils.getField(node, "limit");
        assertThat(actualLimit).isEqualTo(expectedLimit);
    }

    private static Stream<Arguments> givenFetchModeAndLimit_whenInit_thenVerifyLimit() {
        return Stream.of(
                Arguments.of("FIRST", 0, 1),
                Arguments.of("LAST", 10, 1),
                Arguments.of("ALL", 0, 1000),
                Arguments.of("ALL", 5, 5)
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void givenEmptyOrderBy_whenInit_thenVerify(String orderBy) throws TbNodeException {
        config.setOrderBy(orderBy);

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var actualOrderBy = ReflectionTestUtils.getField(node, "orderByFetchAll");
        assertThat(actualOrderBy).isEqualTo("ASC");
    }

    @Test
    public void givenConfig_whenInit_thenVerify() throws TbNodeException {
        List<String> keys = List.of("temperature", "humidity");
        config.setLatestTsKeyNames(keys);
        config.setFetchMode("ALL");
        config.setLimit(5);
        config.setOrderBy("DESC");
        config.setAggregation("MIN");

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var actualLimit = ReflectionTestUtils.getField(node, "limit");
        var actualTsKeyNames = ReflectionTestUtils.getField(node, "tsKeyNames");
        var actualFetchMode = ReflectionTestUtils.getField(node, "fetchMode");
        var actualOrderByFetchAll = ReflectionTestUtils.getField(node, "orderByFetchAll");
        var actualAggregation = ReflectionTestUtils.getField(node, "aggregation");

        assertThat(actualLimit).isEqualTo(5);
        assertThat(actualTsKeyNames).isEqualTo(keys);
        assertThat(actualFetchMode).isEqualTo("ALL");
        assertThat(actualOrderByFetchAll).isEqualTo("DESC");
        assertThat(actualAggregation).isEqualTo(Aggregation.MIN);
    }

    @Test
    public void givenEmptyTsKeyNames_whenOnMsg_thenTellFailure() throws TbNodeException {
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
        node.onMsg(ctxMock, msg);

        ArgumentCaptor<Throwable> actualException = ArgumentCaptor.forClass(Throwable.class);
        then(ctxMock).should().tellFailure(eq(msg), actualException.capture());
        assertThat(actualException.getValue())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Telemetry is not selected!");
    }

    @Test
    public void givenUseMetadataIntervalPatternsIsTrueAndFetchModeIsAllAndAggregationIsMin_whenOnMsg_thenTellSuccess() throws TbNodeException {
        config.setLatestTsKeyNames(List.of("temperature", "humidity"));
        config.setUseMetadataIntervalPatterns(true);
        config.setStartIntervalPattern("${mdStartInterval}");
        config.setEndIntervalPattern("$[msgEndInterval]");
        config.setFetchMode("ALL");
        config.setAggregation("MIN");

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        mockTimeseriesService();
        List<TsKvEntry> tsKvEntries = List.of(
                new BasicTsKvEntry(System.currentTimeMillis() - 5, new DoubleDataEntry("temperature", 22.4)),
                new BasicTsKvEntry(System.currentTimeMillis() - 4, new DoubleDataEntry("humidity", 55.5))
        );
        given(timeseriesServiceMock.findAll(any(TenantId.class), any(EntityId.class), anyList())).willReturn(Futures.immediateFuture(tsKvEntries));

        long startTs = 1719220350000L;
        long endTs = 1719220353000L;
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("mdStartInterval", String.valueOf(startTs));
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, metaData, "{\"msgEndInterval\":\"" + endTs + "\"}");
        node.onMsg(ctxMock, msg);

        List<ReadTsKvQuery> expectedReadTsKvQueryList = List.of(
                new BaseReadTsKvQuery("temperature", startTs, endTs, endTs - startTs, TbGetTelemetryNodeConfiguration.MAX_FETCH_SIZE, Aggregation.MIN, "ASC"),
                new BaseReadTsKvQuery("humidity", startTs, endTs, endTs - startTs, TbGetTelemetryNodeConfiguration.MAX_FETCH_SIZE, Aggregation.MIN, "ASC")
        );
        verifyReadTsKvQueryList(expectedReadTsKvQueryList, false);
        verifyOutgoingMsg(msg);
    }

    @Test
    public void givenUseMetadataIntervalPatternsIsTrueAndFetchModeIsLastAndAggregationIsMax_whenOnMsg_thenTellSuccess() throws TbNodeException {
        config.setLatestTsKeyNames(List.of("temperature", "humidity"));
        config.setUseMetadataIntervalPatterns(true);
        config.setStartIntervalPattern("${mdStartInterval}");
        config.setEndIntervalPattern("$[msgEndInterval]");
        config.setFetchMode("LAST");
        config.setAggregation("MAX");

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        mockTimeseriesService();
        long ts = System.currentTimeMillis();
        List<TsKvEntry> tsKvEntries = List.of(
                new BasicTsKvEntry(ts - 5, new DoubleDataEntry("temperature", 22.4)),
                new BasicTsKvEntry(ts - 4, new DoubleDataEntry("humidity", 55.5))
        );
        given(timeseriesServiceMock.findAll(any(TenantId.class), any(EntityId.class), anyList())).willReturn(Futures.immediateFuture(tsKvEntries));

        long startTs = 1719220350000L;
        long endTs = 1719220353000L;
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("mdStartInterval", String.valueOf(startTs));
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, metaData, "{\"msgEndInterval\":\"" + endTs + "\"}");
        node.onMsg(ctxMock, msg);

        List<ReadTsKvQuery> expectedReadTsKvQueryList = List.of(
                new BaseReadTsKvQuery("temperature", startTs, endTs, 1, 1, Aggregation.NONE, "DESC"),
                new BaseReadTsKvQuery("humidity", startTs, endTs, 1, 1, Aggregation.NONE, "DESC")
        );
        verifyReadTsKvQueryList(expectedReadTsKvQueryList, false);
        verifyOutgoingMsg(msg);
    }

    @Test
    public void givenUseMetadataIntervalPatternsIsFalseAndTsKeyNamesPatternsAndFetchModeIsFirst_whenOnMsg_thenTellFailure() throws TbNodeException {
        config.setLatestTsKeyNames(List.of("temperature", "${mdTsKey}", "$[msgTsKey]"));;
        config.setFetchMode("FIRST");
        config.setStartInterval(6);
        config.setEndInterval(1);

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        mockTimeseriesService();
        String errorMsg = "Something went wrong";
        given(timeseriesServiceMock.findAll(any(TenantId.class), any(EntityId.class), anyList())).willReturn(Futures.immediateFailedFuture(new RuntimeException(errorMsg)));

        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("mdTsKey", "humidity");
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, metaData, "{\"msgTsKey\":\"pressure\"}");
        node.onMsg(ctxMock, msg);

        long ts = System.currentTimeMillis();
        long startTs = ts - TimeUnit.SECONDS.toMillis(config.getStartInterval());
        long endTs = ts - TimeUnit.SECONDS.toMillis(config.getEndInterval());
        List<ReadTsKvQuery> expecetdReadTsKvQueryList = List.of(
                new BaseReadTsKvQuery("temperature", startTs, endTs, 1, 1, Aggregation.NONE, "ASC"),
                new BaseReadTsKvQuery("humidity", startTs, endTs, 1, 1, Aggregation.NONE, "ASC"),
                new BaseReadTsKvQuery("pressure", startTs, endTs, 1, 1, Aggregation.NONE, "ASC")
        );
        verifyReadTsKvQueryList(expecetdReadTsKvQueryList, true);

        ArgumentCaptor<Throwable> actualException = ArgumentCaptor.forClass(Throwable.class);
        then(ctxMock).should().tellFailure(eq(msg), actualException.capture());
        assertThat(actualException.getValue()).isInstanceOf(RuntimeException.class).hasMessage(errorMsg);
    }

    @ParameterizedTest
    @MethodSource
    public void givenInvalidIntervalPatterns_whenOnMsg_thenTellFailure(String startIntervalPattern, String errorMsg) throws TbNodeException {
        config.setLatestTsKeyNames(List.of("${mdKey}"));
        config.setUseMetadataIntervalPatterns(true);
        config.setStartIntervalPattern(startIntervalPattern);
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, "{\"msgStartInterval\":\"start\"}");
        node.onMsg(ctxMock, msg);

        ArgumentCaptor<Throwable> actualException = ArgumentCaptor.forClass(Throwable.class);
        then(ctxMock).should().tellFailure(eq(msg), actualException.capture());
        assertThat(actualException.getValue()).isInstanceOf(IllegalArgumentException.class).hasMessage(errorMsg);
    }

    private static Stream<Arguments> givenInvalidIntervalPatterns_whenOnMsg_thenTellFailure() {
        return Stream.of(
                Arguments.of("${mdStartInterval}", "Message value: 'mdStartInterval' is undefined"),
                Arguments.of("$[msgStartInterval]", "Message value: 'msgStartInterval' has invalid format")
        );
    }

    private void mockTimeseriesService() {
        given(ctxMock.getTimeseriesService()).willReturn(timeseriesServiceMock);
        given(ctxMock.getTenantId()).willReturn(TENANT_ID);
        given(ctxMock.getDbCallbackExecutor()).willReturn(executor);
    }

    private void verifyReadTsKvQueryList(List<ReadTsKvQuery> expectedReadTsKvQueryList, boolean ignoreTs) {
        ArgumentCaptor<List<ReadTsKvQuery>> actualReadTsKvQueryCaptor = ArgumentCaptor.forClass(List.class);
        then(timeseriesServiceMock).should().findAll(eq(TENANT_ID), eq(DEVICE_ID), actualReadTsKvQueryCaptor.capture());
        List<ReadTsKvQuery> actualReadTsKvQuery = actualReadTsKvQueryCaptor.getValue();
        for (int i = 0; i < expectedReadTsKvQueryList.size(); i++) {
            assertThat(actualReadTsKvQuery.get(i))
                    .usingRecursiveComparison()
                    .ignoringFields(!ignoreTs ? "id" : "endTs", "startTs", "id")
                    .isEqualTo(expectedReadTsKvQueryList.get(i));
        }
    }

    private void verifyOutgoingMsg(TbMsg expectedMsg) {
        ArgumentCaptor<TbMsg> actualMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        then(ctxMock).should().tellSuccess(actualMsgCaptor.capture());
        TbMsg actualMsg = actualMsgCaptor.getValue();
        assertThat(actualMsg).usingRecursiveComparison().ignoringFields("ctx", "metaData").isEqualTo(expectedMsg);
        assertThat(actualMsg.getMetaData().getData()).containsKeys("temperature", "humidity");
    }
}
