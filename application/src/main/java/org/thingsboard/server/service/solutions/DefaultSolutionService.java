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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonParser;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.cf.configuration.ArgumentsBasedCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.iot_hub.SolutionTemplateInstalledItemDescriptor;
import org.thingsboard.server.common.data.kv.BaseDeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceConnectivityService;
import org.thingsboard.server.dao.device.DockerComposeParams;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.exception.EntitiesLimitExceededException;
import org.thingsboard.server.exception.ThingsboardRuntimeException;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.action.EntityActionService;
import org.thingsboard.server.service.entitiy.asset.TbAssetService;
import org.thingsboard.server.service.entitiy.cf.TbCalculatedFieldService;
import org.thingsboard.server.service.entitiy.device.TbDeviceService;
import org.thingsboard.server.service.entitiy.edge.TbEdgeService;
import org.thingsboard.server.service.entitiy.entity.relation.TbEntityRelationService;
import org.thingsboard.server.service.rule.TbRuleChainService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.system.SystemSecurityService;
import org.thingsboard.server.service.solutions.data.CreatedAlarmRuleInfo;
import org.thingsboard.server.service.solutions.data.CreatedCalculatedFieldInfo;
import org.thingsboard.server.service.solutions.data.CreatedEntityInfo;
import org.thingsboard.server.service.solutions.data.DashboardLinkInfo;
import org.thingsboard.server.service.solutions.data.DeviceCredentialsInfo;
import org.thingsboard.server.service.solutions.data.EdgeLinkInfo;
import org.thingsboard.server.service.solutions.data.SolutionInstallContext;
import org.thingsboard.server.service.solutions.data.UserCredentialsInfo;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.thingsboard.server.service.solutions.data.definition.AssetDefinition;
import org.thingsboard.server.service.solutions.data.definition.AssetProfileDefinition;
import org.thingsboard.server.service.solutions.data.definition.DeviceDefinition;
import org.thingsboard.server.service.solutions.data.definition.CustomerDefinition;
import org.thingsboard.server.service.solutions.data.definition.DashboardUserDetailsDefinition;
import org.thingsboard.server.service.solutions.data.definition.CalculatedFieldDefinition;
import org.thingsboard.server.service.solutions.data.definition.EmulatorDefinition;
import org.thingsboard.server.service.solutions.data.definition.UserDefinition;
import org.thingsboard.server.service.solutions.data.definition.DashboardDefinition;
import org.thingsboard.server.service.solutions.data.definition.DeviceProfileDefinition;
import org.thingsboard.server.service.solutions.data.definition.EdgeDefinition;
import org.thingsboard.server.service.solutions.data.definition.RelationDefinition;
import org.thingsboard.server.service.solutions.data.definition.ReferenceableEntityDefinition;
import org.thingsboard.server.service.solutions.data.definition.TenantDefinition;
import org.thingsboard.server.service.solutions.data.emulator.AssetEmulatorLauncher;
import org.thingsboard.server.service.solutions.data.emulator.DeviceEmulatorLauncher;
import org.thingsboard.server.service.solutions.data.names.RandomNameData;
import org.thingsboard.server.service.solutions.data.names.RandomNameUtil;
import org.thingsboard.server.service.solutions.data.solution.SolutionInstallResponse;
import org.thingsboard.server.service.solutions.data.solution.TenantSolutionTemplateInstructions;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class DefaultSolutionService implements SolutionService {

    @Value("${ui.solution_templates.docs_base_url:https://thingsboard.io/docs}")
    private String docsBaseUrl;

    @Value("${iot-hub.max-uncompressed-archive-bytes:209715200}")
    private long maxUncompressedArchiveBytes;

    @Value("${iot-hub.max-uncompressed-entry-bytes:52428800}")
    private long maxUncompressedEntryBytes;

    @Value("${iot-hub.max-archive-entry-count:10000}")
    private int maxArchiveEntryCount;

    @Value("${iot-hub.max-install-timeout-ms:60000}")
    private long maxInstallTimeoutMs;

    private final RuleChainService ruleChainService;
    private final TbRuleChainService tbRuleChainService;
    private final DeviceProfileService deviceProfileService;
    private final DeviceService deviceService;
    private final TbDeviceService tbDeviceService;
    private final DeviceCredentialsService deviceCredentialsService;
    private final AssetProfileService assetProfileService;
    private final AssetService assetService;
    private final TbAssetService tbAssetService;
    private final CustomerService customerService;
    private final UserService userService;
    private final DashboardService dashboardService;
    private final EdgeService edgeService;
    private final TbEdgeService tbEdgeService;
    private final TbEntityRelationService relationService;
    private final AlarmService alarmService;
    private final CalculatedFieldService calculatedFieldService;
    private final TbCalculatedFieldService tbCalculatedFieldService;
    private final AttributesService attributesService;
    private final TimeseriesService tsService;
    private final EntityActionService entityActionService;
    private final SystemSecurityService systemSecurityService;
    private final TbClusterService tbClusterService;
    private final PartitionService partitionService;
    private final TbQueueProducerProvider tbQueueProducerProvider;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TelemetrySubscriptionService tsSubService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final DeviceConnectivityService deviceConnectivityService;

    private final ExecutorService emulatorExecutor = ThingsBoardExecutors.newWorkStealingPool(10, "solution-emulators-executor");

    @PreDestroy
    private void destroy() {
        emulatorExecutor.shutdownNow();
    }

    @Override
    public SolutionInstallResponse installSolution(SecurityUser user, TenantId tenantId, byte[] zipData, HttpServletRequest request) throws Exception {
        Path tempDir = Files.createTempDirectory("iot-hub-solution-");
        try {
            try {
                extractZip(zipData, tempDir);
            } catch (Throwable e) {
                log.error("[{}] Failed to extract solution template zip", tenantId, e);
                deleteDirectory(tempDir);
                TenantSolutionTemplateInstructions instructions = new TenantSolutionTemplateInstructions();
                instructions.setDetails(e.getMessage());
                return new SolutionInstallResponse(instructions, false, List.of());
            }

            String solutionId = loadSolutionId(tempDir);
            if (solutionId == null) {
                throw new IllegalArgumentException("Solution template is missing solution.json or its 'title' field");
            }

            SolutionInstallResponse validateResult = validateSolution(tenantId, tempDir);
            if (validateResult != null && !validateResult.isSuccess()) {
                return validateResult;
            }
            return doInstallSolution(user, tenantId, solutionId, tempDir, request);
        } finally {
            deleteDirectory(tempDir);
        }
    }

    @Override
    public void deleteSolution(TenantId tenantId, SolutionTemplateInstalledItemDescriptor descriptor, SecurityUser user) throws ThingsboardException {
        try {
            if (descriptor.getCreatedEntityIds() != null && !descriptor.getCreatedEntityIds().isEmpty()) {
                List<EntityId> entityIds = new ArrayList<>(descriptor.getCreatedEntityIds());
                // Delete in the descending order of creation to avoid dependency issues.
                Collections.reverse(entityIds);
                for (EntityId entityId : entityIds) {
                    try {
                        deleteEntity(tenantId, entityId, user);
                    } catch (RuntimeException e) {
                        log.error("[{}] Failed to delete the entity: {}", tenantId, entityId, e);
                    }
                }
            }
            List<String> tsKeys = descriptor.getTenantTelemetryKeys();
            if (tsKeys != null && !tsKeys.isEmpty()) {
                List<DeleteTsKvQuery> queries = new ArrayList<>(tsKeys.size());
                for (String tsKey : tsKeys) {
                    queries.add(new BaseDeleteTsKvQuery(tsKey, 0, System.currentTimeMillis(), false));
                }
                tsService.remove(tenantId, tenantId, queries).get();
            }
            List<String> attrKeys = descriptor.getTenantAttributeKeys();
            if (attrKeys != null && !attrKeys.isEmpty()) {
                attributesService.removeAll(tenantId, tenantId, AttributeScope.SERVER_SCOPE, attrKeys).get();
            }
        } catch (Exception e) {
            log.error("[{}] Failed to delete the solution", tenantId, e);
            throw new ThingsboardException(e, ThingsboardErrorCode.GENERAL);
        }
    }

    private SolutionInstallResponse validateSolution(TenantId tenantId, Path tempDir) {
        Map<EntityType, List<HasName>> alreadyExistingEntities = new HashMap<>();

        //TODO: check other entities.

        List<ReferenceableEntityDefinition> ruleChains = loadListOfEntitiesIfFileExists(tempDir, "rule_chains.json", new TypeReference<>() {
        });
        if (!ruleChains.isEmpty()) {
            for (ReferenceableEntityDefinition ruleChain : ruleChains) {
                List<RuleChain> savedRuleChains = ruleChainService.findTenantRuleChainsByType(tenantId, RuleChainType.CORE, new PageLink(1, 0, ruleChain.getName())).getData();
                if (savedRuleChains != null && !savedRuleChains.isEmpty()) {
                    alreadyExistingEntities.computeIfAbsent(EntityType.RULE_CHAIN, key -> new ArrayList<>()).add(savedRuleChains.get(0));
                }
            }
        }

        List<DeviceProfileDefinition> deviceProfiles = loadListOfEntitiesIfFileExists(tempDir, "device_profiles.json", new TypeReference<>() {
        });
        deviceProfiles.addAll(loadListOfEntitiesFromDirectory(tempDir, "device_profiles", DeviceProfileDefinition.class));
        // Validate that entities with such name does not exist entities
        if (!deviceProfiles.isEmpty()) {
            for (DeviceProfile deviceProfile : deviceProfiles) {
                DeviceProfile savedProfile = deviceProfileService.findDeviceProfileByName(tenantId, deviceProfile.getName());
                if (savedProfile != null) {
                    alreadyExistingEntities.computeIfAbsent(EntityType.DEVICE_PROFILE, key -> new ArrayList<>()).add(savedProfile);
                }
            }
        }

        List<AssetProfileDefinition> assetProfiles = loadListOfEntitiesIfFileExists(tempDir, "asset_profiles.json", new TypeReference<>() {
        });
        assetProfiles.addAll(loadListOfEntitiesFromDirectory(tempDir, "asset_profiles", AssetProfileDefinition.class));
        // Validate that entities with such name does not exist entities
        if (!assetProfiles.isEmpty()) {
            for (AssetProfile assetProfile : assetProfiles) {
                AssetProfile savedProfile = assetProfileService.findAssetProfileByName(tenantId, assetProfile.getName());
                if (savedProfile != null) {
                    alreadyExistingEntities.computeIfAbsent(EntityType.ASSET_PROFILE, key -> new ArrayList<>()).add(savedProfile);
                }
            }
        }

        List<DashboardDefinition> dashboards = loadListOfEntitiesIfFileExists(tempDir, "dashboards.json", new TypeReference<>() {
        });
        if (!dashboards.isEmpty()) {
            for (DashboardDefinition dashboard : dashboards) {
                List<DashboardInfo> savedDashboards = dashboardService.findDashboardsByTenantId(tenantId, new PageLink(1, 0, dashboard.getName())).getData();
                if (savedDashboards != null && !savedDashboards.isEmpty()) {
                    alreadyExistingEntities.computeIfAbsent(EntityType.DASHBOARD, key -> new ArrayList<>()).add(savedDashboards.get(0));
                }
            }
        }
        if (!alreadyExistingEntities.isEmpty()) {
            SolutionInstallResponse solutionInstallResponse = new SolutionInstallResponse();
            StringBuilder detailsBuilder = new StringBuilder();
            detailsBuilder.append("## Validation failed").append(System.lineSeparator()).append(System.lineSeparator());
            alreadyExistingEntities.forEach((type, list) -> detailsBuilder.append("The following **").append(getTypeLabel(type)).append("** entities already exist: ")
                    .append(list.stream().map(HasName::getName).map(name -> "'" + name + "'").collect(Collectors.joining(","))).append(";")
                    .append(System.lineSeparator()).append(System.lineSeparator()));
            solutionInstallResponse.setSuccess(false);
            solutionInstallResponse.setDetails(detailsBuilder.toString());
            return solutionInstallResponse;
        } else {
            return null;
        }
    }

    private SolutionInstallResponse doInstallSolution(User user, TenantId tenantId, String solutionId, Path tempDir, HttpServletRequest request) {
        SolutionInstallContext ctx = new SolutionInstallContext(tenantId, solutionId, tempDir, user, new TenantSolutionTemplateInstructions());

        try {

            registerEmulatorsAndComputeOldestTelemetryTs(ctx);

            provisionTenantDetails(ctx);

            provisionRuleChains(ctx);

            provisionDeviceProfiles(ctx);

            provisionAssetProfiles(ctx);

            List<CustomerDefinition> customers = loadListOfEntitiesIfFileExists(ctx.getTempDir(), "customers.json", new TypeReference<>() {});

            provisionCustomers(ctx, customers);

            var assets = provisionAssets(ctx);

            var devices = provisionDevices(ctx);

            provisionDashboards(ctx);

            provisionCustomerUsers(ctx, customers);

            provisionRelations(ctx);

            updateRuleChains(ctx);

            provisionEdges(ctx);

            provisionAlarmRules(ctx);

            Set<CompletableFuture<Void>> telemetryLoading = launchEmulators(ctx, devices, assets);

            waitForTelemetryCompletion(telemetryLoading);

            provisionCalculatedFields(ctx);

            ctx.getSolutionInstructions().setDetails(prepareInstructions(ctx, request));

            List<ReferenceableEntityDefinition> ruleChainDefs = loadListOfEntitiesIfFileExists(ctx.getTempDir(), "rule_chains.json", new TypeReference<>() {});
            if (ruleChainDefs.stream().anyMatch(r -> StringUtils.isNotEmpty(r.getUpdate()))) {
                long timeout = Math.min(loadInstallTimeoutMs(ctx.getTempDir()), maxInstallTimeoutMs);
                if (timeout > 0) {
                    Thread.sleep(timeout);
                }
                finalUpdateRuleChains(ctx);
            }

            log.info("[{}] Solution template installed, created {} entities", tenantId, ctx.getCreatedEntitiesList().size());

            return new SolutionInstallResponse(
                    new TenantSolutionTemplateInstructions(ctx.getSolutionInstructions()),
                    true,
                    ctx.getCreatedEntitiesList(),
                    loadTenantTelemetryKeys(ctx.getTempDir()),
                    loadTenantAttributeKeys(ctx.getTempDir())
            );
        } catch (Throwable e) {
            log.error("[{}][{}] Failed to install solution template", tenantId, solutionId, e);
            rollback(tenantId, solutionId, ctx, e);
            if (e instanceof EntitiesLimitExceededException el) {
                throw el;
            }
            return new SolutionInstallResponse(
                    new TenantSolutionTemplateInstructions(ctx.getSolutionInstructions()),
                    false,
                    ctx.getCreatedEntitiesList()
            );
        }
    }

    private void waitForTelemetryCompletion(Set<CompletableFuture<Void>> futures) throws InterruptedException {
        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        try {
            all.get();
            Thread.sleep(futures.size() * 100L);
        } catch (ExecutionException e) {
            throw new RuntimeException("Telemetry processing failed", e.getCause());
        }
    }

    private void rollback(TenantId tenantId, String solutionId, SolutionInstallContext ctx, Throwable e) {
        List<EntityId> createdEntities = new ArrayList<>(ctx.getCreatedEntitiesList());
        Collections.reverse(createdEntities);
        for (EntityId entityId : createdEntities) {
            try {
                deleteEntity(tenantId, entityId, ctx.getUser());
            } catch (RuntimeException re) {
                log.error("[{}][{}] Failed to delete the entity: {}", tenantId, solutionId, entityId, re);
            }
        }
        ctx.getCreatedEntitiesList().clear();
        ctx.getSolutionInstructions().setDetails(e.getMessage());
    }

    private String prepareInstructions(SolutionInstallContext ctx, HttpServletRequest request) {

        Path instructionsFile = ctx.getTempDir().resolve("instructions.md");
        if (!Files.exists(instructionsFile)) {
            return null;
        }
        String template;
        try {
            template = Files.readString(instructionsFile);
        } catch (IOException e) {
            log.warn("[{}] Failed to read instructions.md", ctx.getTenantId(), e);
            return null;
        }

        String baseUrl = systemSecurityService.getBaseUrl(ctx.getTenantId(), null, request);

        // Inject edge instructions first, then run the full replacement logic on the combined string
        if (template.contains("${edge_instructions}")) {
            if (ctx.getCreatedEdges().isEmpty()) {
                template = template.replace("${edge_instructions}", "");
            } else {
                Path edgeFile = ctx.getTempDir().resolve("edge_instructions.md");
                String edgeTemplate = Files.exists(edgeFile) ? readFileContent(edgeFile) : "";
                template = template.replace("${edge_instructions}", edgeTemplate);
            }
        }

        template = template.replace("${DOCS_BASE_URL}", docsBaseUrl);
        template = template.replace("${BASE_URL}", baseUrl);

        TenantSolutionTemplateInstructions solutionInstructions = ctx.getSolutionInstructions();

        if (solutionInstructions.getDashboardId() != null) {
            template = template.replace("${MAIN_DASHBOARD_URL}",
                    getDashboardLink(solutionInstructions, solutionInstructions.getDashboardId(), false));
            if (solutionInstructions.isMainDashboardPublic()) {
                template = template.replace("${MAIN_DASHBOARD_PUBLIC_URL}",
                        getDashboardLink(solutionInstructions, solutionInstructions.getDashboardId(), true));
            }
        }

        for (DashboardLinkInfo dashboardLinkInfo : ctx.getDashboardLinks()) {
            template = template.replace("${" + dashboardLinkInfo.getName() + "DASHBOARD_URL}",
                    getDashboardLink(solutionInstructions, dashboardLinkInfo.getDashboardId(), false));
            if (dashboardLinkInfo.isPublic()) {
                template = template.replace("${" + dashboardLinkInfo.getName() + "DASHBOARD_PUBLIC_URL}",
                        getDashboardLink(solutionInstructions, dashboardLinkInfo.getDashboardId(), true));
            }
        }

        if (template.contains("${GATEWAYS_URL}")) {
            template = template.replace("${GATEWAYS_URL}", "/gateways");
        }

        // Device list and credentials
        StringBuilder devList = new StringBuilder();
        devList.append("| Device name | Access token | Owner |");
        devList.append(System.lineSeparator());
        devList.append("| :---   | :---  | :---  |");
        devList.append(System.lineSeparator());

        for (DeviceCredentialsInfo credentialsInfo : ctx.getCreatedDevices().values()) {
            devList.append("|").append(credentialsInfo.getName())
                    .append("|").append(credentialsInfo.getCredentials().getCredentialsId()).append("{:copy-code}")
                    .append("|").append(credentialsInfo.getCustomerName() != null ? credentialsInfo.getCustomerName() : "Tenant");
            devList.append(System.lineSeparator());

            template = template.replace("${" + credentialsInfo.getName() + "ACCESS_TOKEN}", credentialsInfo.getCredentials().getCredentialsId());

            if (credentialsInfo.isGateway()) {
                template = template.replace("${DOCKER_CONFIG}",
                        prepareDockerComposeFile(ctx.getTenantId(), ctx.getSolutionId(), baseUrl, credentialsInfo.getCredentials().getDeviceId()));
            }
        }

        template = template.replace("${device_list_and_credentials}", devList.toString());

        // User list (without user group column)
        StringBuilder userList = new StringBuilder();
        userList.append("| Name | Login | Password | Customer name |");
        userList.append(System.lineSeparator());
        userList.append("| :---  | :---  | :---  | :---  |");
        userList.append(System.lineSeparator());

        for (UserCredentialsInfo credentialsInfo : ctx.getCreatedUsers().values()) {
            userList.append("|").append(credentialsInfo.getName())
                    .append("|").append(credentialsInfo.getLogin()).append("{:copy-code}")
                    .append("|").append(credentialsInfo.getPassword()).append("{:copy-code}")
                    .append("|").append(credentialsInfo.getCustomerName() != null ? credentialsInfo.getCustomerName() : "");
            userList.append(System.lineSeparator());
        }

        template = template.replace("${user_list}", userList.toString());

        // Edge detail URLs
        for (Map.Entry<String, EdgeLinkInfo> edgeLinkInfoEntry : ctx.getCreatedEdges().entrySet()) {
            EdgeLinkInfo edgeLinkInfo = edgeLinkInfoEntry.getValue();
            StringBuilder edgeDetailsUrl = new StringBuilder();
            if (EntityType.CUSTOMER.equals(edgeLinkInfo.getOwnerId().getEntityType())) {
                edgeDetailsUrl.append("/customers/").append(edgeLinkInfo.getOwnerId().getId());
                edgeDetailsUrl.append("/edgeInstances/").append(edgeLinkInfo.getEdgeId().getId());
            } else {
                edgeDetailsUrl.append("/edgeManagement/instances/").append(edgeLinkInfo.getEdgeId().getId());
            }
            String edgeName = edgeLinkInfoEntry.getKey();
            String edgeDetailsPlaceholder = "${" + edgeName + "EDGE_DETAILS_URL}";
            template = template.replace(edgeDetailsPlaceholder, edgeDetailsUrl.toString());
        }

        template = replaceAlarmRules(ctx, template);
        template = replaceCalculatedFields(ctx, template);
        template = replaceCreatedEntities(ctx, template);

        return template;
    }

    private static String replaceAlarmRules(SolutionInstallContext ctx, String template) {
        StringBuilder alarmRules = new StringBuilder();

        alarmRules.append("| Entity Profile Name | Alarm Type | Severities |").append(System.lineSeparator());
        alarmRules.append("| :--- | :--- | :--- |").append(System.lineSeparator());

        ctx.getCreatedAlarmRules().entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.comparing(CreatedAlarmRuleInfo::entityName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(CreatedAlarmRuleInfo::alarmType, String.CASE_INSENSITIVE_ORDER)))
                .forEach(entry -> {
                    UUID key = entry.getKey();
                    var alarmRuleInfo = entry.getValue();

                    String alarmType = alarmRuleInfo.alarmType();
                    String link = alarmRuleInfo.getCfPageLink(key);

                    String alarmTypeWithLink = "<a href=\"" + link + "\" target=\"_blank\">" + alarmType + "</a>";

                    String profileName = alarmRuleInfo.entityId() != null ?
                            "<a href=\"" + alarmRuleInfo.getEntityPageLink() + "\" target=\"_blank\">" + alarmRuleInfo.entityName() + "</a>"
                            : alarmRuleInfo.entityName();

                    alarmRules.append("|")
                            .append(profileName).append("|")
                            .append(alarmTypeWithLink).append("|")
                            .append(alarmRuleInfo.severities()).append("|")
                            .append(System.lineSeparator());
                });

        return template.replace("${alarm_rules}", alarmRules.toString());
    }

    private static String replaceCalculatedFields(SolutionInstallContext ctx, String template) {
        StringBuilder calculatedFields = new StringBuilder();

        calculatedFields.append("| Entity Profile Name | Field Name | Field Type |").append(System.lineSeparator());
        calculatedFields.append("| :--- | :--- | :--- |").append(System.lineSeparator());

        ctx.getCreatedCalculatedFields().entrySet().stream()
                .sorted(Map.Entry.comparingByValue(
                        Comparator.comparing(CreatedCalculatedFieldInfo::entityName, String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(CreatedCalculatedFieldInfo::name, String.CASE_INSENSITIVE_ORDER)
                ))
                .forEach(entry -> {
                    UUID key = entry.getKey();
                    var cfInfo = entry.getValue();

                    String cfTitle = cfInfo.name();
                    String link = cfInfo.getCfPageLink(key);

                    String cfTitleWithLink = "<a href=\"" + link + "\" target=\"_blank\">" + cfTitle + "</a>";

                    String profileName = cfInfo.entityId() != null ?
                            "<a href=\"" + cfInfo.getEntityPageLink() + "\" target=\"_blank\">" + cfInfo.entityName() + "</a>"
                            : cfInfo.entityName();

                    calculatedFields.append("|")
                            .append(profileName).append("|")
                            .append(cfTitleWithLink).append("|")
                            .append(cfInfo.type()).append("|")
                            .append(System.lineSeparator());
                });

        return template.replace("${calculated_fields}", calculatedFields.toString());
    }

    private static String replaceCreatedEntities(SolutionInstallContext ctx, String template) {
        StringBuilder entityList = new StringBuilder();

        entityList.append("| Name | Type | Owner |").append(System.lineSeparator());
        entityList.append("| :--- | :--- | :--- |").append(System.lineSeparator());

        for (Map.Entry<UUID, CreatedEntityInfo> entry : ctx.getCreatedEntities().entrySet()) {
            UUID key = entry.getKey();
            var entityInfo = entry.getValue();
            String link = entityInfo.getEntityPageLink(key);
            String entityName = entityInfo.getName();

            String name = link != null ?
                    "<a href=\"" + link + "\" target=\"_blank\">" + entityName + "</a>"
                    : entityName;

            entityList.append("|")
                    .append(name).append("|")
                    .append(entityInfo.getType().getNormalName()).append("|")
                    .append(entityInfo.getOwner()).append("|")
                    .append(System.lineSeparator());
        }
        return template.replace("${all_entities}", entityList.toString());
    }

    private String getDashboardLink(TenantSolutionTemplateInstructions solutionInstructions, DashboardId dashboardId, boolean isPublic) {
        if (isPublic && solutionInstructions.getPublicId() != null) {
            return "/dashboard/" + dashboardId.getId() + "?publicId=" + solutionInstructions.getPublicId();
        }
        return "/dashboards/" + dashboardId.getId();
    }

    private String prepareDockerComposeFile(TenantId tenantId, String solutionId, String baseUrl, DeviceId deviceId) {
        Device device = new Device(deviceId);
        device.setTenantId(tenantId);
        String containerName = "tb-gateway-" + solutionId.replace('_', '-');
        DockerComposeParams params = new DockerComposeParams(false, containerName, false, true, false, false);
        try (InputStream inputStream = deviceConnectivityService.createGatewayDockerComposeFile(baseUrl, device, params).getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
        ) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to read or process the docker-compose.yml file.", e);
        }
    }

    private void provisionRuleChains(SolutionInstallContext ctx) {
        List<ReferenceableEntityDefinition> ruleChainDefs = loadListOfEntitiesIfFileExists(ctx.getTempDir(), "rule_chains.json", new TypeReference<>() {});
        for (ReferenceableEntityDefinition entityDef : ruleChainDefs) {
            Path ruleChainPath = ctx.getTempDir().resolve("rule_chains").resolve(entityDef.getFile());
            if (!Files.exists(ruleChainPath)) {
                log.warn("[{}] Rule chain file not found: {}", ctx.getTenantId(), entityDef.getFile());
                continue;
            }
            JsonNode ruleChainJson = replaceIds(ctx, JacksonUtil.toJsonNode(ruleChainPath));

            RuleChain ruleChain = JacksonUtil.treeToValue(ruleChainJson.get("ruleChain"), RuleChain.class);
            ruleChain.setId(null);
            ruleChain.setTenantId(ctx.getTenantId());
            String metadataStr = JacksonUtil.toString(ruleChainJson.get("metadata"));
            RuleChainMetaData metadata = JacksonUtil.treeToValue(JacksonUtil.toJsonNode(metadataStr), RuleChainMetaData.class);

            RuleChain savedRuleChain = ruleChainService.saveRuleChain(ruleChain);
            metadata.setRuleChainId(savedRuleChain.getId());
            metadata.setVersion(savedRuleChain.getVersion());
            ruleChainService.saveRuleChainMetaData(ctx.getTenantId(), metadata, tbRuleChainService::updateRuleNodeConfiguration);
            if (ruleChain.isRoot()) {
                ruleChainService.setRootRuleChain(ctx.getTenantId(), savedRuleChain.getId());
            }

            ctx.register(entityDef.getJsonId(), savedRuleChain);
            log.debug("[{}] Rule chain provisioned: {}", ctx.getTenantId(), savedRuleChain.getName());
        }
    }

    private void updateRuleChains(SolutionInstallContext ctx) {
        List<ReferenceableEntityDefinition> ruleChainDefs = loadListOfEntitiesIfFileExists(ctx.getTempDir(), "rule_chains.json", new TypeReference<>() {});
        for (ReferenceableEntityDefinition entityDef : ruleChainDefs) {
            Path ruleChainPath = ctx.getTempDir().resolve("rule_chains").resolve(entityDef.getFile());
            if (!Files.exists(ruleChainPath)) {
                continue;
            }
            String realId = ctx.getRealIds().get(entityDef.getJsonId());
            if (realId == null) {
                continue;
            }
            RuleChainId ruleChainId = new RuleChainId(UUID.fromString(realId));
            RuleChain savedRuleChain = ruleChainService.findRuleChainById(ctx.getTenantId(), ruleChainId);
            if (savedRuleChain == null) {
                continue;
            }
            JsonNode ruleChainJson = JacksonUtil.toJsonNode(ruleChainPath);
            String metadataStr = JacksonUtil.toString(ruleChainJson.get("metadata"));
            String oldMetadataStr = metadataStr;
            for (var entry : ctx.getRealIds().entrySet()) {
                metadataStr = metadataStr.replace(entry.getKey(), entry.getValue());
            }
            if (metadataStr.equals(oldMetadataStr)) {
                continue;
            }
            RuleChainMetaData metadata = JacksonUtil.treeToValue(JacksonUtil.toJsonNode(metadataStr), RuleChainMetaData.class);
            metadata.setRuleChainId(ruleChainId);
            metadata.setVersion(savedRuleChain.getVersion());
            ruleChainService.saveRuleChainMetaData(ctx.getTenantId(), metadata, tbRuleChainService::updateRuleNodeConfiguration);
        }
    }

    private void finalUpdateRuleChains(SolutionInstallContext ctx) {
        List<ReferenceableEntityDefinition> ruleChains = loadListOfEntitiesIfFileExists(ctx.getTempDir(), "rule_chains.json", new TypeReference<>() {});
        for (ReferenceableEntityDefinition entityDefinition : ruleChains) {
            if (StringUtils.isEmpty(entityDefinition.getUpdate())) {
                continue;
            }
            Path ruleChainPath = ctx.getTempDir().resolve("rule_chains").resolve(entityDefinition.getUpdate());
            JsonNode ruleChainJson = JacksonUtil.toJsonNode(ruleChainPath);
            RuleChain ruleChain = JacksonUtil.treeToValue(ruleChainJson.get("ruleChain"), RuleChain.class);
            ruleChain.setTenantId(ctx.getTenantId());
            String metadataStr = JacksonUtil.toString(ruleChainJson.get("metadata"));
            for (var entry : ctx.getRealIds().entrySet()) {
                metadataStr = metadataStr.replace(entry.getKey(), entry.getValue());
            }
            RuleChainMetaData ruleChainMetaData = JacksonUtil.treeToValue(JacksonUtil.toJsonNode(metadataStr), RuleChainMetaData.class);

            String realRuleChainId = ctx.getRealIds().get(entityDefinition.getJsonId());
            if (StringUtils.isEmpty(realRuleChainId)) {
                continue;
            }
            RuleChainId ruleChainId = new RuleChainId(UUID.fromString(realRuleChainId));
            RuleChain savedRuleChain = ruleChainService.findRuleChainById(ctx.getTenantId(), ruleChainId);
            ruleChainMetaData.setRuleChainId(savedRuleChain.getId());
            ruleChainMetaData.setVersion(savedRuleChain.getVersion());
            ruleChainService.saveRuleChainMetaData(ctx.getTenantId(), ruleChainMetaData, tbRuleChainService::updateRuleNodeConfiguration);
        }
    }

    private void provisionDeviceProfiles(SolutionInstallContext ctx) {
        List<DeviceProfileDefinition> deviceProfiles = loadListOfEntitiesIfFileExists(ctx.getTempDir(), "device_profiles.json", new TypeReference<>() {});
        deviceProfiles.addAll(loadListOfEntitiesFromDirectory(ctx.getTempDir(), "device_profiles", DeviceProfileDefinition.class));
        deviceProfiles.forEach(deviceProfile -> {
            deviceProfile.setId(null);
            deviceProfile.setCreatedTime(0L);
            deviceProfile.setTenantId(ctx.getTenantId());
            if (deviceProfile.getDefaultRuleChainId() != null) {
                String newId = ctx.getRealIds().get(deviceProfile.getDefaultRuleChainId().getId().toString());
                if (newId != null) {
                    deviceProfile.setDefaultRuleChainId(new RuleChainId(UUID.fromString(newId)));
                } else {
                    log.error("[{}] Device profile: {} references non existing rule chain.", ctx.getTenantId(), deviceProfile.getName());
                    throw new RuntimeException("Device profile: " + deviceProfile.getName() + " references non existing rule chain.");
                }
            }
            if (deviceProfile.getDefaultEdgeRuleChainId() != null) {
                String newId = ctx.getRealIds().get(deviceProfile.getDefaultEdgeRuleChainId().getId().toString());
                if (StringUtils.isEmpty(newId)) {
                    deviceProfile.setDefaultEdgeRuleChainId(null);
                } else {
                    deviceProfile.setDefaultEdgeRuleChainId(new RuleChainId(UUID.fromString(newId)));
                }
            }
            if (deviceProfile.getDefaultDashboardId() != null) {
                String newId = ctx.getRealIds().get(deviceProfile.getDefaultDashboardId().getId().toString());
                if (newId != null) {
                    deviceProfile.setDefaultDashboardId(new DashboardId(UUID.fromString(newId)));
                }
            }
        });

        deviceProfiles.forEach(deviceProfileDefinition -> {
            DeviceProfile deviceProfile = new DeviceProfile(deviceProfileDefinition);
            deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
            ctx.register(deviceProfileDefinition, deviceProfile);
        });
    }

    private void provisionAssetProfiles(SolutionInstallContext ctx) {
        List<AssetProfileDefinition> assetProfiles = loadListOfEntitiesIfFileExists(ctx.getTempDir(), "asset_profiles.json", new TypeReference<>() {});
        assetProfiles.addAll(loadListOfEntitiesFromDirectory(ctx.getTempDir(), "asset_profiles", AssetProfileDefinition.class));
        assetProfiles.forEach(assetProfile -> {
            assetProfile.setId(null);
            assetProfile.setCreatedTime(0L);
            assetProfile.setTenantId(ctx.getTenantId());
            if (assetProfile.getDefaultRuleChainId() != null) {
                String newId = ctx.getRealIds().get(assetProfile.getDefaultRuleChainId().getId().toString());
                if (newId != null) {
                    assetProfile.setDefaultRuleChainId(new RuleChainId(UUID.fromString(newId)));
                } else {
                    log.error("[{}] Asset profile: {} references non existing rule chain.", ctx.getTenantId(), assetProfile.getName());
                    throw new RuntimeException("Asset profile: " + assetProfile.getName() + " references non existing rule chain.");
                }
            }
            if (assetProfile.getDefaultEdgeRuleChainId() != null) {
                String newId = ctx.getRealIds().get(assetProfile.getDefaultEdgeRuleChainId().getId().toString());
                if (StringUtils.isEmpty(newId)) {
                    assetProfile.setDefaultEdgeRuleChainId(null);
                } else {
                    assetProfile.setDefaultEdgeRuleChainId(new RuleChainId(UUID.fromString(newId)));
                }
            }
        });

        assetProfiles.forEach(assetProfileDefinition -> {
            AssetProfile assetProfile = new AssetProfile(assetProfileDefinition);
            assetProfile = assetProfileService.saveAssetProfile(assetProfile);
            ctx.register(assetProfileDefinition, assetProfile);
        });
    }

    private CustomerId getPublicCustomerId(SolutionInstallContext ctx) {
        CustomerId publicId = ctx.getSolutionInstructions().getPublicId();
        if (publicId != null) {
            return publicId;
        }
        Customer publicCustomer = customerService.findOrCreatePublicCustomer(ctx.getTenantId());
        ctx.getSolutionInstructions().setPublicId(publicCustomer.getId());
        return publicCustomer.getId();
    }

    private void provisionDashboards(SolutionInstallContext ctx) {
        List<DashboardDefinition> dashboardDefs = loadListOfEntitiesIfFileExists(ctx.getTempDir(), "dashboards.json", new TypeReference<>() {});
        for (DashboardDefinition entityDef : dashboardDefs) {
            CustomerId customerId = entityDef.isMakePublic() ? getPublicCustomerId(ctx) : ctx.getIdFromMap(EntityType.CUSTOMER, entityDef.getCustomer());
            Path dashboardPath = ctx.getTempDir().resolve("dashboards").resolve(entityDef.getFile());
            if (!Files.exists(dashboardPath)) {
                log.warn("[{}] Dashboard file not found: {}", ctx.getTenantId(), entityDef.getFile());
                continue;
            }
            JsonNode dashboardJson = replaceIds(ctx, JacksonUtil.toJsonNode(dashboardPath));
            Dashboard dashboardTemplate = JacksonUtil.treeToValue(dashboardJson, Dashboard.class);

            Dashboard dashboard = new Dashboard();
            dashboard.setTenantId(ctx.getTenantId());
            dashboard.setTitle(entityDef.getName());
            dashboard.setConfiguration(dashboardTemplate.getConfiguration());
            dashboard.setImage(dashboardTemplate.getImage());
            dashboard.setResources(dashboardTemplate.getResources());
            if (dashboardJson.has("mobileHide") && dashboardJson.get("mobileHide").isBoolean()) {
                dashboard.setMobileHide(dashboardJson.get("mobileHide").asBoolean());
            }
            if (dashboardJson.has("mobileOrder") && dashboardJson.get("mobileOrder").isInt()) {
                dashboard.setMobileOrder(dashboardJson.get("mobileOrder").asInt());
            }

            dashboard = dashboardService.saveDashboard(dashboard);
            if (customerId != null) {
                dashboardService.assignDashboardToCustomer(ctx.getTenantId(), dashboard.getId(), customerId);
            }
            ctx.register(entityDef, dashboard);
            ctx.putIdToMap(EntityType.DASHBOARD, entityDef.getName(), dashboard.getId());

            if (entityDef.isMain()) {
                ctx.getSolutionInstructions().setDashboardId(dashboard.getId());
                ctx.getSolutionInstructions().setMainDashboardPublic(entityDef.isMakePublic());
            }
            ctx.getDashboardLinks().add(new DashboardLinkInfo(dashboard.getTitle(), dashboard.getId(), entityDef.isMakePublic()));

            log.debug("[{}] Dashboard provisioned: {}", ctx.getTenantId(), dashboard.getTitle());
        }
    }

    private void provisionRelations(SolutionInstallContext ctx) {
        ctx.getRelationDefinitions().forEach((id, relations) -> {
            for (RelationDefinition relationDef : relations) {
                log.info("[{}] Saving relation: {}", id, relationDef);
                EntityRelation entityRelation = new EntityRelation();
                EntityId otherId = resolveRelatedEntityId(relationDef, ctx);
                if (EntitySearchDirection.FROM.equals(relationDef.getDirection())) {
                    entityRelation.setFrom(otherId);
                    entityRelation.setTo(id);
                } else {
                    entityRelation.setFrom(id);
                    entityRelation.setTo(otherId);
                }
                entityRelation.setTypeGroup(RelationTypeGroup.COMMON);
                entityRelation.setType(relationDef.getType());
                try {
                    relationService.save(ctx.getTenantId(), null, entityRelation, null);
                } catch (Exception e) {
                    log.info("[{}] Failed to save relation: {}, cause: {}", id, relationDef, e.getMessage());
                }
            }
        });
    }

    private EntityId resolveRelatedEntityId(RelationDefinition relationDef, SolutionInstallContext ctx) {
        if (relationDef.getEntityType() == EntityType.TENANT) {
            return ctx.getTenantId();
        }
        return ctx.getIdFromMap(relationDef.getEntityType(), relationDef.getEntityName());
    }

    private Map<Device, DeviceDefinition> provisionDevices(SolutionInstallContext ctx) {
        Map<Device, DeviceDefinition> result = new HashMap<>();
        Set<String> deviceTypeSet = new HashSet<>();
        List<DeviceDefinition> devices = loadListOfEntitiesIfFileExists(ctx.getTempDir(), "devices.json", new TypeReference<>() {});
        for (DeviceDefinition entityDef : devices) {
            CustomerId customerId = entityDef.isMakePublic() ? getPublicCustomerId(ctx) : ctx.getIdFromMap(EntityType.CUSTOMER, entityDef.getCustomer());
            Device entity = new Device();
            entity.setTenantId(ctx.getTenantId());
            entity.setName(entityDef.getName());
            entity.setLabel(entityDef.getLabel());
            ensureDeviceProfileExists(ctx, deviceTypeSet, entityDef);
            entity.setType(entityDef.getType());
            entity.setCustomerId(customerId);
            entity.setAdditionalInfo(entityDef.getAdditionalInfo());
            entity = deviceService.saveDevice(entity);
            entityActionService.logEntityAction(ctx.getUser(), entity.getId(), entity, customerId, ActionType.ADDED, null);
            ctx.register(entityDef, entity);
            log.info("[{}] Saved device: {}", entity.getId(), entity);
            DeviceId entityId = entity.getId();
            ctx.putIdToMap(entityDef, entityId);
            saveServerSideAttributes(ctx, entityId, entityDef.getAttributes());
            saveSharedAttributes(ctx, entityId, entityDef.getSharedAttributes());
            ctx.put(entityId, entityDef.getRelations());

            DeviceCredentialsInfo deviceCredentialsInfo = new DeviceCredentialsInfo();
            deviceCredentialsInfo.setName(entity.getName());
            deviceCredentialsInfo.setType(entity.getType());
            deviceCredentialsInfo.setCustomerName(entityDef.getCustomer());
            deviceCredentialsInfo.setCredentials(deviceCredentialsService.findDeviceCredentialsByDeviceId(ctx.getTenantId(), entityId));
            JsonNode additionalInfo = entity.getAdditionalInfo();
            boolean isGateway = additionalInfo != null && additionalInfo.hasNonNull("gateway") && additionalInfo.get("gateway").asBoolean();
            deviceCredentialsInfo.setGateway(isGateway);
            ctx.addDeviceCredentials(deviceCredentialsInfo);

            result.put(entity, entityDef);
        }
        return result;
    }

    private void ensureDeviceProfileExists(SolutionInstallContext ctx, Set<String> deviceTypeSet, DeviceDefinition entityDef) {
        if (!deviceTypeSet.contains(entityDef.getType())) {
            DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileByName(ctx.getTenantId(), entityDef.getType());
            if (deviceProfile == null) {
                DeviceProfile created = deviceProfileService.findOrCreateDeviceProfile(ctx.getTenantId(), entityDef.getType());
                ctx.register(created.getId());
                log.info("Saved device profile: {}", created.getId());
            }
            deviceTypeSet.add(entityDef.getType());
        }
    }

    private void registerEmulatorsAndComputeOldestTelemetryTs(SolutionInstallContext ctx) {
        List<EmulatorDefinition> emulatorDefinitions = loadListOfEntitiesIfFileExists(ctx.getTempDir(), "device_emulators.json", new TypeReference<>() {
        });
        Map<String, EmulatorDefinition> deviceEmulators = emulatorDefinitions.stream().collect(Collectors.toMap(EmulatorDefinition::getName, Function.identity()));
        emulatorDefinitions.stream().filter(ed -> StringUtils.isNotEmpty(ed.getExtendz()))
                .forEach(ed -> {
                    EmulatorDefinition parent = deviceEmulators.get(ed.getExtendz());
                    if (parent != null) {
                        ed.enrich(parent);
                    }
                });
        Map<String, EmulatorDefinition> assetEmulators = loadListOfEntitiesIfFileExists(ctx.getTempDir(), "asset_emulators.json", new TypeReference<List<EmulatorDefinition>>() {
        }).stream().collect(Collectors.toMap(EmulatorDefinition::getName, Function.identity()));

        ctx.setDeviceEmulators(deviceEmulators);
        ctx.setAssetEmulators(assetEmulators);

        long solutionInstallTs = ctx.getInstallTs();
        long oldestDeviceEmulatorsTs = deviceEmulators.values().stream()
                .mapToLong(value -> value.getOldestTs(solutionInstallTs))
                .min().orElse(solutionInstallTs);
        long oldestAssetEmulatorsTs = assetEmulators.values().stream()
                .mapToLong(value -> value.getOldestTs(solutionInstallTs))
                .min().orElse(solutionInstallTs);
        long solutionOldestTs = Math.min(oldestDeviceEmulatorsTs, oldestAssetEmulatorsTs);

        ctx.setOldestTelemetryTs(solutionOldestTs);
    }

    private Set<CompletableFuture<Void>> launchEmulators(SolutionInstallContext ctx, Map<Device, DeviceDefinition> devicesMap, Map<Asset, AssetDefinition> assets) throws Exception {
        Set<CompletableFuture<Void>> results = new HashSet<>();

        for (var entry : devicesMap.entrySet().stream().filter(e -> StringUtils.isNotBlank(e.getValue().getEmulator())).collect(Collectors.toSet())) {
            results.add(DeviceEmulatorLauncher.builder()
                    .entity(entry.getKey())
                    .emulatorDefinition(ctx.getDeviceEmulators().get(entry.getValue().getEmulator()))
                    .oldTelemetryExecutor(emulatorExecutor)
                    .tbClusterService(tbClusterService)
                    .partitionService(partitionService)
                    .tbQueueProducerProvider(tbQueueProducerProvider)
                    .serviceInfoProvider(serviceInfoProvider)
                    .tsSubService(tsSubService)
                    .build().launch());
        }

        for (var entry : assets.entrySet().stream().filter(e -> StringUtils.isNotBlank(e.getValue().getEmulator())).collect(Collectors.toSet())) {
            results.add(AssetEmulatorLauncher.builder()
                    .entity(entry.getKey())
                    .emulatorDefinition(ctx.getAssetEmulators().get(entry.getValue().getEmulator()))
                    .oldTelemetryExecutor(emulatorExecutor)
                    .tbClusterService(tbClusterService)
                    .partitionService(partitionService)
                    .tbQueueProducerProvider(tbQueueProducerProvider)
                    .serviceInfoProvider(serviceInfoProvider)
                    .tsSubService(tsSubService)
                    .build().launch());
        }

        return results;
    }

    private void provisionTenantDetails(SolutionInstallContext ctx) {
        TenantDefinition tenant = loadEntityIfFileExists(ctx.getTempDir(), "tenant.json", TenantDefinition.class);
        if (tenant != null) {
            saveServerSideAttributes(ctx, ctx.getTenantId(), tenant.getAttributes());
            ctx.put(ctx.getTenantId(), tenant.getRelations());
        }
    }

    private Map<Asset, AssetDefinition> provisionAssets(SolutionInstallContext ctx) {
        Map<Asset, AssetDefinition> result = new HashMap<>();
        Set<String> assetTypeSet = new HashSet<>();
        List<AssetDefinition> assets = loadListOfEntitiesIfFileExists(ctx.getTempDir(), "assets.json", new TypeReference<>() {});
        for (AssetDefinition entityDef : assets) {
            Asset entity = new Asset();
            entity.setTenantId(ctx.getTenantId());
            entity.setName(entityDef.getName());
            entity.setLabel(entityDef.getLabel());
            entity.setType(entityDef.getType());
            if (entityDef.isMakePublic()) {
                entity.setCustomerId(getPublicCustomerId(ctx));
            } else {
                entity.setCustomerId(ctx.getIdFromMap(EntityType.CUSTOMER, entityDef.getCustomer()));
            }
            ensureAssetProfileExists(ctx, assetTypeSet, entityDef);
            entity = assetService.saveAsset(entity);
            ctx.register(entityDef, entity);
            log.info("[{}] Saved asset: {}", entity.getId(), entity);
            AssetId entityId = entity.getId();
            ctx.putIdToMap(entityDef, entityId);
            saveServerSideAttributes(ctx, entityId, entityDef.getAttributes());
            ctx.put(entityId, entityDef.getRelations());
            result.put(entity, entityDef);
        }
        return result;
    }

    private void ensureAssetProfileExists(SolutionInstallContext ctx, Set<String> assetTypeSet, AssetDefinition entityDef) {
        if (!assetTypeSet.contains(entityDef.getType())) {
            AssetProfile assetProfile = assetProfileService.findAssetProfileByName(ctx.getTenantId(), entityDef.getType());
            if (assetProfile == null) {
                AssetProfile created = assetProfileService.findOrCreateAssetProfile(ctx.getTenantId(), entityDef.getType());
                ctx.register(created.getId());
                log.info("Saved asset profile: {}", created.getId());
            }
            assetTypeSet.add(entityDef.getType());
        }
    }

    private void provisionCustomers(SolutionInstallContext ctx, List<CustomerDefinition> customers) {
        for (CustomerDefinition entityDef : customers) {
            entityDef.setRandomNameData(generateRandomName(ctx));
            Customer customer = new Customer();
            customer.setTenantId(ctx.getTenantId());
            customer.setTitle(randomize(entityDef.getName(), entityDef.getRandomNameData()));
            customer.setEmail(randomize(entityDef.getEmail(), entityDef.getRandomNameData()));
            customer.setCountry(entityDef.getCountry());
            customer.setCity(entityDef.getCity());
            customer.setState(entityDef.getState());
            customer.setZip(entityDef.getZip());
            customer.setAddress(entityDef.getAddress());
            customer = customerService.saveCustomer(customer);
            log.info("[{}] Saved customer: {}", customer.getId(), customer);
            ctx.register(entityDef, customer);
            CustomerId entityId = customer.getId();
            ctx.putIdToMap(entityDef, entityId);
            saveServerSideAttributes(ctx, entityId, entityDef.getAttributes(), entityDef.getRandomNameData());
            ctx.put(entityId, entityDef.getRelations());
            entityDef.setName(customer.getName());
        }
    }

    private void provisionCustomerUsers(SolutionInstallContext ctx, List<CustomerDefinition> customers) {
        for (CustomerDefinition entityDef : customers) {
            Customer customer = customerService.findCustomerByTenantIdAndTitle(ctx.getTenantId(), entityDef.getName()).get();
            for (UserDefinition uDef : entityDef.getUsers()) {
                String originalName = uDef.getName();
                User user = createUser(ctx, customer, uDef, entityDef);

                UserCredentials credentials = userService.findUserCredentialsByUserId(ctx.getTenantId(), user.getId());
                credentials.setEnabled(true);
                credentials.setActivateToken(null);
                credentials.setPassword(passwordEncoder.encode(uDef.getPassword()));
                userService.saveUserCredentials(ctx.getTenantId(), credentials);

                DashboardUserDetailsDefinition dd = uDef.getDashboard();
                if (dd != null) {
                    DashboardId dashboardId = ctx.getIdFromMap(EntityType.DASHBOARD, dd.getName());
                    ObjectNode additionalInfo = JacksonUtil.newObjectNode();
                    additionalInfo.put("defaultDashboardId", dashboardId.getId().toString());
                    additionalInfo.put("defaultDashboardFullscreen", dd.isFullScreen());
                    user.setAdditionalInfo(additionalInfo);
                    userService.saveUser(ctx.getTenantId(), user);
                    log.info("[{}] Added default dashboard for user {}", customer.getId(), user.getEmail());
                }

                UserCredentialsInfo credentialsInfo = new UserCredentialsInfo();
                credentialsInfo.setName(user.getFirstName() + " " + user.getLastName());
                credentialsInfo.setLogin(uDef.getName());
                credentialsInfo.setPassword(uDef.getPassword());
                credentialsInfo.setCustomerName(entityDef.getName());
                ctx.addUserCredentials(credentialsInfo);
                ctx.register(entityDef, user);
                ctx.put(user.getId(), uDef.getRelations());
                ctx.putIdToMap(EntityType.USER, originalName, user.getId());
                ctx.putIdToMap(EntityType.USER, uDef.getName(), user.getId());
                saveServerSideAttributes(ctx, user.getId(), uDef.getAttributes());
            }
        }
    }

    private User createUser(SolutionInstallContext ctx, Customer customer, UserDefinition uDef, CustomerDefinition cDef) {
        int maxAttempts = 10;
        int attempts = 0;
        Exception finalE = null;
        while (attempts < maxAttempts) {
            try {
                boolean lastAttempt = maxAttempts == (attempts + 1);
                var randomName = lastAttempt ? RandomNameUtil.nextSuperRandom() : RandomNameUtil.next();
                User user = new User();
                if (!StringUtils.isEmpty(uDef.getFirstname())) {
                    user.setFirstName(randomize(uDef.getFirstname(), randomName, cDef.getRandomNameData()));
                } else {
                    user.setFirstName(randomName.getFirstName());
                }
                if (!StringUtils.isEmpty(uDef.getLastname())) {
                    user.setLastName(randomize(uDef.getLastname(), randomName, cDef.getRandomNameData()));
                } else {
                    user.setLastName(randomName.getLastName());
                }
                user.setAuthority(Authority.CUSTOMER_USER);
                user.setEmail(randomize(uDef.getName(), randomName, cDef.getRandomNameData()));
                user.setCustomerId(customer.getId());
                user.setTenantId(ctx.getTenantId());
                log.info("[{}] Saving user: {}", customer.getId(), user);
                user = userService.saveUser(ctx.getTenantId(), user);
                uDef.setName(user.getEmail());
                return user;
            } catch (Exception e) {
                finalE = e;
                attempts++;
            }
        }
        throw new RuntimeException(finalE);
    }

    private void provisionEdges(SolutionInstallContext ctx) throws Exception {
        List<EdgeDefinition> edges = loadListOfEntitiesIfFileExists(ctx.getTempDir(), "edges.json", new TypeReference<>() {});
        RuleChain edgeTemplateRootRuleChain = ruleChainService.getEdgeTemplateRootRuleChain(ctx.getTenantId());
        for (EdgeDefinition entityDef : edges) {
            CustomerId customerId = entityDef.isMakePublic() ? getPublicCustomerId(ctx) : ctx.getIdFromMap(EntityType.CUSTOMER, entityDef.getCustomer());
            Edge entity = new Edge();
            entity.setTenantId(ctx.getTenantId());
            entity.setName(entityDef.getName());
            entity.setLabel(entityDef.getLabel());
            entity.setType(entityDef.getType());
            entity.setCustomerId(customerId);
            entity.setRoutingKey(UUID.randomUUID().toString());
            entity.setSecret(StringUtils.randomAlphanumeric(20));
            RuleChainId rootRuleChainId = edgeTemplateRootRuleChain.getId();
            if (StringUtils.isNotBlank(entityDef.getRootRuleChainId())) {
                String newId = ctx.getRealIds().get(entityDef.getRootRuleChainId());
                if (newId != null) {
                    rootRuleChainId = new RuleChainId(UUID.fromString(newId));
                } else {
                    log.error("[{}] Edge: {} references non existing rule chain.", ctx.getTenantId(), entity.getName());
                    throw new RuntimeException("Edge: " + entity.getName() + " references non existing rule chain.");
                }
            }
            entity.setRootRuleChainId(rootRuleChainId);
            RuleChain rootRuleChain = ruleChainService.findRuleChainById(ctx.getTenantId(), rootRuleChainId);
            entity = tbEdgeService.save(entity, rootRuleChain, ctx.getUser());
            ctx.register(entityDef, entity);
            assignRuleChainsToEdge(ctx, entityDef.getRuleChainIds(), entity);
            assignAssetsToEdge(ctx, entityDef.getAssetIds(), entity);
            assignDevicesToEdge(ctx, entityDef.getDeviceIds(), entity);
            assignDashboardsToEdge(ctx, entityDef.getDashboardIds(), entity);
            log.info("[{}] Saved edge: {}", entity.getId(), entity);
            EdgeId entityId = entity.getId();
            ctx.putIdToMap(entityDef, entityId);
            saveServerSideAttributes(ctx, entityId, entityDef.getAttributes());
            ctx.put(entityId, entityDef.getRelations());
            ctx.addEdgeLinkInfo(entity.getName(), new EdgeLinkInfo(entity.getId(), customerId != null ? customerId : ctx.getTenantId()));
        }
    }

    private void assignRuleChainsToEdge(SolutionInstallContext ctx, List<String> ruleChainIds, Edge entity) {
        if (ruleChainIds == null || ruleChainIds.isEmpty()) {
            return;
        }
        for (String strRuleChainId : ruleChainIds) {
            String newId = ctx.getRealIds().get(strRuleChainId);
            if (newId != null) {
                RuleChainId ruleChainId = new RuleChainId(UUID.fromString(newId));
                ruleChainService.assignRuleChainToEdge(ctx.getTenantId(), ruleChainId, entity.getId());
            } else {
                log.error("[{}] Edge: {} references non existing edge rule chain.", ctx.getTenantId(), entity.getName());
                throw new RuntimeException("Edge: " + entity.getName() + " references non existing edge rule chain.");
            }
        }
    }

    private void assignAssetsToEdge(SolutionInstallContext ctx, List<String> assetIds, Edge entity) {
        if (assetIds == null || assetIds.isEmpty()) {
            return;
        }
        for (String strAssetId : assetIds) {
            String newId = ctx.getRealIds().get(strAssetId);
            if (newId != null) {
                AssetId assetId = new AssetId(UUID.fromString(newId));
                assetService.assignAssetToEdge(ctx.getTenantId(), assetId, entity.getId());
            } else {
                log.error("[{}] Edge: {} references non existing asset.", ctx.getTenantId(), entity.getName());
                throw new RuntimeException("Edge: " + entity.getName() + " references non existing asset.");
            }
        }
    }

    private void assignDevicesToEdge(SolutionInstallContext ctx, List<String> deviceIds, Edge entity) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return;
        }
        for (String strDeviceId : deviceIds) {
            String newId = ctx.getRealIds().get(strDeviceId);
            if (newId != null) {
                DeviceId deviceId = new DeviceId(UUID.fromString(newId));
                deviceService.assignDeviceToEdge(ctx.getTenantId(), deviceId, entity.getId());
            } else {
                log.error("[{}] Edge: {} references non existing device.", ctx.getTenantId(), entity.getName());
                throw new RuntimeException("Edge: " + entity.getName() + " references non existing device.");
            }
        }
    }

    private void assignDashboardsToEdge(SolutionInstallContext ctx, List<String> dashboardIds, Edge entity) {
        if (dashboardIds == null || dashboardIds.isEmpty()) {
            return;
        }
        for (String strDashboardId : dashboardIds) {
            String newId = ctx.getRealIds().get(strDashboardId);
            if (newId != null) {
                DashboardId dashboardId = new DashboardId(UUID.fromString(newId));
                dashboardService.assignDashboardToEdge(ctx.getTenantId(), dashboardId, entity.getId());
            } else {
                log.error("[{}] Edge: {} references non existing dashboard.", ctx.getTenantId(), entity.getName());
                throw new RuntimeException("Edge: " + entity.getName() + " references non existing dashboard.");
            }
        }
    }

    private void provisionAlarmRules(SolutionInstallContext ctx) {
        List<CalculatedField> cfs = loadListOfEntitiesIfFileExists(ctx.getTempDir(), "alarm_rules.json", new TypeReference<>() {});
        cfs.addAll(loadListOfEntitiesFromDirectory(ctx.getTempDir(), "alarm_rules", CalculatedField.class));
        cfs.forEach(cf -> ctx.register(createCalculatedField(cf, ctx)));
    }

    private void provisionCalculatedFields(SolutionInstallContext ctx) {
        List<CalculatedFieldDefinition> cfs = loadListOfEntitiesIfFileExists(ctx.getTempDir(), "calculated_fields.json", new TypeReference<>() {
        });
        cfs.addAll(loadListOfEntitiesFromDirectory(ctx.getTempDir(), "calculated_fields", CalculatedFieldDefinition.class));

        List<CalculatedFieldDefinition> createOnly = new ArrayList<>();
        TreeMap<Integer, List<CalculatedFieldDefinition>> ordered = new TreeMap<>();

        for (CalculatedFieldDefinition cf : cfs) {
            if (cf.getReprocessingOrder() == null || cf.getReprocessingOrder() < 0) {
                createOnly.add(cf);
            } else {
                ordered.computeIfAbsent(cf.getReprocessingOrder(), integer -> new ArrayList<>()).add(cf);
            }
        }

        createOnly.forEach(cf -> ctx.register(createCalculatedField(cf, ctx)));

        for (Map.Entry<Integer, List<CalculatedFieldDefinition>> entry : ordered.entrySet()) {
            List<CalculatedFieldDefinition> cfDefs = entry.getValue();
            for (CalculatedFieldDefinition cfDef : cfDefs) {
                CalculatedField calculatedField = createCalculatedField(cfDef, ctx);
                ctx.register(calculatedField);
            }
        }
    }

    private CalculatedField createCalculatedField(CalculatedField cf, SolutionInstallContext ctx) {
        cf.setId(null);
        cf.setCreatedTime(0L);
        cf.setTenantId(ctx.getTenantId());
        cf.setDebugSettings(new DebugSettings(true, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(15)));

        Map<String, String> realIds = ctx.getRealIds();

        EntityId entityId = cf.getEntityId();
        if (entityId != null) {
            String newEntityId = realIds.get(entityId.getId().toString());
            if (newEntityId != null) {
                cf.setEntityId(EntityIdFactory.getByTypeAndUuid(entityId.getEntityType(), newEntityId));
            } else {
                log.error("[{}] Calculated field: {} references non existing entity.", ctx.getTenantId(), cf.getName());
                throw new RuntimeException("Calculated field: " + cf.getName() + " references non existing entity.");
            }
        }
        if (cf.getConfiguration() instanceof ArgumentsBasedCalculatedFieldConfiguration argBasedCfg) {
            argBasedCfg.getArguments().forEach((key, argument) -> {
                EntityId refEntityId = argument.getRefEntityId();
                if (refEntityId != null) {
                    if (refEntityId.getEntityType() == EntityType.TENANT) {
                        argument.setRefEntityId(ctx.getTenantId());
                    } else {
                        String newId = realIds.get(refEntityId.getId().toString());
                        if (newId != null) {
                            argument.setRefEntityId(EntityIdFactory.getByTypeAndUuid(refEntityId.getEntityType(), newId));
                        } else {
                            log.error("[{}][{}] Calculated field: {} references non existing entity.", ctx.getTenantId(), ctx.getSolutionId(), cf.getName());
                            throw new ThingsboardRuntimeException();
                        }
                    }
                }
            });
        }

        CalculatedField calculatedField = new CalculatedField(cf);
        return calculatedFieldService.save(calculatedField);
    }

    private RandomNameData generateRandomName(SolutionInstallContext ctx) {
        int i = 0;
        while (i < 10) {
            var randomName = RandomNameUtil.next();
            var user = userService.findUserByEmail(ctx.getTenantId(), randomName.getEmail());
            if (user == null) {
                return randomName;
            } else {
                i++;
            }
        }
        String firstName = StringUtils.randomAlphanumeric(5);
        String lastName = StringUtils.randomAlphanumeric(5);
        return new RandomNameData(firstName, lastName, firstName + "." + lastName + "@thingsboard.io");
    }

    private String randomize(String src, RandomNameData name) {
        return randomize(src, name, null);
    }

    private String randomize(String src, RandomNameData name, RandomNameData customer) {
        if (src == null) {
            return null;
        } else {
            String result = src
                    .replace("$randomFirstName", name.getFirstName())
                    .replace("$randomLastName", name.getLastName())
                    .replace("$randomEmail", name.getEmail());
            if (customer != null) {
                result = result
                        .replace("$customerFirstName", customer.getFirstName())
                        .replace("$customerLastName", customer.getLastName())
                        .replace("$customerEmail", customer.getEmail());
            }
            return result.replace("$random", StringUtils.randomAlphanumeric(10).toLowerCase());
        }
    }

    private void saveServerSideAttributes(SolutionInstallContext ctx, EntityId entityId, JsonNode attributes) {
        saveServerSideAttributes(ctx, entityId, attributes, null);
    }

    private void saveServerSideAttributes(SolutionInstallContext ctx, EntityId entityId, JsonNode attributes, RandomNameData randomNameData) {
        saveAttributes(ctx, entityId, attributes, randomNameData, AttributeScope.SERVER_SCOPE);
    }

    private void saveSharedAttributes(SolutionInstallContext ctx, EntityId entityId, JsonNode attributes) {
        if (!EntityType.DEVICE.equals(entityId.getEntityType())) {
            throw new IllegalArgumentException(entityId.getEntityType() + " cannot have shared attributes.");
        }
        saveAttributes(ctx, entityId, attributes, null, AttributeScope.SHARED_SCOPE);
    }

    private void saveAttributes(SolutionInstallContext ctx, EntityId entityId, JsonNode attributes, RandomNameData randomNameData, AttributeScope attributeScope) {
        if (attributes != null && !attributes.isNull() && !attributes.isEmpty()) {
            attributes = prepareAttributes(attributes);
            log.info("[{}] Saving attributes: {}", entityId, attributes);
            if (randomNameData != null) {
                attributes = JacksonUtil.toJsonNode(randomize(JacksonUtil.toString(attributes), randomNameData, null));
            }
            attributesService.save(ctx.getTenantId(), entityId, attributeScope,
                    new ArrayList<>(JsonConverter.convertToAttributes(JsonParser.parseString(JacksonUtil.toString(attributes)), ctx.getOldestTelemetryTs())));
        }
    }

    private JsonNode prepareAttributes(JsonNode attributes) {
        ObjectNode attributesObj = (ObjectNode) attributes;
        attributes.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            if (value.isTextual() && isTimeExpression(value.asText())) {
                value = JacksonUtil.toJsonNode(parseTimeExpression(value.asText()));
            }
            attributesObj.set(entry.getKey(), value);
        });
        return attributesObj;
    }

    private boolean isTimeExpression(String text) {
        return Pattern.matches("\\$\\{currentTime(?:([+-])(\\d+)([mwdh]|min))?}", text);
    }

    private String parseTimeExpression(String timeExpression) {
        Matcher matcher = Pattern.compile("\\$\\{currentTime(?:([+-])(\\d+)([mwdh]|min))?}").matcher(timeExpression);

        if (!matcher.matches()) {
            return timeExpression;
        }

        String operator = matcher.group(1);
        String amountStr = matcher.group(2);
        String unit = matcher.group(3);

        ZonedDateTime now = ZonedDateTime.now();

        if (operator != null && amountStr != null && unit != null) {
            int amount = Integer.parseInt(amountStr);
            now = switch (unit) {
                case "m" -> operator.equals("+") ? now.plusMonths(amount) : now.minusMonths(amount);
                case "w" -> operator.equals("+") ? now.plusWeeks(amount) : now.minusWeeks(amount);
                case "d" -> operator.equals("+") ? now.plusDays(amount) : now.minusDays(amount);
                case "h" -> operator.equals("+") ? now.plusHours(amount) : now.minusHours(amount);
                case "min" -> operator.equals("+") ? now.plusMinutes(amount) : now.minusMinutes(amount);
                default -> now;
            };
        }

        return String.valueOf(now.toInstant().toEpochMilli());
    }

    private String getTypeLabel(EntityType type) {
        return type.name().toLowerCase().replace('_', ' ');
    }

    private <T> T loadEntityIfFileExists(Path tempDir, String fileName, Class<T> clazz) {
        Path filePath = tempDir.resolve("entities").resolve(fileName);
        if (Files.exists(filePath)) {
            return JacksonUtil.readValue(filePath.toFile(), clazz);
        } else {
            return null;
        }
    }

    private <T> List<T> loadListOfEntitiesIfFileExists(Path tempDir, String fileName, TypeReference<List<T>> typeReference) {
        Path filePath = tempDir.resolve("entities").resolve(fileName);
        if (Files.exists(filePath)) {
            try {
                return JacksonUtil.readValue(filePath.toFile(), typeReference);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid json file " + fileName + " data structure", e);
            }
        } else {
            return new ArrayList<>();
        }
    }

    private <T> List<T> loadListOfEntitiesFromDirectory(Path tempDir, String dirName, Class<T> clazz) {
        Path dirPath = tempDir.resolve(dirName);
        if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
            List<T> result = new ArrayList<>();
            try {
                for (Path filePath : Files.list(dirPath).collect(Collectors.toList())) {
                    try {
                        result.add(JacksonUtil.readValue(filePath.toFile(), clazz));
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Invalid json file " + filePath.getFileName() + " data structure", e);
                    }
                }
            } catch (IOException e) {
                log.warn("Failed to read directory: {}", dirName, e);
                throw new RuntimeException(e);
            }
            return result;
        } else {
            return new ArrayList<>();
        }
    }

    private String loadSolutionId(Path tempDir) {
        Path solutionJson = tempDir.resolve("solution.json");
        if (Files.exists(solutionJson)) {
            JsonNode node = JacksonUtil.toJsonNode(solutionJson);
            if (node != null && node.has("title")) {
                String title = node.get("title").asText("");
                return title.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
            }
        }
        return null;
    }

    private long loadInstallTimeoutMs(Path tempDir) {
        Path solutionJson = tempDir.resolve("solution.json");
        if (Files.exists(solutionJson)) {
            JsonNode node = JacksonUtil.toJsonNode(solutionJson);
            if (node != null && node.has("installTimeoutMs")) {
                return node.get("installTimeoutMs").asLong(0L);
            }
        }
        return 0L;
    }

    private List<String> loadTenantTelemetryKeys(Path tempDir) {
        Path solutionJson = tempDir.resolve("solution.json");
        if (Files.exists(solutionJson)) {
            JsonNode node = JacksonUtil.toJsonNode(solutionJson);
            if (node != null && node.has("tenantTelemetryKeys")) {
                return JacksonUtil.convertValue(node.get("tenantTelemetryKeys"), new TypeReference<>() {});
            }
        }
        return Collections.emptyList();
    }

    private List<String> loadTenantAttributeKeys(Path tempDir) {
        Path solutionJson = tempDir.resolve("solution.json");
        if (Files.exists(solutionJson)) {
            JsonNode node = JacksonUtil.toJsonNode(solutionJson);
            if (node != null && node.has("tenantAttributeKeys")) {
                return JacksonUtil.convertValue(node.get("tenantAttributeKeys"), new TypeReference<>() {});
            }
        }
        return Collections.emptyList();
    }

    private static final int EXTRACT_BUFFER_SIZE = 8 * 1024;

    private void extractZip(byte[] zipData, Path destDir) throws IOException {
        long totalBytes = 0;
        int entryCount = 0;
        byte[] buf = new byte[EXTRACT_BUFFER_SIZE];
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (++entryCount > maxArchiveEntryCount) {
                    throw new IOException("Solution template archive exceeds max entry count: " + maxArchiveEntryCount);
                }
                Path entryPath = destDir.resolve(entry.getName()).normalize();
                if (!entryPath.startsWith(destDir)) {
                    throw new IOException("ZIP entry outside of target directory: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    try (OutputStream out = Files.newOutputStream(entryPath)) {
                        long entryBytes = 0;
                        int n;
                        while ((n = zis.read(buf)) > 0) {
                            entryBytes += n;
                            totalBytes += n;
                            if (entryBytes > maxUncompressedEntryBytes) {
                                throw new IOException("Solution template entry exceeds max uncompressed size: " + entry.getName());
                            }
                            if (totalBytes > maxUncompressedArchiveBytes) {
                                throw new IOException("Solution template archive exceeds max uncompressed size: " + maxUncompressedArchiveBytes + " bytes");
                            }
                            out.write(buf, 0, n);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private static void deleteDirectory(Path dir) {
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                    Files.delete(d);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.warn("Failed to clean up temp directory: {}", dir, e);
        }
    }

    private void deleteEntity(TenantId tenantId, EntityId entityId, User user) {
        try {
            List<AlarmId> alarmIds = alarmService.findAlarms(tenantId, new AlarmQuery(entityId, new TimePageLink(Integer.MAX_VALUE), null, null, null, false))
                    .getData().stream().map(AlarmInfo::getId).collect(Collectors.toList());
            Set<String> typesToRemove = new HashSet<>();
            alarmIds.forEach(alarmId -> {
                var result = alarmService.delAlarm(tenantId, alarmId, false);
                if (result.isSuccessful()) {
                    typesToRemove.add(result.getAlarm().getType());
                }
            });
            alarmService.delAlarmTypes(tenantId, typesToRemove);
        } catch (Exception e) {
            log.error("[{}] Failed to delete alarms for entity", entityId.getId(), e);
        }
        switch (entityId.getEntityType()) {
            case CALCULATED_FIELD:
                CalculatedField cf = calculatedFieldService.findById(tenantId, new CalculatedFieldId(entityId.getId()));
                if (cf != null) {
                    tbCalculatedFieldService.delete(cf, user);
                }
                break;
            case RULE_CHAIN:
                ruleChainService.deleteRuleChainById(tenantId, new RuleChainId(entityId.getId()));
                break;
            case DEVICE:
                Device device = deviceService.findDeviceById(tenantId, new DeviceId(entityId.getId()));
                if (device != null) {
                    tbDeviceService.delete(device, user);
                }
                break;
            case DEVICE_PROFILE:
                deviceProfileService.deleteDeviceProfile(tenantId, new DeviceProfileId(entityId.getId()));
                break;
            case ASSET:
                Asset asset = assetService.findAssetById(tenantId, new AssetId(entityId.getId()));
                if (asset != null) {
                    tbAssetService.delete(asset, user);
                }
                break;
            case ASSET_PROFILE:
                assetProfileService.deleteAssetProfile(tenantId, new AssetProfileId(entityId.getId()));
                break;
            case CUSTOMER:
                customerService.deleteCustomer(tenantId, new CustomerId(entityId.getId()));
                break;
            case USER:
                User userToDelete = userService.findUserById(tenantId, new UserId(entityId.getId()));
                if (userToDelete != null) {
                    userService.deleteUser(tenantId, userToDelete);
                }
                break;
            case EDGE:
                Edge edge = edgeService.findEdgeById(tenantId, new EdgeId(entityId.getId()));
                if (edge != null) {
                    tbEdgeService.delete(edge, user);
                }
                break;
            case DASHBOARD:
                dashboardService.deleteDashboard(tenantId, new DashboardId(entityId.getId()));
                break;
            default:
                log.warn("[{}] Unsupported entity type for deletion: {}", tenantId, entityId.getEntityType());
        }
    }

    private JsonNode replaceIds(SolutionInstallContext ctx, JsonNode json) {
        String jsonStr = JacksonUtil.toString(json);
        for (var e : ctx.getRealIds().entrySet()) {
            jsonStr = jsonStr.replace(e.getKey(), e.getValue());
        }
        return JacksonUtil.toJsonNode(jsonStr);
    }

    private String readFileContent(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            log.warn("Failed to read file: {}", path, e);
            return "";
        }
    }

}
