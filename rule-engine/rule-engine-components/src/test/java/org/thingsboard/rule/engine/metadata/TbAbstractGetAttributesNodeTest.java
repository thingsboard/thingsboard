/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
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
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

@RunWith(MockitoJUnitRunner.class)
public class TbAbstractGetAttributesNodeTest {

    final ObjectMapper mapper = new ObjectMapper();

    private EntityId originator = new DeviceId(Uuids.timeBased());
    private TenantId tenantId = TenantId.fromUUID(Uuids.timeBased());

    @Mock
    private TbContext ctx;
    @Mock
    private AttributesService attributesService;
    @Mock
    private TimeseriesService tsService;
    private AbstractListeningExecutor dbExecutor;

    private List<String> clientAttributes;
    private List<String> serverAttributes;
    private List<String> sharedAttributes;
    private List<String> tsKeys;

    @Before
    public void before() throws TbNodeException {
        dbExecutor = new AbstractListeningExecutor() {
            @Override
            protected int getThreadPollSize() {
                return 3;
            }
        };
        dbExecutor.init();

        Mockito.reset(ctx);
        Mockito.reset(attributesService);
        Mockito.reset(tsService);

        Mockito.reset(ctx);
        Mockito.reset(attributesService);
        Mockito.reset(tsService);

        lenient().when(ctx.getAttributesService()).thenReturn(attributesService);
        lenient().when(ctx.getTimeseriesService()).thenReturn(tsService);
        lenient().when(ctx.getTenantId()).thenReturn(tenantId);
        lenient().when(ctx.getDbCallbackExecutor()).thenReturn(dbExecutor);

        clientAttributes = getAttributeNames("client");
        serverAttributes = getAttributeNames("server");
        sharedAttributes = getAttributeNames("shared");
        tsKeys = List.of("temperature", "humidity", "unknown");

        Mockito.when(attributesService.find(tenantId, originator, DataConstants.CLIENT_SCOPE, clientAttributes))
                .thenReturn(Futures.immediateFuture(getListAttributeKvEntry(clientAttributes)));


        Mockito.when(attributesService.find(tenantId, originator, DataConstants.SERVER_SCOPE, serverAttributes))
                .thenReturn(Futures.immediateFuture(getListAttributeKvEntry(serverAttributes)));


        Mockito.when(attributesService.find(tenantId, originator, DataConstants.SHARED_SCOPE, sharedAttributes))
                .thenReturn(Futures.immediateFuture(getListAttributeKvEntry(sharedAttributes)));

        Mockito.when(tsService.findLatest(tenantId, originator, tsKeys))
                .thenReturn(Futures.immediateFuture(getListTelemetryKvEntry(tsKeys)));
    }

    @After
    public void after() {
        dbExecutor.destroy();
    }

    @Test
    public void fetchToMetadata_whenOnMsg_then_success() throws Exception {
        TbGetAttributesNode node = initNode(false, false, false);
        TbMsg msg = getTbMsg(originator);
        node.onMsg(ctx, msg);

        TbMsg resultMsg = checkMsg();
        TbMsgMetaData msgMetaData = resultMsg.getMetaData();

        //check attributes
        checkAttributes(clientAttributes, "cs_", false, msgMetaData, null);
        checkAttributes(serverAttributes, "ss_", false, msgMetaData, null);
        checkAttributes(sharedAttributes, "shared_", false, msgMetaData, null);

        //check timeseries
        checkTs(tsKeys, false, false, msgMetaData, null);
    }

    @Test
    public void fetchToData_whenOnMsg_then_success() throws Exception {
        TbGetAttributesNode node = initNode(true, true, false);
        TbMsg msg = getTbMsg(originator);
        node.onMsg(ctx, msg);

        TbMsg resultMsg = checkMsg();
        JsonNode msgData = JacksonUtil.toJsonNode(resultMsg.getData());

        //check attributes
        checkAttributes(clientAttributes, "cs_", true, null, msgData);
        checkAttributes(serverAttributes, "ss_", true, null, msgData);
        checkAttributes(sharedAttributes, "shared_", true, null, msgData);

        //check timeseries with ts
        checkTs(tsKeys, true, true, null, msgData);
    }

    @Test
    public void fetchToData_whenOnMsg_then_failure() throws Exception {
        TbGetAttributesNode node = initNode(true, true, true);
        TbMsg msg = getTbMsg(originator);
        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        Mockito.verify(ctx, never()).tellSuccess(any());
        Mockito.verify(ctx, Mockito.timeout(5000)).tellFailure(newMsgCaptor.capture(), exceptionCaptor.capture());

        Assert.assertSame(newMsgCaptor.getValue(), msg);
        Assert.assertNotNull(exceptionCaptor.getValue());
    }

