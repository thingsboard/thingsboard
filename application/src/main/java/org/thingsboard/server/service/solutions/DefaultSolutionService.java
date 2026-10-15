// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.iot_hub.SolutionTemplateInstalledItemDescriptor;
import org.thingsboard.server.common.data.kv.BaseDeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.configuration.ArgumentsBasedCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.job.task.CfReprocessingTask;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.subscription.SubscriptionException;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceConnectivityService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.device.DockerComposeParams;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.subscription.PlatformFeature;
import org.thingsboard.server.dao.subscription.SubscriptionService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.exception.EntitiesLimitExceededException;
import org.thingsboard.server.exception.ThingsboardRuntimeException;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.action.EntityActionService;
import org.thingsboard.server.service.cf.CalculatedFieldReprocessingService;
import org.thingsboard.server.service.entitiy.asset.TbAssetService;
import org.thingsboard.server.service.entitiy.cf.TbCalculatedFieldService;
import org.thingsboard.server.service.entitiy.device.TbDeviceService;
import org.thingsboard.server.service.entitiy.edge.TbEdgeService;
import org.thingsboard.server.service.entitiy.entity.group.TbEntityGroupService;
import org.thingsboard.server.service.entitiy.entity.relation.TbEntityRelationService;
import org.thingsboard.server.service.rule.TbRuleChainService;
import org.thingsboard.server.service.scheduler.SchedulerService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.system.SystemSecurityService;
import org.thingsboard.server.service.solutions.data.CreatedAlarmRuleInfo;
import org.thingsboard.server.service.solutions.data.CreatedCalculatedFieldInfo;
import org.thingsboard.server.service.solutions.data.CreatedEntityInfo;
import org.thingsboard.server.service.solutions.data.DashboardLinkInfo;
import org.thingsboard.server.service.solutions.data.DeviceCredentialsInfo;
import org.thingsboard.server.service.solutions.data.EdgeLinkInfo;
import org.thingsboard.server.service.solutions.data.SolutionInstallContext;
import org.thingsboard.server.service.solutions.data.SolutionValidationResult;
import org.thingsboard.server.service.solutions.data.UserCredentialsInfo;
import org.thingsboard.server.service.solutions.data.definition.AssetDefinition;
import org.thingsboard.server.service.solutions.data.definition.AssetProfileDefinition;
import org.thingsboard.server.service.solutions.data.definition.CalculatedFieldDefinition;
import org.thingsboard.server.service.solutions.data.definition.CustomerDefinition;
import org.thingsboard.server.service.solutions.data.definition.CustomerEntityDefinition;
import org.thingsboard.server.service.solutions.data.definition.DashboardDefinition;
import org.thingsboard.server.service.solutions.data.definition.DashboardUserDetailsDefinition;
import org.thingsboard.server.service.solutions.data.definition.DeviceDefinition;
import org.thingsboard.server.service.solutions.data.definition.DeviceProfileDefinition;
import org.thingsboard.server.service.solutions.data.definition.EdgeDefinition;
import org.thingsboard.server.service.solutions.data.definition.EdgeEntityGroupDefinition;
import org.thingsboard.server.service.solutions.data.definition.EmulatorDefinition;
import org.thingsboard.server.service.solutions.data.definition.EntityDefinition;
import org.thingsboard.server.service.solutions.data.definition.GroupRoleDefinition;
import org.thingsboard.server.service.solutions.data.definition.ReferenceableEntityDefinition;
import org.thingsboard.server.service.solutions.data.definition.RelationDefinition;
import org.thingsboard.server.service.solutions.data.definition.RoleDefinition;
import org.thingsboard.server.service.solutions.data.definition.SchedulerEventDefinition;
import org.thingsboard.server.service.solutions.data.definition.TenantDefinition;
import org.thingsboard.server.service.solutions.data.definition.UserDefinition;
import org.thingsboard.server.service.solutions.data.definition.UserGroupDefinition;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@TbCoreComponent
@RequiredArgsConstructor
@Service
@Slf4j
public class DefaultSolutionService implements SolutionService {

    private static final String BLANK_LINE = System.lineSeparator() + System.lineSeparator();
    private static final String CONFLICTS_INTRO =
            "Some entities of the solution template already exist. Rename or delete them and install the template again:";
    private static final String RANDOM_PLACEHOLDER = "$random";
    private static final int MAX_LISTED_NAMES_PER_TYPE = 10;
    private static final String TRUNCATION_NOTE = "Only the first " + MAX_LISTED_NAMES_PER_TYPE
            + " names of each type are listed. The rest are reported the next time the template is installed.";

    @Value("${ui.solution_templates.docs_base_url:https://thingsboard.io/docs/pe}")
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
    private final AssetProfileService assetProfileService;
    private final AttributesService attributesService;
    private final DashboardService dashboardService;
    private final TbEntityRelationService relationService;
    private final DeviceService deviceService;
    private final TbDeviceService tbDeviceService;
    private final DeviceCredentialsService deviceCredentialsService;
    private final AssetService assetService;
    private final TbAssetService tbAssetService;
    private final CustomerService customerService;
    private final UserService userService;
    private final CalculatedFieldService calculatedFieldService;
    private final TbCalculatedFieldService tbCalculatedFieldService;
    private final CalculatedFieldReprocessingService calculatedFieldReprocessingService;
    private final EdgeService edgeService;
    private final TbEdgeService tbEdgeService;
    private final EntityGroupService entityGroupService;
    private final TbEntityGroupService tbEntityGroupService;
    private final GroupPermissionService groupPermissionService;
    private final RoleService roleService;
    private final TimeseriesService tsService;
    private final SystemSecurityService systemSecurityService;
    private final TbClusterService tbClusterService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final TbQueueProducerProvider tbQueueProducerProvider;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final PartitionService partitionService;
    private final TelemetrySubscriptionService tsSubService;
    private final EntityActionService entityActionService;
    private final SchedulerEventService schedulerEventService;
    private final SchedulerService schedulerService;
    private final DeviceConnectivityService deviceConnectivityService;
    private final ExecutorService emulatorExecutor = ThingsBoardExecutors.newWorkStealingPool(10, getClass());
    private final ExecutorService cfsReprocessingExecutor = ThingsBoardExecutors.newWorkStealingPool(Math.max(4, Runtime.getRuntime().availableProcessors()), "solution-cfs-reprocessing-executor");
    private final SubscriptionService subscriptionService;

    @PreDestroy
    private void destroy() {
        emulatorExecutor.shutdownNow();
        cfsReprocessingExecutor.shutdownNow();
    }

