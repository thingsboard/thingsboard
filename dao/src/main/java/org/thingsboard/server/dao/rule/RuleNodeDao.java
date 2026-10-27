// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.rule;

import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.Dao;

import java.util.List;

/**
 * Created by igor on 3/12/18.
 */
public interface RuleNodeDao extends Dao<RuleNode> {

    List<RuleNode> findRuleNodesByTenantIdAndType(TenantId tenantId, String type, String configurationSearch);

    PageData<RuleNode> findAllRuleNodesByType(String type, PageLink pageLink);

    PageData<RuleNode> findAllRuleNodesByTypeAndVersionLessThan(String type, int version, PageLink pageLink);

    PageData<RuleNodeId> findAllRuleNodeIdsByTypeAndVersionLessThan(String type, int version, PageLink pageLink);

    List<RuleNode> findAllRuleNodeByIds(List<RuleNodeId> ruleNodeIds);

    List<RuleNode> findByExternalIds(RuleChainId ruleChainId, List<RuleNodeId> externalIds);

    void deleteByIdIn(List<RuleNodeId> ruleNodeIds);

}
