// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.dao.model.ModelConstants;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.RULE_CHAIN_TABLE_NAME)
public class RuleChainEntity extends AbstractRuleChainEntity<RuleChain> {

    public RuleChainEntity() {
        super();
    }

    public RuleChainEntity(RuleChain ruleChain) {
        super(ruleChain);
    }

    @Override
    public RuleChain toData() {
        return super.toRuleChain();
    }

}
