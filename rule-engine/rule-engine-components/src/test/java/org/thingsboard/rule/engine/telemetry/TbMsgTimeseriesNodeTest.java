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
package org.thingsboard.rule.engine.telemetry;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.thingsboard.rule.engine.api.RuleEngineTelemetryService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TbMsgTimeseriesNodeTest {

    private final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("c8f34868-603a-4433-876a-7d356e5cf377"));
    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("e5095e9a-04f4-44c9-b443-1cf1b97d3384"));
    private final TenantProfileId TENANT_PROFILE_ID = new TenantProfileId(UUID.fromString("ab78dd78-83d0-43fa-869f-d42ec9ed1744"));

    private TbMsgTimeseriesNode node;
    private TbMsgTimeseriesNodeConfiguration config;
    private long tenantProfileDefaultStorageTtl;

    @Mock
    private TbContext ctxMock;
    @Mock
    private RuleEngineTelemetryService telemetryServiceMock;

    @BeforeEach
    public void setUp() throws TbNodeException {
        node = new TbMsgTimeseriesNode();
        config = new TbMsgTimeseriesNodeConfiguration().defaultConfiguration();
    }

    @ParameterizedTest
    @EnumSource(TbMsgType.class)
    void givenMsgTypeAndEmptyMsgData_whenOnMsg_thenVerifyFailureMsg(TbMsgType msgType) throws TbNodeException {
        init();
        TbMsg msg = TbMsg.newMsg(msgType, DEVICE_ID, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_ARRAY);
        node.onMsg(ctxMock, msg);
        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctxMock).tellFailure(eq(msg), throwableCaptor.capture());

        if (TbMsgType.POST_TELEMETRY_REQUEST.equals(msgType)) {
            assertThat(throwableCaptor.getValue()).isInstanceOf(IllegalArgumentException.class).hasMessage("Msg body is empty: " + msg.getData());
            return;
        }
        assertThat(throwableCaptor.getValue()).isInstanceOf(IllegalArgumentException.class).hasMessage("Unsupported msg type: " + msgType);
        verifyNoMoreInteractions(ctxMock);
    }

    @Test
    void givenTtlFromConfigIsZeroAndUseServiceTsIsTrue_whenOnMsg_thenSaveTimeseriesUsingTenantProfileDefaultTtl() throws TbNodeException {
        config.setUseServerTs(true);
        init();
        String data = """
                {
                    "temp": 45,
                    "humidity": 77
                }
                """;
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, TbMsgMetaData.EMPTY, data);

        when(ctxMock.getTelemetryService()).thenReturn(telemetryServiceMock);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        doAnswer(invocation -> {
            TelemetryNodeCallback callback = invocation.getArgument(5);
            callback.onSuccess(null);
            return null;
        }).when(telemetryServiceMock).saveAndNotify(any(), any(), any(), anyList(), anyLong(), any());

        node.onMsg(ctxMock, msg);

        ArgumentCaptor<List<TsKvEntry>> entryListCaptor = ArgumentCaptor.forClass(List.class);
        verify(telemetryServiceMock).saveAndNotify(eq(TENANT_ID), isNull(), eq(DEVICE_ID), entryListCaptor.capture(),
                eq(tenantProfileDefaultStorageTtl), any(TelemetryNodeCallback.class));
        List<TsKvEntry> entryListCaptorValue = entryListCaptor.getValue();
        assertThat(entryListCaptorValue.size()).isEqualTo(2);
        verifyTimeseriesToSave(entryListCaptorValue, msg);
        verify(ctxMock).tellSuccess(msg);
        verifyNoMoreInteractions(ctxMock, telemetryServiceMock);
    }

    @Test
    void givenSkipLatestPersistenceIsTrueAndTtlFromConfig_whenOnMsg_thenSaveTimeseriesUsingTtlFromConfig() throws TbNodeException {
        long ttlFromConfig = 5L;
        config.setDefaultTTL(ttlFromConfig);
        config.setSkipLatestPersistence(true);
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        var tenantProfile = getTenantProfile();
        when(ctxMock.getTenantProfile()).thenReturn(tenantProfile);
        node.init(ctxMock, configuration);

        String data = """
                {
                    "temp": 45,
                    "humidity": 77
                }
                """;
        long ts = System.currentTimeMillis();
        var metadata = Map.of("ts", String.valueOf(ts));
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, new TbMsgMetaData(metadata), data);

        when(ctxMock.getTelemetryService()).thenReturn(telemetryServiceMock);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        doAnswer(invocation -> {
            TelemetryNodeCallback callback = invocation.getArgument(5);
            callback.onSuccess(null);
            return null;
        }).when(telemetryServiceMock).saveWithoutLatestAndNotify(any(), any(), any(), anyList(), anyLong(), any());

        node.onMsg(ctxMock, msg);

        verify(ctxMock).addTenantProfileListener(any());
        ArgumentCaptor<List<TsKvEntry>> entryListCaptor = ArgumentCaptor.forClass(List.class);
        verify(telemetryServiceMock).saveWithoutLatestAndNotify(
                eq(TENANT_ID), isNull(), eq(DEVICE_ID), entryListCaptor.capture(), eq(ttlFromConfig), any(TelemetryNodeCallback.class));
        List<TsKvEntry> entryListCaptorValue = entryListCaptor.getValue();
        assertThat(entryListCaptorValue.size()).isEqualTo(2);
        verifyTimeseriesToSave(entryListCaptorValue, msg, ts);
        verify(ctxMock).tellSuccess(msg);
        verifyNoMoreInteractions(ctxMock, telemetryServiceMock);
    }

    @ParameterizedTest
    @MethodSource
    void givenTtlFromConfigAndTtlFromMd_whenOnMsg_thenVerifyTtl(String ttlFromMd, long ttlFromConfig, long expectedTtl) throws TbNodeException {
        config.setDefaultTTL(ttlFromConfig);
        init();

        when(ctxMock.getTelemetryService()).thenReturn(telemetryServiceMock);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);

        String data = """
                {
                    "temp": 45,
                    "humidity": 77
                }
                """;
        var metadata = new HashMap<String, String>();
        metadata.put("TTL", ttlFromMd);
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DEVICE_ID, new TbMsgMetaData(metadata), data);
        node.onMsg(ctxMock, msg);

        verify(telemetryServiceMock).saveAndNotify(eq(TENANT_ID), isNull(), eq(DEVICE_ID), anyList(), eq(expectedTtl), any(TelemetryNodeCallback.class));
    }

    private static Stream<Arguments> givenTtlFromConfigAndTtlFromMd_whenOnMsg_thenVerifyTtl() {
        return Stream.of(
                Arguments.of("5", 1L, 5L),
                Arguments.of("", 3L, 3L),
                Arguments.of(null, 8L, 8L)
        );
    }

    private void verifyTimeseriesToSave(List<TsKvEntry> tsKvEntryList, TbMsg incomingMsg) {
        verifyTimeseriesToSave(tsKvEntryList, incomingMsg, null);
    }

    private void verifyTimeseriesToSave(List<TsKvEntry> tsKvEntryList, TbMsg incomingMsg, Long ts) {
        JsonNode msgData = JacksonUtil.toJsonNode(incomingMsg.getData());
        tsKvEntryList.forEach(tsKvEntry -> {
            if (ts != null) {
                assertThat(tsKvEntry.getTs()).isEqualTo(ts);
            }
            String key = tsKvEntry.getKey();
            assertThat(msgData.has(key)).isTrue();
            String value = tsKvEntry.getValueAsString();
            assertThat(value).isEqualTo(msgData.findValue(key).asText());
        });
    }

    private void init() throws TbNodeException {
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        var tenantProfile = getTenantProfile();
        when(ctxMock.getTenantProfile()).thenReturn(tenantProfile);
        node.init(ctxMock, configuration);
        tenantProfile.getProfileConfiguration().ifPresent(profileConfiguration ->
                tenantProfileDefaultStorageTtl = TimeUnit.DAYS.toSeconds(profileConfiguration.getDefaultStorageTtlDays()));
        verify(ctxMock).addTenantProfileListener(any());
    }

    private TenantProfile getTenantProfile() {
        var tenantProfile = new TenantProfile(TENANT_PROFILE_ID);
        var tenantProfileData = new TenantProfileData();
        var tenantProfileConfiguration = new DefaultTenantProfileConfiguration();
        tenantProfileData.setConfiguration(tenantProfileConfiguration);
        tenantProfile.setProfileData(tenantProfileData);
        return tenantProfile;
    }

}
