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
package org.thingsboard.server.service.sync.ie.exporting.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.sync.ie.EntityExportSettings;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.common.data.sync.ie.RuleChainExportData;

import java.util.Set;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class RuleChainExportService extends BaseEntityExportService<RuleChainId, RuleChain, RuleChainExportData> {

    private final RuleChainService ruleChainService;

    @Override
    protected void setRelatedEntities(TenantId tenantId, RuleChain ruleChain, RuleChainExportData exportData, EntityExportSettings exportSettings) {
        exportData.setMetaData(ruleChainService.loadRuleChainMetaData(tenantId, ruleChain.getId()));
    }

    @Override
    protected RuleChainExportData newExportData() {
        return new RuleChainExportData();
    }

    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.RULE_CHAIN);
    }

}
