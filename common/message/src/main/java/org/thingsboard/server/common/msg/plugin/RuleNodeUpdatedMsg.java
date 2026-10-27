// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.plugin;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.MsgType;

/**
 * @author Andrew Shvayka
 * This class used only to tell local rule-node actor like 'existing.getSelfActor().tellWithHighPriority(new RuleNodeUpdatedMs( ...'
 * Never serialized to/from proto, otherwise you need to change proto mappers in ProtoUtils class
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class RuleNodeUpdatedMsg extends ComponentLifecycleMsg {

    public RuleNodeUpdatedMsg(TenantId tenantId, EntityId entityId) {
        super(tenantId, entityId, ComponentLifecycleEvent.UPDATED);
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.RULE_NODE_UPDATED_MSG;
    }
}