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
package org.thingsboard.monitoring.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.RegexUtils;
import org.thingsboard.monitoring.client.TbClient;
import org.thingsboard.monitoring.config.transport.DeviceConfig;
import org.thingsboard.monitoring.config.transport.TransportMonitoringConfig;
import org.thingsboard.monitoring.config.transport.TransportMonitoringTarget;
import org.thingsboard.monitoring.config.transport.TransportType;
import org.thingsboard.monitoring.util.ResourceUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.ShortCustomerInfo;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.OutputType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.ScriptCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MBootstrapClientCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.NoSecBootstrapClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.NoSecClientCredential;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.device.data.Lwm2mDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.thingsboard.monitoring.service.BaseHealthChecker.TEST_CF_TELEMETRY_KEY;
import static org.thingsboard.monitoring.service.BaseHealthChecker.TEST_TELEMETRY_KEY;

@Service
@Slf4j
@RequiredArgsConstructor
public class MonitoringEntityService {

    private static final String DASHBOARD_TITLE = "[Monitoring] Cloud monitoring";
    private static final String DASHBOARD_RESOURCE_PATH = "dashboard_cloud_monitoring.json";

    private final TbClient tbClient;

    @Value("${monitoring.calculated_fields.enabled:true}")
    private boolean calculatedFieldsMonitoringEnabled;

    DashboardId dashboardId = null;

    public void checkEntities() {
        RuleChain ruleChain = tbClient.getRuleChains(RuleChainType.CORE, new PageLink(10)).getData().stream()
                .filter(RuleChain::isRoot)
                .findFirst().orElseThrow();
        RuleChainId ruleChainId = ruleChain.getId();

        JsonNode ruleChainDescriptor = ResourceUtils.getResource("rule_chain.json");
        List<String> attributeKeys = tbClient.getAttributeKeys(ruleChainId);
        Map<String, String> attributes = tbClient.getAttributeKvEntries(ruleChainId, attributeKeys).stream()
                .collect(Collectors.toMap(KvEntry::getKey, KvEntry::getValueAsString));

        int currentVersion = Integer.parseInt(attributes.getOrDefault("version", "0"));
        int newVersion = ruleChainDescriptor.get("version").asInt();
        if (currentVersion == newVersion) {
            log.debug("Not updating rule chain, version is the same ({})", currentVersion);
        } else {
            log.info("Updating rule chain '{}' from version {} to {}", ruleChain.getName(), currentVersion, newVersion);

            String metadataJson = RegexUtils.replace(ruleChainDescriptor.get("metadata").toString(),
                    "\\$\\{MONITORING:(.+?)}", matchResult -> {
                        String key = matchResult.group(1);
                        String value = attributes.get(key);
                        if (value == null) {
                            throw new IllegalArgumentException("No attribute found for key " + key);
                        }
                        log.info("Using {}: {}", key, value);
                        return value;
                    });
            RuleChainMetaData metaData = JacksonUtil.fromString(metadataJson, RuleChainMetaData.class);
            metaData.setRuleChainId(ruleChainId);
            tbClient.saveRuleChainMetaData(metaData);
            tbClient.saveEntityAttributesV2(ruleChainId, DataConstants.SERVER_SCOPE, JacksonUtil.newObjectNode()
                    .put("version", newVersion));
        }

        Asset asset = getOrCreateMonitoringAsset();
        Dashboard dashboard = getOrCreateMonitoringDashboard();

        tbClient.assignAssetToPublicCustomer(asset.getId());
        tbClient.assignDashboardToPublicCustomer(dashboard.getId());

        this.dashboardId = Optional.ofNullable(dashboard).map(Dashboard::getId).orElse(null);
    }

    public Asset getOrCreateMonitoringAsset() {
        String assetName = "[Monitoring] Latencies";
        return tbClient.findAsset(assetName).orElseGet(() -> {
            Asset asset = new Asset();
            asset.setType("Monitoring");
            asset.setName(assetName);
            asset = tbClient.saveAsset(asset);
            log.info("Created monitoring asset {}", asset.getId());
            return asset;
        });
    }

    public void checkEntities(TransportMonitoringConfig config, TransportMonitoringTarget target) {
        Device device = getOrCreateDevice(config, target);
        DeviceCredentials credentials = tbClient.getDeviceCredentialsByDeviceId(device.getId())
                .orElseThrow(() -> new IllegalArgumentException("No credentials found for device " + device.getId()));

        DeviceConfig deviceConfig = new DeviceConfig();
        deviceConfig.setId(device.getId().toString());
        deviceConfig.setName(device.getName());
        deviceConfig.setCredentials(credentials);
        target.setDevice(deviceConfig);
    }

