/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.dao.notification;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.UpdateMessage;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.dao.alarm.AlarmApiCallResult;

public interface NotificationRuleProcessingService {

    void process(TenantId tenantId, TbMsg ruleEngineMsg);

    // for handling internal component lifecycle events that are not getting to rule chain
    void process(ComponentLifecycleMsg componentLifecycleMsg);

    void process(TenantId tenantId, AlarmApiCallResult alarmUpdate);

    void process(TenantId tenantId, RuleChainId ruleChainId, String ruleChainName,
                 EntityId componentId, String componentName, ComponentLifecycleEvent eventType, Exception error);

    void process(UpdateMessage platformUpdateMessage);

    void process(TenantId tenantId, EntityType entityType, long limit, long currentCount);

}
