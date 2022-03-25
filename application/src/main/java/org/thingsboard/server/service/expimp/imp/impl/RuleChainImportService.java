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
package org.thingsboard.server.service.expimp.imp.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.export.impl.RuleChainExportData;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.expimp.imp.EntityImportSettings;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class RuleChainImportService extends AbstractEntityImportService<RuleChainId, RuleChain, RuleChainExportData> {

    private final RuleChainService ruleChainService;


    @Transactional
    @Override
    protected RuleChain prepareAndSaveEntity(TenantId tenantId, RuleChain ruleChain, RuleChain existingRuleChain, RuleChainExportData exportData, EntityImportSettings importSettings) {
        ruleChain.setFirstRuleNodeId(null); // will be set during metadata persisting
        if (existingRuleChain != null) {
            ruleChainService.deleteRuleNodes(tenantId, existingRuleChain.getId());
        }

        ruleChain = ruleChainService.saveRuleChain(ruleChain);

        RuleChainMetaData metaData = exportData.getMetaData();
        metaData.setRuleChainId(ruleChain.getId());
        metaData.getNodes().forEach(ruleNode -> {
            ruleNode.setId(null);
            ruleNode.setRuleChainId(null);
        });
        metaData.getRuleChainConnections().forEach(ruleChainConnectionInfo -> {
//            ruleChainConnectionInfo.setTargetRuleChainId();
            // TODO [viacheslav]: check if this thing is needed for "Other Rule Chain Node"
            // TODO [viacheslav]: and check import of tenant rule chains
        });
        ruleChainService.saveRuleChainMetaData(tenantId, metaData);

        return ruleChain;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.RULE_CHAIN;
    }

}
