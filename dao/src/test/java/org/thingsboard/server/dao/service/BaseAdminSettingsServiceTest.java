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
package org.thingsboard.server.dao.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.dao.exception.DataValidationException;

public abstract class BaseAdminSettingsServiceTest extends AbstractServiceTest {

    @Test
    public void testFindAdminSettingsByKey() {
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey("general");
        Assert.assertNotNull(adminSettings);
        adminSettings = adminSettingsService.findAdminSettingsByKey("mail");
        Assert.assertNotNull(adminSettings);
        adminSettings = adminSettingsService.findAdminSettingsByKey("unknown");
        Assert.assertNull(adminSettings);
    }
    
    @Test
    public void testFindAdminSettingsById() {
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey("general");
        AdminSettings foundAdminSettings = adminSettingsService.findAdminSettingsById(adminSettings.getId());
        Assert.assertNotNull(foundAdminSettings);
        Assert.assertEquals(adminSettings, foundAdminSettings);
    }
    
    @Test
    public void testSaveAdminSettings() throws Exception {
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey("general");
        JsonNode json = adminSettings.getJsonValue();
        ((ObjectNode) json).put("baseUrl", "http://myhost.org");
        adminSettings.setJsonValue(json);
        adminSettingsService.saveAdminSettings(adminSettings);
        AdminSettings savedAdminSettings = adminSettingsService.findAdminSettingsByKey("general");
        Assert.assertNotNull(savedAdminSettings);
        Assert.assertEquals(adminSettings.getJsonValue(), savedAdminSettings.getJsonValue());
    }
    
    @Test(expected = DataValidationException.class)
    public void testSaveAdminSettingsWithEmptyKey() {
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey("mail");
        adminSettings.setKey(null);
        adminSettingsService.saveAdminSettings(adminSettings);
    }
    
    @Test(expected = DataValidationException.class)
    public void testChangeAdminSettingsKey() {
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey("mail");
        adminSettings.setKey("newKey");
        adminSettingsService.saveAdminSettings(adminSettings);
    }
    
    @Test(expected = DataValidationException.class)
    public void testSaveAdminSettingsWithNewJsonStructure() throws Exception {
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey("mail");
        JsonNode json = adminSettings.getJsonValue();
        ((ObjectNode) json).put("newKey", "my new value");
        adminSettings.setJsonValue(json);
        adminSettingsService.saveAdminSettings(adminSettings);
    }
    
    @Test(expected = DataValidationException.class)
    public void testSaveAdminSettingsWithNonTextValue() throws Exception {
        AdminSettings adminSettings = adminSettingsService.findAdminSettingsByKey("mail");
        JsonNode json = adminSettings.getJsonValue();
        ((ObjectNode) json).put("timeout", 10000L);
        adminSettings.setJsonValue(json);
        adminSettingsService.saveAdminSettings(adminSettings);
    }
}
