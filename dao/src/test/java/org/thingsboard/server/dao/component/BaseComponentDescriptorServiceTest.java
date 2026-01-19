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
