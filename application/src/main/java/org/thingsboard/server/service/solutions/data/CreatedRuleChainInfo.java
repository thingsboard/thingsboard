// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.solutions.data;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.rule.RuleChainType;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class CreatedRuleChainInfo extends CreatedEntityInfo {

    private RuleChainType ruleChainType;

    public CreatedRuleChainInfo(String name, RuleChainType ruleChainType, String owner) {
        super(name, EntityType.RULE_CHAIN, owner);
        this.ruleChainType = ruleChainType;
    }

    @Override
    public String getEntityPageLink(UUID id) {
        return ruleChainType == RuleChainType.EDGE ? "/edgeManagement/templates/ruleChains/" + id : "/ruleChains/" + id;
    }
}
