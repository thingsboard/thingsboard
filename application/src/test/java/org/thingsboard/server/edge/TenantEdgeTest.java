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
package org.thingsboard.server.edge;

import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.TenantUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

import java.util.UUID;

@DaoSqlTest
public class TenantEdgeTest extends AbstractEdgeTest {

    @Test
    public void testUpdateTenant() throws Exception {
        loginSysAdmin();

        // save current value into tmp to revert after test
        Tenant tmp = doGet("/api/tenant/" + tenantId, Tenant.class);

        // updated edge tenant
        savedTenant.setTitle("TenantEdgeTest title");
        savedTenant.setRegion("Test Region");
        edgeImitator.expectMessageAmount(1);
        savedTenant = doPost("/api/tenant", savedTenant, Tenant.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof TenantUpdateMsg);
        TenantUpdateMsg tenantUpdateMsg = (TenantUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, tenantUpdateMsg.getMsgType());
        Assert.assertEquals(savedTenant.getUuidId().getMostSignificantBits(), tenantUpdateMsg.getIdMSB());
        Assert.assertEquals(savedTenant.getUuidId().getLeastSignificantBits(), tenantUpdateMsg.getIdLSB());
        Assert.assertEquals(savedTenant.getTitle(), tenantUpdateMsg.getTitle());
        Assert.assertEquals("TenantEdgeTest title", tenantUpdateMsg.getTitle());
        Assert.assertEquals(savedTenant.getTenantProfileId(), new TenantProfileId
                (new UUID(tenantUpdateMsg.getProfileIdMSB(), tenantUpdateMsg.getProfileIdLSB())));

        // revert back to default values
        savedTenant = doPost("/api/tenant", tmp, Tenant.class);
        Assert.assertEquals(savedTenant, tmp);
    }

    @Test
    public void whenTenantChangeTenantProfileId_thenNotifyEdgeToUpdateTenantAndTenantProfile() throws Exception {
        loginSysAdmin();

        // save current value into tmp to revert after test
        Tenant tmp = doGet("/api/tenant/" + tenantId, Tenant.class);

        // updated edge tenant
        TenantProfile tenantProfile = createTenantProfile();
        savedTenant.setTenantProfileId(tenantProfile.getId());
        edgeImitator.expectMessageAmount(1);
        savedTenant = doPost("/api/tenant", savedTenant, Tenant.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof TenantUpdateMsg);
        TenantUpdateMsg tenantUpdateMsg = (TenantUpdateMsg) latestMessage;
        // tenant update
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, tenantUpdateMsg.getMsgType());
        Assert.assertEquals(savedTenant.getUuidId().getMostSignificantBits(), tenantUpdateMsg.getIdMSB());
        Assert.assertEquals(savedTenant.getUuidId().getLeastSignificantBits(), tenantUpdateMsg.getIdLSB());
        Assert.assertEquals(savedTenant.getTitle(), tenantUpdateMsg.getTitle());
        Assert.assertEquals(savedTenant.getTenantProfileId(), new TenantProfileId
                (new UUID(tenantUpdateMsg.getProfileIdMSB(), tenantUpdateMsg.getProfileIdLSB())));

        // revert back to default values
        savedTenant = doPost("/api/tenant", tmp, Tenant.class);
        Assert.assertEquals(savedTenant, tmp);
    }

    private TenantProfile createTenantProfile() {
        TenantProfile tenantProfile = new TenantProfile();
        tenantProfile.setName("TestEdge tenant profile");
        return doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);
    }
}
