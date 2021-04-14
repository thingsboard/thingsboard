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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
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
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.security.Authority;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public class BaseEdgeEventControllerTest extends AbstractControllerTest {

    private Tenant savedTenant;
    private User tenantAdmin;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");
        // sleep 1 seconds to avoid CREDENTIALS updated message for the user
        // user credentials is going to be stored and updated event pushed to edge notification service
        // while service will be processing this event edge could be already added and additional message will be pushed
        Thread.sleep(1000);
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

        Asset asset = constructAsset("TestAsset", "default");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);

        doPost("/api/edge/" + edge.getId().toString() + "/asset/" + savedAsset.getId().toString(), Asset.class);

        EntityRelation relation = new EntityRelation(savedAsset.getId(), savedDevice.getId(), EntityRelation.CONTAINS_TYPE);

        doPost("/api/relation", relation);

        // wait while edge event for the relation entity persisted to DB
        Thread.sleep(100);
        List<EdgeEvent> edgeEvents;
        int attempt = 1;
        do {
            edgeEvents = doGetTypedWithTimePageLink("/api/edge/" + edge.getId().toString() + "/events?",
                            new TypeReference<PageData<EdgeEvent>>() {}, new TimePageLink(4)).getData();
            attempt++;
            Thread.sleep(100);
        } while (edgeEvents.size() != 4 || attempt < 5);
        Assert.assertEquals(4, edgeEvents.size());
        Assert.assertTrue(edgeEvents.stream().anyMatch(ee -> EdgeEventType.RULE_CHAIN.equals(ee.getType())));
        Assert.assertTrue(edgeEvents.stream().anyMatch(ee -> EdgeEventType.DEVICE.equals(ee.getType())));
        Assert.assertTrue(edgeEvents.stream().anyMatch(ee -> EdgeEventType.ASSET.equals(ee.getType())));
        Assert.assertTrue(edgeEvents.stream().anyMatch(ee -> EdgeEventType.RELATION.equals(ee.getType())));
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
