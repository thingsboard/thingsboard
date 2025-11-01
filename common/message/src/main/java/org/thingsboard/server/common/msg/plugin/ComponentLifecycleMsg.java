/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.aware.TenantAwareMsg;
import org.thingsboard.server.common.msg.cluster.ToAllNodesMsg;

import java.io.Serial;
import java.util.Optional;

/**
 * @author Andrew Shvayka
 */
@Data
public class ComponentLifecycleMsg implements TenantAwareMsg, ToAllNodesMsg {

    @Serial
    private static final long serialVersionUID = -5303421482781273062L;

    private final TenantId tenantId;
    private final EntityId entityId;
    private final ComponentLifecycleEvent event;
    private final String oldName;
    private final String name;
    private final EntityId oldProfileId;
    private final EntityId profileId;
    private final boolean ownerChanged;
    private final JsonNode info;

    public ComponentLifecycleMsg(TenantId tenantId, EntityId entityId, ComponentLifecycleEvent event) {
        this(tenantId, entityId, event, null, null, null, null, false, null);
    }

    @Builder
    private ComponentLifecycleMsg(TenantId tenantId, EntityId entityId, ComponentLifecycleEvent event, String oldName, String name, EntityId oldProfileId, EntityId profileId, boolean ownerChanged, JsonNode info) {
        this.tenantId = tenantId;
        this.entityId = entityId;
        this.event = event;
        this.oldName = oldName;
        this.name = name;
        this.oldProfileId = oldProfileId;
        this.profileId = profileId;
        this.ownerChanged = ownerChanged;
        this.info = info;
    }

    public Optional<RuleChainId> getRuleChainId() {
        return entityId.getEntityType() == EntityType.RULE_CHAIN ? Optional.of((RuleChainId) entityId) : Optional.empty();
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.COMPONENT_LIFE_CYCLE_MSG;
    }

}
