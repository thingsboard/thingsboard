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
package org.thingsboard.server.controller;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.core.type.TypeReference;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.thingsboard.rule.engine.filter.TbJsFilterNode;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.ComponentDescriptorId;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentScope;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.component.ComponentDescriptorDao;
import org.thingsboard.server.dao.service.AbstractServiceTest;

import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {BaseComponentDescriptorControllerTest.Config.class})
public abstract class BaseComponentDescriptorControllerTest extends AbstractControllerTest {

    private static final int AMOUNT_OF_DEFAULT_FILTER_NODES = 4;
    private Tenant savedTenant;
    private User tenantAdmin;

    @Autowired
    ComponentDescriptorDao componentDescriptorDao;

    static class Config {
        @Bean
        @Primary
        public ComponentDescriptorDao componentDescriptorDao(ComponentDescriptorDao componentDescriptorDao) {
            return Mockito.mock(ComponentDescriptorDao.class, AdditionalAnswers.delegatesTo(componentDescriptorDao));
        }
    }

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
                doGet("/api/components?componentTypes={componentTypes}&ruleChainType={ruleChainType}", ComponentType.FILTER, RuleChainType.CORE).andExpect(status().isOk()), new TypeReference<List<ComponentDescriptor>>() {
                });

        Assert.assertNotNull(descriptors);
        Assert.assertTrue(descriptors.size() >= AMOUNT_OF_DEFAULT_FILTER_NODES);

        for (ComponentType type : ComponentType.values()) {
            doGet("/api/components?componentTypes={componentTypes}&ruleChainType={ruleChainType}", type, RuleChainType.CORE).andExpect(status().isOk());
        }
    }

    @Test
    public void testDeleteComponentDescriptorWithTransactionalOk() throws Exception {
        ComponentDescriptorId componentDescriptorId =  createComponentDescriptor("MOCK_TransactionalOk");
        ComponentDescriptor descriptorBefore = componentDescriptorDao.findById(AbstractServiceTest.SYSTEM_TENANT_ID, componentDescriptorId);
        Assert.assertNotNull(descriptorBefore);

        componentDescriptorDao.removeById(AbstractServiceTest.SYSTEM_TENANT_ID, componentDescriptorId.getId());

        ComponentDescriptor descriptorAfter = componentDescriptorDao.findById(AbstractServiceTest.SYSTEM_TENANT_ID, componentDescriptorId);
        Assert.assertNull(descriptorAfter);
    }

    @Test
    public void testDeleteComponentDescriptorWithTransactionalException() throws Exception {
        ComponentDescriptorId componentDescriptorId =  createComponentDescriptor("MOCK_TransactionalException");
        ComponentDescriptor descriptorBefore = componentDescriptorDao.findById(AbstractServiceTest.SYSTEM_TENANT_ID, componentDescriptorId);
        Assert.assertNotNull(descriptorBefore);

        Mockito.doThrow(new ConstraintViolationException("mock message", new SQLException(), "MOCK_CONSTRAINT")).when(componentDescriptorDao).removeById(any(), any());
        try {
            final Throwable raisedException = catchThrowable(() -> componentDescriptorDao.removeById(AbstractServiceTest.SYSTEM_TENANT_ID, componentDescriptorId.getId()));
            assertThat(raisedException).isInstanceOf(ConstraintViolationException.class)
                    .hasMessageContaining("mock message");

            ComponentDescriptor descriptorAfter = componentDescriptorDao.findById(AbstractServiceTest.SYSTEM_TENANT_ID, componentDescriptorId.getId());
            Assert.assertNotNull(descriptorAfter);
            Mockito.doReturn(true).when(componentDescriptorDao).removeById(any(), any());
        } finally {
            Mockito.reset(componentDescriptorDao);
        }
    }

    private ComponentDescriptorId createComponentDescriptor(String name) {
        ComponentDescriptor component = new ComponentDescriptor();
        component.setId(new ComponentDescriptorId(Uuids.timeBased()));
        component.setType(ComponentType.FILTER);
        component.setScope(ComponentScope.SYSTEM);
        component.setName(name);
        componentDescriptorDao.save(AbstractServiceTest.SYSTEM_TENANT_ID, component);
        return component.getId();
    }

}
