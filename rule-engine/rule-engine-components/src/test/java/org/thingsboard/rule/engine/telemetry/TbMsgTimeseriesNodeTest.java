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
import org.junit.jupiter.params.provider.EnumSource;
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

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        var tenantProfile = new TenantProfile(new TenantProfileId(UUID.fromString("8c45d0fe-d437-40e9-8c31-b695b315bf40")));
        var tenantProfileData = new TenantProfileData();
        var tenantProfileConfiguration = new DefaultTenantProfileConfiguration();
        tenantProfileData.setConfiguration(tenantProfileConfiguration);
        tenantProfile.setProfileData(tenantProfileData);
        when(ctxMock.getTenantProfile()).thenReturn(tenantProfile);
        doAnswer(invocation -> {
            invocation.getArgument(0);
            return null;
        }).when(ctxMock).addTenantProfileListener(any());
        node.init(ctxMock, configuration);
        tenantProfileDefaultStorageTtl = TimeUnit.DAYS.toSeconds(tenantProfileConfiguration.getDefaultStorageTtlDays());
    }

    @ParameterizedTest
    @EnumSource(TbMsgType.class)
    void givenMsgTypeAndEmptyMsgData_whenOnMsg_thenVerifyFailureMsg(TbMsgType msgType) {
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
    void givenSkipLatestPersistenceIsFalse_whenOnMsg_thenSaveTimeseries() {
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
        verify(telemetryServiceMock).saveAndNotify(
                eq(TENANT_ID), isNull(), eq(DEVICE_ID), entryListCaptor.capture(), eq(tenantProfileDefaultStorageTtl), any(TelemetryNodeCallback.class));
        List<TsKvEntry> entryListCaptorValue = entryListCaptor.getValue();
        assertThat(entryListCaptorValue.size()).isEqualTo(2);
        verifyTimeseriesToSave(entryListCaptorValue, msg);
        verify(ctxMock).tellSuccess(eq(msg));
        verifyNoMoreInteractions(ctxMock, telemetryServiceMock);
    }

    @Test
    void givenSkipLatestPersistenceIsTrue_whenOnMsg_thenSaveTimeseries() throws TbNodeException {
        config.setSkipLatestPersistence(true);
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);

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
        }).when(telemetryServiceMock).saveWithoutLatestAndNotify(any(), any(), any(), anyList(), anyLong(), any());

        node.onMsg(ctxMock, msg);

        ArgumentCaptor<List<TsKvEntry>> entryListCaptor = ArgumentCaptor.forClass(List.class);
        verify(telemetryServiceMock).saveWithoutLatestAndNotify(
                eq(TENANT_ID), isNull(), eq(DEVICE_ID), entryListCaptor.capture(), eq(tenantProfileDefaultStorageTtl), any(TelemetryNodeCallback.class));
        List<TsKvEntry> entryListCaptorValue = entryListCaptor.getValue();
        assertThat(entryListCaptorValue.size()).isEqualTo(2);
        verifyTimeseriesToSave(entryListCaptorValue, msg);
        verify(ctxMock).tellSuccess(eq(msg));
        verifyNoMoreInteractions(ctxMock, telemetryServiceMock);
    }

    private void verifyTimeseriesToSave(List<TsKvEntry> tsKvEntryList, TbMsg incomingMsg) {
        JsonNode msgData = JacksonUtil.toJsonNode(incomingMsg.getData());
        tsKvEntryList.forEach(tsKvEntry -> {
            String key = tsKvEntry.getKey();
            assertThat(msgData.has(key)).isTrue();
            String value = tsKvEntry.getValueAsString();
            assertThat(value).isEqualTo(msgData.findValue(key).asText());
        });
    }

}