    @Test
    public void fetchToData_whenOnMsg_then_data_not_object_failure() throws Exception {
        TbGetAttributesNode node = initNode(true, true, true);
        TbMsg msg = TbMsg.newMsg("TEST", originator, new TbMsgMetaData(), "[]");
        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        Mockito.verify(ctx, never()).tellSuccess(any());
        Mockito.verify(ctx, Mockito.timeout(5000)).tellFailure(newMsgCaptor.capture(), exceptionCaptor.capture());

        Assert.assertSame(newMsgCaptor.getValue(), msg);
        Assert.assertNotNull(exceptionCaptor.getValue());
    }

    private TbMsg checkMsg() {
        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        Mockito.verify(ctx, Mockito.timeout(5000)).tellSuccess(msgCaptor.capture());

        TbMsg resultMsg = msgCaptor.getValue();
        Assert.assertNotNull(resultMsg);
        Assert.assertNotNull(resultMsg.getMetaData());
        Assert.assertNotNull(resultMsg.getData());
        return resultMsg;
    }

    private void checkAttributes(List<String> attributes, String prefix, boolean fetchToData, TbMsgMetaData msgMetaData, JsonNode msgData) {
        attributes.stream()
                .filter(attribute -> !attribute.equals("unknown"))
                .forEach(attribute -> {
                    String result;
                    if (fetchToData) {
                        result = msgData.get(prefix + attribute).asText();
                    } else {
                        result = msgMetaData.getValue(prefix + attribute);
                    }
                    Assert.assertNotNull(result);
                    Assert.assertEquals(attribute + "_value", result);
                });
    }

    private void checkTs(List<String> tsKeys, boolean fetchToData, boolean getLatestValueWithTs, TbMsgMetaData msgMetaData, JsonNode msgData) {
        long tsValue = 1L;
        for (String key : tsKeys) {
            if (key.equals("unknown")) {
                continue;
            }
            String result;
            if (fetchToData) {
                if (getLatestValueWithTs) {
                    JsonNode resultTs = msgData.get(key);
                    Assert.assertNotNull(resultTs);
                    Assert.assertNotNull(resultTs.get("value"));
                    Assert.assertNotNull(resultTs.get("ts"));
                    result = resultTs.get("value").asText();
                } else {
                    result = msgData.get(key).asText();
                }
            } else {
                result = msgMetaData.getValue(key);
            }
            Assert.assertNotNull(result);
            Assert.assertEquals(String.valueOf(tsValue), result);
            tsValue++;
        }
    }

    private TbGetAttributesNode initNode(boolean fetchToData, boolean getLatestValueWithTs, boolean isTellFailureIfAbsent) throws TbNodeException {
        TbGetAttributesNodeConfiguration config = new TbGetAttributesNodeConfiguration();
        config.setClientAttributeNames(List.of("client_attr_1", "client_attr_2", "${client_attr_metadata}", "unknown"));
        config.setServerAttributeNames(List.of("server_attr_1", "server_attr_2", "${server_attr_metadata}", "unknown"));
        config.setSharedAttributeNames(List.of("shared_attr_1", "shared_attr_2", "$[shared_attr_data]", "unknown"));
        config.setLatestTsKeyNames(List.of("temperature", "humidity", "unknown"));
        config.setFetchToData(fetchToData);
        config.setGetLatestValueWithTs(getLatestValueWithTs);
        config.setTellFailureIfAbsent(isTellFailureIfAbsent);
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));
        TbGetAttributesNode node = new TbGetAttributesNode();
        node.init(ctx, nodeConfiguration);
        return node;
    }

    private TbMsg getTbMsg(EntityId entityId) {
        ObjectNode msgData = JacksonUtil.newObjectNode();
        msgData.put("shared_attr_data", "shared_attr_3");

        TbMsgMetaData msgMetaData = new TbMsgMetaData();
        msgMetaData.putValue("client_attr_metadata", "client_attr_3");
        msgMetaData.putValue("server_attr_metadata", "server_attr_3");

        return TbMsg.newMsg("TEST", entityId, msgMetaData, msgData.toString());
    }

    private List<String> getAttributeNames(String prefix) {
        return List.of(prefix + "_attr_1", prefix + "_attr_2", prefix + "_attr_3", "unknown");
    }

    private List<AttributeKvEntry> getListAttributeKvEntry(List<String> attributes) {
        List<AttributeKvEntry> attributeKvEntries = new ArrayList<>();
        attributes.stream().filter(attribute -> !attribute.equals("unknown")).forEach(attribute -> attributeKvEntries.add(new BaseAttributeKvEntry(System.currentTimeMillis(), new StringDataEntry(attribute, attribute + "_value"))));
        return attributeKvEntries;
    }

    private List<TsKvEntry> getListTelemetryKvEntry(List<String> keys) {
        long value = 1L;
        List<TsKvEntry> kvEntries = new ArrayList<>();
        for (String key : keys) {
            if (key.equals("unknown")) {
                continue;
            }
            kvEntries.add(new BasicTsKvEntry(System.currentTimeMillis(), new LongDataEntry(key, value)));
            value++;
        }
        return kvEntries;
    }

}
