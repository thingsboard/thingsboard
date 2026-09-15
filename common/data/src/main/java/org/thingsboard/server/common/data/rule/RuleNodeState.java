// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.rule;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.RuleNodeStateId;

@Data
@EqualsAndHashCode(callSuper = true)
public class RuleNodeState extends BaseData<RuleNodeStateId> {

    private RuleNodeId ruleNodeId;
    private EntityId entityId;
    private String stateData;

    public RuleNodeState() {
        super();
    }

    public RuleNodeState(RuleNodeStateId id) {
        super(id);
    }

    public RuleNodeState(RuleNodeState event) {
        super(event);
    }
}
