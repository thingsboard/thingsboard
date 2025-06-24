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
package org.thingsboard.server.service.install.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.query.DynamicValue;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.service.component.ComponentDiscoveryService;
import org.thingsboard.server.service.component.RuleNodeClassInfo;
import org.thingsboard.server.service.install.DbUpgradeExecutorService;
import org.thingsboard.server.utils.TbNodeUpgradeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.thingsboard.server.dao.rule.BaseRuleChainService.TB_RULE_CHAIN_INPUT_NODE;

@Service
@Profile("install")
@Slf4j
public class DefaultDataUpdateService implements DataUpdateService {

    private static final int MAX_PENDING_SAVE_RULE_NODE_FUTURES = 256;
    private static final int DEFAULT_PAGE_SIZE = 1024;

    @Autowired
    private RuleChainService ruleChainService;

    @Autowired
    private RelationService relationService;

    @Autowired
    private ComponentDiscoveryService componentDiscoveryService;

    @Autowired
    private DbUpgradeExecutorService executorService;

    @Autowired
    private TenantProfileService tenantProfileService;

    @Override
    public void updateData() throws Exception {
        log.info("Updating data ...");
        //TODO: should be cleaned after each release
        updateInputNodes();
        deduplicateRateLimitsPerSecondsConfigurations();
        log.info("Data updated.");
    }

    private void deduplicateRateLimitsPerSecondsConfigurations() {
        log.info("Starting update of tenant profiles...");

        int totalProfiles = 0;
        int updatedTenantProfiles = 0;
        int skippedProfiles = 0;
        int failedProfiles = 0;

        var tenantProfiles = new PageDataIterable<>(
                pageLink -> tenantProfileService.findTenantProfiles(TenantId.SYS_TENANT_ID, pageLink), 1024);

        for (TenantProfile tenantProfile : tenantProfiles) {
            totalProfiles++;
            String profileName = tenantProfile.getName();
            UUID profileId = tenantProfile.getId().getId();
            try {
                Optional<DefaultTenantProfileConfiguration> profileConfiguration = tenantProfile.getProfileConfiguration();
                if (profileConfiguration.isEmpty()) {
                    log.debug("[{}][{}] Skipping tenant profile with non-default configuration.", profileId, profileName);
                    skippedProfiles++;
                    continue;
                }

                DefaultTenantProfileConfiguration defaultTenantProfileConfiguration = profileConfiguration.get();
                defaultTenantProfileConfiguration.deduplicateRateLimitsConfigs();
                tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, tenantProfile);
                updatedTenantProfiles++;
                log.debug("[{}][{}] Successfully updated tenant profile.", profileId, profileName);
            } catch (Exception e) {
                log.error("[{}][{}] Failed to updated tenant profile: ", profileId, profileName, e);
                failedProfiles++;
            }
        }

