// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.rule;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Created by igor on 3/13/18.
 */
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RuleChainUpdateResult {

    private final boolean success;
    private final List<RuleNodeUpdateResult> updatedRuleNodes;

    public static RuleChainUpdateResult failed(){
        return new RuleChainUpdateResult(false, null);
    }

    public static RuleChainUpdateResult successful(List<RuleNodeUpdateResult> updatedRuleNodes){
        return new RuleChainUpdateResult(true, updatedRuleNodes);
    }

}
