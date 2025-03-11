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
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.query.DynamicValue;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.sql.JpaExecutorService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.service.component.ComponentDiscoveryService;
import org.thingsboard.server.service.component.RuleNodeClassInfo;
import org.thingsboard.server.service.install.DbUpgradeExecutorService;
import org.thingsboard.server.service.install.InstallScripts;
import org.thingsboard.server.utils.TbNodeUpgradeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.thingsboard.server.common.data.relation.EntityRelation.USES_TYPE;

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
    private TenantService tenantService;

    @Autowired
    private ComponentDiscoveryService componentDiscoveryService;

    @Autowired
    JpaExecutorService jpaExecutorService;

    @Autowired
    private InstallScripts installScripts;

    @Autowired
    private DbUpgradeExecutorService executorService;

    @Override
    public void updateData() throws Exception {
        log.info("Updating data ...");
        //TODO: should be cleaned after each release
        inputNodesUpdater.updateEntities();
        log.info("Data updated.");
    }

    //TODO: should be removed after release
    private final PaginatedUpdater<String, Tenant> inputNodesUpdater = new PaginatedUpdater<>() {
        @Override
        protected String getName() {
            return "Input nodes updater";
        }

        @Override
        protected PageData<Tenant> findEntities(String type, PageLink pageLink) {
            return tenantService.findTenants(pageLink);
        }

        @Override
        protected void updateEntity(Tenant tenant) {
            TenantId tenantId = tenant.getId();
            try {
                var inputNodes = ruleChainService.findRuleNodesByTenantIdAndType(tenantId, "org.thingsboard.rule.engine.flow.TbRuleChainInputNode");
                var resultFutures = inputNodes.stream().map(ruleNode -> {
                    try {
                        JsonNode id = ruleNode.getConfiguration().get("ruleChainId");
                        if (id != null) {
                            RuleChainId toRuleChainId = new RuleChainId(UUID.fromString(id.asText()));
                            RuleChainId fromRuleChainId = ruleNode.getRuleChainId();
                            var isExistFuture = relationService.checkRelationAsync(null, fromRuleChainId, toRuleChainId, USES_TYPE, RelationTypeGroup.COMMON);
                            Futures.transformAsync(isExistFuture, isExist -> {
                                if (!isExist) {
                                    EntityRelation relation = new EntityRelation();
                                    relation.setFrom(fromRuleChainId);
                                    relation.setTo(toRuleChainId);
                                    relation.setType(EntityRelation.USES_TYPE);
                                    relation.setTypeGroup(RelationTypeGroup.COMMON);
                                    return relationService.saveRelationAsync(tenantId, relation);
                                }
                                return Futures.immediateFuture(null);
                            }, executorService);
                        }
                    } catch (Exception e) {
                        log.error("[{}] Create relation for input node: [{}]", tenantId, ruleNode, e);
                    }
                    return Futures.immediateFuture(null);
                }).toList();

                Futures.allAsList(resultFutures).get();
            } catch (Exception e) {
                log.error("[{}] Unable to update Tenant input nodes", tenantId, e);
            }
        }
    };

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
                saveFutures.add(jpaExecutorService.submit(() -> {
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
