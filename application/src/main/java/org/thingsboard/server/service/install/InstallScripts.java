/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.service.install;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.thingsboard.server.service.install.DatabaseHelper.objectMapper;

/**
 * Created by ashvayka on 18.04.18.
 */
@Component
@Slf4j
public class InstallScripts {

    public static final String APP_DIR = "application";
    public static final String SRC_DIR = "src";
    public static final String MAIN_DIR = "main";
    public static final String DATA_DIR = "data";
    public static final String JSON_DIR = "json";
    public static final String SYSTEM_DIR = "system";
    public static final String TENANT_DIR = "tenant";
    public static final String DEMO_DIR = "demo";
    public static final String RULE_CHAINS_DIR = "rule_chains";
    public static final String WIDGET_BUNDLES_DIR = "widget_bundles";
    public static final String DASHBOARDS_DIR = "dashboards";

    public static final String JSON_EXT = ".json";

    @Value("${install.data_dir:}")
    private String dataDir;

    @Autowired
    private RuleChainService ruleChainService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private WidgetTypeService widgetTypeService;

    @Autowired
    private WidgetsBundleService widgetsBundleService;

    public Path getTenantRuleChainsDir() {
        return Paths.get(getDataDir(), JSON_DIR, TENANT_DIR, RULE_CHAINS_DIR);
    }

    public String getDataDir() {
        if (!StringUtils.isEmpty(dataDir)) {
            if (!Paths.get(this.dataDir).toFile().isDirectory()) {
                throw new RuntimeException("'install.data_dir' property value is not a valid directory!");
            }
            return dataDir;
        } else {
            String workDir = System.getProperty("user.dir");
            if (workDir.endsWith("application")) {
                return Paths.get(workDir, SRC_DIR, MAIN_DIR, DATA_DIR).toString();
            } else {
                Path dataDirPath = Paths.get(workDir, APP_DIR, SRC_DIR, MAIN_DIR, DATA_DIR);
                if (Files.exists(dataDirPath)) {
                    return dataDirPath.toString();
                } else {
                    throw new RuntimeException("Not valid working directory: " + workDir + ". Please use either root project directory, application module directory or specify valid \"install.data_dir\" ENV variable to avoid automatic data directory lookup!");
                }
            }
        }
    }

