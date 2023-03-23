/**
 * Copyright © 2016-2023 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.List;
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
public class TbGetTenantAttributeNodeTest extends TbAbstractAttributeNodeTest {
    User user = new User();
    Asset asset = new Asset();
    Device device = new Device();

    @Before
    public void initDataForTests() throws TbNodeException {
        init(new TbGetTenantAttributeNode());

        user.setTenantId(tenantId);
        user.setId(new UserId(UUID.randomUUID()));

        asset.setTenantId(tenantId);
        asset.setId(new AssetId(UUID.randomUUID()));

        device.setTenantId(tenantId);
        device.setId(new DeviceId(UUID.randomUUID()));

        when(ctx.getTenantId()).thenReturn(tenantId);
    }

    @Override
    protected TbAbstractGetEntityAttrNode getEmptyNode() {
        return new TbGetTenantAttributeNode();
    }

    @Override
    EntityId getEntityId() {
        return tenantId;
    }

    @Test
    public void errorThrownIfFetchToIsNull() {
        var node = new TbGetTenantAttributeNode();
        var config = new TbGetEntityAttrNodeConfiguration().defaultConfiguration();
        config.setFetchTo(null);
        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        var exception = assertThrows(TbNodeException.class, () -> node.init(ctx, nodeConfiguration));

        assertThat(exception.getMessage()).isEqualTo("FetchTo cannot be NULL!");
        verify(ctx, never()).tellSuccess(any());
    }

    @Test
    public void errorThrownIfMsgDataIsNotAnObjectAndFetchToData() {
        node.fetchTo = FetchTo.DATA;
        node.config.setFetchTo(FetchTo.DATA);
        msg = TbMsg.newMsg("SOME_MESSAGE_TYPE", new TenantId(UUID.randomUUID()), new TbMsgMetaData(), "[]");

        var exception = assertThrows(IllegalArgumentException.class, () -> node.onMsg(ctx, msg));

        assertThat(exception.getMessage()).isEqualTo("Message body is not an object!");
        verify(ctx, never()).tellSuccess(any());
    }

    @Test
    public void errorThrownIfCannotLoadAttributes() {
        errorThrownIfCannotLoadAttributes(user);
    }

    @Test
    public void errorThrownIfCannotLoadAttributesAsync() {
        errorThrownIfCannotLoadAttributesAsync(user);
    }

    @Test
    public void failedChainUsedIfTenantIdFromCtxCannotBeFound() {
        when(ctx.getTenantId()).thenReturn(null);
        failedChainUsedIfCustomerCannotBeFound(user);
    }

    @Test
    public void customerAttributeAddedInMetadata() {
        entityAttributeAddedInMetadata(tenantId, "TENANT");
    }

    @Test
    public void customerAttributeAddedInData() {
        node.fetchTo = FetchTo.DATA;
        node.config.setFetchTo(FetchTo.DATA);

        msg = TbMsg.newMsg("TENANT", tenantId, new TbMsgMetaData(metaData), TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);

        List<AttributeKvEntry> attributes = Lists.newArrayList(new BaseAttributeKvEntry(new StringDataEntry("temperature", "high"), 1L));

        when(ctx.getAttributesService()).thenReturn(attributesService);
        when(attributesService.find(any(), eq(tenantId), eq(SERVER_SCOPE), anyCollection()))
                .thenReturn(Futures.immediateFuture(attributes));

        node.onMsg(ctx, msg);

        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellSuccess(actualMessageCaptor.capture());

        var expectedMsgData = "{\"answer\":\"high\"}";

        assertThat(actualMessageCaptor.getValue().getData()).isEqualTo(expectedMsgData);
    }

    @Test
    public void usersCustomerAttributesFetched() {
        usersCustomerAttributesFetched(user);
    }

    @Test
    public void assetsCustomerAttributesFetched() {
        assetsCustomerAttributesFetched(asset);
    }

    @Test
    public void deviceCustomerAttributesFetched() {
        deviceCustomerAttributesFetched(device);
    }

    @Test
    public void deviceCustomerTelemetryFetched() throws TbNodeException {
        deviceCustomerTelemetryFetched(device);
    }
}
