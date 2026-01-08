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
package org.thingsboard.server.service.sms;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.settings.AdminSettingsService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
@TestPropertySource(properties = {
        "usage.stats.report.enabled=true",
        "usage.stats.report.interval=1",
})
public class DefaultSmsServiceTest extends AbstractControllerTest {
    @SpyBean
    private DefaultSmsService defaultSmsService;
    @Autowired
    private AdminSettingsService adminSettingsService;

    private TenantProfile tenantProfile;

    @Before
    public void before() throws Exception {
        loginSysAdmin();
        prepareSmsSystemSetting();
    }

    @After
    public void after() throws Exception {
        saveTenantProfileWitConfiguration(tenantProfile, new DefaultTenantProfileConfiguration());
        adminSettingsService.deleteAdminSettingsByTenantIdAndKey(TenantId.SYS_TENANT_ID, "sms");
        resetTokens();
    }

    @Test
    public void testLimitSmsMessagingByTenantProfileSettings() throws Exception {
        tenantProfile = getDefaultTenantProfile();

        DefaultTenantProfileConfiguration config = createTenantProfileConfigurationWithSmsLimits(10, true);
        saveTenantProfileWitConfiguration(tenantProfile, config);

        for (int i = 0; i < 10; i++) {
            doReturn(1).when(defaultSmsService).sendSms(any(), any());
            defaultSmsService.sendSms(tenantId, null, new String[]{RandomStringUtils.randomNumeric(10)}, "Message");
        }

        //wait 1 sec so that api usage state is updated
        TimeUnit.SECONDS.sleep(1);
        assertThrows(RuntimeException.class, () -> {
            defaultSmsService.sendSms(tenantId, null, new String[]{RandomStringUtils.randomNumeric(10)}, "Message");
        }, "SMS sending is disabled due to API limits!");
    }

    @Test
    public void testLimitSmsMessagingIfSmsDisabled() throws Exception {
        tenantProfile = getDefaultTenantProfile();

        DefaultTenantProfileConfiguration config = createTenantProfileConfigurationWithSmsLimits(0, false);
        saveTenantProfileWitConfiguration(tenantProfile, config);

        TimeUnit.SECONDS.sleep(1);
        assertThrows(RuntimeException.class, () -> {
            defaultSmsService.sendSms(tenantId, null, new String[]{RandomStringUtils.randomNumeric(10)}, "Message");
        }, "SMS sending is disabled due to API limits!");

        //enable sms messaging
        DefaultTenantProfileConfiguration config2 = createTenantProfileConfigurationWithSmsLimits(0, true);
        saveTenantProfileWitConfiguration(tenantProfile, config2);
        TimeUnit.SECONDS.sleep(1);

        for (int i = 0; i < 10; i++) {
            doReturn(1).when(defaultSmsService).sendSms(any(), any());
            defaultSmsService.sendSms(tenantId, null, new String[]{RandomStringUtils.randomNumeric(10)}, "Message");
        }
    }

    private TenantProfile getDefaultTenantProfile() throws Exception {

        PageLink pageLink = new PageLink(17);
        PageData<TenantProfile> pageData = doGetTypedWithPageLink("/api/tenantProfiles?",
                new TypeReference<>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
        List<TenantProfile> tenantProfiles = new ArrayList<>(pageData.getData());

        Optional<TenantProfile> optionalDefaultProfile = tenantProfiles.stream().filter(TenantProfile::isDefault).reduce((a, b) -> null);
        Assert.assertTrue(optionalDefaultProfile.isPresent());

        return optionalDefaultProfile.get();
    }

    private DefaultTenantProfileConfiguration createTenantProfileConfigurationWithSmsLimits(Integer maxSms, Boolean smsEnabled) {
        DefaultTenantProfileConfiguration.DefaultTenantProfileConfigurationBuilder builder = DefaultTenantProfileConfiguration.builder();
        builder.maxSms(maxSms);
        builder.smsEnabled(smsEnabled);
        return builder.build();

    }

    private void saveTenantProfileWitConfiguration(TenantProfile tenantProfile, TenantProfileConfiguration tenantProfileConfiguration) {
        TenantProfileData tenantProfileData = tenantProfile.getProfileData();
        tenantProfileData.setConfiguration(tenantProfileConfiguration);
        TenantProfile savedTenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);
        Assert.assertNotNull(savedTenantProfile);
    }

    private void prepareSmsSystemSetting() throws Exception {
        if (doGet("/api/admin/settings/sms").andReturn().getResponse().getStatus() == 404) {
            AdminSettings adminSettings = new AdminSettings();
            ObjectNode value = JacksonUtil.newObjectNode();
            value.put("numberFrom", "+12543223870");
            value.put("accountSid", "testAcc");
            value.put("accountToken", "testToken");
            value.put("type", "TWILIO");
            adminSettings.setKey("sms");
            adminSettings.setJsonValue(value);

            doPost("/api/admin/settings", adminSettings).andExpect(status().isOk());
        }
    }
}