/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.rule.engine.telemetry;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.api.RuleEngineTelemetryService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.rule.engine.telemetry.strategy.ProcessingStrategy;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.service.ConstraintValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.thingsboard.rule.engine.telemetry.settings.TimeseriesProcessingSettings.Advanced;
import static org.thingsboard.rule.engine.telemetry.settings.TimeseriesProcessingSettings.Deduplicate;
import static org.thingsboard.rule.engine.telemetry.settings.TimeseriesProcessingSettings.OnEveryMessage;
import static org.thingsboard.rule.engine.telemetry.settings.TimeseriesProcessingSettings.WebSocketsOnly;

@ExtendWith(MockitoExtension.class)
public class TbMsgTimeseriesNodeTest extends AbstractRuleNodeUpgradeTest {

    private final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("c8f34868-603a-4433-876a-7d356e5cf377"));
    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("e5095e9a-04f4-44c9-b443-1cf1b97d3384"));

    private TenantProfile tenantProfile;

    private TbMsgTimeseriesNode node;
    private TbMsgTimeseriesNodeConfiguration config;

    @Mock
    private TbContext ctxMock;
    @Mock
    private RuleEngineTelemetryService telemetryServiceMock;

    @BeforeEach
    public void setUp() throws TbNodeException {
        tenantProfile = new TenantProfile(new TenantProfileId(UUID.fromString("ab78dd78-83d0-43fa-869f-d42ec9ed1744")));
        var tenantProfileConfiguration = new DefaultTenantProfileConfiguration();
        tenantProfileConfiguration.setDefaultStorageTtlDays(5);
        var tenantProfileData = new TenantProfileData();
        tenantProfileData.setConfiguration(tenantProfileConfiguration);
        tenantProfile.setProfileData(tenantProfileData);
        lenient().when(ctxMock.getTenantProfile()).thenReturn(tenantProfile);

        lenient().when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        lenient().when(ctxMock.getTelemetryService()).thenReturn(telemetryServiceMock);

        node = spy(new TbMsgTimeseriesNode());
        config = new TbMsgTimeseriesNodeConfiguration().defaultConfiguration();
    }

    @Test
    public void verifyDefaultConfig() {
        assertThat(config.getDefaultTTL()).isEqualTo(0L);
        assertThat(config.getProcessingSettings()).isInstanceOf(OnEveryMessage.class);
        assertThat(config.isUseServerTs()).isFalse();
    }

    @Test
    public void whenInit_thenShouldAddTenantProfileListener() throws Exception {
        // GIVEN-WHEN
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // THEN
        then(ctxMock).should().addTenantProfileListener(any());
    }

    @Test
    public void givenProcessingSettingsAreNull_whenValidatingConstraints_thenThrowsException() {
        // GIVEN
        config.setProcessingSettings(null);

        // WHEN-THEN
        assertThatThrownBy(() -> ConstraintValidator.validateFields(config))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Validation error: processingSettings must not be null");
    }

    @ParameterizedTest
    @EnumSource(TbMsgType.class)
    public void givenMsgTypeAndEmptyMsgData_whenOnMsg_thenVerifyFailureMsg(TbMsgType msgType) throws TbNodeException {
        // GIVEN
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        TbMsg msg = TbMsg.newMsg()
                .type(msgType)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_ARRAY)
                .build();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        then(ctxMock).should().addTenantProfileListener(any());
        then(ctxMock).should().getTenantProfile();

        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctxMock).tellFailure(eq(msg), throwableCaptor.capture());

        if (TbMsgType.POST_TELEMETRY_REQUEST.equals(msgType)) {
            assertThat(throwableCaptor.getValue()).isInstanceOf(IllegalArgumentException.class).hasMessage("Msg body is empty: " + msg.getData());
            verifyNoMoreInteractions(ctxMock);
            return;
        }
        assertThat(throwableCaptor.getValue()).isInstanceOf(IllegalArgumentException.class).hasMessage("Unsupported msg type: " + msgType);
        verifyNoMoreInteractions(ctxMock);
    }

    @Test
    public void givenTtlFromConfigIsZeroAndUseServerTsIsTrue_whenOnMsg_thenSaveTimeseriesUsingTenantProfileDefaultTtl() throws TbNodeException {
        // GIVEN
        config.setUseServerTs(true);

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        String data = """
                {
                    "temp": 45,
                    "humidity": 77
                }
                """;
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(data)
                .build();

        doAnswer(invocation -> {
            TimeseriesSaveRequest request = invocation.getArgument(0);
            request.getCallback().onSuccess(null);
            return null;
        }).when(telemetryServiceMock).saveTimeseries(any(TimeseriesSaveRequest.class));

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        then(ctxMock).should().getTenantId();
        then(ctxMock).should().getTelemetryService();
        then(ctxMock).should().addTenantProfileListener(any());
        then(ctxMock).should().getTenantProfile();

        List<TsKvEntry> expectedList = getTsKvEntriesListWithTs(data, System.currentTimeMillis());
        verify(telemetryServiceMock).saveTimeseries(assertArg(request -> {
            assertThat(request.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(request.getCustomerId()).isNull();
            assertThat(request.getEntityId()).isEqualTo(DEVICE_ID);
            assertThat(request.getEntries()).usingRecursiveFieldByFieldElementComparatorIgnoringFields("ts").containsExactlyElementsOf(expectedList);
            assertThat(request.getTtl()).isEqualTo(extractTtlAsSeconds(tenantProfile));
            assertThat(request.getStrategy()).isEqualTo(TimeseriesSaveRequest.Strategy.PROCESS_ALL);
            assertThat(request.getCallback()).isInstanceOf(TelemetryNodeCallback.class);
        }));
        verify(ctxMock).tellSuccess(msg);
        verifyNoMoreInteractions(ctxMock, telemetryServiceMock);
    }

    @Test
    public void givenSkipLatestProcessingSettingsAndTtlFromConfig_whenOnMsg_thenSaveTimeseriesUsingTtlFromConfig() throws TbNodeException {
        // GIVEN
        config.setDefaultTTL(10L);

        var timeseries = ProcessingStrategy.onEveryMessage();
        var latest = ProcessingStrategy.skip();
        var webSockets = ProcessingStrategy.onEveryMessage();
        var calculatedFields = ProcessingStrategy.onEveryMessage();
        var processingSettings = new Advanced(timeseries, latest, webSockets, calculatedFields);
        config.setProcessingSettings(processingSettings);

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        String data = """
                {
                    "temp": 45,
                    "humidity": 77
                }
                """;
        long ts = System.currentTimeMillis();
        var metadata = Map.of("ts", String.valueOf(ts));
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(new TbMsgMetaData(metadata))
                .data(data)
                .build();

        doAnswer(invocation -> {
            TimeseriesSaveRequest request = invocation.getArgument(0);
            request.getCallback().onSuccess(null);
            return null;
        }).when(telemetryServiceMock).saveTimeseries(any(TimeseriesSaveRequest.class));

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        then(ctxMock).should().getTenantId();
        then(ctxMock).should().getTelemetryService();
        then(ctxMock).should().addTenantProfileListener(any());
        then(ctxMock).should().getTenantProfile();

        List<TsKvEntry> expectedList = getTsKvEntriesListWithTs(data, ts);
        verify(telemetryServiceMock).saveTimeseries(assertArg(request -> {
            assertThat(request.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(request.getCustomerId()).isNull();
            assertThat(request.getEntityId()).isEqualTo(DEVICE_ID);
            assertThat(request.getEntries()).containsExactlyElementsOf(expectedList);
            assertThat(request.getTtl()).isEqualTo(config.getDefaultTTL());
            assertThat(request.getStrategy()).isEqualTo(new TimeseriesSaveRequest.Strategy(true, false, true, true));
            assertThat(request.getCallback()).isInstanceOf(TelemetryNodeCallback.class);
        }));
        verify(ctxMock).tellSuccess(msg);
        verifyNoMoreInteractions(ctxMock, telemetryServiceMock);
    }

    @ParameterizedTest
    @MethodSource
    public void givenTtlFromConfigAndTtlFromMd_whenOnMsg_thenVerifyTtl(String ttlFromMd, long ttlFromConfig, long expectedTtl) throws TbNodeException {
        // GIVEN
        config.setDefaultTTL(ttlFromConfig);

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        String data = """
                {
                    "temp": 45,
                    "humidity": 77
                }
                """;
        var metadata = new TbMsgMetaData();
        metadata.putValue("TTL", ttlFromMd);
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(metadata)
                .data(data)
                .build();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        verify(telemetryServiceMock).saveTimeseries(assertArg(request -> {
            assertThat(request.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(request.getCustomerId()).isNull();
            assertThat(request.getEntityId()).isEqualTo(DEVICE_ID);
            assertThat(request.getTtl()).isEqualTo(expectedTtl);
            assertThat(request.getStrategy()).isEqualTo(TimeseriesSaveRequest.Strategy.PROCESS_ALL);
            assertThat(request.getCallback()).isInstanceOf(TelemetryNodeCallback.class);
        }));
    }

    private static Stream<Arguments> givenTtlFromConfigAndTtlFromMd_whenOnMsg_thenVerifyTtl() {
        return Stream.of(
                // when ttl is present in metadata and it is not zero then ttl = ttl from metadata
                Arguments.of("1", 2L, 1L),
                // when ttl is absent in metadata and present in config and it is not zero then ttl = ttl from config
                Arguments.of("", 3L, 3L),
                Arguments.of(null, 4L, 4L),
                // when ttl is present in metadata or config but it is zero then ttl = default ttl from tenant profile
                Arguments.of("0", 0L, TimeUnit.DAYS.toSeconds(5L))
        );
    }

    private static List<TsKvEntry> getTsKvEntriesListWithTs(String data, long ts) {
        Map<Long, List<KvEntry>> tsKvMap = JsonConverter.convertToTelemetry(JsonParser.parseString(data), ts);
        List<TsKvEntry> expectedList = new ArrayList<>();
        for (Map.Entry<Long, List<KvEntry>> tsKvEntry : tsKvMap.entrySet()) {
            for (KvEntry kvEntry : tsKvEntry.getValue()) {
                expectedList.add(new BasicTsKvEntry(tsKvEntry.getKey(), kvEntry));
            }
        }
        return expectedList;
    }

    @Test
    public void givenOnEveryMessageProcessingSettingsAndSameMessageTwoTimes_whenOnMsg_thenPersistSameMessageTwoTimes() throws TbNodeException {
        // GIVEN
        config.setProcessingSettings(new OnEveryMessage());

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .data(JacksonUtil.newObjectNode().put("temperature", 22.3).toString())
                .metaData(new TbMsgMetaData(Map.of("ts", "123")))
                .build();

        // WHEN-THEN
        var expectedSaveRequest = TimeseriesSaveRequest.builder()
                .tenantId(TENANT_ID)
                .customerId(msg.getCustomerId())
                .entityId(msg.getOriginator())
                .entry(new BasicTsKvEntry(123L, new DoubleDataEntry("temperature", 22.3)))
                .ttl(extractTtlAsSeconds(tenantProfile))
                .strategy(TimeseriesSaveRequest.Strategy.PROCESS_ALL)
                .previousCalculatedFieldIds(msg.getPreviousCalculatedFieldIds())
                .tbMsgId(msg.getId())
                .tbMsgType(msg.getInternalType())
                .build();

        node.onMsg(ctxMock, msg);
        then(telemetryServiceMock).should(times(1)).saveTimeseries(assertArg(
                actualSaveRequest -> assertThat(actualSaveRequest).usingRecursiveComparison().ignoringFields("callback").isEqualTo(expectedSaveRequest)
        ));

        node.onMsg(ctxMock, msg);
        then(telemetryServiceMock).should(times(2)).saveTimeseries(assertArg(
                actualSaveRequest -> assertThat(actualSaveRequest).usingRecursiveComparison().ignoringFields("callback").isEqualTo(expectedSaveRequest)
        ));
    }

    @Test
    public void givenDeduplicateProcessingSettingsAndSameMessageTwoTimes_whenOnMsg_thenPersistThisMessageOnlyFirstTime() throws TbNodeException {
        // GIVEN
        config.setProcessingSettings(new Deduplicate(10));

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .data(JacksonUtil.newObjectNode().put("temperature", 22.3).toString())
                .metaData(new TbMsgMetaData(Map.of("ts", "123")))
                .build();

        // WHEN-THEN
        var expectedSaveRequest = TimeseriesSaveRequest.builder()
                .tenantId(TENANT_ID)
                .customerId(msg.getCustomerId())
                .entityId(msg.getOriginator())
                .entry(new BasicTsKvEntry(123L, new DoubleDataEntry("temperature", 22.3)))
                .ttl(extractTtlAsSeconds(tenantProfile))
                .strategy(TimeseriesSaveRequest.Strategy.PROCESS_ALL)
                .previousCalculatedFieldIds(msg.getPreviousCalculatedFieldIds())
                .tbMsgId(msg.getId())
                .tbMsgType(msg.getInternalType())
                .build();

        node.onMsg(ctxMock, msg);
        then(telemetryServiceMock).should().saveTimeseries(assertArg(
                actualSaveRequest -> assertThat(actualSaveRequest).usingRecursiveComparison().ignoringFields("callback").isEqualTo(expectedSaveRequest)
        ));

        clearInvocations(telemetryServiceMock, ctxMock);

        node.onMsg(ctxMock, msg);
        then(telemetryServiceMock).should(never()).saveTimeseries(any());
    }

    @Test
    public void givenWebSocketsOnlyProcessingSettingsAndSameMessageTwoTimes_whenOnMsg_thenSendsOnlyWsUpdateTwoTimes() throws TbNodeException {
        // GIVEN
        config.setProcessingSettings(new WebSocketsOnly());

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .data(JacksonUtil.newObjectNode().put("temperature", 22.3).toString())
                .metaData(new TbMsgMetaData(Map.of("ts", "123")))
                .build();

        // WHEN-THEN
        var expectedSaveRequest = TimeseriesSaveRequest.builder()
                .tenantId(TENANT_ID)
                .customerId(msg.getCustomerId())
                .entityId(msg.getOriginator())
                .entry(new BasicTsKvEntry(123L, new DoubleDataEntry("temperature", 22.3)))
                .ttl(extractTtlAsSeconds(tenantProfile))
                .strategy(TimeseriesSaveRequest.Strategy.WS_ONLY)
                .previousCalculatedFieldIds(msg.getPreviousCalculatedFieldIds())
                .tbMsgId(msg.getId())
                .tbMsgType(msg.getInternalType())
                .build();

        node.onMsg(ctxMock, msg);
        then(telemetryServiceMock).should(times(1)).saveTimeseries(assertArg(
                actualSaveRequest -> assertThat(actualSaveRequest).usingRecursiveComparison().ignoringFields("callback").isEqualTo(expectedSaveRequest)
        ));

        node.onMsg(ctxMock, msg);
        then(telemetryServiceMock).should(times(2)).saveTimeseries(assertArg(
                actualSaveRequest -> assertThat(actualSaveRequest).usingRecursiveComparison().ignoringFields("callback").isEqualTo(expectedSaveRequest)
        ));
    }

    @Test
    public void givenAdvancedProcessingSettingsWithOnEveryMessageStrategiesForAllActionsAndSameMessageTwoTimes_whenOnMsg_thenPersistSameMessageTwoTimes() throws TbNodeException {
        // GIVEN
        config.setProcessingSettings(new Advanced(
                ProcessingStrategy.onEveryMessage(),
                ProcessingStrategy.onEveryMessage(),
                ProcessingStrategy.onEveryMessage(),
                ProcessingStrategy.onEveryMessage()
        ));

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .data(JacksonUtil.newObjectNode().put("temperature", 22.3).toString())
                .metaData(new TbMsgMetaData(Map.of("ts", "123")))
                .build();

        // WHEN-THEN
        var expectedSaveRequest = TimeseriesSaveRequest.builder()
                .tenantId(TENANT_ID)
                .customerId(msg.getCustomerId())
                .entityId(msg.getOriginator())
                .entry(new BasicTsKvEntry(123L, new DoubleDataEntry("temperature", 22.3)))
                .ttl(extractTtlAsSeconds(tenantProfile))
                .strategy(TimeseriesSaveRequest.Strategy.PROCESS_ALL)
                .previousCalculatedFieldIds(msg.getPreviousCalculatedFieldIds())
                .tbMsgId(msg.getId())
                .tbMsgType(msg.getInternalType())
                .build();

        node.onMsg(ctxMock, msg);
        then(telemetryServiceMock).should(times(1)).saveTimeseries(assertArg(
                actualSaveRequest -> assertThat(actualSaveRequest).usingRecursiveComparison().ignoringFields("callback").isEqualTo(expectedSaveRequest)
        ));

        node.onMsg(ctxMock, msg);
        then(telemetryServiceMock).should(times(2)).saveTimeseries(assertArg(
                actualSaveRequest -> assertThat(actualSaveRequest).usingRecursiveComparison().ignoringFields("callback").isEqualTo(expectedSaveRequest)
        ));
    }

    @Test
    public void givenAdvancedProcessingSettingsWithDifferentDeduplicateStrategyForEachAction_whenOnMsg_thenEvaluatesStrategiesForEachActionsIndependently() throws TbNodeException {
        // GIVEN
        config.setProcessingSettings(new Advanced(
                ProcessingStrategy.deduplicate(1),
                ProcessingStrategy.deduplicate(2),
                ProcessingStrategy.deduplicate(3),
                ProcessingStrategy.deduplicate(4)
        ));

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        long ts1 = 500L;
        long ts2 = 1500L;
        long ts3 = 2500L;
        long ts4 = 3500L;
        long ts5 = 4500L;

        // WHEN-THEN
        node.onMsg(ctxMock, TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .data(JacksonUtil.newObjectNode().put("temperature", 22.3).toString())
                .metaData(new TbMsgMetaData(Map.of("ts", Long.toString(ts1))))
                .build());
        then(telemetryServiceMock).should().saveTimeseries(assertArg(
                actualSaveRequest -> assertThat(actualSaveRequest.getStrategy()).isEqualTo(TimeseriesSaveRequest.Strategy.PROCESS_ALL)
        ));

        clearInvocations(telemetryServiceMock);

        node.onMsg(ctxMock, TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .data(JacksonUtil.newObjectNode().put("temperature", 22.3).toString())
                .metaData(new TbMsgMetaData(Map.of("ts", Long.toString(ts2))))
                .build());
        then(telemetryServiceMock).should().saveTimeseries(assertArg(
                actualSaveRequest -> assertThat(actualSaveRequest.getStrategy()).isEqualTo(
                        new TimeseriesSaveRequest.Strategy(true, false, false, false)
                )
        ));

        clearInvocations(telemetryServiceMock);

        node.onMsg(ctxMock, TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .data(JacksonUtil.newObjectNode().put("temperature", 22.3).toString())
                .metaData(new TbMsgMetaData(Map.of("ts", Long.toString(ts3))))
                .build());
        then(telemetryServiceMock).should().saveTimeseries(assertArg(
                actualSaveRequest -> assertThat(actualSaveRequest.getStrategy()).isEqualTo(
                        new TimeseriesSaveRequest.Strategy(true, true, false, false)
                )
        ));

        clearInvocations(telemetryServiceMock);

        node.onMsg(ctxMock, TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .data(JacksonUtil.newObjectNode().put("temperature", 22.3).toString())
                .metaData(new TbMsgMetaData(Map.of("ts", Long.toString(ts4))))
                .build());
        then(telemetryServiceMock).should().saveTimeseries(assertArg(
                actualSaveRequest -> assertThat(actualSaveRequest.getStrategy()).isEqualTo(
                        new TimeseriesSaveRequest.Strategy(true, false, true, false)
                )
        ));

        clearInvocations(telemetryServiceMock);

        node.onMsg(ctxMock, TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .data(JacksonUtil.newObjectNode().put("temperature", 22.3).toString())
                .metaData(new TbMsgMetaData(Map.of("ts", Long.toString(ts5))))
                .build());
        then(telemetryServiceMock).should().saveTimeseries(assertArg(
                actualSaveRequest -> assertThat(actualSaveRequest.getStrategy()).isEqualTo(
                        new TimeseriesSaveRequest.Strategy(true, true, false, true)
                )
        ));
    }

    @Test
    public void givenAdvancedProcessingSettingsWithSkipStrategiesForAllActionsAndSameMessageTwoTimes_whenOnMsg_thenSkipsSameMessageTwoTimes() throws TbNodeException {
        // GIVEN
        config.setProcessingSettings(new Advanced(
                ProcessingStrategy.skip(),
                ProcessingStrategy.skip(),
                ProcessingStrategy.skip(),
                ProcessingStrategy.skip()
        ));

        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .data(JacksonUtil.newObjectNode().put("temperature", 22.3).toString())
                .metaData(new TbMsgMetaData(Map.of("ts", "123")))
                .build();

        // WHEN-THEN
        node.onMsg(ctxMock, msg);
        then(telemetryServiceMock).should(never()).saveTimeseries(any());
        then(ctxMock).should(times(1)).tellSuccess(msg);

        node.onMsg(ctxMock, msg);
        then(telemetryServiceMock).should(never()).saveTimeseries(any());
        then(ctxMock).should(times(2)).tellSuccess(msg);
    }

    private static long extractTtlAsSeconds(TenantProfile tenantProfile) {
        return TimeUnit.DAYS.toSeconds(tenantProfile.getDefaultProfileConfiguration().getDefaultStorageTtlDays());
    }

    @Override
    protected TbNode getTestNode() {
        return node;
    }

    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                Arguments.of(0, """
                                {
                                  "defaultTTL": 0,
                                  "useServerTs": false,
                                  "skipLatestPersistence": false
                                }""",
                        true,
                        """
                                {
                                    "defaultTTL": 0,
                                    "useServerTs": false,
                                    "processingSettings": {
                                        "type": "ON_EVERY_MESSAGE"
                                    }
                                }"""),
                Arguments.of(0, """
                                {
                                  "defaultTTL": 0,
                                  "useServerTs": false
                                }""",
                        true,
                        """
                                {
                                    "defaultTTL": 0,
                                    "useServerTs": false,
                                    "processingSettings": {
                                        "type": "ON_EVERY_MESSAGE"
                                    }
                                }"""),
                Arguments.of(0, """
                                {
                                  "defaultTTL": 0,
                                  "useServerTs": false,
                                  "skipLatestPersistence": null
                                }""",
                        true,
                        """
                                {
                                    "defaultTTL": 0,
                                    "useServerTs": false,
                                    "processingSettings": {
                                        "type": "ON_EVERY_MESSAGE"
                                    }
                                }"""),
                Arguments.of(0, """
                                {
                                  "defaultTTL": 0,
                                  "useServerTs": false,
                                  "skipLatestPersistence": true
                                }""",
                        true,
                        """
                                {
                                    "defaultTTL": 0,
                                    "useServerTs": false,
                                    "processingSettings": {
                                        "type": "ADVANCED",
                                        "timeseries": {
                                            "type": "ON_EVERY_MESSAGE"
                                        },
                                        "latest": {
                                            "type": "SKIP"
                                        },
                                        "webSockets": {
                                            "type": "ON_EVERY_MESSAGE"
                                        },
                                        "calculatedFields": {
                                            "type": "ON_EVERY_MESSAGE"
                                        }
                                    }
                                }""")
        );
    }

}
