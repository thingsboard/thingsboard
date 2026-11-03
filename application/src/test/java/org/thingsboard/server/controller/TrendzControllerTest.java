// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.controller;

import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.trendz.TrendzSettings;
import org.thingsboard.server.dao.service.DaoSqlTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class TrendzControllerTest extends AbstractControllerTest {

    private final String trendzUrl = "https://some.domain.com:18888/also_necessary_prefix";
    private final String apiKey = "$2a$10$iDjfqYmnrw9gkdw4XhgzFOU.R/pVz3OKgXOdpbR2LuXaKatGcGLiG";

    @Before
    public void setUp() throws Exception {
        loginTenantAdmin();

        TrendzSettings trendzSettings = new TrendzSettings();
        trendzSettings.setEnabled(true);
        trendzSettings.setBaseUrl(trendzUrl);
        trendzSettings.setApiKey(apiKey);

        doPost("/api/trendz/settings", trendzSettings).andExpect(status().isOk());
    }

    @Test
    public void testTrendzSettingsWhenTenant() throws Exception {
        loginTenantAdmin();

        TrendzSettings trendzSettings = doGet("/api/trendz/settings", TrendzSettings.class);

        assertThat(trendzSettings).isNotNull();
        assertThat(trendzSettings.isEnabled()).isTrue();
        assertThat(trendzSettings.getBaseUrl()).isEqualTo(trendzUrl);
        trendzSettings.setApiKey(apiKey);

        String updatedUrl = "https://some.domain.com:18888/tenant_trendz";
        String updatedApiKey = "$2a$10$aRR0bHa8rtzP5jRcE72vp.hRFsGQz4MGIs62oogLbfOCFK3.RIESG";
        trendzSettings.setBaseUrl(updatedUrl);
        trendzSettings.setApiKey(updatedApiKey);

        doPost("/api/trendz/settings", trendzSettings).andExpect(status().isOk());

        TrendzSettings updatedTrendzSettings = doGet("/api/trendz/settings", TrendzSettings.class);
        assertThat(updatedTrendzSettings).isEqualTo(trendzSettings);
    }

    @Test
    public void testTrendzSettingsWhenCustomer() throws Exception {
        loginCustomerUser();

        TrendzSettings newTrendzSettings = new TrendzSettings();
        newTrendzSettings.setEnabled(true);
        newTrendzSettings.setBaseUrl("https://some.domain.com:18888/customer_trendz");
        newTrendzSettings.setApiKey("some_api_key");

        doPost("/api/trendz/settings", newTrendzSettings).andExpect(status().isForbidden());

        TrendzSettings fetchedTrendzSettings = doGet("/api/trendz/settings", TrendzSettings.class);
        assertThat(fetchedTrendzSettings).isNotNull();
        assertThat(fetchedTrendzSettings.isEnabled()).isTrue();
        assertThat(fetchedTrendzSettings.getBaseUrl()).isEqualTo(trendzUrl);
        assertThat(fetchedTrendzSettings.getApiKey()).isEqualTo(apiKey);
    }
}