    private Device getOrCreateDevice(TransportMonitoringConfig config, TransportMonitoringTarget target) {
        TransportType transportType = config.getTransportType();
        String deviceName = String.format("%s %s (%s) - %s", target.getNamePrefix(), transportType.getName(), target.getQueue(), target.getBaseUrl()).trim();
        Device device = tbClient.getTenantDevice(deviceName).orElse(null);
        if (device != null) {
            if (calculatedFieldsMonitoringEnabled) {
                CalculatedField calculatedField = tbClient.getCalculatedFieldsByEntityId(device.getId(), new PageLink(1, 0, TEST_CF_TELEMETRY_KEY))
                        .getData().stream().findFirst().orElse(null);
                if (calculatedField == null) {
                    createCalculatedField(device);
                }
            }
            return device;
        }

        log.info("Creating new device '{}'", deviceName);
        device = new Device();
        device.setName(deviceName);

        DeviceCredentials credentials = new DeviceCredentials();
        credentials.setCredentialsId(RandomStringUtils.randomAlphabetic(20));
        DeviceData deviceData = new DeviceData();
        deviceData.setConfiguration(new DefaultDeviceConfiguration());

        DeviceProfile deviceProfile = getOrCreateDeviceProfile(config, target);
        device.setType(deviceProfile.getName());
        device.setDeviceProfileId(deviceProfile.getId());

        if (transportType != TransportType.LWM2M) {
            deviceData.setTransportConfiguration(new DefaultDeviceTransportConfiguration());
            credentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        } else {
            deviceData.setTransportConfiguration(new Lwm2mDeviceTransportConfiguration());
            credentials.setCredentialsType(DeviceCredentialsType.LWM2M_CREDENTIALS);
            LwM2MDeviceCredentials lwm2mCreds = new LwM2MDeviceCredentials();
            NoSecClientCredential client = new NoSecClientCredential();
            client.setEndpoint(credentials.getCredentialsId());
            lwm2mCreds.setClient(client);
            LwM2MBootstrapClientCredentials bootstrap = new LwM2MBootstrapClientCredentials();
            bootstrap.setBootstrapServer(new NoSecBootstrapClientCredential());
            bootstrap.setLwm2mServer(new NoSecBootstrapClientCredential());
            lwm2mCreds.setBootstrap(bootstrap);
            credentials.setCredentialsValue(JacksonUtil.toString(lwm2mCreds));
        }

        device = tbClient.saveDeviceWithCredentials(device, credentials).get();
        if (calculatedFieldsMonitoringEnabled) {
            createCalculatedField(device);
        }
        return device;
    }

    private DeviceProfile getOrCreateDeviceProfile(TransportMonitoringConfig config, TransportMonitoringTarget target) {
        TransportType transportType = config.getTransportType();
        String profileName = String.format("%s %s (%s)", target.getNamePrefix(), transportType.getName(), target.getQueue()).trim();
        DeviceProfile deviceProfile = tbClient.getDeviceProfiles(new PageLink(1, 0, profileName)).getData()
                .stream().findFirst().orElse(null);
        if (deviceProfile != null) {
            return deviceProfile;
        }

        log.info("Creating new device profile '{}'", profileName);
        if (transportType != TransportType.LWM2M) {
            deviceProfile = new DeviceProfile();
            deviceProfile.setType(DeviceProfileType.DEFAULT);
            deviceProfile.setTransportType(DeviceTransportType.DEFAULT);
            DeviceProfileData profileData = new DeviceProfileData();
            profileData.setConfiguration(new DefaultDeviceProfileConfiguration());
            profileData.setTransportConfiguration(new DefaultDeviceProfileTransportConfiguration());
            deviceProfile.setProfileData(profileData);
        } else {
            tbClient.getResources(new PageLink(1, 0, "LwM2M Monitoring")).getData()
                    .stream().findFirst()
                    .orElseGet(() -> {
                        TbResource newResource = ResourceUtils.getResource("lwm2m/resource.json", TbResource.class);
                        log.info("Creating LwM2M resource");
                        return tbClient.saveResource(newResource);
                    });
            deviceProfile = ResourceUtils.getResource("lwm2m/device_profile.json", DeviceProfile.class);
        }

        deviceProfile.setName(profileName);
        deviceProfile.setDefaultQueueName(target.getQueue());
        return tbClient.saveDeviceProfile(deviceProfile);
    }

