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
package org.thingsboard.server.common.data.notification.info;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;

import java.util.Map;

import static org.thingsboard.server.common.data.util.CollectionsUtil.mapOf;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RuleEngineComponentLifecycleEventNotificationInfo implements RuleOriginatedNotificationInfo {

    private RuleChainId ruleChainId;
    private String ruleChainName;
    private EntityId componentId;
    private String componentName;
    private String action;
    private ComponentLifecycleEvent eventType;
    private String error;

    @Override
    public Map<String, String> getTemplateData() {
        return mapOf(
                "ruleChainId", ruleChainId.toString(),
                "ruleChainName", ruleChainName,
                "componentId", componentId.toString(),
                "componentType", componentId.getEntityType().getNormalName(),
                "componentName", componentName,
                "action", action,
                "eventType", eventType.name().toLowerCase(),
                "error", error
        );
    }

    @Override
    public EntityId getStateEntityId() {
        return ruleChainId;
    }

}
