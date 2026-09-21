// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edge;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.gen.edge.v1.AdminSettingsUpdateMsg;

@DaoSqlTest
public class AdminSettingsEdgeTest extends AbstractEdgeTest {

    @Autowired
    private AdminSettingsService adminSettingsService;

    @Test
    public void testAdminSettings() throws Exception {
        loginSysAdmin();

        // save
        AdminSettings adminSettings = new AdminSettings();
        adminSettings.setKey("edgeTest");
        ObjectNode jsonValue = JacksonUtil.newObjectNode();
        jsonValue.put("key1", "value1");
        adminSettings.setJsonValue(jsonValue);

        edgeImitator.expectMessageAmount(1);
        AdminSettings savedAdminSettings = doPost("/api/admin/settings", adminSettings, AdminSettings.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AdminSettingsUpdateMsg);
        AdminSettingsUpdateMsg adminSettingsUpdateMsg = (AdminSettingsUpdateMsg) latestMessage;
        AdminSettings adminSettingsMsg = JacksonUtil.fromString(adminSettingsUpdateMsg.getEntity(), AdminSettings.class, true);
        Assert.assertNotNull(adminSettingsMsg);
        Assert.assertEquals("edgeTest", adminSettingsMsg.getKey());
        Assert.assertEquals("value1", adminSettingsMsg.getJsonValue().get("key1").asText());

        // update
        ObjectNode updatedJsonValue = (ObjectNode) savedAdminSettings.getJsonValue();
        updatedJsonValue.put("key2", "value2");
        savedAdminSettings.setJsonValue(updatedJsonValue);

        edgeImitator.expectMessageAmount(1);
        doPost("/api/admin/settings", savedAdminSettings, AdminSettings.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AdminSettingsUpdateMsg);
        adminSettingsUpdateMsg = (AdminSettingsUpdateMsg) latestMessage;
        adminSettingsMsg = JacksonUtil.fromString(adminSettingsUpdateMsg.getEntity(), AdminSettings.class, true);
        Assert.assertNotNull(adminSettingsMsg);
        Assert.assertEquals("edgeTest", adminSettingsMsg.getKey());
        Assert.assertEquals("value1", adminSettingsMsg.getJsonValue().get("key1").asText());
        Assert.assertEquals("value2", adminSettingsMsg.getJsonValue().get("key2").asText());

        adminSettingsService.deleteAdminSettingsByTenantIdAndKey(savedAdminSettings.getTenantId(), "edgeTest");
    }

}
