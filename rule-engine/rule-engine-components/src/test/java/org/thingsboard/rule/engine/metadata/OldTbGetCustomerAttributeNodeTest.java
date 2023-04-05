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

import com.datastax.oss.driver.api.core.uuid.Uuids;
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
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
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
public class OldTbGetCustomerAttributeNodeTest extends OldTbAbstractAttributeNodeTest {

    User user = new User();
    Asset asset = new Asset();
    Device device = new Device();

    @Before
    public void initDataForTests() throws TbNodeException {
        init(new TbGetCustomerAttributeNode());
        user.setCustomerId(customerId);
        user.setId(new UserId(UUID.randomUUID()));

        asset.setCustomerId(customerId);
        asset.setId(new AssetId(UUID.randomUUID()));

        device.setCustomerId(customerId);
        device.setId(new DeviceId(Uuids.timeBased()));
    }

    @Override
    protected TbAbstractGetEntityAttrNode<CustomerId> getEmptyNode() {
        return new TbGetCustomerAttributeNode();
    }

    @Override
    protected EntityId getEntityId() {
        return customerId;
    }

    @Test
    public void errorThrownIfFetchToIsNull() {
        var node = new TbGetCustomerAttributeNode();
        var config = new TbGetEntityAttrNodeConfiguration().defaultConfiguration();
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
        msg = TbMsg.newMsg("SOME_MESSAGE_TYPE", new CustomerId(UUID.randomUUID()), new TbMsgMetaData(), "[]");

        var exception = assertThrows(IllegalArgumentException.class, () -> node.onMsg(ctxMock, msg));

        assertThat(exception.getMessage()).isEqualTo("Message body is not an object!");
        verify(ctxMock, never()).tellSuccess(any());
    }

    @Test
    public void errorThrownIfCannotLoadAttributes() {
        mockFindUser(user);
        errorThrownIfCannotLoadAttributes(user);
    }

    @Test
    public void errorThrownIfCannotLoadAttributesAsync() {
        mockFindUser(user);
        errorThrownIfCannotLoadAttributesAsync(user);
    }

    @Test
    public void failedChainUsedIfCustomerCannotBeFound() {
        when(ctxMock.getUserService()).thenReturn(userServiceMock);
        when(userServiceMock.findUserByIdAsync(any(), eq(user.getId()))).thenReturn(Futures.immediateFuture(null));
        failedChainUsedIfCustomerCannotBeFound(user);
    }

    @Test
    public void customerAttributeAddedInMetadata() {
        entityAttributeAddedInMetadata(customerId, "CUSTOMER");
    }

    @Test
    public void customerAttributeAddedInData() {
        node.fetchTo = FetchTo.DATA;
        node.config.setFetchTo(FetchTo.DATA);

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
        mockFindUser(user);
        usersCustomerAttributesFetched(user);
    }

    @Test
    public void assetsCustomerAttributesFetched() {
        mockFindAsset(asset);
        assetsCustomerAttributesFetched(asset);
    }

    @Test
    public void deviceCustomerAttributesFetched() {
        mockFindDevice(device);
        deviceCustomerAttributesFetched(device);
    }

    @Test
    public void deviceCustomerTelemetryFetched() throws TbNodeException {
        mockFindDevice(device);
        deviceCustomerTelemetryFetched(device);
    }

}
