// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service.validator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentScope;
import org.thingsboard.server.common.data.plugin.ComponentType;

import static org.mockito.Mockito.verify;

@SpringBootTest(classes = ComponentDescriptorDataValidator.class)
class ComponentDescriptorDataValidatorTest {
    @MockitoSpyBean
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
