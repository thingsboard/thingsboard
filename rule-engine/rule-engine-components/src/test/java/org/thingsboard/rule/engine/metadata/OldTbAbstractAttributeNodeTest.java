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
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.user.UserService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.common.data.DataConstants.SERVER_SCOPE;

@RunWith(MockitoJUnitRunner.class)
public abstract class OldTbAbstractAttributeNodeTest {
    final CustomerId customerId = new CustomerId(Uuids.timeBased());
    final TenantId tenantId = TenantId.fromUUID(Uuids.timeBased());
    final RuleChainId ruleChainId = new RuleChainId(Uuids.timeBased());
    final RuleNodeId ruleNodeId = new RuleNodeId(Uuids.timeBased());
    final String keyAttrConf = "${word}";
    final String valueAttrConf = "${result}";
    @Mock
    protected TbContext ctxMock;
    @Mock
    protected AttributesService attributesServiceMock;
    @Mock
    protected TimeseriesService timeseriesServiceMock;
    @Mock
    protected UserService userServiceMock;
    @Mock
    protected AssetService assetServiceMock;
    @Mock
    protected DeviceService deviceServiceMock;
    TbMsg msg;
    Map<String, String> metaData;
    TbAbstractGetEntityAttrNode<? extends EntityId> node;

    protected void init(TbAbstractGetEntityAttrNode node) throws TbNodeException {
        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(getTbNodeConfig()));

        metaData = new HashMap<>();
        metaData.putIfAbsent("word", "temperature");
        metaData.putIfAbsent("result", "answer");

