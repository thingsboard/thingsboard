// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.component;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentClusteringMode;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentScope;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.dao.exception.IncorrectParameterException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseComponentDescriptorServiceTest {

    private BaseComponentDescriptorService service;
    private ComponentDescriptor componentDescriptor;
    private TenantId tenantId;

    @BeforeEach
    void setUp() {
        service = Mockito.spy(BaseComponentDescriptorService.class);
        tenantId = TenantId.SYS_TENANT_ID;

        // Create a simple component descriptor
        componentDescriptor = new ComponentDescriptor();
        componentDescriptor.setType(ComponentType.ACTION);
        componentDescriptor.setScope(ComponentScope.TENANT);
        componentDescriptor.setClusteringMode(ComponentClusteringMode.ENABLED);
        componentDescriptor.setName("Test Component");
        componentDescriptor.setClazz("org.thingsboard.test.TestComponent");

        // Create configuration descriptor with schema from JSON string
        String configDescriptorJson = """
                {
                  "schema": {
                    "type": "object",
                    "properties": {
                      "testField": {
                        "type": "string"
                      }
                    },
                    "required": ["testField"]
                  }
                }""";

        componentDescriptor.setConfigurationDescriptor(JacksonUtil.toJsonNode(configDescriptorJson));
    }

    @Test
    void testValidate() {
        // Create valid configuration from JSON string
        String validConfigJson = "{\"testField\": \"test value\"}";
        JsonNode validConfig = JacksonUtil.toJsonNode(validConfigJson);

        // Create invalid configuration (missing required field) from JSON string
        String invalidConfigJson = "{}";
        JsonNode invalidConfig = JacksonUtil.toJsonNode(invalidConfigJson);

        // Test valid configuration
        boolean validResult = service.validate(tenantId, componentDescriptor, validConfig);
        assertTrue(validResult, "Valid configuration should pass validation");

        // Test invalid configuration
        boolean invalidResult = service.validate(tenantId, componentDescriptor, invalidConfig);
        assertFalse(invalidResult, "Invalid configuration should fail validation");

        // Test with component descriptor without schema
        ComponentDescriptor noSchemaDescriptor = new ComponentDescriptor(componentDescriptor);
        noSchemaDescriptor.setConfigurationDescriptor(JacksonUtil.toJsonNode("{}"));

        // Should throw exception when schema is missing
        assertThrows(IncorrectParameterException.class, () -> {
            service.validate(tenantId, noSchemaDescriptor, validConfig);
        }, "Should throw exception when schema is missing");
    }

}
