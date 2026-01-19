/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.dao.service.validator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentScope;
import org.thingsboard.server.common.data.plugin.ComponentType;

import static org.mockito.Mockito.verify;

@SpringBootTest(classes = ComponentDescriptorDataValidator.class)
class ComponentDescriptorDataValidatorTest {
    @SpyBean
    ComponentDescriptorDataValidator validator;

    @Test
    void testValidateNameInvocation() {
        ComponentDescriptor plugin = new ComponentDescriptor();
        plugin.setType(ComponentType.ENRICHMENT);
        plugin.setScope(ComponentScope.SYSTEM);
        plugin.setName("originator attributes");
        plugin.setClazz("org.thingsboard.rule.engine.metadata.TbGetAttributesNode");
        validator.validateDataImpl(TenantId.SYS_TENANT_ID, plugin);
        verify(validator).validateString("Component name", plugin.getName());
    }
}
