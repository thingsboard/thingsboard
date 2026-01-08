/**
 * Copyright © 2016-2026 The Thingsboard Authors
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