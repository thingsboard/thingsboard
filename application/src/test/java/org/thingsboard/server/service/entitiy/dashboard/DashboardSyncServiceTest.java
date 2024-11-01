/**
 * Copyright © 2016-2024 The Thingsboard Authors
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

    /*
    * TODO - DISCUSS
    *  we can't use etag in the image/resource tag (image and resource can be updated) (OR CAN? - on export we convert urls to tags automatically.
    *  if don't want to update all resource's usages - just use link)
    *  but also cannot use key - it can be changed if resource/image with such key already exists.
    *  store resources alongside all system widgets/dashboards - same structure when exporting.
    *  for gateways dashboard repository - use link instead of tag, not to update ref each time the resource is updated.
    *
    *
    *  TODO:
    *   update system widgets with the new structure
    *
    *  TODO CONSIDER
    *   leave image/resource link as is. when importing - keep track of imported resources (if the resource key changed) (map of resourceKey -> importedResourceKey).
    *   then resolve and update resource links if resource key is new (idx added)
    * */

//    FIXME: need to update resource key all the time???? because etag is changed. same for images....

//    @Ignore
    // TODO: update gateway dashboard repository; for now the test will fail because gateway-management-extension.js is referenced by id instead of tb-resource
    @Test
    public void testGatewaysDashboardSync() throws Exception {
        loginTenantAdmin();
        await().atMost(45, TimeUnit.SECONDS).untilAsserted(() -> {
            MockHttpServletResponse response = doGet("/api/resource/dashboard/system/gateways_dashboard.json")
                    .andExpect(status().isOk())
                    .andReturn().getResponse();
            String dashboardJson = response.getContentAsString();

            assertThat(dashboardJson).contains("tb-resource;/api/resource/js_module/system/gateway-management-extension.js"); // checking that resource link is used
            assertThat(dashboardJson).doesNotContain("${RESOURCE");
            Dashboard dashboard = JacksonUtil.fromString(dashboardJson, Dashboard.class);
            assertThat(dashboard).isNotNull();
            assertThat(dashboard.getTitle()).containsIgnoringCase("gateway");
            assertThat(dashboard.getImage()).startsWith("tb-image;/api/images/system/gateway");
            assertThat(response.getHeader("ETag")).isNotBlank();
        });
    }

}