/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.trendz.TrendzSettings;
import org.thingsboard.server.dao.service.DaoSqlTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class TrendzControllerTest extends AbstractControllerTest {

    private final String trendzUrl = "https://some.domain.com:18888/also_necessary_prefix";

    @Before
    public void setUp() throws Exception {
        loginTenantAdmin();

        TrendzSettings trendzSettings = new TrendzSettings();
        trendzSettings.setEnabled(true);
        trendzSettings.setBaseUrl(trendzUrl);

        doPost("/api/trendz/settings", trendzSettings).andExpect(status().isOk());
    }

    @Test
    public void testTrendzSettingsWhenTenant() throws Exception {
        loginTenantAdmin();

        TrendzSettings trendzSettings = doGet("/api/trendz/settings", TrendzSettings.class);

        assertThat(trendzSettings).isNotNull();
        assertThat(trendzSettings.isEnabled()).isTrue();
        assertThat(trendzSettings.getBaseUrl()).isEqualTo(trendzUrl);

        String updatedUrl = "https://some.domain.com:18888/tenant_trendz";
        trendzSettings.setBaseUrl(updatedUrl);

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

        doPost("/api/trendz/settings", newTrendzSettings).andExpect(status().isForbidden());

        TrendzSettings fetchedTrendzSettings = doGet("/api/trendz/settings", TrendzSettings.class);
        assertThat(fetchedTrendzSettings).isNotNull();
        assertThat(fetchedTrendzSettings.isEnabled()).isTrue();
        assertThat(fetchedTrendzSettings.getBaseUrl()).isEqualTo(trendzUrl);
    }

}