    public void createDefaultRuleChains(TenantId tenantId) throws IOException {
        Path tenantChainsDir = getTenantRuleChainsDir();
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(tenantChainsDir, path -> path.toString().endsWith(InstallScripts.JSON_EXT))) {
            dirStream.forEach(
                    path -> loadRuleChainFromFile(tenantId, path)
            );
        }
    }

    public void loadSystemWidgets() throws Exception {
        Path widgetBundlesDir = Paths.get(getDataDir(), JSON_DIR, SYSTEM_DIR, WIDGET_BUNDLES_DIR);
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(widgetBundlesDir, path -> path.toString().endsWith(JSON_EXT))) {
            dirStream.forEach(
                    path -> {
                        try {
                            JsonNode widgetsBundleDescriptorJson = objectMapper.readTree(path.toFile());
                            JsonNode widgetsBundleJson = widgetsBundleDescriptorJson.get("widgetsBundle");
                            WidgetsBundle widgetsBundle = objectMapper.treeToValue(widgetsBundleJson, WidgetsBundle.class);
                            WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);
                            JsonNode widgetTypesArrayJson = widgetsBundleDescriptorJson.get("widgetTypes");
                            widgetTypesArrayJson.forEach(
                                    widgetTypeJson -> {
                                        try {
                                            WidgetType widgetType = objectMapper.treeToValue(widgetTypeJson, WidgetType.class);
                                            widgetType.setBundleAlias(savedWidgetsBundle.getAlias());
                                            widgetTypeService.saveWidgetType(widgetType);
                                        } catch (Exception e) {
                                            log.error("Unable to load widget type from json: [{}]", path.toString());
                                            throw new RuntimeException("Unable to load widget type from json", e);
                                        }
                                    }
                            );
                        } catch (Exception e) {
                            log.error("Unable to load widgets bundle from json: [{}]", path.toString());
                            throw new RuntimeException("Unable to load widgets bundle from json", e);
                        }
                    }
            );
        }
    }

    public void loadDashboards(TenantId tenantId, CustomerId customerId) throws Exception {
        Path dashboardsDir = Paths.get(getDataDir(), JSON_DIR, DEMO_DIR, DASHBOARDS_DIR);
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(dashboardsDir, path -> path.toString().endsWith(JSON_EXT))) {
            dirStream.forEach(
                    path -> {
                        try {
                            JsonNode dashboardJson = objectMapper.readTree(path.toFile());
                            Dashboard dashboard = objectMapper.treeToValue(dashboardJson, Dashboard.class);
                            dashboard.setTenantId(tenantId);
                            Dashboard savedDashboard = dashboardService.saveDashboard(dashboard);
                            if (customerId != null && !customerId.isNullUid()) {
                                dashboardService.assignDashboardToCustomer(new TenantId(EntityId.NULL_UUID), savedDashboard.getId(), customerId);
                            }
                        } catch (Exception e) {
                            log.error("Unable to load dashboard from json: [{}]", path.toString());
                            throw new RuntimeException("Unable to load dashboard from json", e);
                        }
                    }
            );
        }
    }


    public void loadDemoRuleChains(TenantId tenantId) throws Exception {
        Path ruleChainsDir = Paths.get(getDataDir(), JSON_DIR, DEMO_DIR, RULE_CHAINS_DIR);
        try {
            JsonNode ruleChainJson = objectMapper.readTree(ruleChainsDir.resolve("thermostat_alarms.json").toFile());
            RuleChain ruleChain = objectMapper.treeToValue(ruleChainJson.get("ruleChain"), RuleChain.class);
            RuleChainMetaData ruleChainMetaData = objectMapper.treeToValue(ruleChainJson.get("metadata"), RuleChainMetaData.class);
            ruleChain.setTenantId(tenantId);
            ruleChain = ruleChainService.saveRuleChain(ruleChain);
            ruleChainMetaData.setRuleChainId(ruleChain.getId());
            ruleChainService.saveRuleChainMetaData(new TenantId(EntityId.NULL_UUID), ruleChainMetaData);

            JsonNode rootChainJson = objectMapper.readTree(ruleChainsDir.resolve("root_rule_chain.json").toFile());
            RuleChain rootChain = objectMapper.treeToValue(rootChainJson.get("ruleChain"), RuleChain.class);
            RuleChainMetaData rootChainMetaData = objectMapper.treeToValue(rootChainJson.get("metadata"), RuleChainMetaData.class);

            RuleChainId thermostatsRuleChainId = ruleChain.getId();
            rootChainMetaData.getRuleChainConnections().forEach(connection -> connection.setTargetRuleChainId(thermostatsRuleChainId));
            rootChain.setTenantId(tenantId);
            rootChain = ruleChainService.saveRuleChain(rootChain);
            rootChainMetaData.setRuleChainId(rootChain.getId());
            ruleChainService.saveRuleChainMetaData(new TenantId(EntityId.NULL_UUID), rootChainMetaData);

            loadRuleChainFromFile(tenantId, ruleChainsDir.resolve("edge_root_rule_chain.json"));
        } catch (Exception e) {
            log.error("Unable to load dashboard from json", e);
            throw new RuntimeException("Unable to load dashboard from json", e);
        }
    }

    private void loadRuleChainFromFile(TenantId tenantId, Path ruleChainPath) {
        try {
            JsonNode ruleChainJson = objectMapper.readTree(ruleChainPath.toFile());
            RuleChain ruleChain = objectMapper.treeToValue(ruleChainJson.get("ruleChain"), RuleChain.class);
            RuleChainMetaData ruleChainMetaData = objectMapper.treeToValue(ruleChainJson.get("metadata"), RuleChainMetaData.class);

            ruleChain.setTenantId(tenantId);
            ruleChain = ruleChainService.saveRuleChain(ruleChain);

            ruleChainMetaData.setRuleChainId(ruleChain.getId());
            ruleChainService.saveRuleChainMetaData(new TenantId(EntityId.NULL_UUID), ruleChainMetaData);
        } catch (Exception e) {
            log.error("Unable to load rule chain from json: [{}]", ruleChainPath.toString());
            throw new RuntimeException("Unable to load rule chain from json", e);
        }
    }
}
