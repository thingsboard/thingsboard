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
package org.thingsboard.rule.engine.metadata;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.AbstractListeningExecutor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TbGetAttributesNodeTest {

    private static final EntityId ORIGINATOR = new DeviceId(Uuids.timeBased());
    private static final TenantId TENANT_ID = TenantId.fromUUID(Uuids.timeBased());
    private AbstractListeningExecutor dbExecutor;

    @Mock
    private TbContext ctxMock;
    @Mock
    private AttributesService attributesServiceMock;
    @Mock
    private TimeseriesService timeseriesServiceMock;

    private List<String> clientAttributes;
    private List<String> serverAttributes;
    private List<String> sharedAttributes;
    private List<String> tsKeys;
    private long ts;
    private TbGetAttributesNode node;

    @Before
    public void before() throws TbNodeException {
        dbExecutor = new AbstractListeningExecutor() {
            @Override
            protected int getThreadPollSize() {
                return 3;
            }
        };
        dbExecutor.init();

        when(ctxMock.getAttributesService()).thenReturn(attributesServiceMock);
        when(ctxMock.getTimeseriesService()).thenReturn(timeseriesServiceMock);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getDbCallbackExecutor()).thenReturn(dbExecutor);

        clientAttributes = getAttributeNames("client");
        serverAttributes = getAttributeNames("server");
        sharedAttributes = getAttributeNames("shared");
        tsKeys = List.of("temperature", "humidity", "unknown");
        ts = System.currentTimeMillis();

        when(attributesServiceMock.find(TENANT_ID, ORIGINATOR, DataConstants.CLIENT_SCOPE, clientAttributes))
                .thenReturn(Futures.immediateFuture(getListAttributeKvEntry(clientAttributes, ts)));

        when(attributesServiceMock.find(TENANT_ID, ORIGINATOR, DataConstants.SERVER_SCOPE, serverAttributes))
                .thenReturn(Futures.immediateFuture(getListAttributeKvEntry(serverAttributes, ts)));

        when(attributesServiceMock.find(TENANT_ID, ORIGINATOR, DataConstants.SHARED_SCOPE, sharedAttributes))
                .thenReturn(Futures.immediateFuture(getListAttributeKvEntry(sharedAttributes, ts)));

        when(timeseriesServiceMock.findLatest(TENANT_ID, ORIGINATOR, tsKeys))
                .thenReturn(Futures.immediateFuture(getListTsKvEntry(tsKeys, ts)));
    }

    @After
    public void after() {
        dbExecutor.destroy();
    }

    @Test
    public void givenFetchAttributesToMetadata_whenOnMsg_thenShouldTellSuccess() throws Exception {
        // GIVEN
        node = initNode(FetchTo.METADATA, false, false);
        var msg = getTbMsg(ORIGINATOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var resultMsg = checkMsg(true);

        checkAttributes(resultMsg, FetchTo.METADATA, "cs_", clientAttributes);
        checkAttributes(resultMsg, FetchTo.METADATA, "ss_", serverAttributes);
        checkAttributes(resultMsg, FetchTo.METADATA, "shared_", sharedAttributes);

        checkTs(resultMsg, FetchTo.METADATA, false, tsKeys);
    }

    @Test
    public void givenFetchLatestTimeseriesToMetadata_whenOnMsg_thenShouldTellSuccess() throws Exception {
        // GIVEN
        node = initNode(FetchTo.METADATA, true, false);
        var msg = getTbMsg(ORIGINATOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var resultMsg = checkMsg(true);

        checkAttributes(resultMsg, FetchTo.METADATA, "cs_", clientAttributes);
        checkAttributes(resultMsg, FetchTo.METADATA, "ss_", serverAttributes);
        checkAttributes(resultMsg, FetchTo.METADATA, "shared_", sharedAttributes);

        checkTs(resultMsg, FetchTo.METADATA, true, tsKeys);
    }

    @Test
    public void givenFetchAttributesToData_whenOnMsg_thenShouldTellSuccess() throws Exception {
        // GIVEN
        node = initNode(FetchTo.DATA, false, false);
        var msg = getTbMsg(ORIGINATOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var resultMsg = checkMsg(true);

        checkAttributes(resultMsg, FetchTo.DATA, "cs_", clientAttributes);
        checkAttributes(resultMsg, FetchTo.DATA, "ss_", serverAttributes);
        checkAttributes(resultMsg, FetchTo.DATA, "shared_", sharedAttributes);

        checkTs(resultMsg, FetchTo.DATA, false, tsKeys);
    }

    @Test
    public void givenFetchLatestTimeseriesToData_whenOnMsg_thenShouldTellSuccess() throws Exception {
        // GIVEN
        node = initNode(FetchTo.DATA, true, false);
        var msg = getTbMsg(ORIGINATOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var resultMsg = checkMsg(true);

        checkAttributes(resultMsg, FetchTo.DATA, "cs_", clientAttributes);
        checkAttributes(resultMsg, FetchTo.DATA, "ss_", serverAttributes);
        checkAttributes(resultMsg, FetchTo.DATA, "shared_", sharedAttributes);

        checkTs(resultMsg, FetchTo.DATA, true, tsKeys);
    }

    @Test
    public void givenFetchAttributesToMetadata_whenOnMsg_thenShouldTellFailure() throws Exception {
        // GIVEN
        node = initNode(FetchTo.METADATA, false, true);
        var msg = getTbMsg(ORIGINATOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMsg = checkMsg(false);

        checkAttributes(actualMsg, FetchTo.METADATA, "cs_", clientAttributes);
        checkAttributes(actualMsg, FetchTo.METADATA, "ss_", serverAttributes);
        checkAttributes(actualMsg, FetchTo.METADATA, "shared_", sharedAttributes);

        checkTs(actualMsg, FetchTo.METADATA, false, tsKeys);
    }

    @Test
    public void givenFetchLatestTimeseriesToData_whenOnMsg_thenShouldTellFailure() throws Exception {
        // GIVEN
        node = initNode(FetchTo.DATA, true, true);
        var msg = getTbMsg(ORIGINATOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMsg = checkMsg(false);

        checkAttributes(actualMsg, FetchTo.DATA, "cs_", clientAttributes);
        checkAttributes(actualMsg, FetchTo.DATA, "ss_", serverAttributes);
        checkAttributes(actualMsg, FetchTo.DATA, "shared_", sharedAttributes);

        checkTs(actualMsg, FetchTo.DATA, true, tsKeys);
    }

    @Test
    public void givenFetchLatestTimeseriesToDataAndDataIsNotJsonObject_whenOnMsg_thenException() throws Exception {
        // GIVEN
        node = initNode(FetchTo.DATA, true, true);
        var msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, ORIGINATOR, TbMsgMetaData.EMPTY, "[]");

        // WHEN
        var exception = assertThrows(IllegalArgumentException.class, () -> node.onMsg(ctxMock, msg));

        // THEN
        verify(ctxMock, never()).tellSuccess(any());
        assertThat(exception.getMessage()).isEqualTo("Message body is not an object!");
    }

    @Test
    public void givenOldConfig_whenUpgrade_thenShouldReturnTrueResultWithNewConfig() throws Exception {
        var defaultConfig = new TbGetAttributesNodeConfiguration().defaultConfiguration();
        var node = new TbGetAttributesNode();
        String oldConfig = "{\"fetchToData\":false," +
                "\"clientAttributeNames\":[]," +
                "\"sharedAttributeNames\":[]," +
                "\"serverAttributeNames\":[]," +
                "\"latestTsKeyNames\":[]," +
                "\"tellFailureIfAbsent\":true," +
                "\"getLatestValueWithTs\":false}";
        JsonNode configJson = JacksonUtil.toJsonNode(oldConfig);
        TbPair<Boolean, JsonNode> upgrade = node.upgrade(0, configJson);
        Assertions.assertTrue(upgrade.getFirst());
        Assertions.assertEquals(defaultConfig, JacksonUtil.treeToValue(upgrade.getSecond(), defaultConfig.getClass()));
    }

    @Test
    public void givenOldConfigWithNoFetchToDataProperty_whenUpgrade_thenShouldReturnTrueResultWithNewConfig() throws Exception {
        var defaultConfig = new TbGetAttributesNodeConfiguration().defaultConfiguration();
        var node = new TbGetAttributesNode();
        String oldConfig = "{\"clientAttributeNames\":[]," +
                "\"sharedAttributeNames\":[]," +
                "\"serverAttributeNames\":[]," +
                "\"latestTsKeyNames\":[]," +
                "\"tellFailureIfAbsent\":true," +
                "\"getLatestValueWithTs\":false}";
        JsonNode configJson = JacksonUtil.toJsonNode(oldConfig);
        TbPair<Boolean, JsonNode> upgrade = node.upgrade(0, configJson);
        Assertions.assertTrue(upgrade.getFirst());
        Assertions.assertEquals(defaultConfig, JacksonUtil.treeToValue(upgrade.getSecond(), defaultConfig.getClass()));
    }

    private TbMsg checkMsg(boolean checkSuccess) {
        var msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        if (checkSuccess) {
            verify(ctxMock, timeout(5000)).tellSuccess(msgCaptor.capture());
        } else {
            var exceptionCaptor = ArgumentCaptor.forClass(RuntimeException.class);
            verify(ctxMock, never()).tellSuccess(any());
            verify(ctxMock, timeout(5000)).tellFailure(msgCaptor.capture(), exceptionCaptor.capture());
            var exception = exceptionCaptor.getValue();
            assertNotNull(exception);
            assertNotNull(exception.getMessage());
            assertTrue(exception.getMessage().startsWith("The following attribute/telemetry keys is not present in the DB:"));
        }

        var resultMsg = msgCaptor.getValue();
        assertNotNull(resultMsg);
        assertNotNull(resultMsg.getMetaData());
        assertNotNull(resultMsg.getData());
        return resultMsg;
    }

    private void checkAttributes(TbMsg actualMsg, FetchTo fetchTo, String prefix, List<String> attributes) {
        var msgData = JacksonUtil.toJsonNode(actualMsg.getData());
        attributes.stream()
                .filter(attribute -> !attribute.equals("unknown"))
                .forEach(attribute -> {
                    String result = null;
                    if (FetchTo.DATA.equals(fetchTo)) {
                        result = msgData.get(prefix + attribute).asText();
                    } else if (FetchTo.METADATA.equals(fetchTo)) {
                        result = actualMsg.getMetaData().getValue(prefix + attribute);
                    }
                    assertNotNull(result);
                    assertEquals(attribute + "_value", result);
                });
    }

    private void checkTs(TbMsg actualMsg, FetchTo fetchTo, boolean getLatestValueWithTs, List<String> tsKeys) {
        var msgData = JacksonUtil.toJsonNode(actualMsg.getData());
        long value = 1L;
        for (var key : tsKeys) {
            if (key.equals("unknown")) {
                continue;
            }
            String actualValue = null;
            String expectedValue;
            if (getLatestValueWithTs) {
                expectedValue = "{\"ts\":" + ts + ",\"value\":{\"data\":" + value + "}}";
            } else {
                expectedValue = "{\"data\":" + value + "}";
            }
            if (FetchTo.DATA.equals(fetchTo)) {
                actualValue = JacksonUtil.toString(msgData.get(key));
            } else if (FetchTo.METADATA.equals(fetchTo)) {
                actualValue = actualMsg.getMetaData().getValue(key);
            }
            assertNotNull(actualValue);
            assertEquals(expectedValue, actualValue);
            value++;
        }
    }

    private TbGetAttributesNode initNode(FetchTo fetchTo, boolean getLatestValueWithTs, boolean isTellFailureIfAbsent) throws TbNodeException {
        var config = new TbGetAttributesNodeConfiguration();
        config.setClientAttributeNames(List.of("client_attr_1", "client_attr_2", "${client_attr_metadata}", "unknown"));
        config.setServerAttributeNames(List.of("server_attr_1", "server_attr_2", "${server_attr_metadata}", "unknown"));
        config.setSharedAttributeNames(List.of("shared_attr_1", "shared_attr_2", "$[shared_attr_data]", "unknown"));
        config.setLatestTsKeyNames(List.of("temperature", "humidity", "unknown"));
        config.setFetchTo(fetchTo);
        config.setGetLatestValueWithTs(getLatestValueWithTs);
        config.setTellFailureIfAbsent(isTellFailureIfAbsent);

        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        var node = new TbGetAttributesNode();
        node.init(ctxMock, nodeConfiguration);
        return node;
    }

    private TbMsg getTbMsg(EntityId entityId) {
        var msgData = JacksonUtil.newObjectNode();
        msgData.put("shared_attr_data", "shared_attr_3");

        var msgMetaData = new TbMsgMetaData();
        msgMetaData.putValue("client_attr_metadata", "client_attr_3");
        msgMetaData.putValue("server_attr_metadata", "server_attr_3");

        return TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, entityId, msgMetaData, JacksonUtil.toString(msgData));
    }

    private List<String> getAttributeNames(String prefix) {
        return List.of(prefix + "_attr_1", prefix + "_attr_2", prefix + "_attr_3", "unknown");
    }

    private List<AttributeKvEntry> getListAttributeKvEntry(List<String> attributesList, long ts) {
        return attributesList.stream()
                .filter(attribute -> !attribute.equals("unknown"))
                .map(attribute -> toAttributeKvEntry(ts, attribute))
                .collect(Collectors.toList());
    }

    private BaseAttributeKvEntry toAttributeKvEntry(long ts, String attribute) {
        return new BaseAttributeKvEntry(ts, new StringDataEntry(attribute, attribute + "_value"));
    }

    private List<TsKvEntry> getListTsKvEntry(List<String> keysList, long ts) {
        long value = 1L;
        var kvEntriesList = new ArrayList<TsKvEntry>();
        for (var key : keysList) {
            if (key.equals("unknown")) {
                continue;
            }
            String dataValue = "{\"data\":" + value + "}";
            kvEntriesList.add(new BasicTsKvEntry(ts, new JsonDataEntry(key, dataValue)));
            value++;
        }
        return kvEntriesList;
    }

}
