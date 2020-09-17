/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.security.Authority;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public class BaseEdgeEventControllerTest extends AbstractControllerTest {

    private Tenant savedTenant;
    private TenantId tenantId;
    private User tenantAdmin;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        tenantId = savedTenant.getId();
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testGetEdgeEvents() throws Exception {
        Edge edge = constructEdge("TestEdge", "default");
        edge = doPost("/api/edge", edge, Edge.class);

        Device device = constructDevice("TestDevice", "default");
        Device savedDevice = doPost("/api/device", device, Device.class);

        doPost("/api/edge/" + edge.getId().toString() + "/device/" + savedDevice.getId().toString(), Device.class);
        Thread.sleep(1000);

        Asset asset = constructAsset("TestAsset", "default");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);

        doPost("/api/edge/" + edge.getId().toString() + "/asset/" + savedAsset.getId().toString(), Asset.class);
        Thread.sleep(1000);

        EntityRelation relation = new EntityRelation(savedAsset.getId(), savedDevice.getId(), EntityRelation.CONTAINS_TYPE);

        doPost("/api/relation", relation);
        Thread.sleep(1000);

        List<EdgeEvent> edgeEvents = doGetTypedWithTimePageLink("/api/edge/" + edge.getId().toString() + "/events?",
                new TypeReference<TimePageData<EdgeEvent>>() {
                }, new TimePageLink(4)).getData();

        Assert.assertFalse(edgeEvents.isEmpty());
        Assert.assertEquals(4, edgeEvents.size());
        Assert.assertEquals(EdgeEventType.RELATION, edgeEvents.get(0).getType());
        Assert.assertEquals(EdgeEventType.ASSET, edgeEvents.get(1).getType());
        Assert.assertEquals(EdgeEventType.DEVICE, edgeEvents.get(2).getType());
        Assert.assertEquals(EdgeEventType.RULE_CHAIN, edgeEvents.get(3).getType());
    }

    private Device constructDevice(String name, String type) {
        Device device = new Device();
        device.setName(name);
        device.setType(type);
        return device;
    }

    private Asset constructAsset(String name, String type) {
        Asset asset = new Asset();
        asset.setName(name);
        asset.setType(type);
        return asset;
    }

}