    private void createCalculatedField(Device device) {
        log.info("Creating calculated field for device '{}'", device.getName());
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setName(TEST_CF_TELEMETRY_KEY);
        calculatedField.setEntityId(device.getId());
        calculatedField.setType(CalculatedFieldType.SCRIPT);
        ScriptCalculatedFieldConfiguration configuration = new ScriptCalculatedFieldConfiguration();
        Argument testDataArgument = new Argument();
        testDataArgument.setRefEntityKey(new ReferencedEntityKey(TEST_TELEMETRY_KEY, ArgumentType.TS_LATEST, null));
        configuration.setArguments(Map.of(
                TEST_TELEMETRY_KEY, testDataArgument
        ));
        configuration.setExpression("return { \"" + TEST_CF_TELEMETRY_KEY + "\": " + TEST_TELEMETRY_KEY + " + \"-cf\" };");
        Output output = new Output();
        output.setType(OutputType.TIME_SERIES);
        configuration.setOutput(output);
        calculatedField.setConfiguration(configuration);
        calculatedField.setDebugMode(true);
        tbClient.saveCalculatedField(calculatedField);
    }

    public String getDashboardPublicLink() {
        String link = "";
        try {
            Optional<DashboardInfo> infoOpt = tbClient.getDashboardInfoById(dashboardId);
            if (infoOpt.isPresent()) {
                String publicCustomerId = null;
                Set<ShortCustomerInfo> customers = infoOpt.get().getAssignedCustomers();
                if (customers != null) {
                    publicCustomerId = customers.stream()
                            .filter(ShortCustomerInfo::isPublic)
                            .map(c -> c.getCustomerId().getId().toString())
                            .findFirst().orElse(null);
                }
                if (publicCustomerId != null) {
                    link = buildPublicDashboardLink(dashboardId, publicCustomerId);
                    log.info("Public Monitoring dashboard link: {}", link);
                } else {
                    log.warn("Dashboard is not assigned to public customer. Public link can't be generated.");
                }
            }
        } catch (Exception e) {
            log.error("Failed to get a public link to Monitoring dashboard ", e);
        }
        return link;
    }

    private Dashboard getOrCreateMonitoringDashboard() {
        Dashboard existing = findDashboardByTitle(DASHBOARD_TITLE).orElse(null);
        if (existing != null) {
            log.debug("Found Monitoring dashboard '{}' with id {}", existing.getTitle(), existing.getId());
            return existing;
        }

        Dashboard dashboardFromResource = ResourceUtils.getResource(DASHBOARD_RESOURCE_PATH, Dashboard.class);
        dashboardFromResource.setTitle(DASHBOARD_TITLE);
        //Optional.ofNullable(existing).map(Dashboard::getId).ifPresent(dashboardFromResource::setId);
        Dashboard saved = tbClient.saveDashboard(dashboardFromResource);
        log.info("Created Monitoring dashboard '{}' with id {}", saved.getTitle(), saved.getId());
        return saved;
    }

    private Optional<Dashboard> findDashboardByTitle(String title) {
        // Use text search first and then filter by exact title
        PageData<DashboardInfo> page = tbClient.getTenantDashboards(new PageLink(10, 0, title));
        return page.getData().stream()
                .filter(info -> title.equals(info.getTitle()))
                .findFirst()
                .flatMap(info -> tbClient.getDashboardById(info.getId()));
    }

    private String buildPublicDashboardLink(DashboardId dashboardId, String publicCustomerId) {
        String base = getBaseUrl();
        return String.format("%s/dashboard/%s?publicId=%s", base, dashboardId.getId().toString(), publicCustomerId);
    }

    private String getBaseUrl() {
        // TbClient.baseURL contains the root url, without trailing slash
        try {
            var baseUrlField = tbClient.getClass().getSuperclass().getDeclaredField("baseURL");
            baseUrlField.setAccessible(true);
            return (String) baseUrlField.get(tbClient);
        } catch (Exception e) {
            log.warn("Unable to access baseURL from RestClient. Falling back to http://localhost:8080");
            return "http://localhost:8080";
        }
    }

}
