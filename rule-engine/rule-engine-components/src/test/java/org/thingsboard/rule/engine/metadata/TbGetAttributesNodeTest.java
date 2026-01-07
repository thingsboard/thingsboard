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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.AbstractListeningExecutor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.AttributeScope;
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
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TbGetAttributesNodeTest extends AbstractRuleNodeUpgradeTest {

    private final EntityId ORIGINATOR_ID = new DeviceId(UUID.fromString("965f2975-787a-4f21-87e6-9aa4738186ff"));
    private final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("befd3239-79b8-4263-a8d1-95b69f44f798"));
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

    @Spy
    private TbGetAttributesNode node;

    @BeforeEach
    public void before() throws TbNodeException {
        dbExecutor = new AbstractListeningExecutor() {
            @Override
            protected int getThreadPollSize() {
                return 3;
            }
        };
        dbExecutor.init();

        lenient().when(ctxMock.getAttributesService()).thenReturn(attributesServiceMock);
        lenient().when(ctxMock.getTimeseriesService()).thenReturn(timeseriesServiceMock);
        lenient().when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        lenient().when(ctxMock.getDbCallbackExecutor()).thenReturn(dbExecutor);

        clientAttributes = getAttributeNames("client");
        serverAttributes = getAttributeNames("server");
        sharedAttributes = getAttributeNames("shared");
        tsKeys = List.of("temperature", "humidity", "unknown");
        ts = System.currentTimeMillis();

        lenient().when(attributesServiceMock.find(TENANT_ID, ORIGINATOR_ID, AttributeScope.CLIENT_SCOPE, clientAttributes))
                .thenReturn(Futures.immediateFuture(getListAttributeKvEntry(clientAttributes, ts)));

        lenient().when(attributesServiceMock.find(TENANT_ID, ORIGINATOR_ID, AttributeScope.SERVER_SCOPE, serverAttributes))
                .thenReturn(Futures.immediateFuture(getListAttributeKvEntry(serverAttributes, ts)));

        lenient().when(attributesServiceMock.find(TENANT_ID, ORIGINATOR_ID, AttributeScope.SHARED_SCOPE, sharedAttributes))
                .thenReturn(Futures.immediateFuture(getListAttributeKvEntry(sharedAttributes, ts)));

        lenient().when(timeseriesServiceMock.findLatest(TENANT_ID, ORIGINATOR_ID, tsKeys))
                .thenReturn(Futures.immediateFuture(getListTsKvEntry(tsKeys, ts)));
    }

    @AfterEach
    public void after() {
        dbExecutor.destroy();
    }

    @Test
    public void givenFetchAttributesToMetadata_whenOnMsg_thenShouldTellSuccess() throws Exception {
        // GIVEN
        node = initNode(TbMsgSource.METADATA, false, false);
        var msg = getTbMsg(ORIGINATOR_ID);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var resultMsg = checkMsg(true);

        checkAttributes(resultMsg, TbMsgSource.METADATA, "cs_", clientAttributes);
        checkAttributes(resultMsg, TbMsgSource.METADATA, "ss_", serverAttributes);
        checkAttributes(resultMsg, TbMsgSource.METADATA, "shared_", sharedAttributes);

        checkTs(resultMsg, TbMsgSource.METADATA, false, tsKeys);
    }

    @Test
    public void givenFetchLatestTimeseriesToMetadata_whenOnMsg_thenShouldTellSuccess() throws Exception {
        // GIVEN
        node = initNode(TbMsgSource.METADATA, true, false);
        var msg = getTbMsg(ORIGINATOR_ID);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var resultMsg = checkMsg(true);

        checkAttributes(resultMsg, TbMsgSource.METADATA, "cs_", clientAttributes);
        checkAttributes(resultMsg, TbMsgSource.METADATA, "ss_", serverAttributes);
        checkAttributes(resultMsg, TbMsgSource.METADATA, "shared_", sharedAttributes);

        checkTs(resultMsg, TbMsgSource.METADATA, true, tsKeys);
    }

    @Test
    public void givenFetchAttributesToData_whenOnMsg_thenShouldTellSuccess() throws Exception {
        // GIVEN
        node = initNode(TbMsgSource.DATA, false, false);
        var msg = getTbMsg(ORIGINATOR_ID);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var resultMsg = checkMsg(true);

        checkAttributes(resultMsg, TbMsgSource.DATA, "cs_", clientAttributes);
        checkAttributes(resultMsg, TbMsgSource.DATA, "ss_", serverAttributes);
        checkAttributes(resultMsg, TbMsgSource.DATA, "shared_", sharedAttributes);

        checkTs(resultMsg, TbMsgSource.DATA, false, tsKeys);
    }

    @Test
    public void givenFetchLatestTimeseriesToData_whenOnMsg_thenShouldTellSuccess() throws Exception {
        // GIVEN
        node = initNode(TbMsgSource.DATA, true, false);
        var msg = getTbMsg(ORIGINATOR_ID);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var resultMsg = checkMsg(true);

        checkAttributes(resultMsg, TbMsgSource.DATA, "cs_", clientAttributes);
        checkAttributes(resultMsg, TbMsgSource.DATA, "ss_", serverAttributes);
        checkAttributes(resultMsg, TbMsgSource.DATA, "shared_", sharedAttributes);

        checkTs(resultMsg, TbMsgSource.DATA, true, tsKeys);
    }

    @Test
    public void givenFetchAttributesToMetadata_whenOnMsg_thenShouldTellFailure() throws Exception {
        // GIVEN
        node = initNode(TbMsgSource.METADATA, false, true);
        var msg = getTbMsg(ORIGINATOR_ID);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMsg = checkMsg(false);

        checkAttributes(actualMsg, TbMsgSource.METADATA, "cs_", clientAttributes);
        checkAttributes(actualMsg, TbMsgSource.METADATA, "ss_", serverAttributes);
        checkAttributes(actualMsg, TbMsgSource.METADATA, "shared_", sharedAttributes);

        checkTs(actualMsg, TbMsgSource.METADATA, false, tsKeys);
    }

    @Test
    public void givenFetchLatestTimeseriesToData_whenOnMsg_thenShouldTellFailure() throws Exception {
        // GIVEN
        node = initNode(TbMsgSource.DATA, true, true);
        var msg = getTbMsg(ORIGINATOR_ID);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMsg = checkMsg(false);

        checkAttributes(actualMsg, TbMsgSource.DATA, "cs_", clientAttributes);
        checkAttributes(actualMsg, TbMsgSource.DATA, "ss_", serverAttributes);
        checkAttributes(actualMsg, TbMsgSource.DATA, "shared_", sharedAttributes);

        checkTs(actualMsg, TbMsgSource.DATA, true, tsKeys);
    }

    @Test
    public void givenFetchLatestTimeseriesToDataAndDataIsNotJsonObject_whenOnMsg_thenException() throws Exception {
        // GIVEN
        node = initNode(TbMsgSource.DATA, true, true);
        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(ORIGINATOR_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_ARRAY)
                .build();

        // WHEN
        var exception = assertThrows(IllegalArgumentException.class, () -> node.onMsg(ctxMock, msg));

        // THEN
        verify(ctxMock, never()).tellSuccess(any());
        assertThat(exception.getMessage()).isEqualTo("Message body is not an object!");
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

    private void checkAttributes(TbMsg actualMsg, TbMsgSource fetchTo, String prefix, List<String> attributes) {
        var msgData = JacksonUtil.toJsonNode(actualMsg.getData());
        attributes.stream()
                .filter(attribute -> !attribute.equals("unknown"))
                .forEach(attribute -> {
                    String result = null;
                    if (TbMsgSource.DATA.equals(fetchTo)) {
                        result = msgData.get(prefix + attribute).asText();
                    } else if (TbMsgSource.METADATA.equals(fetchTo)) {
                        result = actualMsg.getMetaData().getValue(prefix + attribute);
                    }
                    assertNotNull(result);
                    assertEquals(attribute + "_value", result);
                });
    }

    private void checkTs(TbMsg actualMsg, TbMsgSource fetchTo, boolean getLatestValueWithTs, List<String> tsKeys) {
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
            if (TbMsgSource.DATA.equals(fetchTo)) {
                actualValue = JacksonUtil.toString(msgData.get(key));
            } else if (TbMsgSource.METADATA.equals(fetchTo)) {
                actualValue = actualMsg.getMetaData().getValue(key);
            }
            assertNotNull(actualValue);
            assertEquals(expectedValue, actualValue);
            value++;
        }
    }

    private TbGetAttributesNode initNode(TbMsgSource fetchTo, boolean getLatestValueWithTs, boolean isTellFailureIfAbsent) throws TbNodeException {
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

        return TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(entityId)
                .copyMetaData(msgMetaData)
                .data(JacksonUtil.toString(msgData))
                .build();
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

    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                // config for version 1 with upgrade from version 0
                Arguments.of(0,
                        """
                                {
                                "fetchToData":false,
                                "clientAttributeNames":[],
                                "sharedAttributeNames":[],
                                "serverAttributeNames":[],
                                "latestTsKeyNames":[],
                                "tellFailureIfAbsent":true,
                                "getLatestValueWithTs":false
                                }
                        """,
                        true,
                        """
                                {
                                "tellFailureIfAbsent": true,
                                "fetchTo": "METADATA",
                                "clientAttributeNames": [],
                                "sharedAttributeNames": [],
                                "serverAttributeNames": [],
                                "latestTsKeyNames": [],
                                "getLatestValueWithTs": false
                                }
                        """
                ),
                // config for version 1 with upgrade from version 0 (old config with no fetchToData property)
                Arguments.of(0,
                        """
                                {
                                "clientAttributeNames":[],
                                "sharedAttributeNames":[],"serverAttributeNames":[],
                                "latestTsKeyNames":[],
                                "tellFailureIfAbsent":true,
                                "getLatestValueWithTs":false
                                }
                        """,
                        true,
                        """
                                {
                                "tellFailureIfAbsent": true,
                                "fetchTo": "METADATA",
                                "clientAttributeNames": [],
                                "sharedAttributeNames": [],
                                "serverAttributeNames": [],
                                "latestTsKeyNames": [],
                                "getLatestValueWithTs": false
                                }
                        """
                ),
                // config for version 1 with upgrade from version 0 (old config with null fetchToData property)
                Arguments.of(0,
                        """
                                {
                                "fetchToData":null,
                                "clientAttributeNames":[],
                                "sharedAttributeNames":[],
                                "serverAttributeNames":[],
                                "latestTsKeyNames":[],
                                "tellFailureIfAbsent":true,
                                "getLatestValueWithTs":false
                                }
                        """,
                        true,
                        """
                                {
                                "tellFailureIfAbsent": true,
                                "fetchTo": "METADATA",
                                "clientAttributeNames": [],
                                "sharedAttributeNames": [],
                                "serverAttributeNames": [],
                                "latestTsKeyNames": [],
                                "getLatestValueWithTs": false
                                }
                        """
                ),
                // config for version 1 with upgrade from version 1
                Arguments.of(1,
                        """
                                {
                                "tellFailureIfAbsent": true,
                                "fetchTo": "METADATA",
                                "clientAttributeNames": [],
                                "sharedAttributeNames": [],
                                "serverAttributeNames": [],
                                "latestTsKeyNames": [],
                                "getLatestValueWithTs": false
                                }
                        """,
                        false,
                        """
                                {
                                "tellFailureIfAbsent": true,
                                "fetchTo": "METADATA",
                                "clientAttributeNames": [],
                                "sharedAttributeNames": [],
                                "serverAttributeNames": [],
                                "latestTsKeyNames": [],
                                "getLatestValueWithTs": false
                                }
                        """
                )
        );

    }

    @Override
    protected TbNode getTestNode() {
        return node;
    }

}