        this.node = node;
        this.node.init(null, nodeConfiguration);
    }

    void errorThrownIfCannotLoadAttributes(User user) {
        msg = TbMsg.newMsg("USER", user.getId(), new TbMsgMetaData(), TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);

        when(ctxMock.getAttributesService()).thenReturn(attributesServiceMock);
        when(attributesServiceMock.find(any(), eq(getEntityId()), eq(SERVER_SCOPE), anyCollection()))
                .thenThrow(new IllegalStateException("something wrong"));

        node.onMsg(ctxMock, msg);
        final ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctxMock).tellFailure(same(msg), captor.capture());

        Throwable value = captor.getValue();
        assertEquals("something wrong", value.getMessage());
        assertTrue(msg.getMetaData().getData().isEmpty());
    }

    void errorThrownIfCannotLoadAttributesAsync(User user) {
        msg = TbMsg.newMsg("USER", user.getId(), new TbMsgMetaData(), TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);

        when(ctxMock.getAttributesService()).thenReturn(attributesServiceMock);
        when(attributesServiceMock.find(any(), eq(getEntityId()), eq(SERVER_SCOPE), anyCollection()))
                .thenReturn(Futures.immediateFailedFuture(new IllegalStateException("something wrong")));

        node.onMsg(ctxMock, msg);
        final ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctxMock).tellFailure(same(msg), captor.capture());

        Throwable value = captor.getValue();
        assertEquals("something wrong", value.getMessage());
        assertTrue(msg.getMetaData().getData().isEmpty());
    }

    void failedChainUsedIfCustomerCannotBeFound(User user) {
        msg = TbMsg.newMsg("USER", user.getId(), new TbMsgMetaData(), TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);

        node.onMsg(ctxMock, msg);
        var exceptionCaptor = ArgumentCaptor.forClass(NoSuchElementException.class);
        verify(ctxMock).tellFailure(eq(msg), exceptionCaptor.capture());

        assertThat(exceptionCaptor.getValue().getMessage()).contains("Did not find entity! Msg ID: ");
        assertTrue(msg.getMetaData().getData().isEmpty());
    }

    void entityAttributeAddedInMetadata(EntityId entityId, String type) {
        msg = TbMsg.newMsg(type, entityId, new TbMsgMetaData(metaData), TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);
        entityAttributesFetched(getEntityId());
    }

    void usersCustomerAttributesFetched(User user) {
        msg = TbMsg.newMsg("USER", user.getId(), new TbMsgMetaData(metaData), TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);

        entityAttributesFetched(getEntityId());
    }

    void assetsCustomerAttributesFetched(Asset asset) {
        msg = TbMsg.newMsg("ASSET", asset.getId(), new TbMsgMetaData(metaData), TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);

        entityAttributesFetched(getEntityId());
    }

    void deviceCustomerAttributesFetched(Device device) {
        msg = TbMsg.newMsg("DEVICE", device.getId(), new TbMsgMetaData(metaData), TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);

        entityAttributesFetched(getEntityId());
    }

    void deviceCustomerTelemetryFetched(Device device) throws TbNodeException {
        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(getTbNodeConfigForTelemetry()));

        TbAbstractGetEntityAttrNode node = getEmptyNode();
        node.init(null, nodeConfiguration);

        msg = TbMsg.newMsg("DEVICE", device.getId(), new TbMsgMetaData(metaData), TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);

        List<TsKvEntry> timeseries = Lists.newArrayList(new BasicTsKvEntry(1L, new StringDataEntry("temperature", "highest")));

        when(ctxMock.getTimeseriesService()).thenReturn(timeseriesServiceMock);
        when(timeseriesServiceMock.findLatest(any(), eq(getEntityId()), anyCollection()))
                .thenReturn(Futures.immediateFuture(timeseries));

        node.onMsg(ctxMock, msg);
        verify(ctxMock).tellSuccess(msg);
        assertEquals(msg.getMetaData().getValue("answer"), "highest");
    }

    protected void entityAttributesFetched(EntityId entityId) {
        List<AttributeKvEntry> attributes = List.of(new BaseAttributeKvEntry(new StringDataEntry("temperature", "high"), 1L));

        when(ctxMock.getAttributesService()).thenReturn(attributesServiceMock);
        when(attributesServiceMock.find(any(), eq(entityId), eq(SERVER_SCOPE), anyCollection()))
                .thenReturn(Futures.immediateFuture(attributes));

        node.onMsg(ctxMock, msg);

        verify(ctxMock).tellSuccess(msg);
        assertEquals(msg.getMetaData().getValue("answer"), "high");
    }

    protected TbGetEntityAttrNodeConfiguration getTbNodeConfig() {
        return getConfig(false);
    }

    protected TbGetEntityAttrNodeConfiguration getTbNodeConfigForTelemetry() {
        return getConfig(true);
    }

    private TbGetEntityAttrNodeConfiguration getConfig(boolean isTelemetry) {
        TbGetEntityAttrNodeConfiguration config = new TbGetEntityAttrNodeConfiguration();
        Map<String, String> conf = new HashMap<>();
        conf.put(keyAttrConf, valueAttrConf);
        config.setAttrMapping(conf);
        config.setTelemetry(isTelemetry);
        config.setFetchTo(FetchTo.METADATA);
        return config;
    }

    protected abstract TbAbstractGetEntityAttrNode getEmptyNode();

    abstract EntityId getEntityId();

    void mockFindDevice(Device device) {
        when(ctxMock.getDeviceService()).thenReturn(deviceServiceMock);
        when(deviceServiceMock.findDeviceByIdAsync(any(), eq(device.getId()))).thenReturn(Futures.immediateFuture(device));
    }

    void mockFindAsset(Asset asset) {
        when(ctxMock.getAssetService()).thenReturn(assetServiceMock);
        when(assetServiceMock.findAssetByIdAsync(any(), eq(asset.getId()))).thenReturn(Futures.immediateFuture(asset));
    }

    void mockFindUser(User user) {
        when(ctxMock.getUserService()).thenReturn(userServiceMock);
        when(userServiceMock.findUserByIdAsync(any(), eq(user.getId()))).thenReturn(Futures.immediateFuture(user));
    }
}
