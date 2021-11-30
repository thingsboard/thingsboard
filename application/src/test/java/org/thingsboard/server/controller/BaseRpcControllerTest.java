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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.ReflectionUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceCredentialsId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.model.ModelConstants;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

public abstract class BaseRpcControllerTest extends AbstractControllerTest {

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
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testSaveRpc() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);

        //Creating RPC
        ObjectNode rpc = JacksonUtil.newObjectNode();
        rpc.put("method", "setGpio");

        ObjectNode params = JacksonUtil.newObjectNode();

        params.put("pin", 7);
        params.put("value", 1);

        rpc.set("params", params);
        rpc.put("persistent", true);
        rpc.put("timeout", 5000);

        String url = "/api/rpc/oneway/" + savedDevice.getId().getId().toString();

        //Send one-way RPC
        String result = doPostAsync(url, JacksonUtil.toString(rpc), String.class, status().isOk());
        String rpcId = JacksonUtil.fromString(result, JsonNode.class)
                .get("rpcId")
                .asText();

        //Get RPC by id
        url = "/api/rpc/persistent/" + rpcId;
        Rpc savedRpc = doGet(url, Rpc.class);

        //Assertion
        Assert.assertNotNull(savedRpc);
        Assert.assertEquals(savedDevice.getId(), savedRpc.getDeviceId());
    }

    @Test
    public void testDeleteRpc() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);

        //Creating RPC
        ObjectNode rpc = JacksonUtil.newObjectNode();
        rpc.put("method", "setGpio");

        ObjectNode params = JacksonUtil.newObjectNode();

        params.put("pin", 7);
        params.put("value", 1);

        rpc.set("params", params);
        rpc.put("persistent", true);
        rpc.put("timeout", 5000);

        String url = "/api/rpc/oneway/" + savedDevice.getId().getId().toString();

        //Send one-way RPC
        String result = doPostAsync(url, JacksonUtil.toString(rpc), String.class, status().isOk());
        String rpcId = JacksonUtil.fromString(result, JsonNode.class)
                .get("rpcId")
                .asText();

        //Get RPC by id
        url = "/api/rpc/persistent/" + rpcId;
        Rpc savedRpc = doGet(url, Rpc.class);

        //Deleting RPC
        url = "/api/rpc/persistent/" + savedRpc.getId().getId().toString();
        MvcResult mvcResult = doDelete(url).andReturn();

        //Try to get deleted RPC
        url = "/api/rpc/persistent/" + rpcId;
        MvcResult res = doGet(url).andExpect(status().isNotFound()).andReturn();

        //Getting statusCode from delete-response
        JsonNode deleteResponse = JacksonUtil.fromString(res.getResponse().getContentAsString(), JsonNode.class);
        int status = deleteResponse.get("status").asInt();

        //Assert it
        Assert.assertEquals(404, status);

        //Try to get deleted RPC by device
        url = "/api/rpc/persistent/device/" + savedDevice.getUuidId()
                + "?" + "page=0" + "&" +
                "pageSize=" + Integer.MAX_VALUE + "&" +
                "rpcStatus=" + RpcStatus.DELETED.name();
        MvcResult byDeviceResult = doGet(url).andReturn();
        JsonNode byDeviceResponse = JacksonUtil.fromString(byDeviceResult.getResponse().getContentAsString(), JsonNode.class);
        status = byDeviceResponse.get("status").asInt();

        //Assert 2
        Assert.assertEquals(500, status);
    }
}
