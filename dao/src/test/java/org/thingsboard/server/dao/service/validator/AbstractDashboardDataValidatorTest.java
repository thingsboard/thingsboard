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

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.UUID;

import static org.mockito.BDDMockito.willReturn;

@SpringBootTest(classes = DashboardDataValidator.class)
public abstract class AbstractDashboardDataValidatorTest {

    protected static final String ALIAS_UUID = "a1ddb8fa-90ff-5598-e7f2-e254194d055d";

    @MockitoBean
    protected TenantService tenantService;
    @Autowired
    protected DashboardDataValidator validator;

    protected final TenantId tenantId = TenantId.fromUUID(UUID.fromString("9ef79cdf-37a8-4119-b682-2e7ed4e018da"));

    @BeforeEach
    void setUp() {
        willReturn(true).given(tenantService).tenantExists(tenantId);
    }

    protected Dashboard dashboardWith(JsonNode configuration) {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("test dashboard");
        dashboard.setTenantId(tenantId);
        dashboard.setConfiguration(configuration);
        return dashboard;
    }

    protected Dashboard filterAlias(String aliasName, String filterJson) {
        String json = """
                {
                  "entityAliases": {
                    "%s": {
                      "id": "%s",
                      "alias": "%s",
                      "filter": %s
                    }
                  }
                }""".formatted(ALIAS_UUID, ALIAS_UUID, aliasName, filterJson);
        return dashboardWith(JacksonUtil.toJsonNode(json));
    }

    protected void validate(Dashboard dashboard) {
        validator.validateDataImpl(tenantId, dashboard);
    }

}
