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

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.device.DeviceService;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TbGetOriginatorFieldsNodeTest {

    private static final DeviceId DUMMY_DEVICE_ORIGINATOR = new DeviceId(UUID.randomUUID());
    private static final TenantId DUMMY_TENANT_ID = new TenantId(UUID.randomUUID());
    private static final ListeningExecutor DB_EXECUTOR = new TestDbCallbackExecutor();
    @Mock
    private TbContext ctxMock;
    @Mock
    private DeviceService deviceServiceMock;
    private TbGetOriginatorFieldsNode node;
    private TbGetOriginatorFieldsConfiguration config;
    private TbNodeConfiguration nodeConfiguration;
    private TbMsg msg;

    @BeforeEach
    public void setUp() {
        node = new TbGetOriginatorFieldsNode();
        config = new TbGetOriginatorFieldsConfiguration().defaultConfiguration();
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
    }

    @Test
    public void givenConfigWithNullFetchTo_whenInit_thenException() {
        // GIVEN
        config.setFetchTo(null);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        // WHEN
        var exception = assertThrows(TbNodeException.class, () -> node.init(ctxMock, nodeConfiguration));

        // THEN
        assertThat(exception.getMessage()).isEqualTo("FetchTo option can't be null! Allowed values: " + Arrays.toString(TbMsgSource.values()));
        verify(ctxMock, never()).tellSuccess(any());
    }

    @Test
    public void givenDefaultConfig_whenInit_thenOK() throws TbNodeException {
        // GIVEN-WHEN
        node.init(ctxMock, nodeConfiguration);

        // THEN
        assertThat(node.config).isEqualTo(config);
        assertThat(config.getDataMapping()).isEqualTo(Map.of(
                "name", "originatorName",
                "type", "originatorType"));
        assertThat(config.isIgnoreNullStrings()).isEqualTo(false);
        assertThat(config.getFetchTo()).isEqualTo(TbMsgSource.METADATA);
        assertThat(node.fetchTo).isEqualTo(TbMsgSource.METADATA);
    }

    @Test
    public void givenCustomConfig_whenInit_thenOK() throws TbNodeException {
        // GIVEN
        config.setDataMapping(Map.of(
                "email", "originatorEmail",
                "title", "originatorTitle",
                "country", "originatorCountry"));
        config.setIgnoreNullStrings(true);
        config.setFetchTo(TbMsgSource.DATA);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        // WHEN
        node.init(ctxMock, nodeConfiguration);

        // THEN
        assertThat(node.config).isEqualTo(config);
        assertThat(config.getDataMapping()).isEqualTo(Map.of(
                "email", "originatorEmail",
                "title", "originatorTitle",
                "country", "originatorCountry"));
        assertThat(config.isIgnoreNullStrings()).isEqualTo(true);
        assertThat(config.getFetchTo()).isEqualTo(TbMsgSource.DATA);
        assertThat(node.fetchTo).isEqualTo(TbMsgSource.DATA);
    }

    @Test
    public void givenMsgDataIsNotAnJsonObjectAndFetchToData_whenOnMsg_thenException() {
        // GIVEN
        node.fetchTo = TbMsgSource.DATA;
        msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DUMMY_DEVICE_ORIGINATOR)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_ARRAY)
                .build();

        // WHEN
        var exception = assertThrows(IllegalArgumentException.class, () -> node.onMsg(ctxMock, msg));

        // THEN
        assertThat(exception.getMessage()).isEqualTo("Message body is not an object!");
        verify(ctxMock, never()).tellSuccess(any());
    }

    @Test
    public void givenValidMsgAndFetchToData_whenOnMsg_thenShouldTellSuccessAndFetchToData() throws TbNodeException, ExecutionException, InterruptedException {
        // GIVEN
        var device = new Device();
        device.setId(DUMMY_DEVICE_ORIGINATOR);
        device.setName("Test device");
        device.setType("Test device type");

        config.setDataMapping(Map.of(
                "name", "originatorName",
                "type", "originatorType",
                "label", "originatorLabel"));
        config.setIgnoreNullStrings(true);
        config.setFetchTo(TbMsgSource.DATA);

        node.config = config;
        node.fetchTo = TbMsgSource.DATA;
        var msgMetaData = new TbMsgMetaData();
        var msgData = "{\"temp\":42,\"humidity\":77}";
        msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DUMMY_DEVICE_ORIGINATOR)
                .copyMetaData(msgMetaData)
                .data(msgData)
                .build();

        when(ctxMock.getDeviceService()).thenReturn(deviceServiceMock);
        when(ctxMock.getTenantId()).thenReturn(DUMMY_TENANT_ID);
        when(deviceServiceMock.findDeviceById(eq(DUMMY_TENANT_ID), eq(device.getId()))).thenReturn(device);

        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctxMock, times(1)).tellSuccess(actualMessageCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgData = "{\"temp\":42,\"humidity\":77,\"originatorName\":\"Test device\",\"originatorType\":\"Test device type\"}";

        assertThat(actualMessageCaptor.getValue().getData()).isEqualTo(expectedMsgData);
        assertThat(actualMessageCaptor.getValue().getMetaData()).isEqualTo(msgMetaData);
    }

    @Test
    public void givenDeviceWithEmptyLabel_whenOnMsg_thenShouldTellSuccessAndFetchToData() throws TbNodeException, ExecutionException, InterruptedException {
        // GIVEN
        var device = new Device();
        device.setId(DUMMY_DEVICE_ORIGINATOR);
        device.setName("Test device");
        device.setType("Test device type");
        device.setLabel("");

        config.setDataMapping(Map.of(
                "name", "originatorName",
                "type", "originatorType",
                "label", "originatorLabel"));
        config.setIgnoreNullStrings(true);
        config.setFetchTo(TbMsgSource.DATA);

        node.config = config;
        node.fetchTo = TbMsgSource.DATA;
        var msgMetaData = new TbMsgMetaData();
        var msgData = "{\"temp\":42,\"humidity\":77}";
        msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DUMMY_DEVICE_ORIGINATOR)
                .copyMetaData(msgMetaData)
                .data(msgData)
                .build();

        when(ctxMock.getDeviceService()).thenReturn(deviceServiceMock);
        when(ctxMock.getTenantId()).thenReturn(DUMMY_TENANT_ID);
        when(deviceServiceMock.findDeviceById(eq(DUMMY_TENANT_ID), eq(device.getId()))).thenReturn(device);

        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctxMock, times(1)).tellSuccess(actualMessageCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgData = "{\"temp\":42,\"humidity\":77,\"originatorName\":\"Test device\",\"originatorType\":\"Test device type\"}";

        assertThat(actualMessageCaptor.getValue().getData()).isEqualTo(expectedMsgData);
        assertThat(actualMessageCaptor.getValue().getMetaData()).isEqualTo(msgMetaData);
    }

    @Test
    public void givenValidMsgAndFetchToMetaData_whenOnMsg_thenShouldTellSuccessAndFetchToMetaData() throws TbNodeException, ExecutionException, InterruptedException {
        // GIVEN
        var device = new Device();
        device.setId(DUMMY_DEVICE_ORIGINATOR);
        device.setName("Test device");
        device.setType("Test device type");

        config.setDataMapping(Map.of(
                "name", "originatorName",
                "type", "originatorType",
                "label", "originatorLabel"));
        config.setIgnoreNullStrings(true);
        config.setFetchTo(TbMsgSource.METADATA);

        node.config = config;
        node.fetchTo = TbMsgSource.METADATA;
        var msgMetaData = new TbMsgMetaData(Map.of(
                "testKey1", "testValue1",
                "testKey2", "123"));
        var msgData = "[\"value1\",\"value2\"]";
        msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DUMMY_DEVICE_ORIGINATOR)
                .copyMetaData(msgMetaData)
                .data(msgData)
                .build();

        when(ctxMock.getDeviceService()).thenReturn(deviceServiceMock);
        when(ctxMock.getTenantId()).thenReturn(DUMMY_TENANT_ID);
        when(deviceServiceMock.findDeviceById(eq(DUMMY_TENANT_ID), eq(device.getId()))).thenReturn(device);

        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctxMock, times(1)).tellSuccess(actualMessageCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgMetaData = new TbMsgMetaData(Map.of(
                "testKey1", "testValue1",
                "testKey2", "123",
                "originatorName", "Test device",
                "originatorType", "Test device type"
        ));

        assertThat(actualMessageCaptor.getValue().getData()).isEqualTo(msgData);
        assertThat(actualMessageCaptor.getValue().getMetaData()).isEqualTo(expectedMsgMetaData);
    }

    @Test
    public void givenNullEntityFieldsAndIgnoreNullStringsFalse_whenOnMsg_thenShouldTellSuccessAndFetchNullField() throws TbNodeException, ExecutionException, InterruptedException {
        // GIVEN
        var device = new Device();
        device.setId(DUMMY_DEVICE_ORIGINATOR);
        device.setName("Test device");
        device.setType("Test device type");

        config.setDataMapping(Map.of(
                "name", "originatorName",
                "type", "originatorType",
                "label", "originatorLabel"));
        config.setIgnoreNullStrings(false);
        config.setFetchTo(TbMsgSource.METADATA);

        node.config = config;
        node.fetchTo = TbMsgSource.METADATA;
        var msgMetaData = new TbMsgMetaData(Map.of(
                "testKey1", "testValue1",
                "testKey2", "123"));
        var msgData = "[\"value1\",\"value2\"]";
        msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DUMMY_DEVICE_ORIGINATOR)
                .copyMetaData(msgMetaData)
                .data(msgData)
                .build();

        when(ctxMock.getDeviceService()).thenReturn(deviceServiceMock);
        when(ctxMock.getTenantId()).thenReturn(DUMMY_TENANT_ID);
        when(deviceServiceMock.findDeviceById(eq(DUMMY_TENANT_ID), eq(device.getId()))).thenReturn(device);

        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctxMock, times(1)).tellSuccess(actualMessageCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgMetaData = new TbMsgMetaData(Map.of(
                "testKey1", "testValue1",
                "testKey2", "123",
                "originatorName", "Test device",
                "originatorType", "Test device type",
                "originatorLabel", "null"
        ));

        assertThat(actualMessageCaptor.getValue().getData()).isEqualTo(msgData);
        assertThat(actualMessageCaptor.getValue().getMetaData()).isEqualTo(expectedMsgMetaData);
    }

    @Test
    public void givenEmptyFieldsMapping_whenInit_thenException() {
        // GIVEN
        config.setDataMapping(Collections.emptyMap());
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        // WHEN
        var exception = assertThrows(TbNodeException.class, () -> node.init(ctxMock, nodeConfiguration));

        // THEN
        assertThat(exception.getMessage()).isEqualTo("At least one mapping entry should be specified!");
        verify(ctxMock, never()).tellSuccess(any());
    }

    @Test
    public void givenUnsupportedEntityType_whenOnMsg_thenShouldTellFailureWithSameMsg() throws TbNodeException, ExecutionException, InterruptedException {
        // GIVEN
        config.setDataMapping(Map.of(
                "name", "originatorName",
                "type", "originatorType",
                "label", "originatorLabel"));
        config.setIgnoreNullStrings(false);
        config.setFetchTo(TbMsgSource.METADATA);

        node.config = config;
        node.fetchTo = TbMsgSource.METADATA;
        var msgMetaData = new TbMsgMetaData(Map.of(
                "testKey1", "testValue1",
                "testKey2", "123"));
        var msgData = "[\"value1\",\"value2\"]";
        msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(new DashboardId(UUID.randomUUID()))
                .copyMetaData(msgMetaData)
                .data(msgData)
                .build();

        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctxMock, times(1)).tellFailure(actualMessageCaptor.capture(), any());
        verify(ctxMock, never()).tellSuccess(any());

        assertThat(actualMessageCaptor.getValue().getData()).isEqualTo(msgData);
        assertThat(actualMessageCaptor.getValue().getMetaData()).isEqualTo(msgMetaData);
    }

    @Test
    public void givenOldConfig_whenUpgrade_thenShouldReturnTrueResultWithNewConfig() throws Exception {
        var defaultConfig = new TbGetOriginatorFieldsConfiguration().defaultConfiguration();
        var node = new TbGetOriginatorFieldsNode();
        String oldConfig = "{\"fieldsMapping\":{\"name\":\"originatorName\",\"type\":\"originatorType\"},\"ignoreNullStrings\":false}";
        JsonNode configJson = JacksonUtil.toJsonNode(oldConfig);
        TbPair<Boolean, JsonNode> upgrade = node.upgrade(0, configJson);
        Assertions.assertTrue(upgrade.getFirst());
        Assertions.assertEquals(defaultConfig, JacksonUtil.treeToValue(upgrade.getSecond(), defaultConfig.getClass()));
    }

}