        log.info("Tenant profiles update completed. Total: {}, Updated: {}, Skipped: {}, Failed: {}",
                totalProfiles, updatedTenantProfiles, skippedProfiles, failedProfiles);
    }


    private void updateInputNodes() {
        log.info("Creating relations for input nodes...");
        int n = 0;
        var inputNodes = new PageDataIterable<>(pageLink -> ruleChainService.findAllRuleNodesByType(TB_RULE_CHAIN_INPUT_NODE, pageLink), 1024);
        for (RuleNode inputNode : inputNodes) {
            try {
                RuleChainId targetRuleChainId = Optional.ofNullable(inputNode.getConfiguration().get("ruleChainId"))
                        .filter(JsonNode::isTextual).map(JsonNode::asText).map(id -> new RuleChainId(UUID.fromString(id)))
                        .orElse(null);
                if (targetRuleChainId == null) {
                    continue;
                }

                EntityRelation relation = new EntityRelation();
                relation.setFrom(inputNode.getRuleChainId());
                relation.setTo(targetRuleChainId);
                relation.setType(EntityRelation.USES_TYPE);
                relation.setTypeGroup(RelationTypeGroup.COMMON);
                relationService.saveRelation(TenantId.SYS_TENANT_ID, relation);
                n++;
            } catch (Exception e) {
                log.error("Failed to save relation for input node: {}", inputNode, e);
            }
        }
        log.info("Created {} relations for input nodes", n);
    }

    @Override
    public void upgradeRuleNodes() {
        int totalRuleNodesUpgraded = 0;
        log.info("Starting rule nodes upgrade ...");
        var nodeClassToVersionMap = componentDiscoveryService.getVersionedNodes();
        log.debug("Found {} versioned nodes to check for upgrade!", nodeClassToVersionMap.size());
        for (var ruleNodeClassInfo : nodeClassToVersionMap) {
            var ruleNodeTypeForLogs = ruleNodeClassInfo.getSimpleName();
            var toVersion = ruleNodeClassInfo.getCurrentVersion();
            try {
                log.debug("Going to check for nodes with type: {} to upgrade to version: {}.", ruleNodeTypeForLogs, toVersion);
                var ruleNodesIdsToUpgrade = getRuleNodesIdsWithTypeAndVersionLessThan(ruleNodeClassInfo.getClassName(), toVersion);
                if (ruleNodesIdsToUpgrade.isEmpty()) {
                    log.debug("There are no active nodes with type {}, or all nodes with this type already set to latest version!", ruleNodeTypeForLogs);
                    continue;
                }
                var ruleNodeIdsPartitions = Lists.partition(ruleNodesIdsToUpgrade, MAX_PENDING_SAVE_RULE_NODE_FUTURES);
                for (var ruleNodePack : ruleNodeIdsPartitions) {
                    totalRuleNodesUpgraded += processRuleNodePack(ruleNodePack, ruleNodeClassInfo);
                    log.info("{} upgraded rule nodes so far ...", totalRuleNodesUpgraded);
                }
            } catch (Exception e) {
                log.error("Unexpected error during {} rule nodes upgrade: ", ruleNodeTypeForLogs, e);
            }
        }
        log.info("Finished rule nodes upgrade. Upgraded rule nodes count: {}", totalRuleNodesUpgraded);
    }

    private int processRuleNodePack(List<RuleNodeId> ruleNodeIdsBatch, RuleNodeClassInfo ruleNodeClassInfo) {
        var saveFutures = new ArrayList<ListenableFuture<?>>(MAX_PENDING_SAVE_RULE_NODE_FUTURES);
        String ruleNodeType = ruleNodeClassInfo.getSimpleName();
        int toVersion = ruleNodeClassInfo.getCurrentVersion();
        var ruleNodesPack = ruleChainService.findAllRuleNodesByIds(ruleNodeIdsBatch);
        for (var ruleNode : ruleNodesPack) {
            if (ruleNode == null) {
                continue;
            }
            var ruleNodeId = ruleNode.getId();
            int fromVersion = ruleNode.getConfigurationVersion();
            log.debug("Going to upgrade rule node with id: {} type: {} fromVersion: {} toVersion: {}",
                    ruleNodeId, ruleNodeType, fromVersion, toVersion);
            try {
                TbNodeUpgradeUtils.upgradeConfigurationAndVersion(ruleNode, ruleNodeClassInfo);
                saveFutures.add(executorService.submit(() -> {
                    ruleChainService.saveRuleNode(TenantId.SYS_TENANT_ID, ruleNode);
                    log.debug("Successfully upgrade rule node with id: {} type: {} fromVersion: {} toVersion: {}",
                            ruleNodeId, ruleNodeType, fromVersion, toVersion);
                }));
            } catch (Exception e) {
                log.warn("Failed to upgrade rule node with id: {} type: {} fromVersion: {} toVersion: {} due to: ",
                        ruleNodeId, ruleNodeType, fromVersion, toVersion, e);
            }
        }
        try {
            return Futures.allAsList(saveFutures).get().size();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Failed to process save rule nodes requests due to: ", e);
        }
    }

    private List<RuleNodeId> getRuleNodesIdsWithTypeAndVersionLessThan(String type, int toVersion) {
        var ruleNodeIds = new ArrayList<RuleNodeId>();
        new PageDataIterable<>(pageLink ->
                ruleChainService.findAllRuleNodeIdsByTypeAndVersionLessThan(type, toVersion, pageLink), DEFAULT_PAGE_SIZE
        ).forEach(ruleNodeIds::add);
        return ruleNodeIds;
    }

    boolean convertDeviceProfileForVersion330(JsonNode profileData) {
        boolean isUpdated = false;
        if (profileData.has("alarms") && !profileData.get("alarms").isNull()) {
            JsonNode alarms = profileData.get("alarms");
            for (JsonNode alarm : alarms) {
                if (alarm.has("createRules")) {
                    JsonNode createRules = alarm.get("createRules");
                    for (AlarmSeverity severity : AlarmSeverity.values()) {
                        if (createRules.has(severity.name())) {
                            JsonNode spec = createRules.get(severity.name()).get("condition").get("spec");
                            if (convertDeviceProfileAlarmRulesForVersion330(spec)) {
                                isUpdated = true;
                            }
                        }
                    }
                }
                if (alarm.has("clearRule") && !alarm.get("clearRule").isNull()) {
                    JsonNode spec = alarm.get("clearRule").get("condition").get("spec");
                    if (convertDeviceProfileAlarmRulesForVersion330(spec)) {
                        isUpdated = true;
                    }
                }
            }
        }
        return isUpdated;
    }

    boolean convertDeviceProfileAlarmRulesForVersion330(JsonNode spec) {
        if (spec != null) {
            if (spec.has("type") && spec.get("type").asText().equals("DURATION")) {
                if (spec.has("value")) {
                    long value = spec.get("value").asLong();
                    var predicate = new FilterPredicateValue<>(
                            value, null, new DynamicValue<>(null, null, false)
                    );
                    ((ObjectNode) spec).remove("value");
                    ((ObjectNode) spec).putPOJO("predicate", predicate);
                    return true;
                }
            } else if (spec.has("type") && spec.get("type").asText().equals("REPEATING")) {
                if (spec.has("count")) {
                    int count = spec.get("count").asInt();
                    var predicate = new FilterPredicateValue<>(
                            count, null, new DynamicValue<>(null, null, false)
                    );
                    ((ObjectNode) spec).remove("count");
                    ((ObjectNode) spec).putPOJO("predicate", predicate);
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean getEnv(String name, boolean defaultValue) {
        String env = System.getenv(name);
        if (env == null) {
            return defaultValue;
        } else {
            return Boolean.parseBoolean(env);
        }
    }

}
