// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.entitiy.dashboard;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.sql.resource.TbResourceRepository;
import org.thingsboard.server.dao.sql.widget.WidgetTypeRepository;
import org.thingsboard.server.dao.sql.widget.WidgetsBundleRepository;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
@TestPropertySource(properties = {
        "transport.gateway.dashboard.sync.enabled=true"
})
public class DashboardSyncServiceTest extends AbstractControllerTest {

    @Autowired
    private WidgetTypeRepository widgetTypeRepository;
    @Autowired
    private WidgetsBundleRepository widgetsBundleRepository;
    @Autowired
    private TbResourceRepository resourceRepository;

    @After
    public void after() throws Exception {
        widgetsBundleRepository.deleteAll();
        widgetTypeRepository.deleteAll();
        resourceRepository.deleteAll();
    }

    @Test
    public void testGatewaysDashboardSync() throws Exception {
        loginTenantAdmin();
        await().atMost(45, TimeUnit.SECONDS).untilAsserted(() -> {
            MockHttpServletResponse response = doGet("/api/resource/dashboard/system/gateways_dashboard.json")
                    .andExpect(status().isOk())
                    .andReturn().getResponse();
            String dashboardJson = response.getContentAsString();
            Dashboard dashboard = JacksonUtil.fromString(dashboardJson, Dashboard.class);
            assertThat(dashboard).isNotNull();
            assertThat(dashboard.getTitle()).containsIgnoringCase("gateway");
            assertThat(response.getHeader("ETag")).isNotBlank();
        });
    }

}