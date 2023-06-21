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
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.TenantProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

@DaoSqlTest
public class TenantProfileEdgeTest extends AbstractEdgeTest {

    @Test
    public void testTenantProfiles() throws Exception {
        loginSysAdmin();

        // save current values into tmp to revert after test
        TenantProfile edgeTenantProfile = doGet("/api/tenantProfile/" + savedTenant.getTenantProfileId().getId(), TenantProfile.class);
        TenantProfile tmp = edgeTenantProfile;

        // updated edge tenant profile
        edgeTenantProfile.setName("Tenant Profile Edge");
        edgeTenantProfile.setDescription("Updated tenant profile Edge");
        edgeImitator.expectMessageAmount(1);
        edgeTenantProfile = doPost("/api/tenantProfile", edgeTenantProfile, TenantProfile.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof TenantProfileUpdateMsg);
        TenantProfileUpdateMsg tenantProfileUpdateMsg = (TenantProfileUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, tenantProfileUpdateMsg.getMsgType());
        Assert.assertEquals(edgeTenantProfile.getUuidId().getMostSignificantBits(), tenantProfileUpdateMsg.getIdMSB());
        Assert.assertEquals(edgeTenantProfile.getUuidId().getLeastSignificantBits(), tenantProfileUpdateMsg.getIdLSB());
        Assert.assertEquals(edgeTenantProfile.getDescription(), tenantProfileUpdateMsg.getDescription());
        Assert.assertEquals("Updated tenant profile Edge", tenantProfileUpdateMsg.getDescription());
        Assert.assertEquals("Tenant Profile Edge", tenantProfileUpdateMsg.getName());

        // revert back to default values
        edgeTenantProfile = doPost("/api/tenantProfile", tmp, TenantProfile.class);
        Assert.assertEquals(edgeTenantProfile, tmp);
    }
}
