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
package org.thingsboard.server.service.solutions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.service.solutions.data.solution.SolutionInstallResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultSolutionServiceTest {

    private static final String CONFLICTS_INTRO =
            "Some entities of the solution template already exist. Rename or delete them and install the template again:";

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());

    @Mock
    private CustomerService customerService;
    @Mock
    private DeviceService deviceService;
    @Mock
    private AssetService assetService;
    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private DefaultSolutionService service;

    @TempDir
    private Path tempDir;

    @Test
    void testValidateSolutionNamesEveryConflictingEntity() throws IOException {
        writeEntitiesFile("customers.json", "[{\"name\": \"Existing customer\"}, {\"name\": \"Customer $random\"}]");
        writeEntitiesFile("devices.json", "[{\"name\": \"Existing device\"}, {\"name\": \"New device\"}]");
        writeEntitiesFile("assets.json", "[{\"name\": \"New asset\"}]");

        Customer customer = new Customer();
        customer.setTitle("Existing customer");
        Device device = new Device();
        device.setName("Existing device");

        when(customerService.findCustomerByTenantIdAndTitle(tenantId, "Existing customer")).thenReturn(Optional.of(customer));
        when(assetService.findAssetByTenantIdAndName(tenantId, "New asset")).thenReturn(null);
        when(deviceService.findDeviceByTenantIdAndName(tenantId, "Existing device")).thenReturn(device);
        when(deviceService.findDeviceByTenantIdAndName(tenantId, "New device")).thenReturn(null);

        SolutionInstallResponse result = service.validateSolution(tenantId, tempDir);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getDetails().lines().toList())
                .containsSubsequence(
                        CONFLICTS_INTRO,
                        "- **Customer**: 'Existing customer'",
                        "- **Device**: 'Existing device'");
        // the randomized customer title and the entities that do not exist yet are not reported
        assertThat(result.getDetails())
                .doesNotContain("Customer $random")
                .doesNotContain("New device")
                .doesNotContain("New asset");
    }

    @Test
    void testValidateSolutionMatchesDashboardTitleExactly() throws IOException {
        writeEntitiesFile("dashboards.json", "[{\"name\": \"Overview\", \"file\": \"overview.json\"}]");

        DashboardInfo otherDashboard = new DashboardInfo();
        otherDashboard.setTitle("Overview copy");
        // the title search is a 'contains' match, so a dashboard with a different title must not be reported
        when(dashboardService.findDashboardsByTenantId(eq(tenantId), any()))
                .thenReturn(new PageData<>(List.of(otherDashboard), 1, 1, false));

        assertThat(service.validateSolution(tenantId, tempDir)).isNull();
    }

    @Test
    void testValidateSolutionWithoutConflicts() throws IOException {
        writeEntitiesFile("devices.json", "[{\"name\": \"New device\"}]");
        when(deviceService.findDeviceByTenantIdAndName(tenantId, "New device")).thenReturn(null);

        assertThat(service.validateSolution(tenantId, tempDir)).isNull();
    }

    @Test
    void testValidateSolutionWithoutEntityFiles() {
        assertThat(service.validateSolution(tenantId, tempDir)).isNull();
        verifyNoInteractions(customerService, deviceService, assetService, dashboardService);
    }

    private void writeEntitiesFile(String fileName, String content) throws IOException {
        Path entitiesDir = Files.createDirectories(tempDir.resolve("entities"));
        Files.writeString(entitiesDir.resolve(fileName), content);
    }

}
