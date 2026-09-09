// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.rule;

import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleNodeState;

public interface RuleNodeStateService {

    PageData<RuleNodeState> findByRuleNodeId(TenantId tenantId, RuleNodeId ruleNodeId, PageLink pageLink);

    RuleNodeState findByRuleNodeIdAndEntityId(TenantId tenantId, RuleNodeId ruleNodeId, EntityId entityId);

    RuleNodeState save(TenantId tenantId, RuleNodeState ruleNodeState);

    void removeByRuleNodeId(TenantId tenantId, RuleNodeId selfId);

    void removeByRuleNodeIdAndEntityId(TenantId tenantId, RuleNodeId selfId, EntityId entityId);
}
