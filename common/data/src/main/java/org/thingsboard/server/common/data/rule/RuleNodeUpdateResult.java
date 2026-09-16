// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.rule;

import lombok.Data;

/**
 * Created by igor on 3/13/18.
 */
@Data
public class RuleNodeUpdateResult {

    private final RuleNode oldRuleNode;
    private final RuleNode newRuleNode;

}
