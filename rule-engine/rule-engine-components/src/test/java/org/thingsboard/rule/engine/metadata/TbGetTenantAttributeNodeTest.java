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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UserId;

import java.util.UUID;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TbGetTenantAttributeNodeTest extends AbstractAttributeNodeTest {

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
    protected TbEntityGetAttrNode getEmptyNode() {
        return new TbGetTenantAttributeNode();
    }

    @Override
    EntityId getEntityId() {
        return tenantId;
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
    public void failedChainUsedIfCustomerCannotBeFound() {
        when(ctx.getTenantId()).thenReturn(null);
        failedChainUsedIfCustomerCannotBeFound(user);
    }

    @Test
    public void customerAttributeAddedInMetadata() {
        entityAttributeAddedInMetadata(tenantId, "TENANT");
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
