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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.rule.engine.filter.TbJsFilterNode;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentScope;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.security.Authority;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class BaseComponentDescriptorControllerTest extends AbstractControllerTest {

    private static final int AMOUNT_OF_DEFAULT_FILTER_NODES = 4;
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
    public void testGetByClazz() throws Exception {
        ComponentDescriptor descriptor =
                doGet("/api/component/" + TbJsFilterNode.class.getName(), ComponentDescriptor.class);

        Assert.assertNotNull(descriptor);
        Assert.assertNotNull(descriptor.getId());
        Assert.assertNotNull(descriptor.getName());
        Assert.assertEquals(ComponentScope.TENANT, descriptor.getScope());
        Assert.assertEquals(ComponentType.FILTER, descriptor.getType());
        Assert.assertEquals(descriptor.getClazz(), descriptor.getClazz());
    }

    @Test
    public void testGetByType() throws Exception {
        List<ComponentDescriptor> descriptors = readResponse(
                doGet("/api/components/" + ComponentType.FILTER).andExpect(status().isOk()), new TypeReference<List<ComponentDescriptor>>() {
                });

        Assert.assertNotNull(descriptors);
        Assert.assertTrue(descriptors.size() >= AMOUNT_OF_DEFAULT_FILTER_NODES);

        for (ComponentType type : ComponentType.values()) {
            doGet("/api/components/" + type).andExpect(status().isOk());
        }
    }

}
