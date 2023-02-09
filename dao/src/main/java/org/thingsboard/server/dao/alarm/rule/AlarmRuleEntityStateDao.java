/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.dao.alarm.rule;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.alarm.rule.AlarmRuleEntityState;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.List;
import java.util.UUID;

public interface AlarmRuleEntityStateDao {

    AlarmRuleEntityState saveAlarmRuleEntityState(TenantId tenantId, AlarmRuleEntityState alarmComment);

    PageData<AlarmRuleEntityState> findAll(PageLink pageLink);

    List<AlarmRuleEntityState> findAllByIds(List<EntityId> entityIds);

    void deleteByEntityId(TenantId tenantId, UUID id);

    List<JsonNode> findRuleNodeStatesByRuleChainIdAndType(DeviceProfileId deviceProfileId, RuleChainId ruleChainId, String type);
}
