/**
 * Copyright © 2016-2017 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.thingsboard.server.common.data.AdminSettings;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


public abstract class BaseAdminControllerTest extends AbstractControllerTest {

    @Test
    public void testFindAdminSettingsByKey() throws Exception {
        loginSysAdmin();
        doGet("/api/admin/settings/general")
        .andExpect(status().isOk())
        .andExpect(content().contentType(contentType))
        .andExpect(jsonPath("$.id", notNullValue()))
        .andExpect(jsonPath("$.key", is("general")))
        .andExpect(jsonPath("$.jsonValue.baseUrl", is("http://localhost:8080")));
        
        doGet("/api/admin/settings/mail")
        .andExpect(status().isOk())
        .andExpect(content().contentType(contentType))
        .andExpect(jsonPath("$.id", notNullValue()))
        .andExpect(jsonPath("$.key", is("mail")))
        .andExpect(jsonPath("$.jsonValue.smtpProtocol", is("smtp")))
        .andExpect(jsonPath("$.jsonValue.smtpHost", is("localhost")))
        .andExpect(jsonPath("$.jsonValue.smtpPort", is("25")));
        
        doGet("/api/admin/settings/unknown")
        .andExpect(status().isNotFound());
        
    }
    
    @Test
    public void testSaveAdminSettings() throws Exception {
        loginSysAdmin();
        AdminSettings adminSettings = doGet("/api/admin/settings/general", AdminSettings.class); 
        
        JsonNode jsonValue = adminSettings.getJsonValue();
        ((ObjectNode) jsonValue).put("baseUrl", "http://myhost.org");
        adminSettings.setJsonValue(jsonValue);

        doPost("/api/admin/settings", adminSettings).andExpect(status().isOk());
        
        doGet("/api/admin/settings/general")
        .andExpect(status().isOk())
        .andExpect(content().contentType(contentType))
        .andExpect(jsonPath("$.jsonValue.baseUrl", is("http://myhost.org")));
        
        ((ObjectNode) jsonValue).put("baseUrl", "http://localhost:8080");
        adminSettings.setJsonValue(jsonValue);
        
        doPost("/api/admin/settings", adminSettings)
        .andExpect(status().isOk());
    }

    @Test
    public void testSaveAdminSettingsWithEmptyKey() throws Exception {
        loginSysAdmin();
        AdminSettings adminSettings = doGet("/api/admin/settings/mail", AdminSettings.class); 
        adminSettings.setKey(null);
        doPost("/api/admin/settings", adminSettings)
        .andExpect(status().isBadRequest())
        .andExpect(statusReason(containsString("Key should be specified")));
    }
    
    @Test
    public void testChangeAdminSettingsKey() throws Exception {
        loginSysAdmin();
        AdminSettings adminSettings = doGet("/api/admin/settings/mail", AdminSettings.class); 
        adminSettings.setKey("newKey");
        doPost("/api/admin/settings", adminSettings)
        .andExpect(status().isBadRequest())
        .andExpect(statusReason(containsString("is prohibited")));
    }
    
    @Test
    public void testSaveAdminSettingsWithNewJsonStructure() throws Exception {
        loginSysAdmin();
        AdminSettings adminSettings = doGet("/api/admin/settings/mail", AdminSettings.class); 
        JsonNode json = adminSettings.getJsonValue();
        ((ObjectNode) json).put("newKey", "my new value");
        adminSettings.setJsonValue(json);
        doPost("/api/admin/settings", adminSettings)
        .andExpect(status().isBadRequest())
        .andExpect(statusReason(containsString("Provided json structure is different")));
    }
    
    @Test
    public void testSaveAdminSettingsWithNonTextValue() throws Exception {
        loginSysAdmin();
        AdminSettings adminSettings = doGet("/api/admin/settings/mail", AdminSettings.class); 
        JsonNode json = adminSettings.getJsonValue();
        ((ObjectNode) json).put("timeout", 10000L);
        adminSettings.setJsonValue(json);
        doPost("/api/admin/settings", adminSettings)
        .andExpect(status().isBadRequest())
        .andExpect(statusReason(containsString("Provided json structure can't contain non-text values")));
    }
    
    @Test
    public void testSendTestMail() throws Exception {
        loginSysAdmin();
        AdminSettings adminSettings = doGet("/api/admin/settings/mail", AdminSettings.class);
        doPost("/api/admin/settings/testMail", adminSettings)
        .andExpect(status().isOk());
    }
    
}
