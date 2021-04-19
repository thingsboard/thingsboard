/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.dao.rule;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainData;
import org.thingsboard.server.common.data.rule.RuleChainImportResult;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;

import java.util.List;

/**
 * Created by igor on 3/12/18.
 */
public interface RuleChainService {

    RuleChain saveRuleChain(RuleChain ruleChain);

    boolean setRootRuleChain(TenantId tenantId, RuleChainId ruleChainId);

    boolean saveRuleChainMetaData(TenantId tenantId, RuleChainMetaData ruleChainMetaData);

    RuleChainMetaData loadRuleChainMetaData(TenantId tenantId, RuleChainId ruleChainId);

    RuleChain findRuleChainById(TenantId tenantId, RuleChainId ruleChainId);

    RuleNode findRuleNodeById(TenantId tenantId, RuleNodeId ruleNodeId);

    ListenableFuture<RuleChain> findRuleChainByIdAsync(TenantId tenantId, RuleChainId ruleChainId);

    ListenableFuture<RuleNode> findRuleNodeByIdAsync(TenantId tenantId, RuleNodeId ruleNodeId);

    RuleChain getRootTenantRuleChain(TenantId tenantId);

    List<RuleNode> getRuleChainNodes(TenantId tenantId, RuleChainId ruleChainId);

    List<RuleNode> getReferencingRuleChainNodes(TenantId tenantId, RuleChainId ruleChainId);

    List<EntityRelation> getRuleNodeRelations(TenantId tenantId, RuleNodeId ruleNodeId);

    PageData<RuleChain> findTenantRuleChainsByType(TenantId tenantId, RuleChainType type, PageLink pageLink);

    void deleteRuleChainById(TenantId tenantId, RuleChainId ruleChainId);

    void deleteRuleChainsByTenantId(TenantId tenantId);

    RuleChainData exportTenantRuleChains(TenantId tenantId, PageLink pageLink) throws ThingsboardException;

    List<RuleChainImportResult> importTenantRuleChains(TenantId tenantId, RuleChainData ruleChainData, RuleChainType type, boolean overwrite);

    RuleChain assignRuleChainToEdge(TenantId tenantId, RuleChainId ruleChainId, EdgeId edgeId);

    RuleChain unassignRuleChainFromEdge(TenantId tenantId, RuleChainId ruleChainId, EdgeId edgeId, boolean remove);

    PageData<RuleChain> findRuleChainsByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, PageLink pageLink);

    RuleChain getEdgeTemplateRootRuleChain(TenantId tenantId);

    boolean setEdgeTemplateRootRuleChain(TenantId tenantId, RuleChainId ruleChainId);

    boolean setAutoAssignToEdgeRuleChain(TenantId tenantId, RuleChainId ruleChainId);

    boolean unsetAutoAssignToEdgeRuleChain(TenantId tenantId, RuleChainId ruleChainId);

    ListenableFuture<List<RuleChain>> findAutoAssignToEdgeRuleChainsByTenantId(TenantId tenantId);

}
