/**
 * Copyright © 2016-2018 The Thingsboard Authors
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

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.user.UserService;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.common.data.DataConstants.SERVER_SCOPE;

@RunWith(MockitoJUnitRunner.class)
public class TbGetCustomerAttributeNodeTest {

    private TbGetCustomerAttributeNode node;

    @Mock
    private TbContext ctx;

    @Mock
    private AttributesService attributesService;
    @Mock
    private UserService userService;
    @Mock
    private AssetService assetService;
    @Mock
    private DeviceService deviceService;

    private TbMsg msg;

    @Before
    public void init() throws TbNodeException {
        TbGetEntityAttrNodeConfiguration config = new TbGetEntityAttrNodeConfiguration();
        Map<String, String> attrMapping = new HashMap<>();
        attrMapping.putIfAbsent("temperature", "tempo");
        config.setAttrMapping(attrMapping);
        ObjectMapper mapper = new ObjectMapper();
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration();
        nodeConfiguration.setData(mapper.valueToTree(config));

        node = new TbGetCustomerAttributeNode();
        node.init(nodeConfiguration, null);
    }

    @Test
    public void errorThrownIfCannotLoadAttributes() {
        UserId userId = new UserId(UUIDs.timeBased());
        CustomerId customerId = new CustomerId(UUIDs.timeBased());
        User user = new User();
        user.setCustomerId(customerId);

        msg = new TbMsg(UUIDs.timeBased(), "USER", userId, new TbMsgMetaData(), new byte[4]);

        when(ctx.getUserService()).thenReturn(userService);
        when(userService.findUserByIdAsync(userId)).thenReturn(Futures.immediateFuture(user));

        when(ctx.getAttributesService()).thenReturn(attributesService);
        when(attributesService.find(customerId, SERVER_SCOPE, Collections.singleton("temperature")))
                .thenThrow(new IllegalStateException("something wrong"));

        node.onMsg(ctx, msg);
        final ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctx).tellError(same(msg), captor.capture());

        Throwable value = captor.getValue();
        assertEquals("something wrong", value.getMessage());
        assertTrue(msg.getMetaData().getData().isEmpty());
    }

    @Test
    public void errorThrownIfCannotLoadAttributesAsync() {
        UserId userId = new UserId(UUIDs.timeBased());
        CustomerId customerId = new CustomerId(UUIDs.timeBased());
        User user = new User();
        user.setCustomerId(customerId);

        msg = new TbMsg(UUIDs.timeBased(), "USER", userId, new TbMsgMetaData(), new byte[4]);

        when(ctx.getUserService()).thenReturn(userService);
        when(userService.findUserByIdAsync(userId)).thenReturn(Futures.immediateFuture(user));

        when(ctx.getAttributesService()).thenReturn(attributesService);
        when(attributesService.find(customerId, SERVER_SCOPE, Collections.singleton("temperature")))
                .thenReturn(Futures.immediateFailedFuture(new IllegalStateException("something wrong")));

        node.onMsg(ctx, msg);
        final ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctx).tellError(same(msg), captor.capture());

        Throwable value = captor.getValue();
        assertEquals("something wrong", value.getMessage());
        assertTrue(msg.getMetaData().getData().isEmpty());
    }

    @Test
    public void errorThrownIfCustomerCannotBeFound() {
        UserId userId = new UserId(UUIDs.timeBased());
        CustomerId customerId = new CustomerId(UUIDs.timeBased());
        User user = new User();
        user.setCustomerId(customerId);

        msg = new TbMsg(UUIDs.timeBased(), "USER", userId, new TbMsgMetaData(), new byte[4]);

        when(ctx.getUserService()).thenReturn(userService);
        when(userService.findUserByIdAsync(userId)).thenReturn(Futures.immediateFuture(null));

        node.onMsg(ctx, msg);
        final ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctx).tellError(same(msg), captor.capture());

        Throwable value = captor.getValue();
        assertEquals(IllegalStateException.class, value.getClass());
        assertEquals("Customer not found", value.getMessage());
        assertTrue(msg.getMetaData().getData().isEmpty());
    }

    @Test
    public void customerAttributeAddedInMetadata() {
        CustomerId customerId = new CustomerId(UUIDs.timeBased());
        msg = new TbMsg(UUIDs.timeBased(), "CUSTOMER", customerId, new TbMsgMetaData(), new byte[4]);
        entityAttributeFetched(customerId);
    }

    @Test
    public void usersCustomerAttributesFetched() {
        UserId userId = new UserId(UUIDs.timeBased());
        CustomerId customerId = new CustomerId(UUIDs.timeBased());
        User user = new User();
        user.setCustomerId(customerId);

        msg = new TbMsg(UUIDs.timeBased(), "USER", userId, new TbMsgMetaData(), new byte[4]);

        when(ctx.getUserService()).thenReturn(userService);
        when(userService.findUserByIdAsync(userId)).thenReturn(Futures.immediateFuture(user));

        entityAttributeFetched(customerId);
    }

    @Test
    public void assetsCustomerAttributesFetched() {
        AssetId assetId = new AssetId(UUIDs.timeBased());
        CustomerId customerId = new CustomerId(UUIDs.timeBased());
        Asset asset = new Asset();
        asset.setCustomerId(customerId);

        msg = new TbMsg(UUIDs.timeBased(), "USER", assetId, new TbMsgMetaData(), new byte[4]);

        when(ctx.getAssetService()).thenReturn(assetService);
        when(assetService.findAssetByIdAsync(assetId)).thenReturn(Futures.immediateFuture(asset));

        entityAttributeFetched(customerId);
    }

    @Test
    public void deviceCustomerAttributesFetched() {
        DeviceId deviceId = new DeviceId(UUIDs.timeBased());
        CustomerId customerId = new CustomerId(UUIDs.timeBased());
        Device device = new Device();
        device.setCustomerId(customerId);

        msg = new TbMsg(UUIDs.timeBased(), "USER", deviceId, new TbMsgMetaData(), new byte[4]);

        when(ctx.getDeviceService()).thenReturn(deviceService);
        when(deviceService.findDeviceByIdAsync(deviceId)).thenReturn(Futures.immediateFuture(device));

        entityAttributeFetched(customerId);
    }

    private void entityAttributeFetched(CustomerId customerId) {
        List<AttributeKvEntry> attributes = Lists.newArrayList(new BaseAttributeKvEntry(new StringDataEntry("temperature", "high"), 1L));

        when(ctx.getAttributesService()).thenReturn(attributesService);
        when(attributesService.find(customerId, SERVER_SCOPE, Collections.singleton("temperature")))
                .thenReturn(Futures.immediateFuture(attributes));

        node.onMsg(ctx, msg);
        verify(ctx).tellNext(msg);
        assertEquals(msg.getMetaData().getValue("tempo"), "high");
    }
}