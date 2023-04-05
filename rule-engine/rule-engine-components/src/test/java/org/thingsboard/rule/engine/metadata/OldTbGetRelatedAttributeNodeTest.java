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

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.common.data.DataConstants.SERVER_SCOPE;

@RunWith(MockitoJUnitRunner.class)
public class OldTbGetRelatedAttributeNodeTest extends OldTbAbstractAttributeNodeTest {
    User user = new User();
    Asset asset = new Asset();
    Device device = new Device();
    @Mock
    private RelationService relationService;
    private EntityRelation entityRelation;

    @Before
    public void initDataForTests() throws TbNodeException {
        init(new TbGetRelatedAttributeNode());
        entityRelation = new EntityRelation();
        entityRelation.setTo(customerId);
        entityRelation.setType(EntityRelation.CONTAINS_TYPE);
        when(ctxMock.getRelationService()).thenReturn(relationService);

        user.setCustomerId(customerId);
        user.setId(new UserId(UUID.randomUUID()));
        entityRelation.setFrom(user.getId());

        asset.setCustomerId(customerId);
        asset.setId(new AssetId(UUID.randomUUID()));

        device.setCustomerId(customerId);
        device.setId(new DeviceId(UUID.randomUUID()));
    }

    @Override
    protected TbAbstractGetEntityAttrNode getEmptyNode() {
        return new TbGetRelatedAttributeNode();
    }

    @Override
    protected TbGetEntityAttrNodeConfiguration getTbNodeConfig() {
        return getConfig(false);
    }

    @Override
    protected TbGetEntityAttrNodeConfiguration getTbNodeConfigForTelemetry() {
        return getConfig(true);
    }

    private TbGetEntityAttrNodeConfiguration getConfig(boolean isTelemetry) {
        TbGetRelatedAttrNodeConfiguration config = new TbGetRelatedAttrNodeConfiguration();
        config = config.defaultConfiguration();
        Map<String, String> conf = new HashMap<>();
        conf.put(keyAttrConf, valueAttrConf);
        config.setAttrMapping(conf);
        config.setTelemetry(isTelemetry);
        config.setFetchTo(FetchTo.METADATA);
        return config;
    }

    @Override
    EntityId getEntityId() {
        return customerId;
    }

    @Test
    public void errorThrownIfFetchToIsNull() {
        var node = new TbGetRelatedAttributeNode();
        var config = new TbGetRelatedAttrNodeConfiguration().defaultConfiguration();
        config.setFetchTo(null);
        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        var exception = assertThrows(TbNodeException.class, () -> node.init(ctxMock, nodeConfiguration));

        assertThat(exception.getMessage()).isEqualTo("FetchTo cannot be NULL!");
        verify(ctxMock, never()).tellSuccess(any());
    }

    @Test
    public void errorThrownIfMsgDataIsNotAnObjectAndFetchToData() {
        node.fetchTo = FetchTo.DATA;
        node.config.setFetchTo(FetchTo.DATA);
        msg = TbMsg.newMsg("SOME_MESSAGE_TYPE", new DeviceId(UUID.randomUUID()), new TbMsgMetaData(), "[]");

        var exception = assertThrows(IllegalArgumentException.class, () -> node.onMsg(ctxMock, msg));

        assertThat(exception.getMessage()).isEqualTo("Message body is not an object!");
        verify(ctxMock, never()).tellSuccess(any());
    }

    @Test
    public void errorThrownIfCannotLoadAttributes() {
        entityRelation.setFrom(user.getId());
        entityRelation.setTo(customerId);
        when(relationService.findByQuery(any(), any())).thenReturn(Futures.immediateFuture(List.of(entityRelation)));
        errorThrownIfCannotLoadAttributes(user);
    }

    @Test
    public void errorThrownIfCannotLoadAttributesAsync() {
        entityRelation.setFrom(user.getId());
        entityRelation.setTo(customerId);
        when(relationService.findByQuery(any(), any())).thenReturn(Futures.immediateFuture(List.of(entityRelation)));
        errorThrownIfCannotLoadAttributesAsync(user);
    }

    @Test
    public void failedChainUsedIfCustomerCannotBeFound() {
        entityRelation.setFrom(customerId);
        entityRelation.setTo(null);
        when(relationService.findByQuery(any(), any())).thenReturn(Futures.immediateFuture(List.of(entityRelation)));
        failedChainUsedIfCustomerCannotBeFound(user);
    }

    @Test
    public void customerAttributeAddedInMetadata() {
        entityRelation.setFrom(customerId);
        entityRelation.setTo(customerId);
        when(relationService.findByQuery(any(), any())).thenReturn(Futures.immediateFuture(List.of(entityRelation)));
        entityAttributeAddedInMetadata(customerId, "CUSTOMER");
    }

    @Test
    public void customerAttributeAddedInData() {
        node.fetchTo = FetchTo.DATA;
        node.config.setFetchTo(FetchTo.DATA);

        entityRelation.setFrom(customerId);
        entityRelation.setTo(customerId);
        when(relationService.findByQuery(any(), any())).thenReturn(Futures.immediateFuture(List.of(entityRelation)));

        msg = TbMsg.newMsg("CUSTOMER", customerId, new TbMsgMetaData(metaData), TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);

        List<AttributeKvEntry> attributes = Lists.newArrayList(new BaseAttributeKvEntry(new StringDataEntry("temperature", "high"), 1L));

        when(ctxMock.getAttributesService()).thenReturn(attributesServiceMock);
        when(attributesServiceMock.find(any(), eq(customerId), eq(SERVER_SCOPE), anyCollection()))
                .thenReturn(Futures.immediateFuture(attributes));

        node.onMsg(ctxMock, msg);

        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctxMock, times(1)).tellSuccess(actualMessageCaptor.capture());

        var expectedMsgData = "{\"answer\":\"high\"}";

        assertThat(actualMessageCaptor.getValue().getData()).isEqualTo(expectedMsgData);
    }

    @Test
    public void usersCustomerAttributesFetched() {
        entityRelation.setFrom(user.getId());
        entityRelation.setTo(customerId);
        when(relationService.findByQuery(any(), any())).thenReturn(Futures.immediateFuture(List.of(entityRelation)));
        usersCustomerAttributesFetched(user);
    }

    @Test
    public void assetsCustomerAttributesFetched() {
        entityRelation.setFrom(asset.getId());
        entityRelation.setTo(customerId);
        when(relationService.findByQuery(any(), any())).thenReturn(Futures.immediateFuture(List.of(entityRelation)));
        assetsCustomerAttributesFetched(asset);
    }

    @Test
    public void deviceCustomerAttributesFetched() {
        entityRelation.setFrom(device.getId());
        entityRelation.setTo(customerId);
        when(relationService.findByQuery(any(), any())).thenReturn(Futures.immediateFuture(List.of(entityRelation)));
        deviceCustomerAttributesFetched(device);
    }

    @Test
    public void deviceCustomerTelemetryFetched() throws TbNodeException {
        entityRelation.setFrom(device.getId());
        entityRelation.setTo(customerId);
        when(relationService.findByQuery(any(), any())).thenReturn(Futures.immediateFuture(List.of(entityRelation)));
        deviceCustomerTelemetryFetched(device);
    }
}