    @Override
    public SolutionInstallResponse installSolution(SecurityUser user, TenantId tenantId, byte[] zipData, HttpServletRequest request) throws Exception {
        Path tempDir = Files.createTempDirectory("iot-hub-solution-");
        try {
            try {
                extractZip(zipData, tempDir);
            } catch (Throwable e) {
                log.error("[{}] Failed to extract solution template zip", tenantId, e);
                TenantSolutionTemplateInstructions instructions = new TenantSolutionTemplateInstructions();
                instructions.setDetails(e.getMessage());
                return new SolutionInstallResponse(instructions, false, List.of());
            }

            String solutionId = loadSolutionId(tempDir);
            if (solutionId == null) {
                throw new IllegalArgumentException("Solution template is missing solution.json or its 'title' field");
            }

            SolutionValidationResult validation = validateSolution(tenantId, tempDir);
            if (!validation.isPassed()) {
                return conflictResponse(validation.getConflictReport());
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

    SolutionValidationResult validateSolution(TenantId tenantId, Path tempDir) {
        checkSchedulerEventsAllowed(tenantId, tempDir);

        //TODO: validate entity counts before install.
        //TODO: pre-validate what still only fails at provision time: customer users (unique by email),
        // alarm rules and calculated fields.

        List<RoleDefinition> roles = loadListOfEntitiesIfFileExists(tempDir, "roles.json", new TypeReference<>() {
        });
        List<ReferenceableEntityDefinition> ruleChains = loadListOfEntitiesIfFileExists(tempDir, "rule_chains.json", new TypeReference<>() {
        });
        List<DeviceProfileDefinition> deviceProfiles = loadListOfEntitiesIfFileExists(tempDir, "device_profiles.json", new TypeReference<>() {
        });
        deviceProfiles.addAll(loadListOfEntitiesFromDirectory(tempDir, "device_profiles", DeviceProfileDefinition.class));
        List<AssetProfileDefinition> assetProfiles = loadListOfEntitiesIfFileExists(tempDir, "asset_profiles.json", new TypeReference<>() {
        });
        assetProfiles.addAll(loadListOfEntitiesFromDirectory(tempDir, "asset_profiles", AssetProfileDefinition.class));
        List<CustomerDefinition> customers = loadListOfEntitiesIfFileExists(tempDir, "customers.json", new TypeReference<>() {
        });
        List<DashboardDefinition> dashboards = loadListOfEntitiesIfFileExists(tempDir, "dashboards.json", new TypeReference<>() {
        });
        List<AssetDefinition> assets = loadListOfEntitiesIfFileExists(tempDir, "assets.json", new TypeReference<>() {
        });
        List<DeviceDefinition> devices = loadListOfEntitiesIfFileExists(tempDir, "devices.json", new TypeReference<>() {
        });
        List<EdgeDefinition> edges = loadListOfEntitiesIfFileExists(tempDir, "edges.json", new TypeReference<>() {
        });

        // Insertion ordered, so that the reported sections keep the order the entities are provisioned in. The
        // group section comes last, since the groups of every type are collected together.
        Map<EntityType, List<String>> conflicts = new LinkedHashMap<>();
        collectConflicts(conflicts, roles, name -> roleService.findRoleByTenantIdAndName(tenantId, name).orElse(null));
        collectRuleChainConflicts(conflicts, tenantId, tempDir, ruleChains);
        collectConflicts(conflicts, EntityType.DEVICE_PROFILE, deviceProfiles, DeviceProfile::getName,
                name -> deviceProfileService.findDeviceProfileByName(tenantId, name));
        collectConflicts(conflicts, EntityType.ASSET_PROFILE, assetProfiles, AssetProfile::getName,
                name -> assetProfileService.findAssetProfileByName(tenantId, name));
        collectConflicts(conflicts, customers,
                title -> isRandomizedCustomerTitle(title) ? null : customerService.findCustomerByTenantIdAndTitle(tenantId, title).orElse(null));
        collectConflicts(conflicts, assets, name -> assetService.findAssetByTenantIdAndName(tenantId, name));
        collectConflicts(conflicts, devices, name -> deviceService.findDeviceByTenantIdAndName(tenantId, name));
        collectConflicts(conflicts, dashboards, title -> dashboardService.findFirstDashboardInfoByTenantIdAndName(tenantId, title));
        collectConflicts(conflicts, edges, name -> edgeService.findEdgeByTenantIdAndName(tenantId, name));

        // The entities whose install creates a group with createEntityGroup, so these are the four group types that
        // can hit the unique constraint. CUSTOMER and USER groups are find-or-created and can never clash.
        List<CustomerEntityDefinition> groupedEntities = new ArrayList<>();
        groupedEntities.addAll(dashboards);
        groupedEntities.addAll(assets);
        groupedEntities.addAll(devices);
        groupedEntities.addAll(edges);
        collectGroupConflicts(conflicts, tenantId, groupedEntities, templateCustomerNames(customers));

        if (conflicts.isEmpty()) {
            return SolutionValidationResult.passed();
        }
        StringBuilder details = new StringBuilder(CONFLICTS_INTRO).append(BLANK_LINE);
        conflicts.forEach((entityType, conflictDescriptions) -> appendConflicts(details, entityType, conflictDescriptions));
        if (conflicts.values().stream().anyMatch(descriptions -> descriptions.size() > MAX_LISTED_NAMES_PER_TYPE)) {
            details.append(System.lineSeparator()).append(TRUNCATION_NOTE);
        }

        return SolutionValidationResult.conflictsFound(details.toString());
    }

    private static SolutionInstallResponse conflictResponse(String conflictReport) {
        SolutionInstallResponse response = new SolutionInstallResponse();
        response.setSuccess(false);
        response.setDetails(conflictReport);
        return response;
    }

    /**
     * The entity type and the name both come from the definition, so a list can not end up checked against the
     * wrong type by mistake.
     */
    private void collectConflicts(Map<EntityType, List<String>> conflicts, List<? extends EntityDefinition> definitions,
                                  Function<String, ? extends HasName> lookup) {
        Set<String> checked = new HashSet<>();
        for (EntityDefinition definition : definitions) {
            if (checked.add(definition.getName())) {
                collectConflict(conflicts, definition.getEntityType(), definition.getName(), lookup);
            }
        }
    }

    private <T> void collectConflicts(Map<EntityType, List<String>> conflicts, EntityType entityType, List<T> definitions,
                                      Function<T, String> nameExtractor, Function<String, ? extends HasName> lookup) {
        Set<String> checked = new HashSet<>();
        for (T definition : definitions) {
            String name = nameExtractor.apply(definition);
            if (checked.add(name)) {
                collectConflict(conflicts, entityType, name, lookup);
            }
        }
    }

    private void collectConflict(Map<EntityType, List<String>> conflicts, EntityType entityType, String name,
                                 Function<String, ? extends HasName> lookup) {
        if (StringUtils.isEmpty(name)) {
            return;
        }
        HasName existing = lookup.apply(name);
        if (existing != null) {
            conflicts.computeIfAbsent(entityType, key -> new ArrayList<>()).add(quoted(existing.getName()));
        }
    }

    /**
     * A rule chain is created from the rule chain file, so both the name and the type of the chain the install will
     * produce live there - the name in rule_chains.json is only a reference and the type defaults to CORE.
     */
    private void collectRuleChainConflicts(Map<EntityType, List<String>> conflicts, TenantId tenantId, Path tempDir,
                                           List<ReferenceableEntityDefinition> definitions) {
        Set<String> checked = new HashSet<>();
        for (ReferenceableEntityDefinition definition : definitions) {
            if (StringUtils.isEmpty(definition.getFile())) {
                continue;
            }
            Path ruleChainPath = tempDir.resolve("rule_chains").resolve(definition.getFile());
            if (!Files.exists(ruleChainPath)) {
                // provisionRuleChains logs and skips such a definition, so there is nothing to validate
                continue;
            }
            JsonNode ruleChain = JacksonUtil.toJsonNode(ruleChainPath).get("ruleChain");
            if (ruleChain == null || !ruleChain.hasNonNull("name")) {
                continue;
            }
            RuleChainType type = ruleChain.hasNonNull("type")
                    ? RuleChainType.valueOf(ruleChain.get("type").asText()) : RuleChainType.CORE;
            String name = ruleChain.get("name").asText();
            if (!checked.add(type + name)) {
                continue;
            }
            collectConflict(conflicts, EntityType.RULE_CHAIN, name,
                    ignored -> ruleChainService.findTenantRuleChainsByTypeAndName(tenantId, type, name)
                            .stream().findFirst().orElse(null));
        }
    }


    /**
     * Entity group names are unique per owner and group type, so the groups the template creates under the tenant
     * may clash with the groups the tenant already has. Groups owned by the customers of the template never clash:
     * those customers are created from scratch during the install, so they own no groups yet.
     */
    private void collectGroupConflicts(Map<EntityType, List<String>> conflicts, TenantId tenantId,
                                       List<CustomerEntityDefinition> definitions, Set<String> templateCustomers) {
        // deduplicated by type and name, since the entities of a template usually share a handful of groups
        Set<Map.Entry<EntityType, String>> checked = new HashSet<>();
        for (CustomerEntityDefinition definition : definitions) {
            // the group type of an entity is the entity type itself, so it is read from the definition
            EntityType groupType = definition.getEntityType();
            String groupName = definition.getGroup();
            if (StringUtils.isEmpty(groupName) || templateCustomers.contains(definition.getCustomer())
                    || !checked.add(Map.entry(groupType, groupName))) {
                continue;
            }
            // not put in the cache: the group is about to be created if the validation passes
            entityGroupService.findEntityGroupByTypeAndName(tenantId, tenantId, groupType, groupName, false)
                    .ifPresent(group -> conflicts.computeIfAbsent(EntityType.ENTITY_GROUP, key -> new ArrayList<>())
                            .add(groupDescription(group)));
        }
    }

    /**
     * The owner is always the tenant, since only tenant groups are looked up, but the report names the type and the
     * owner of a conflicting group next to its name: those are the three facts that tell the user which of the groups
     * they own has to be renamed.
     */
    private static String groupDescription(EntityGroup group) {
        return quoted(group.getName()) + " (Type: " + group.getType().getNormalName()
                + ", Owner: " + group.getOwnerId().getEntityType().getNormalName() + ")";
    }

    private Set<String> templateCustomerNames(List<CustomerDefinition> customers) {
        return customers.stream()
                .map(CustomerDefinition::getName)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());
    }

    /**
     * Only customer titles, user names and attribute values go through {@link #randomize}, so a {@code $random}
     * placeholder in a customer title is replaced at install time and there is nothing to check upfront. Names of
     * every other entity are used as written, placeholder included, and must be looked up as is.
     */
    private static boolean isRandomizedCustomerTitle(String title) {
        return title.contains(RANDOM_PLACEHOLDER);
    }

    /**
     * One markdown list item per entity type, so that the names of the conflicting entities are what the user reads,
     * instead of the same sentence repeated for every type. Only the first {@value #MAX_LISTED_NAMES_PER_TYPE}
     * names of a type are listed, the rest are counted - {@link #TRUNCATION_NOTE} tells the user that the list of
     * that type is not complete.
     */
    private static void appendConflicts(StringBuilder details, EntityType entityType, List<String> conflictDescriptions) {
        details.append("- **").append(entityType.getNormalName()).append("**: ")
                .append(conflictDescriptions.stream().limit(MAX_LISTED_NAMES_PER_TYPE).collect(Collectors.joining(", ")));
        if (conflictDescriptions.size() > MAX_LISTED_NAMES_PER_TYPE) {
            details.append(" and ").append(conflictDescriptions.size() - MAX_LISTED_NAMES_PER_TYPE)
                    .append(" more (").append(conflictDescriptions.size()).append(" in total)");
        }
        details.append(System.lineSeparator());
    }

    private static String quoted(String value) {
        return "'" + value + "'";
    }

    /**
     * Solution template references an entity that is not part of the template. Without a message the install dialog
     * shows an empty error, so both the referencing entity and the missing reference are named here.
     */
    private ThingsboardRuntimeException solutionConfigurationError(EntityType entityType, String entityName, String missingReference) {
        return new ThingsboardRuntimeException("Invalid solution configuration: " + entityType.getNormalName()
                + " \"" + entityName + "\" references " + missingReference + "!", ThingsboardErrorCode.GENERAL);
    }

    private SolutionInstallResponse doInstallSolution(User user, TenantId tenantId, String solutionId, Path tempDir, HttpServletRequest request) {
        SolutionInstallContext ctx = new SolutionInstallContext(tenantId, solutionId, tempDir, user, new TenantSolutionTemplateInstructions());
        try {

            registerEmulatorsAndComputeOldestTelemetryTs(ctx);

            provisionRoles(ctx);

            provisionTenantDetails(ctx);

            provisionRuleChains(ctx);

            provisionDeviceProfiles(ctx);

            provisionAssetProfiles(ctx);

            List<CustomerDefinition> customers = loadListOfEntitiesIfFileExists(ctx.getTempDir(), "customers.json", new TypeReference<>() {});

            provisionCustomers(ctx, customers);

            var assets = provisionAssets(ctx);

            var devices = provisionDevices(user, ctx);

            provisionDashboards(ctx);

            provisionCustomerUsers(ctx, customers);

            provisionRelations(ctx);

            provisionSchedulerEvents(ctx);

            updateRuleChains(ctx);

            provisionEdges(user, ctx, request);

            provisionAlarmRules(ctx);

            Set<CompletableFuture<Void>> telemetryLoading = launchEmulators(ctx, devices, assets);

            waitForTelemetryCompletion(telemetryLoading);

            provisionCalculatedFields(ctx);

            ctx.getSolutionInstructions().setDetails(prepareInstructions(ctx, request));

            List<ReferenceableEntityDefinition> ruleChains = loadListOfEntitiesIfFileExists(ctx.getTempDir(), "rule_chains.json", new TypeReference<>() {});
            if (ruleChains.stream().anyMatch(r -> StringUtils.isNotEmpty(r.getUpdate()))) {
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
            } else if (e instanceof SubscriptionException se) {
                throw se;
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
            all.get(); // wait until all done
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
                    getDashboardLink(solutionInstructions, solutionInstructions.getDashboardGroupId(), solutionInstructions.getDashboardId(), false));
            if (solutionInstructions.isMainDashboardPublic()) {
                template = template.replace("${MAIN_DASHBOARD_PUBLIC_URL}",
                        getDashboardLink(solutionInstructions, solutionInstructions.getDashboardGroupId(), solutionInstructions.getDashboardId(), true));
            }
        }

        for (DashboardLinkInfo dashboardLinkInfo : ctx.getDashboardLinks()) {
            template = template.replace("${" + dashboardLinkInfo.getName() + "DASHBOARD_URL}",
                    getDashboardLink(solutionInstructions, dashboardLinkInfo.getEntityGroupId(), dashboardLinkInfo.getDashboardId(), false));
            if (dashboardLinkInfo.isPublic()) {
                template = template.replace("${" + dashboardLinkInfo.getName() + "DASHBOARD_PUBLIC_URL}",
                        getDashboardLink(solutionInstructions, dashboardLinkInfo.getEntityGroupId(), dashboardLinkInfo.getDashboardId(), true));
            }
        }

        if (template.contains("${GATEWAYS_URL}")) {
            template = template.replace("${GATEWAYS_URL}", "/entities/gateways");
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
                template = template.replace("${DOCKER_CONFIG}", prepareDockerComposeFile(ctx.getTenantId(), ctx.getSolutionId(), baseUrl, credentialsInfo.getCredentials().getDeviceId()));
            }
        }

        template = template.replace("${device_list_and_credentials}", devList.toString());

        StringBuilder userList = new StringBuilder();

        // User list (without user group column)
        userList.append("| Name | Login | Password | Customer name | User Group |");
        userList.append(System.lineSeparator());
        userList.append("| :---  | :---  | :---  | :---  | :---  |");
        userList.append(System.lineSeparator());

        for (UserCredentialsInfo credentialsInfo : ctx.getCreatedUsers().values()) {
            userList.append("|").append(credentialsInfo.getName())
                    .append("|").append(credentialsInfo.getLogin()).append("{:copy-code}")
                    .append("|").append(credentialsInfo.getPassword()).append("{:copy-code}")
                    .append("|").append(credentialsInfo.getCustomerName() != null ? credentialsInfo.getCustomerName() : "")
                    .append("|").append(credentialsInfo.getCustomerGroup() != null ? credentialsInfo.getCustomerGroup() : "");
            userList.append(System.lineSeparator());
        }

        template = template.replace("${user_list}", userList.toString());

        // Edge detail URLs
        for (Map.Entry<String, EdgeLinkInfo> edgeLinkInfoEntry : ctx.getCreatedEdges().entrySet()) {
            EdgeLinkInfo edgeLinkInfo = edgeLinkInfoEntry.getValue();
            StringBuilder edgeDetailsUrl = new StringBuilder();
            if (EntityType.CUSTOMER.equals(edgeLinkInfo.getOwnerId().getEntityType())) {
                edgeDetailsUrl.append("/customers/all/").append(edgeLinkInfo.getOwnerId().getId());
            }
            edgeDetailsUrl.append("/edgeManagement/edges/all/").append(edgeLinkInfo.getEdgeId().getId());
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

    private String getDashboardLink(TenantSolutionTemplateInstructions solutionInstructions, EntityGroupId dashboardGroupId, DashboardId dashboardId, boolean isPublic) {
        String dashboardLink;
        if (isPublic) {
            dashboardLink = "/dashboard/" + dashboardId.getId() + "?publicId=" + solutionInstructions.getPublicId();
        } else {
            dashboardLink = "/dashboardGroups/" + dashboardGroupId.getId() + "/" + dashboardId.getId();
        }
        return dashboardLink;
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

    private void provisionRoles(SolutionInstallContext ctx) {
        List<RoleDefinition> roleDefinitions = loadListOfEntitiesIfFileExists(ctx.getTempDir(), "roles.json", new TypeReference<>() {
        });
        for (RoleDefinition roleDef : roleDefinitions) {
            Role role = new Role();
            role.setTenantId(ctx.getTenantId());
            role.setName(roleDef.getName());
            role.setType(roleDef.getType());
            role.setPermissions(roleDef.getOperations());
            role = roleService.saveRole(ctx.getTenantId(), role);
            ctx.register(role);
            ctx.putIdToMap(roleDef, role.getId());
        }
    }

    private void provisionRuleChains(SolutionInstallContext ctx) {
        boolean edgeAllowed = subscriptionService.isCreateEdgeAllowed(ctx.getTenantId());
        List<ReferenceableEntityDefinition> ruleChainDefs = loadListOfEntitiesIfFileExists(ctx.getTempDir(), "rule_chains.json", new TypeReference<>() {});
        for (ReferenceableEntityDefinition entityDef : ruleChainDefs) {
            Path ruleChainPath = ctx.getTempDir().resolve("rule_chains").resolve(entityDef.getFile());
            if (!Files.exists(ruleChainPath)) {
                log.warn("[{}] Rule chain file not found: {}", ctx.getTenantId(), entityDef.getFile());
                continue;
            }
            JsonNode ruleChainJson = replaceIds(ctx, JacksonUtil.toJsonNode(ruleChainPath));

            RuleChain ruleChain = JacksonUtil.treeToValue(ruleChainJson.get("ruleChain"), RuleChain.class);
            if (!edgeAllowed && RuleChainType.EDGE.equals(ruleChain.getType())) {
                log.warn("[{}][{}] Skipping EDGE rule chain provisioning due to subscription limitations: {}", ctx.getTenantId(), ctx.getSolutionId(), ruleChain.getName());
                continue;
            }
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
        List<DeviceProfileDefinition> deviceProfiles = loadListOfEntitiesIfFileExists(ctx.getTempDir(), "device_profiles.json", new TypeReference<>() {
        });
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
                    log.error("[{}][{}] Device profile: {} references non existing rule chain.", ctx.getTenantId(), ctx.getSolutionId(), deviceProfile.getName());
                    throw solutionConfigurationError(EntityType.DEVICE_PROFILE, deviceProfile.getName(), "non existing rule chain");
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
        List<AssetProfileDefinition> assetProfiles = loadListOfEntitiesIfFileExists(ctx.getTempDir(), "asset_profiles.json", new TypeReference<>() {
        });
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
                    log.error("[{}][{}] Asset profile: {} references non existing rule chain.", ctx.getTenantId(), ctx.getSolutionId(), assetProfile.getName());
                    throw solutionConfigurationError(EntityType.ASSET_PROFILE, assetProfile.getName(), "non existing rule chain");
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

    /**
     * The licence side of installing scheduler events, checked from the {@code validateSolution} pre-flight
     * rather than from {@code provisionSchedulerEvents}: by provisioning time a dozen other entity types have
     * already been written, and refusing then would mean unwinding them one by one. The refusal is a thrown
     * {@link SubscriptionException} rather than a failed {@code SolutionInstallResponse}, so the caller still
     * answers with the licence error the provisioning step would have produced.
     */
    private void checkSchedulerEventsAllowed(TenantId tenantId, Path tempDir) {
        List<SchedulerEventDefinition> schedulerEvents = loadSchedulerEvents(tempDir);
        if (schedulerEvents.isEmpty()) {
            return;
        }
        subscriptionService.checkFeatureAllowed(tenantId, PlatformFeature.SCHEDULER);
        // A report-producing event goes on to generate reports on its own schedule, so installing one is a
        // reporting write too.
        boolean reportProducing = schedulerEvents.stream().anyMatch(entityDef ->
                SchedulerEvent.isReportProducing(entityDef.getType(), entityDef.getConfiguration()));
        if (reportProducing) {
            subscriptionService.checkFeatureAllowed(tenantId, PlatformFeature.REPORTING);
        }
    }

    private List<SchedulerEventDefinition> loadSchedulerEvents(Path tempDir) {
        List<SchedulerEventDefinition> schedulerEvents = loadListOfEntitiesIfFileExists(tempDir, "scheduler_events.json", new TypeReference<>() {
        });
        schedulerEvents.addAll(loadListOfEntitiesFromDirectory(tempDir, "scheduler_events", SchedulerEventDefinition.class));
        return schedulerEvents;
    }

    private void provisionSchedulerEvents(SolutionInstallContext ctx) {
        // The licence check for these lives in the install pre-flight; see checkSchedulerEventsAllowed.
        List<SchedulerEventDefinition> schedulerEvents = loadSchedulerEvents(ctx.getTempDir());
        schedulerEvents.forEach(entityDef -> {
            SchedulerEvent schedulerEvent = getSchedulerEvent(ctx, entityDef);
            //TODO: use tbSchedulerService here when it becomes available.
            SchedulerEvent savedSchedulerEvent = schedulerEventService.saveSchedulerEvent(schedulerEvent);

            if (schedulerEvent.getId() == null) {
                schedulerService.onSchedulerEventAdded(savedSchedulerEvent);
            } else {
                schedulerService.onSchedulerEventUpdated(savedSchedulerEvent);
            }
            log.info("[{}] Saved scheduler event: {}", schedulerEvent.getId(), schedulerEvent);
            ctx.register(entityDef, savedSchedulerEvent);
        });
    }

    private SchedulerEvent getSchedulerEvent(SolutionInstallContext ctx, SchedulerEventDefinition entityDef) {
        SchedulerEvent schedulerEvent = new SchedulerEvent();
        schedulerEvent.setTenantId(ctx.getTenantId());
        schedulerEvent.setName(entityDef.getName());
        schedulerEvent.setType(entityDef.getType());
        schedulerEvent.setConfiguration(entityDef.getConfiguration());
        schedulerEvent.setSchedule(entityDef.getSchedule());
        schedulerEvent.setCustomerId(ctx.getIdFromMap(EntityType.CUSTOMER, entityDef.getCustomer()));
        if (entityDef.getOriginatorId() != null) {
            String newIdStr = ctx.getRealIds().get(entityDef.getOriginatorId().getId().toString());
            if (newIdStr != null) {
                EntityId newId = EntityIdFactory.getByTypeAndUuid(entityDef.getOriginatorId().getEntityType(), UUID.fromString(newIdStr));
                schedulerEvent.setOriginatorId(newId);
            } else {
                log.error("[{}][{}] Scheduler event: {} references non existing entity.", ctx.getTenantId(), ctx.getSolutionId(), entityDef.getName());
                throw new ThingsboardRuntimeException(
                        String.format("[{}][{}] Scheduler event: {} references non existing entity.",
                                ctx.getTenantId(), ctx.getSolutionId(), entityDef.getName()),
                        ThingsboardErrorCode.GENERAL);
            }
        }
        return schedulerEvent;
    }

    private void provisionDashboards(SolutionInstallContext ctx) throws ThingsboardException {
        List<DashboardDefinition> dashboards = loadListOfEntitiesIfFileExists(ctx.getTempDir(), "dashboards.json", new TypeReference<>() {
        });
        for (DashboardDefinition entityDef : dashboards) {
            CustomerId customerId = ctx.getIdFromMap(EntityType.CUSTOMER, entityDef.getCustomer());
            Path dashboardsPath = ctx.getTempDir().resolve("dashboards").resolve(entityDef.getFile());
            JsonNode dashboardJson = replaceIds(ctx, JacksonUtil.toJsonNode(dashboardsPath));
            Dashboard dashboardTemplate = JacksonUtil.treeToValue(dashboardJson, Dashboard.class);

            Dashboard dashboard = new Dashboard();
            dashboard.setTenantId(ctx.getTenantId());
            dashboard.setTitle(entityDef.getName());
            dashboard.setConfiguration(dashboardTemplate.getConfiguration());
            dashboard.setCustomerId(customerId);
            dashboard.setImage(dashboardTemplate.getImage());
            dashboard.setResources(dashboardTemplate.getResources());
            dashboard = dashboardService.saveDashboard(dashboard);

            ctx.register(entityDef, dashboard);
            ctx.putIdToMap(EntityType.DASHBOARD, entityDef.getName(), dashboard.getId());
            EntityGroupId entityGroupId = addEntityToGroup(ctx, entityDef, dashboard.getId());
            if (entityGroupId == null) {
                entityGroupId = entityGroupService.findEntityGroupByTypeAndName(ctx.getTenantId(), dashboard.getOwnerId(), EntityType.DASHBOARD, EntityGroup.GROUP_ALL_NAME).get().getId();
            }
            if (entityDef.isMain()) {
                ctx.getSolutionInstructions().setDashboardGroupId(entityGroupId);
                ctx.getSolutionInstructions().setDashboardId(dashboard.getId());
                ctx.getSolutionInstructions().setMainDashboardPublic(entityDef.isMakePublic());
            }
            ctx.getDashboardLinks().add(new DashboardLinkInfo(dashboard.getName(), entityGroupId, dashboard.getId(), entityDef.isMakePublic()));
        }
    }

    protected void provisionRelations(SolutionInstallContext ctx) {
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
        if (EntityType.TENANT == relationDef.getEntityType()) {
            return ctx.getTenantId();
        }
        return ctx.getIdFromMap(relationDef.getEntityType(), relationDef.getEntityName());
    }

    protected Map<Device, DeviceDefinition> provisionDevices(User user, SolutionInstallContext ctx) throws Exception {
        Map<Device, DeviceDefinition> result = new HashMap<>();
        Set<String> deviceTypeSet = new HashSet<>();
        List<DeviceDefinition> devices = loadListOfEntitiesIfFileExists(ctx.getTempDir(), "devices.json", new TypeReference<>() {
        });

        for (DeviceDefinition entityDef : devices) {
            CustomerId customerId = ctx.getIdFromMap(EntityType.CUSTOMER, entityDef.getCustomer());
            Device entity = new Device();
            entity.setTenantId(ctx.getTenantId());
            entity.setName(entityDef.getName());
            entity.setLabel(entityDef.getLabel());
            ensureDeviceProfileExists(ctx, deviceTypeSet, entityDef);
            entity.setType(entityDef.getType());
            entity.setCustomerId(customerId);
            entity.setAdditionalInfo(entityDef.getAdditionalInfo());
            entity = deviceService.saveDevice(entity);

            entityActionService.logEntityAction(user, entity.getId(), entity, customerId, ActionType.ADDED, null);

            ctx.register(entityDef, entity);
            log.info("[{}] Saved device: {}", entity.getId(), entity);
            DeviceId entityId = entity.getId();
            ctx.putIdToMap(entityDef, entityId);

            saveServerSideAttributes(ctx, entityId, entityDef.getAttributes());
            saveSharedAttributes(ctx, entityId, entityDef.getSharedAttributes());
            ctx.put(entityId, entityDef.getRelations());
            addEntityToGroup(ctx, entityDef, entityId);

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
                deviceTypeSet.add(entityDef.getType());
            }
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
                .mapToLong(value -> value.getOldestTs(ctx))
                .min().orElse(solutionInstallTs);
        long oldestAssetEmulatorsTs = assetEmulators.values().stream()
                .mapToLong(value -> value.getOldestTs(ctx))
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

    protected void provisionTenantDetails(SolutionInstallContext ctx) throws Exception {
        TenantDefinition tenant = loadEntityIfFileExists(ctx.getTempDir(), "tenant.json", TenantDefinition.class);
        if (tenant != null) {
            saveServerSideAttributes(ctx, ctx.getTenantId(), tenant.getAttributes());
            ctx.put(ctx.getTenantId(), tenant.getRelations());

            for (UserGroupDefinition ugDef : tenant.getUserGroups()) {
                EntityGroup ugEntity = getUserGroupInfo(ctx, ctx.getTenantId(), ugDef.getName());
                ctx.registerReferenceOnly(ugDef.getJsonId(), ugEntity.getId());

                for (String genericRoleName : ugDef.getGenericRoles()) {
                    RoleId roleId = ctx.getIdFromMap(EntityType.ROLE, genericRoleName);
                    GroupPermission gp = new GroupPermission();
                    gp.setRoleId(roleId);
                    gp.setTenantId(ctx.getTenantId());
                    gp.setUserGroupId(ugEntity.getId());
                    log.info("[{}] Saving group permission: {}", ctx.getTenantId(), gp);
                    groupPermissionService.saveGroupPermission(ctx.getTenantId(), gp);

                }
                for (GroupRoleDefinition grDef : ugDef.getGroupRoles()) {
                    RoleId roleId = ctx.getIdFromMap(EntityType.ROLE, grDef.getRoleName());
                    EntityGroupId entityGroupId = ctx.getGroupIdFromMap(grDef.getGroupType(), grDef.getGroupName());
                    if (entityGroupId == null) {
                        throw new RuntimeException("Invalid solution configuration. EntityGroup does not exist:" + grDef.getGroupType() + grDef.getGroupName());
                    }
                    GroupPermission gp = new GroupPermission();
                    gp.setRoleId(roleId);
                    gp.setTenantId(ctx.getTenantId());
                    gp.setUserGroupId(ugEntity.getId());
                    gp.setEntityGroupId(entityGroupId);
                    gp.setEntityGroupType(grDef.getGroupType());
                    log.info("[{}] Saving group permission: {}", ctx.getTenantId(), gp);
                    groupPermissionService.saveGroupPermission(ctx.getTenantId(), gp);
                }
            }
        }
    }

    protected Map<Asset, AssetDefinition> provisionAssets(SolutionInstallContext ctx) throws ThingsboardException {
        Map<Asset, AssetDefinition> result = new HashMap<>();
        Set<String> assetTypeSet = new HashSet<>();
        List<AssetDefinition> assets = loadListOfEntitiesIfFileExists(ctx.getTempDir(), "assets.json", new TypeReference<>() {
        });
        for (AssetDefinition entityDef : assets) {
            Asset entity = new Asset();
            entity.setTenantId(ctx.getTenantId());
            entity.setName(entityDef.getName());
            entity.setLabel(entityDef.getLabel());
            entity.setType(entityDef.getType());
            entity.setCustomerId(ctx.getIdFromMap(EntityType.CUSTOMER, entityDef.getCustomer()));
            ensureAssetProfileExists(ctx, assetTypeSet, entityDef);
            entity = assetService.saveAsset(entity);
            ctx.register(entityDef, entity);
            log.info("[{}] Saved asset: {}", entity.getId(), entity);
            AssetId entityId = entity.getId();
            ctx.putIdToMap(entityDef, entityId);
            saveServerSideAttributes(ctx, entityId, entityDef.getAttributes());
            ctx.put(entityId, entityDef.getRelations());
            addEntityToGroup(ctx, entityDef, entityId);
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
                assetTypeSet.add(entityDef.getType());
            }
        }
    }

    private void provisionCustomers(SolutionInstallContext ctx, List<CustomerDefinition> customers) throws ExecutionException, InterruptedException {
        for (CustomerDefinition entityDef : customers) {
            EntityGroup groupEntity = null;
            if (!StringUtils.isEmpty(entityDef.getGroup())) {
                groupEntity = getCustomerGroupInfo(ctx, ctx.getTenantId(), entityDef.getGroup());
            }
            entityDef.setRandomNameData(generateRandomName(ctx));
            Customer entity = new Customer();
            entity.setTenantId(ctx.getTenantId());
            entity.setTitle(randomize(entityDef.getName(), entityDef.getRandomNameData()));
            entity.setEmail(randomize(entityDef.getEmail(), entityDef.getRandomNameData()));
            entity.setCountry(entityDef.getCountry());
            entity.setCity(entityDef.getCity());
            entity.setState(entityDef.getState());
            entity.setZip(entityDef.getZip());
            entity.setAddress(entityDef.getAddress());
            entity = customerService.saveCustomer(entity);
            log.info("[{}] Saved customer: {}", entity.getId(), entity);
            ctx.register(entityDef, entity);
            CustomerId entityId = entity.getId();
            ctx.putIdToMap(entityDef, entityId);
            saveServerSideAttributes(ctx, entityId, entityDef.getAttributes(), entityDef.getRandomNameData());
            ctx.put(entityId, entityDef.getRelations());

            entityDef.getAssetGroups().forEach(name -> createEntityGroup(ctx, entityId, name, EntityType.ASSET));
            entityDef.getDeviceGroups().forEach(name -> createEntityGroup(ctx, entityId, name, EntityType.DEVICE));
            entityDef.setName(entity.getName());
            if (groupEntity != null) {
                entityGroupService.addEntitiesToEntityGroup(ctx.getTenantId(), groupEntity.getId(), Collections.singletonList(entity.getId()));
            }
        }
    }

    private void provisionCustomerUsers(SolutionInstallContext ctx, List<CustomerDefinition> customers) throws ExecutionException, InterruptedException {
        for (CustomerDefinition entityDef : customers) {
            Customer entity = customerService.findCustomerByTenantIdAndTitle(ctx.getTenantId(), entityDef.getName()).get();
            for (UserGroupDefinition ugDef : entityDef.getUserGroups()) {
                EntityGroup ugEntity = getUserGroupInfo(ctx, entity.getId(), ugDef.getName());
                ctx.registerReferenceOnly(ugDef.getJsonId(), ugEntity.getId());
                for (String genericRoleName : ugDef.getGenericRoles()) {
                    RoleId roleId = ctx.getIdFromMap(EntityType.ROLE, genericRoleName);
                    GroupPermission gp = new GroupPermission();
                    gp.setRoleId(roleId);
                    gp.setTenantId(ctx.getTenantId());
                    gp.setUserGroupId(ugEntity.getId());
                    log.info("[{}] Saving group permission: {}", entity.getId(), gp);
                    groupPermissionService.saveGroupPermission(ctx.getTenantId(), gp);

                }
                for (GroupRoleDefinition grDef : ugDef.getGroupRoles()) {
                    RoleId roleId = ctx.getIdFromMap(EntityType.ROLE, grDef.getRoleName());
                    EntityGroupId entityGroupId = ctx.getGroupIdFromMap(grDef.getGroupType(), grDef.getGroupName());
                    if (entityGroupId == null) {
                        throw new RuntimeException("Invalid solution configuration. EntityGroup does not exist:" + grDef.getGroupType() + grDef.getGroupName());
                    }
                    GroupPermission gp = new GroupPermission();
                    gp.setRoleId(roleId);
                    gp.setTenantId(ctx.getTenantId());
                    gp.setUserGroupId(ugEntity.getId());
                    gp.setEntityGroupId(entityGroupId);
                    gp.setEntityGroupType(grDef.getGroupType());
                    log.info("[{}] Saving group permission: {}", entity.getId(), gp);
                    groupPermissionService.saveGroupPermission(ctx.getTenantId(), gp);
                }
            }

            for (UserDefinition uDef : entityDef.getUsers()) {
                String originalName = uDef.getName(); // May not be unique;
                EntityGroup ugEntity = getUserGroupInfo(ctx, entity.getId(), uDef.getGroup());
                User user = createUser(ctx, entity, uDef, entityDef);
                // TODO: get activation token, etc..
                UserCredentials credentials = userService.findUserCredentialsByUserId(user.getTenantId(), user.getId());
                credentials.setEnabled(true);
                credentials.setActivateToken(null);
                credentials.setPassword(passwordEncoder.encode(uDef.getPassword()));
                userService.saveUserCredentials(ctx.getTenantId(), credentials);
                entityGroupService.addEntitiesToEntityGroup(ctx.getTenantId(), ugEntity.getId(), Collections.singletonList(user.getId()));
                DashboardUserDetailsDefinition dd = uDef.getDashboard();
                if (dd != null) {
                    DashboardId dashboardId = ctx.getIdFromMap(EntityType.DASHBOARD, dd.getName());
                    ObjectNode additionalInfo = JacksonUtil.newObjectNode();
                    additionalInfo.put("defaultDashboardId", dashboardId.getId().toString());
                    additionalInfo.put("defaultDashboardFullscreen", dd.isFullScreen());
                    user.setAdditionalInfo(additionalInfo);
                    userService.saveUser(ctx.getTenantId(), user);
                    log.info("[{}] Added default dashboard for user {}", entity.getId(), user.getEmail());
                }
                UserCredentialsInfo credentialsInfo = new UserCredentialsInfo();
                credentialsInfo.setName(user.getFirstName() + " " + user.getLastName());
                credentialsInfo.setLogin(uDef.getName());
                credentialsInfo.setPassword(uDef.getPassword());
                credentialsInfo.setCustomerName(entityDef.getName());
                credentialsInfo.setCustomerGroup(uDef.getGroup());
                ctx.addUserCredentials(credentialsInfo);
                ctx.register(entityDef, uDef, user);
                ctx.put(user.getId(), uDef.getRelations());
                ctx.putIdToMap(EntityType.USER, originalName, user.getId());
                ctx.putIdToMap(EntityType.USER, uDef.getName(), user.getId());
                saveServerSideAttributes(ctx, user.getId(), uDef.getAttributes());
            }
        }
    }

    private User createUser(SolutionInstallContext ctx, Customer entity, UserDefinition uDef, CustomerDefinition cDef) {
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
                user.setCustomerId(entity.getId());
                user.setTenantId(ctx.getTenantId());
                log.info("[{}] Saving user: {}", entity.getId(), user);
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

    private void provisionEdges(User user, SolutionInstallContext ctx, HttpServletRequest request) throws Exception {
        List<EdgeDefinition> edges = loadListOfEntitiesIfFileExists(ctx.getTempDir(), "edges.json", new TypeReference<>() {
        });
        RuleChain edgeTemplateRootRuleChain = ruleChainService.getEdgeTemplateRootRuleChain(ctx.getTenantId());
        for (EdgeDefinition entityDef : edges) {
            if (!subscriptionService.isCreateEdgeAllowed(ctx.getTenantId())) {
                log.warn("Skipping edge provisioning for tenant {}", ctx.getTenantId());
                break;
            }
            Edge entity = new Edge();
            entity.setTenantId(ctx.getTenantId());
            entity.setName(entityDef.getName());
            entity.setLabel(entityDef.getLabel());
            entity.setType(entityDef.getType());
            entity.setCustomerId(ctx.getIdFromMap(EntityType.CUSTOMER, entityDef.getCustomer()));
            entity.setRoutingKey(UUID.randomUUID().toString());
            entity.setSecret(StringUtils.randomAlphanumeric(20));
            entity.setEdgeLicenseKey(EdgeUtils.DEFAULT_EDGE_LICENSE_KEY);
            entity.setCloudEndpoint(systemSecurityService.getBaseUrl(ctx.getTenantId(), null, request));
            RuleChainId rootRuleChainId = edgeTemplateRootRuleChain.getId();
            if (StringUtils.isNotBlank(entityDef.getRootRuleChainId())) {
                String newId = ctx.getRealIds().get(entityDef.getRootRuleChainId());
                if (newId != null) {
                    rootRuleChainId = new RuleChainId(UUID.fromString(newId));
                } else {
                    log.error("[{}][{}] Edge: {} references non existing rule chain.", ctx.getTenantId(), ctx.getSolutionId(), entity.getName());
                    throw solutionConfigurationError(EntityType.EDGE, entity.getName(), "non existing rule chain");
                }
            }
            entity.setRootRuleChainId(rootRuleChainId);
            RuleChain rootRuleChain = ruleChainService.findRuleChainById(ctx.getTenantId(), rootRuleChainId);
            entity = tbEdgeService.save(entity, rootRuleChain, Collections.emptyList(), user);
            ctx.register(entityDef, entity);
            assignRuleChainsToEdge(ctx, entityDef.getRuleChainIds(), entity);
            assignEntityGroupsToEdge(ctx, EntityType.ASSET, entityDef.getAssetGroups(), entity);
            assignEntityGroupsToEdge(ctx, EntityType.DEVICE, entityDef.getDeviceGroups(), entity);
            assignEntityGroupsToEdge(ctx, EntityType.USER, entityDef.getUserGroups(), entity);
            assignEntityGroupsToEdge(ctx, EntityType.DASHBOARD, entityDef.getDashboardGroups(), entity);
            assignAssetsToEdge(ctx, entityDef.getAssetIds(), entity);
            assignDevicesToEdge(ctx, entityDef.getDeviceIds(), entity);
            assignSchedulerEventsToEdge(ctx, entityDef.getSchedulerEventIds(), entity);
            log.info("[{}] Saved edge: {}", entity.getId(), entity);
            EdgeId entityId = entity.getId();
            ctx.putIdToMap(entityDef, entityId);
            saveServerSideAttributes(ctx, entityId, entityDef.getAttributes());
            ctx.put(entityId, entityDef.getRelations());
            addEntityToGroup(ctx, entityDef, entityId);

            EdgeLinkInfo edgeLinkInfo = new EdgeLinkInfo(entity.getId(), entity.getOwnerId());
            ctx.addEdgeLinkInfo(entity.getName(), edgeLinkInfo);
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
                log.error("[{}][{}] Edge: {} references non existing edge rule chain.", ctx.getTenantId(), ctx.getSolutionId(), entity.getName());
                throw solutionConfigurationError(EntityType.EDGE, entity.getName(), "non existing edge rule chain");
            }
        }
    }

    private void assignEntityGroupsToEdge(SolutionInstallContext ctx, EntityType entityType, List<EdgeEntityGroupDefinition> entityGroupDefinitions, Edge edge) {
        for (EdgeEntityGroupDefinition entityGroupDefinition : entityGroupDefinitions) {
            EntityId parentEntityId = ctx.getTenantId();
            if (entityGroupDefinition.getCustomer() != null) {
                parentEntityId = ctx.getIdFromMap(EntityType.CUSTOMER, entityGroupDefinition.getCustomer());
            }
            Optional<EntityGroup> entityGroupOptional = entityGroupService.findEntityGroupByTypeAndName(ctx.getTenantId(), parentEntityId, entityType, entityGroupDefinition.getName());
            entityGroupOptional.ifPresent(entityGroup -> entityGroupService.assignEntityGroupToEdge(ctx.getTenantId(), entityGroup.getId(), edge.getId(), entityType));
        }
    }

    private void assignSchedulerEventsToEdge(SolutionInstallContext ctx, List<String> schedulerEventIds, Edge entity) {
        if (schedulerEventIds == null || schedulerEventIds.isEmpty()) {
            return;
        }
        for (String strSchedulerEventId : schedulerEventIds) {
            String newId = ctx.getRealIds().get(strSchedulerEventId);
            if (newId != null) {
                SchedulerEventId schedulerEventId = new SchedulerEventId(UUID.fromString(newId));
                schedulerEventService.assignSchedulerEventToEdge(ctx.getTenantId(), schedulerEventId, entity.getId());
            } else {
                log.error("[{}][{}] Edge: {} references non existing scheduler event.", ctx.getTenantId(), ctx.getSolutionId(), entity.getName());
                throw solutionConfigurationError(EntityType.EDGE, entity.getName(), "non existing scheduler event");
            }
        }
    }

    private void assignAssetsToEdge(SolutionInstallContext ctx, List<String> assetIds, Edge entity) throws ThingsboardException {
        if (assetIds == null || assetIds.isEmpty()) {
            return;
        }
        EntityGroup edgeAssetGroup;
        try {
            edgeAssetGroup = entityGroupService.findOrCreateEdgeAllGroupAsync(ctx.getTenantId(), entity, entity.getName(), EntityType.TENANT, EntityType.ASSET).get();
            ctx.register(edgeAssetGroup.getId());
            ctx.putIdToMap(edgeAssetGroup.getOwnerId(), EntityType.ASSET, edgeAssetGroup.getName(), edgeAssetGroup.getId());
        } catch (Exception e) {
            log.error("[{}] Failed to find or create edge all asset group", ctx.getTenantId(), e);
            throw new ThingsboardException(e, ThingsboardErrorCode.GENERAL);
        }
        for (String strAssetId : assetIds) {
            String newId = ctx.getRealIds().get(strAssetId);
            if (newId != null) {
                AssetId assetId = new AssetId(UUID.fromString(newId));
                entityGroupService.addEntityToEntityGroup(ctx.getTenantId(), edgeAssetGroup.getId(), assetId);
            } else {
                log.error("[{}][{}] Edge: {} references non existing asset.", ctx.getTenantId(), ctx.getSolutionId(), entity.getName());
                throw solutionConfigurationError(EntityType.EDGE, entity.getName(), "non existing asset");
            }
        }
    }

    private void assignDevicesToEdge(SolutionInstallContext ctx, List<String> deviceIds, Edge entity) throws ThingsboardException {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return;
        }
        EntityGroup edgeDeviceGroup;
        try {
            edgeDeviceGroup = entityGroupService.findOrCreateEdgeAllGroupAsync(ctx.getTenantId(), entity, entity.getName(), EntityType.TENANT, EntityType.DEVICE).get();
            ctx.register(edgeDeviceGroup.getId());
            ctx.putIdToMap(edgeDeviceGroup.getOwnerId(), EntityType.DEVICE, edgeDeviceGroup.getName(), edgeDeviceGroup.getId());
        } catch (Exception e) {
            log.error("[{}] Failed to find or create edge all device group", ctx.getTenantId(), e);
            throw new ThingsboardException(e, ThingsboardErrorCode.GENERAL);
        }
        for (String strDeviceId : deviceIds) {
            String newId = ctx.getRealIds().get(strDeviceId);
            if (newId != null) {
                DeviceId deviceId = new DeviceId(UUID.fromString(newId));
                entityGroupService.addEntityToEntityGroup(ctx.getTenantId(), edgeDeviceGroup.getId(), deviceId);
            } else {
                log.error("[{}][{}] Edge: {} references non existing device.", ctx.getTenantId(), ctx.getSolutionId(), entity.getName());
                throw solutionConfigurationError(EntityType.EDGE, entity.getName(), "non existing device");
            }
        }
    }

    private void provisionAlarmRules(SolutionInstallContext ctx) {
        List<CalculatedField> cfs = loadListOfEntitiesIfFileExists(ctx.getTempDir(), "alarm_rules.json", new TypeReference<>() {});
        cfs.addAll(loadListOfEntitiesFromDirectory(ctx.getTempDir(), "alarm_rules", CalculatedField.class));
        cfs.forEach(cf -> ctx.register(createCalculatedField(cf, ctx)));
    }

    protected void provisionCalculatedFields(SolutionInstallContext ctx) {
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
            Integer order = entry.getKey();
            List<CalculatedFieldDefinition> cfDefs = entry.getValue();

            log.debug("Starting reprocessing calculated fields for order: {}", order);

            List<CompletableFuture<Void>> futures = new ArrayList<>();

            log.debug("Start reprocessing calculated fields for order {}", order);
            for (CalculatedFieldDefinition cfDef : cfDefs) {
                CalculatedField calculatedField = createCalculatedField(cfDef, ctx);
                ctx.register(calculatedField);
                Iterable<EntityInfo> targetEntities = resolveTargetEntities(calculatedField);
                targetEntities.forEach(entityInfo ->
                        futures.add(CompletableFuture.runAsync(() -> {
                            log.debug("Reprocessing calculated field: {}", calculatedField.getName());
                            reprocessCf(ctx, entityInfo, calculatedField);
                        }, cfsReprocessingExecutor)));
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            log.debug("Finished reprocessing calculated fields for order: {}", order);
        }
    }

    private Iterable<EntityInfo> resolveTargetEntities(CalculatedField cf) {
        EntityId cfEntityId = cf.getEntityId();
        TenantId tenantId = cf.getTenantId();
        return switch (cfEntityId.getEntityType()) {
            case DEVICE -> List.of(deviceService.findDeviceEntityInfoById(tenantId, new DeviceId(cfEntityId.getId())));
            case ASSET -> List.of(assetService.findAssetEntityInfoById(tenantId, new AssetId(cfEntityId.getId())));
            case DEVICE_PROFILE ->
                    new PageDataIterable<>(pageLink -> deviceService.findDeviceEntityInfosByTenantIdAndDeviceProfileId(tenantId, new DeviceProfileId(cfEntityId.getId()), pageLink), 512);
            case ASSET_PROFILE -> new PageDataIterable<>(pageLink -> assetService.findAssetEntityInfosByTenantIdAndAssetProfileId(tenantId, new AssetProfileId(cfEntityId.getId()), pageLink), 512);
            default -> throw new IllegalArgumentException("Unsupported CF entity type " + cfEntityId.getEntityType());
        };
    }

    private void reprocessCf(SolutionInstallContext ctx, EntityInfo entityInfo, CalculatedField cf) {
        try {
            // NOTE: We use solutionOldestTelemetryTs as a reprocessing startTs for all calculated fields.
            // This assumes a telemetry emulation window is effectively uniform across all entities in the solution.
            // If different emulators start generating telemetry with different history windows (e.g., 7d vs 1d),
            // then CF reprocessing should use per-profile/per-emulator oldestTs instead of the global minimum.
            long reprocessingStartTs = ctx.getOldestTelemetryTs();
            CfReprocessingTask task = createTask(ctx.getTenantId(), entityInfo, cf, reprocessingStartTs, System.currentTimeMillis());
            calculatedFieldReprocessingService.reprocess(task);
        } catch (Exception e) {
            log.error("Failed to reprocess calculated field {}", cf.getName(), e);
        }
    }

    private CfReprocessingTask createTask(TenantId tenantId, EntityInfo entityInfo, CalculatedField calculatedField, long startTs, long endTs) {
        return CfReprocessingTask.builder()
                .tenantId(tenantId)
                .retries(0) // only 1 attempt
                .calculatedField(calculatedField)
                .entityInfo(entityInfo)
                .startTs(startTs)
                .endTs(endTs)
                .build();
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
                log.error("[{}][{}] Calculated field: {} references non existing entity.", ctx.getTenantId(), ctx.getSolutionId(), cf.getName());
                throw solutionConfigurationError(EntityType.CALCULATED_FIELD, cf.getName(), "non existing entity");
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
                            throw solutionConfigurationError(EntityType.CALCULATED_FIELD, cf.getName(), "non existing entity");
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

    private EntityGroup getCustomerGroupInfo(SolutionInstallContext ctx, EntityId entityId, String ugName) throws ExecutionException, InterruptedException {
        return getGroupInfo(ctx, entityId, EntityType.CUSTOMER, ugName);
    }

    private EntityGroup getUserGroupInfo(SolutionInstallContext ctx, EntityId entityId, String ugName) throws ExecutionException, InterruptedException {
        return getGroupInfo(ctx, entityId, EntityType.USER, ugName);
    }

    private EntityGroup getGroupInfo(SolutionInstallContext ctx, EntityId entityId, EntityType entityType, String ugName) throws ExecutionException, InterruptedException {
        Optional<EntityGroup> ugEntityOpt = entityGroupService.findEntityGroupByTypeAndName(ctx.getTenantId(), entityId, entityType, ugName);
        EntityGroup ugEntity;
        if (ugEntityOpt.isPresent()) {
            ugEntity = ugEntityOpt.get();
        } else {
            EntityGroup entityGroup = new EntityGroup();
            entityGroup.setName(ugName);
            entityGroup.setType(entityType);
            ugEntity = entityGroupService.saveEntityGroup(ctx.getTenantId(), entityId, entityGroup);
            ctx.register(ugEntity.getId());
        }
        return ugEntity;
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
        attributes.properties().forEach(entry -> {
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
                default -> throw new IllegalArgumentException("Unsupported time unit: " + unit);
            };
        }
        return String.valueOf(now.toInstant().toEpochMilli());
    }

    protected EntityGroup createEntityGroup(SolutionInstallContext ctx, EntityId ownerId, String name, EntityType type) {
        EntityGroup eg = new EntityGroup();
        eg.setName(name);
        eg.setType(type);
        eg.setOwnerId(ownerId);
        eg = entityGroupService.saveEntityGroup(ctx.getTenantId(), ownerId, eg);
        ctx.register(eg.getId());
        ctx.putIdToMap(eg.getOwnerId(), type, name, eg.getId());
        log.info("[{}] Created entityGroup {}", ownerId, eg);
        return eg;
    }

    private EntityGroupId addEntityToGroup(SolutionInstallContext ctx, CustomerEntityDefinition entityDef, EntityId entityId) throws ThingsboardException {
        CustomerId customerId = ctx.getIdFromMap(EntityType.CUSTOMER, entityDef.getCustomer());
        if (!StringUtils.isEmpty(entityDef.getGroup())) {
            EntityId ownerId = customerId == null ? ctx.getTenantId() : customerId;
            EntityGroupId egId = ctx.getGroupIdFromMap(ownerId, entityId.getEntityType(), entityDef.getGroup());
            if (egId == null) {
                if (EntityType.TENANT.equals(ownerId.getEntityType())) {
                    log.info("Creating tenant {} group: {}", entityId.getEntityType(), entityDef.getGroup());
                    egId = createEntityGroup(ctx, ctx.getTenantId(), entityDef.getGroup(), entityId.getEntityType()).getId();
                } else {
                    log.info("[{}] Creating customer {} group: {}", entityDef.getCustomer(), entityId.getEntityType(), entityDef.getGroup());
                    egId = createEntityGroup(ctx, customerId, entityDef.getGroup(), entityId.getEntityType()).getId();
                }
            }
            entityGroupService.addEntitiesToEntityGroup(ctx.getTenantId(), egId, Collections.singletonList(entityId));

            if (entityDef.isMakePublic()) {
                EntityGroup eg = entityGroupService.findEntityGroupById(ctx.getTenantId(), egId);
                TenantSolutionTemplateInstructions solutionInstructions = ctx.getSolutionInstructions();
                if (!eg.isPublic()) {
                    EntityId publicId = tbEntityGroupService.makePublic(ctx.getTenantId(), eg, ctx.getUser());
                    solutionInstructions.setPublicId(new CustomerId(publicId.getId()));
                } else {
                    if (solutionInstructions.getPublicId() == null) {
                        solutionInstructions.setPublicId(new CustomerId(
                                customerService.findOrCreatePublicUserGroup(ctx.getTenantId(), ctx.getUser().getOwnerId()).getOwnerId().getId()));
                    }
                }
            }

            return egId;
        } else {
            if (entityDef.isMakePublic()) {
                throw new IllegalArgumentException("Entity is assigned to group 'All' only. Can't make entity public!");
            } else {
                return null;
            }
        }
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
                return Objects.requireNonNullElseGet(JacksonUtil.readValue(filePath.toFile(), typeReference), ArrayList::new);
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
        if (dir == null || !Files.exists(dir)) {
            return;
        }
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
            log.warn("Failed to delete temp directory {}", dir, e);
        }
    }

    private void deleteEntity(TenantId tenantId, EntityId entityId, User user) {
        // Alarms, alarm types and alarm comments of the entity are cleaned up asynchronously by the Housekeeper,
        // which reacts to the DeleteEntityEvent published when the entity is removed in the switch below.
        switch (entityId.getEntityType()) {
            case CALCULATED_FIELD:
                CalculatedField cf = calculatedFieldService.findById(tenantId, new CalculatedFieldId(entityId.getId()));
                if (cf != null) {
                    tbCalculatedFieldService.delete(cf, user);
                }
                break;
            case RULE_CHAIN:
                var ruleChainId = new RuleChainId(entityId.getId());
                ruleChainService.deleteRuleChainById(tenantId, ruleChainId);
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
                    tbAssetService.delete(new AssetId(entityId.getId()), user);
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
            case ROLE:
                roleService.deleteRole(tenantId, new RoleId(entityId.getId()));
                break;
            case ENTITY_GROUP:
                entityGroupService.deleteEntityGroup(tenantId, new EntityGroupId(entityId.getId()));
                break;
            case SCHEDULER_EVENT:
                schedulerEventService.deleteSchedulerEvent(tenantId, new SchedulerEventId(entityId.getId()));
                break;
            default:
                log.warn("[{}] Unsupported entity type for deletion: {}", tenantId, entityId.getEntityType());
        }
    }

    private JsonNode replaceIds(SolutionInstallContext ctx, JsonNode dashboardJson) {
        String jsonStr = JacksonUtil.toString(dashboardJson);
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
