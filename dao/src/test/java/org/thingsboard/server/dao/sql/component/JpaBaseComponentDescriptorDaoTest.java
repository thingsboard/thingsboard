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
package org.thingsboard.server.dao.sql.component;

import com.datastax.driver.core.utils.UUIDs;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.id.ComponentDescriptorId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentScope;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.component.ComponentDescriptorDao;
import org.thingsboard.server.dao.service.AbstractServiceTest;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
public class JpaBaseComponentDescriptorDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private ComponentDescriptorDao componentDescriptorDao;

    @Test
    public void findByType() {
        for (int i = 0; i < 20; i++) {
            createComponentDescriptor(ComponentType.FILTER, ComponentScope.SYSTEM, i);
            createComponentDescriptor(ComponentType.ACTION, ComponentScope.TENANT, i + 20);
        }

        TextPageLink pageLink1 = new TextPageLink(15, "COMPONENT_");
        List<ComponentDescriptor> components1 = componentDescriptorDao.findByTypeAndPageLink(AbstractServiceTest.SYSTEM_TENANT_ID, ComponentType.FILTER, pageLink1);
        assertEquals(15, components1.size());

        TextPageLink pageLink2 = new TextPageLink(15, "COMPONENT_", components1.get(14).getId().getId(), null);
        List<ComponentDescriptor> components2 = componentDescriptorDao.findByTypeAndPageLink(AbstractServiceTest.SYSTEM_TENANT_ID,ComponentType.FILTER, pageLink2);
        assertEquals(5, components2.size());
    }

    @Test
    public void findByTypeAndSocpe() {
        for (int i = 0; i < 20; i++) {
            createComponentDescriptor(ComponentType.ENRICHMENT, ComponentScope.SYSTEM, i);
            createComponentDescriptor(ComponentType.ACTION, ComponentScope.TENANT, i + 20);
            createComponentDescriptor(ComponentType.FILTER, ComponentScope.SYSTEM, i + 40);
        }

        TextPageLink pageLink1 = new TextPageLink(15, "COMPONENT_");
        List<ComponentDescriptor> components1 = componentDescriptorDao.findByScopeAndTypeAndPageLink(AbstractServiceTest.SYSTEM_TENANT_ID,
                ComponentScope.SYSTEM, ComponentType.FILTER, pageLink1);
        assertEquals(15, components1.size());

        TextPageLink pageLink2 = new TextPageLink(15, "COMPONENT_", components1.get(14).getId().getId(), null);
        List<ComponentDescriptor> components2 = componentDescriptorDao.findByScopeAndTypeAndPageLink(AbstractServiceTest.SYSTEM_TENANT_ID,
                ComponentScope.SYSTEM, ComponentType.FILTER, pageLink2);
        assertEquals(5, components2.size());
    }

    private void createComponentDescriptor(ComponentType type, ComponentScope scope, int index) {
        ComponentDescriptor component = new ComponentDescriptor();
        component.setId(new ComponentDescriptorId(UUIDs.timeBased()));
        component.setType(type);
        component.setScope(scope);
        component.setName("COMPONENT_" + index);
        componentDescriptorDao.save(AbstractServiceTest.SYSTEM_TENANT_ID,component);
    }

}
