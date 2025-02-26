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
package org.thingsboard.server.service.edge.rpc.processor.rule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.function.Function;

@Slf4j
public class BaseRuleChainProcessor extends BaseEdgeProcessor {

    @Autowired
    private DataValidator<RuleChain> ruleChainValidator;

    protected Pair<Boolean, Boolean> saveOrUpdateRuleChain(TenantId tenantId, RuleChainId ruleChainId, RuleChainUpdateMsg ruleChainUpdateMsg, RuleChainType ruleChainType) {
        boolean created = false;
        RuleChain ruleChain = edgeCtx.getRuleChainService().findRuleChainById(tenantId, ruleChainId);
        if (ruleChain == null) {
            created = true;
        }

        ruleChain = JacksonUtil.fromString(ruleChainUpdateMsg.getEntity(), RuleChain.class, true);
        if (ruleChain == null) {
            throw new RuntimeException("[{" + tenantId + "}] ruleChainUpdateMsg {" + ruleChainUpdateMsg + "} cannot be converted to rule chain");
        }
        boolean isRoot = ruleChain.isRoot();
        ruleChain.setRoot(false);
        ruleChain.setType(ruleChainType);

        ruleChainValidator.validate(ruleChain, RuleChain::getTenantId);
        if (created) {
            ruleChain.setId(ruleChainId);
        }

        edgeCtx.getRuleChainService().saveRuleChain(ruleChain);
        return Pair.of(created, isRoot);
    }

    protected void saveOrUpdateRuleChainMetadata(TenantId tenantId, RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg) {
        RuleChainMetaData ruleChainMetadata = JacksonUtil.fromString(ruleChainMetadataUpdateMsg.getEntity(), RuleChainMetaData.class, true);
        if (ruleChainMetadata == null) {
            throw new RuntimeException("[{" + tenantId + "}] ruleChainMetadataUpdateMsg {" + ruleChainMetadataUpdateMsg + "} cannot be converted to rule chain metadata");
        }
        if (!ruleChainMetadata.getNodes().isEmpty()) {
            edgeCtx.getRuleChainService().saveRuleChainMetaData(tenantId, ruleChainMetadata, Function.identity(), true, false);
        }
    }
}
