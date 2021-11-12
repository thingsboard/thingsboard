/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.user.UserService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.rule.engine.api.TbRelationTypes.FAILURE;
import static org.thingsboard.server.common.data.DataConstants.SERVER_SCOPE;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TbGetRelatedAttributeNodeTest {
    private final CustomerId customerId = new CustomerId(Uuids.timeBased());
    private final RuleChainId ruleChainId = new RuleChainId(Uuids.timeBased());
    private final RuleNodeId ruleNodeId = new RuleNodeId(Uuids.timeBased());
    private TbGetRelatedAttributeNode node;
    @Mock
    private TbContext ctx;
    @Mock
    private AttributesService attributesService;
    @Mock
    private TimeseriesService timeseriesService;
    @Mock
    private UserService userService;
    @Mock
    private AssetService assetService;
    @Mock
    private DeviceService deviceService;
    @Mock
    private RelationService relationService;
    private TbMsg msg;
    private Map<String, String> metaData;
    private EntityRelation entityRelation;

    @Before
    public void init() throws TbNodeException {
        TbGetRelatedAttrNodeConfiguration config = new TbGetRelatedAttrNodeConfiguration();
        config = config.defaultConfiguration();
        Map<String, String> conf = new HashMap<>();
        conf.put("${word}", "result");
        config.setAttrMapping(conf);
        config.setTelemetry(false);
        ObjectMapper mapper = new ObjectMapper();
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));

        metaData = new HashMap<>();
        metaData.putIfAbsent("word", "temperature");

        entityRelation = new EntityRelation();
        entityRelation.setTo(customerId);
        entityRelation.setType(EntityRelation.CONTAINS_TYPE);
        when(ctx.getRelationService()).thenReturn(relationService);

        node = new TbGetRelatedAttributeNode();
        node.init(null, nodeConfiguration);
    }

    @Test
    public void errorThrownIfCannotLoadAttributes() {
        UserId userId = new UserId(Uuids.timeBased());
        User user = new User();
        user.setCustomerId(customerId);
        entityRelation.setFrom(userId);
        when(relationService.findByQuery(any(), any())).thenReturn(Futures.immediateFuture(List.of(entityRelation)));

        msg = TbMsg.newMsg("USER", userId, new TbMsgMetaData(), TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);

        when(ctx.getUserService()).thenReturn(userService);
        when(userService.findUserByIdAsync(any(), eq(userId))).thenReturn(Futures.immediateFuture(user));

        when(ctx.getAttributesService()).thenReturn(attributesService);
        when(attributesService.find(any(), eq(customerId), eq(SERVER_SCOPE), anyCollection()))
                .thenThrow(new IllegalStateException("something wrong"));

        node.onMsg(ctx, msg);
        final ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctx).tellFailure(same(msg), captor.capture());

        Throwable value = captor.getValue();
        assertEquals("something wrong", value.getMessage());
        assertTrue(msg.getMetaData().getData().isEmpty());
    }

    @Test
    public void errorThrownIfCannotLoadAttributesAsync() {
        UserId userId = new UserId(Uuids.timeBased());
        User user = new User();
        user.setCustomerId(customerId);
        entityRelation.setFrom(userId);
        when(relationService.findByQuery(any(), any())).thenReturn(Futures.immediateFuture(List.of(entityRelation)));

        msg = TbMsg.newMsg("USER", userId, new TbMsgMetaData(), TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);

        when(ctx.getUserService()).thenReturn(userService);
        when(userService.findUserByIdAsync(any(), eq(userId))).thenReturn(Futures.immediateFuture(user));

        when(ctx.getAttributesService()).thenReturn(attributesService);

        when(attributesService.find(any(), eq(customerId), eq(SERVER_SCOPE), anyCollection()))
                .thenReturn(Futures.immediateFailedFuture(new IllegalStateException("something wrong")));

        node.onMsg(ctx, msg);
        final ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctx).tellFailure(same(msg), captor.capture());

        Throwable value = captor.getValue();
        assertEquals("something wrong", value.getMessage());
        assertTrue(msg.getMetaData().getData().isEmpty());
    }

    @Test
    public void failedChainUsedIfCustomerCannotBeFound() {
        UserId userId = new UserId(Uuids.timeBased());
        User user = new User();
        user.setCustomerId(customerId);
        entityRelation.setFrom(customerId);
        entityRelation.setTo(null);
        when(relationService.findByQuery(any(), any())).thenReturn(Futures.immediateFuture(List.of(entityRelation)));

        msg = TbMsg.newMsg("USER", userId, new TbMsgMetaData(), TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);

        when(ctx.getUserService()).thenReturn(userService);
        when(userService.findUserByIdAsync(any(), eq(userId))).thenReturn(Futures.immediateFuture(null));


        node.onMsg(ctx, msg);
        verify(ctx).tellNext(msg, FAILURE);
        assertTrue(msg.getMetaData().getData().isEmpty());

        entityRelation.setTo(customerId);
    }

    @Test
    public void customerAttributeAddedInMetadata() {
        entityRelation.setFrom(customerId);
        when(relationService.findByQuery(any(), any())).thenReturn(Futures.immediateFuture(List.of(entityRelation)));
        msg = TbMsg.newMsg("CUSTOMER", customerId, new TbMsgMetaData(metaData), TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);
        entityAttributeFetched(customerId);
    }

    @Test
    public void usersCustomerAttributesFetched() {
        UserId userId = new UserId(Uuids.timeBased());
        User user = new User();
        user.setCustomerId(customerId);
        entityRelation.setFrom(userId);
        when(relationService.findByQuery(any(), any())).thenReturn(Futures.immediateFuture(List.of(entityRelation)));

        msg = TbMsg.newMsg("USER", userId, new TbMsgMetaData(metaData), TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);

        when(ctx.getUserService()).thenReturn(userService);
        when(userService.findUserByIdAsync(any(), eq(userId))).thenReturn(Futures.immediateFuture(user));

        entityAttributeFetched(customerId);
    }

    @Test
    public void assetsCustomerAttributesFetched() {
        AssetId assetId = new AssetId(Uuids.timeBased());
        Asset asset = new Asset();
        asset.setCustomerId(customerId);
        entityRelation.setFrom(assetId);
        when(relationService.findByQuery(any(), any())).thenReturn(Futures.immediateFuture(List.of(entityRelation)));

        msg = TbMsg.newMsg("USER", assetId, new TbMsgMetaData(metaData), TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);

        when(ctx.getAssetService()).thenReturn(assetService);
        when(assetService.findAssetByIdAsync(any(), eq(assetId))).thenReturn(Futures.immediateFuture(asset));

        entityAttributeFetched(customerId);
    }

    @Test
    public void deviceCustomerAttributesFetched() {
        DeviceId deviceId = new DeviceId(Uuids.timeBased());
        Device device = new Device();
        device.setCustomerId(customerId);
        entityRelation.setFrom(deviceId);
        when(relationService.findByQuery(any(), any())).thenReturn(Futures.immediateFuture(List.of(entityRelation)));

        msg = TbMsg.newMsg("USER", deviceId, new TbMsgMetaData(metaData), TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);

        when(ctx.getDeviceService()).thenReturn(deviceService);
        when(deviceService.findDeviceByIdAsync(any(), eq(deviceId))).thenReturn(Futures.immediateFuture(device));

        entityAttributeFetched(customerId);
    }

    @Test
    public void deviceCustomerTelemetryFetched() throws TbNodeException {
        TbGetRelatedAttrNodeConfiguration config = new TbGetRelatedAttrNodeConfiguration();
        config = config.defaultConfiguration();

        Map<String, String> conf = new HashMap<>();
        conf.put("${word}", "result");
        config.setAttrMapping(conf);
        config.setTelemetry(true);
        ObjectMapper mapper = new ObjectMapper();
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));

        node = new TbGetRelatedAttributeNode();
        node.init(null, nodeConfiguration);


        DeviceId deviceId = new DeviceId(Uuids.timeBased());
        Device device = new Device();
        device.setCustomerId(customerId);

        entityRelation.setFrom(deviceId);
        when(relationService.findByQuery(any(), any())).thenReturn(Futures.immediateFuture(List.of(entityRelation)));

        msg = TbMsg.newMsg("USER", deviceId, new TbMsgMetaData(metaData), TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);

        when(ctx.getDeviceService()).thenReturn(deviceService);
        when(deviceService.findDeviceByIdAsync(any(), eq(deviceId))).thenReturn(Futures.immediateFuture(device));

        List<TsKvEntry> timeseries = Lists.newArrayList(new BasicTsKvEntry(1L, new StringDataEntry("temperature", "highest")));

        when(ctx.getTimeseriesService()).thenReturn(timeseriesService);
        when(timeseriesService.findLatest(any(), eq(customerId), anyCollection()))
                .thenReturn(Futures.immediateFuture(timeseries));

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        assertEquals(msg.getMetaData().getValue("result"), "highest");
    }

    private void entityAttributeFetched(CustomerId customerId) {
        List<AttributeKvEntry> attributes = Lists.newArrayList(new BaseAttributeKvEntry(new StringDataEntry("temperature", "high"), 1L));

        when(ctx.getAttributesService()).thenReturn(attributesService);
        when(attributesService.find(any(), eq(customerId), eq(SERVER_SCOPE), anyCollection()))
                .thenReturn(Futures.immediateFuture(attributes));

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        assertEquals(msg.getMetaData().getValue("result"), "high");
    }
}