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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.service.solutions.data.SolutionValidationResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultSolutionServiceTest {

    private static final String CONFLICTS_INTRO =
            "Some entities of the solution template already exist. Rename or delete them and install the template again:";
    // mirrors DefaultSolutionService.MAX_LISTED_NAMES_PER_TYPE and the note that goes with it
    private static final int LISTED_NAMES_PER_TYPE = 10;
    private static final String TRUNCATION_NOTE = "Only the first " + LISTED_NAMES_PER_TYPE
            + " names of each type are listed. The rest are reported the next time the template is installed.";

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());

    @Mock
    private CustomerService customerService;
    @Mock
    private DeviceService deviceService;
    @Mock
    private AssetService assetService;
    @Mock
    private DashboardService dashboardService;
    @Mock
    private RuleChainService ruleChainService;
    @Mock
    private DeviceProfileService deviceProfileService;
    @Mock
    private AssetProfileService assetProfileService;
    @Mock
    private EdgeService edgeService;

    @InjectMocks
    private DefaultSolutionService service;

    @TempDir
    private Path tempDir;

    @Test
    void testValidateSolutionNamesEveryConflictingEntity() throws IOException {
        writeEntitiesFile("customers.json", "[{\"name\": \"Existing customer\"}, {\"name\": \"Customer $random\"}]");
        writeEntitiesFile("devices.json", "[{\"name\": \"Existing device\"}, {\"name\": \"New device\"}]");
        writeEntitiesFile("assets.json", "[{\"name\": \"Existing asset\"}, {\"name\": \"New asset\"}]");

        Customer customer = new Customer();
        customer.setTitle("Existing customer");

        when(customerService.findCustomerByTenantIdAndTitle(tenantId, "Existing customer")).thenReturn(Optional.of(customer));
        Asset asset = new Asset();
        asset.setName("Existing asset");
        when(assetService.findAssetByTenantIdAndName(tenantId, "Existing asset")).thenReturn(asset);
        when(assetService.findAssetByTenantIdAndName(tenantId, "New asset")).thenReturn(null);
        when(deviceService.findDeviceByTenantIdAndName(tenantId, "Existing device"))
                .thenReturn(existingDevice("Existing device"));
        when(deviceService.findDeviceByTenantIdAndName(tenantId, "New device")).thenReturn(null);

        SolutionValidationResult result = service.validateSolution(tenantId, tempDir);

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getConflictReport().lines().toList())
                .containsSubsequence(
                        CONFLICTS_INTRO,
                        "- **Customer**: 'Existing customer'",
                        "- **Asset**: 'Existing asset'",
                        "- **Device**: 'Existing device'");
        // the randomized customer title and the entities that do not exist yet are not reported
        assertThat(result.getConflictReport())
                .doesNotContain("Customer $random")
                .doesNotContain("New device")
                .doesNotContain("New asset");
    }

    @Test
    void testValidateSolutionReportsConflictingDashboard() throws IOException {
        writeEntitiesFile("dashboards.json", "[{\"name\": \"Overview\", \"file\": \"overview.json\"}]");

        DashboardInfo dashboard = new DashboardInfo();
        dashboard.setTitle("Overview");
        when(dashboardService.findFirstDashboardInfoByTenantIdAndName(tenantId, "Overview")).thenReturn(dashboard);

        SolutionValidationResult result = service.validateSolution(tenantId, tempDir);

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getConflictReport().lines().toList())
                .containsSubsequence(CONFLICTS_INTRO, "- **Dashboard**: 'Overview'");
    }

    @Test
    void testValidateSolutionUsesTheRuleChainNameFromTheRuleChainFile() throws IOException {
        writeEntitiesFile("rule_chains.json", "[{\"name\": \"Name in the definition\", \"file\": \"rc.json\"}]");
        writeFile("rule_chains/rc.json", "{\"ruleChain\": {\"name\": \"Name the install creates\"}}");

        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("Name the install creates");
        when(ruleChainService.findTenantRuleChainsByTypeAndName(tenantId, RuleChainType.CORE, "Name the install creates"))
                .thenReturn(List.of(ruleChain));

        SolutionValidationResult result = service.validateSolution(tenantId, tempDir);

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getConflictReport().lines().toList())
                .containsSubsequence(CONFLICTS_INTRO, "- **Rule chain**: 'Name the install creates'");
    }

    @Test
    void testValidateSolutionReportsConflictingProfilesAndEdge() throws IOException {
        writeEntitiesFile("device_profiles.json", "[{\"name\": \"thermostat\"}]");
        writeEntitiesFile("asset_profiles.json", "[{\"name\": \"building\"}]");
        writeEntitiesFile("edges.json", "[{\"name\": \"Main edge\"}]");

        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setName("thermostat");
        AssetProfile assetProfile = new AssetProfile();
        assetProfile.setName("building");
        Edge edge = new Edge();
        edge.setName("Main edge");
        when(deviceProfileService.findDeviceProfileByName(tenantId, "thermostat")).thenReturn(deviceProfile);
        when(assetProfileService.findAssetProfileByName(tenantId, "building")).thenReturn(assetProfile);
        when(edgeService.findEdgeByTenantIdAndName(tenantId, "Main edge")).thenReturn(edge);

        SolutionValidationResult result = service.validateSolution(tenantId, tempDir);

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getConflictReport().lines().toList()).containsSubsequence(
                "- **Device profile**: 'thermostat'",
                "- **Asset profile**: 'building'",
                "- **Edge**: 'Main edge'");
    }

    @Test
    void testValidateSolutionLooksTheSameNameUpOnce() throws IOException {
        writeEntitiesFile("devices.json", "[{\"name\": \"Sensor\"}, {\"name\": \"Sensor\"}]");

        when(deviceService.findDeviceByTenantIdAndName(tenantId, "Sensor")).thenReturn(existingDevice("Sensor"));

        SolutionValidationResult result = service.validateSolution(tenantId, tempDir);

        assertThat(result.getConflictReport().lines().toList()).contains("- **Device**: 'Sensor'");
        verify(deviceService, times(1)).findDeviceByTenantIdAndName(tenantId, "Sensor");
    }

    @Test
    void testValidateSolutionSkipsRuleChainsWithoutAResolvableName() throws IOException {
        writeEntitiesFile("rule_chains.json", "[{\"name\": \"No file\", \"file\": \"missing.json\"}," +
                "{\"name\": \"No name inside\", \"file\": \"nameless.json\"}]");
        writeFile("rule_chains/nameless.json", "{\"ruleChain\": {}}");

        assertThat(service.validateSolution(tenantId, tempDir).isPassed()).isTrue();
        verifyNoInteractions(ruleChainService);
    }

    @Test
    void testValidateSolutionUsesTheRuleChainTypeFromTheRuleChainFile() throws IOException {
        writeEntitiesFile("rule_chains.json", "[{\"name\": \"Edge chain\", \"file\": \"rc.json\"}]");
        writeFile("rule_chains/rc.json", "{\"ruleChain\": {\"name\": \"Edge chain\", \"type\": \"EDGE\"}}");

        RuleChain ruleChain = new RuleChain();
        ruleChain.setName("Edge chain");
        when(ruleChainService.findTenantRuleChainsByTypeAndName(tenantId, RuleChainType.EDGE, "Edge chain"))
                .thenReturn(List.of(ruleChain));

        SolutionValidationResult result = service.validateSolution(tenantId, tempDir);

        assertThat(result.getConflictReport().lines().toList()).contains("- **Rule chain**: 'Edge chain'");
    }

    @Test
    void testValidateSolutionTreatsAJsonNullFileAsEmpty() throws IOException {
        writeEntitiesFile("devices.json", "null");

        assertThat(service.validateSolution(tenantId, tempDir).isPassed()).isTrue();
        verifyNoInteractions(deviceService);
    }

    @ParameterizedTest
    @ValueSource(ints = {LISTED_NAMES_PER_TYPE, LISTED_NAMES_PER_TYPE + 1, LISTED_NAMES_PER_TYPE + 2})
    void testValidateSolutionListsAtMostTenNamesOfAType(int conflictCount) throws IOException {
        List<String> names = IntStream.rangeClosed(1, conflictCount).mapToObj(i -> "Sensor " + i).toList();
        writeEntitiesFile("devices.json", names.stream()
                .map(name -> "{\"name\": \"" + name + "\"}")
                .collect(Collectors.joining(",", "[", "]")));
        names.forEach(name -> when(deviceService.findDeviceByTenantIdAndName(tenantId, name))
                .thenReturn(existingDevice(name)));

        SolutionValidationResult result = service.validateSolution(tenantId, tempDir);

        int hidden = conflictCount - LISTED_NAMES_PER_TYPE;
        String listed = names.stream().limit(LISTED_NAMES_PER_TYPE)
                .map(name -> "'" + name + "'").collect(Collectors.joining(", "));
        assertThat(result.getConflictReport().lines().toList()).contains("- **Device**: " + listed
                + (hidden > 0 ? " and " + hidden + " more (" + conflictCount + " in total)" : ""));
        if (hidden > 0) {
            assertThat(result.getConflictReport()).contains(TRUNCATION_NOTE);
        } else {
            assertThat(result.getConflictReport()).doesNotContain(TRUNCATION_NOTE);
        }
    }

    /**
     * The profile lists are the json file plus every file in the directory of the same name merged into one list, so
     * this is where the same name genuinely can appear twice.
     */
    @Test
    void testValidateSolutionChecksAProfileFromBothSourcesOnce() throws IOException {
        writeEntitiesFile("device_profiles.json", "[{\"name\": \"Sensor profile\"}]");
        writeFile("device_profiles/sensor_profile.json", "{\"name\": \"Sensor profile\"}");

        DeviceProfile profile = new DeviceProfile();
        profile.setName("Sensor profile");
        when(deviceProfileService.findDeviceProfileByName(tenantId, "Sensor profile")).thenReturn(profile);

        SolutionValidationResult result = service.validateSolution(tenantId, tempDir);

        assertThat(result.getConflictReport().lines().toList()).contains("- **Device profile**: 'Sensor profile'");
        verify(deviceProfileService, times(1)).findDeviceProfileByName(tenantId, "Sensor profile");
    }

    @Test
    void testValidateSolutionWithoutConflicts() throws IOException {
        writeEntitiesFile("devices.json", "[{\"name\": \"New device\"}]");
        when(deviceService.findDeviceByTenantIdAndName(tenantId, "New device")).thenReturn(null);

        assertThat(service.validateSolution(tenantId, tempDir).isPassed()).isTrue();
    }

    @Test
    void testValidateSolutionWithoutEntityFiles() {
        assertThat(service.validateSolution(tenantId, tempDir).isPassed()).isTrue();
        verifyNoInteractions(customerService, deviceService, assetService, dashboardService, ruleChainService,
                deviceProfileService, assetProfileService, edgeService);
    }

    private static Device existingDevice(String name) {
        Device device = new Device();
        device.setName(name);
        return device;
    }

    private void writeFile(String relativePath, String content) throws IOException {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private void writeEntitiesFile(String fileName, String content) throws IOException {
        Path entitiesDir = Files.createDirectories(tempDir.resolve("entities"));
        Files.writeString(entitiesDir.resolve(fileName), content);
    }

}
