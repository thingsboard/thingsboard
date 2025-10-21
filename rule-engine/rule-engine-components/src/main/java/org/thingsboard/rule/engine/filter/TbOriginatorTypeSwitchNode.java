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
package org.thingsboard.rule.engine.filter;

import org.thingsboard.rule.engine.api.EmptyNodeConfiguration;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;

@RuleNode(
        type = ComponentType.FILTER,
        name = "entity type switch",
        configClazz = EmptyNodeConfiguration.class,
        relationTypes = {}, // should always be empty. We add the relation types for this node in AnnotationComponentDiscoveryService.
        nodeDescription = "Route incoming messages by Message Originator Type",
        nodeDetails = "Routes messages to chain according to the entity type ('Device', 'Asset', etc.).<br><br>" +
                "Output connections: <i>Message originator type</i> or <code>Failure</code>",
        configDirective = "tbNodeEmptyConfig",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/filter/entity-type-switch/"
)
public class TbOriginatorTypeSwitchNode extends TbAbstractTypeSwitchNode {

    @Override
    protected String getRelationType(TbContext ctx, EntityId originator) {
        return originator.getEntityType().getNormalName();
    }

}
