/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.sync.exportimport.importing.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleChainUpdateResult;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.sync.exportimport.importing.data.EntityImportSettings;
import org.thingsboard.server.service.sync.exportimport.exporting.data.RuleChainExportData;
import org.thingsboard.server.utils.RegexUtils;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class RuleChainImportService extends BaseEntityImportService<RuleChainId, RuleChain, RuleChainExportData> {

    private final RuleChainService ruleChainService;

    @Override
    protected void setOwner(TenantId tenantId, RuleChain ruleChain, IdProvider idProvider) {
        ruleChain.setTenantId(tenantId);
    }

    @Override
    protected RuleChain findExistingEntity(TenantId tenantId, RuleChain ruleChain, EntityImportSettings importSettings) {
        RuleChain existingRuleChain = super.findExistingEntity(tenantId, ruleChain, importSettings);
        if (existingRuleChain == null && importSettings.isFindExistingByName()) {
            existingRuleChain = ruleChainService.findTenantRuleChainsByTypeAndName(tenantId, ruleChain.getType(), ruleChain.getName()).stream().findFirst().orElse(null);
        }
        return existingRuleChain;
    }

    @Override
    protected RuleChain prepareAndSave(TenantId tenantId, RuleChain ruleChain, RuleChainExportData exportData, IdProvider idProvider) {
        RuleChainMetaData metaData = exportData.getMetaData();
        Optional.ofNullable(metaData.getNodes()).orElse(Collections.emptyList())
                .forEach(ruleNode -> {
                    ruleNode.setId(null);
                    ruleNode.setRuleChainId(null);

                    JsonNode ruleNodeConfig = ruleNode.getConfiguration();
                    String newRuleNodeConfigJson = RegexUtils.replace(ruleNodeConfig.toString(), RegexUtils.UUID_PATTERN, uuid -> {
                        return idProvider.getInternalIdByUuid(UUID.fromString(uuid))
                                .map(entityId -> entityId.getId().toString())
                                .orElse(uuid);
                    });
                    ruleNodeConfig = JacksonUtil.toJsonNode(newRuleNodeConfigJson);
                    ruleNode.setConfiguration(ruleNodeConfig);
                });
        Optional.ofNullable(metaData.getRuleChainConnections()).orElse(Collections.emptyList())
                .forEach(ruleChainConnectionInfo -> {
                    ruleChainConnectionInfo.setTargetRuleChainId(idProvider.getInternalId(ruleChainConnectionInfo.getTargetRuleChainId()));
                });
        ruleChain.setFirstRuleNodeId(null);

        ruleChain = ruleChainService.saveRuleChain(ruleChain);
        exportData.getMetaData().setRuleChainId(ruleChain.getId());
        RuleChainUpdateResult updateResult = ruleChainService.saveRuleChainMetaData(tenantId, exportData.getMetaData());
        // FIXME [viacheslav]: send events for nodes
        return ruleChainService.findRuleChainById(tenantId, ruleChain.getId());
    }

    @Override
    protected void onEntitySaved(SecurityUser user, RuleChain savedRuleChain, RuleChain oldRuleChain) throws ThingsboardException {
        super.onEntitySaved(user, savedRuleChain, oldRuleChain);
        if (savedRuleChain.getType() == RuleChainType.CORE) {
            clusterService.broadcastEntityStateChangeEvent(user.getTenantId(), savedRuleChain.getId(),
                    oldRuleChain == null ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
        } else if (savedRuleChain.getType() == RuleChainType.EDGE && oldRuleChain != null) {
            entityActionService.sendEntityNotificationMsgToEdgeService(user.getTenantId(), savedRuleChain.getId(), EdgeEventActionType.UPDATED);
        }
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.RULE_CHAIN;
    }

}
