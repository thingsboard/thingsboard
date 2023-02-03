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
package org.thingsboard.rule.engine.api;

import org.thingsboard.server.common.data.id.AlarmRuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;

public interface TbAlarmRuleStateService {

    void process(TbContext tbContext, TbMsg msg) throws Exception;

    void processEntityDeleted(TbMsg msg);

    void createAlarmRule(TenantId tenantId, AlarmRuleId alarmRuleId);

    void updateAlarmRule(TenantId tenantId, AlarmRuleId alarmRuleId);

    void deleteAlarmRule(TenantId tenantId, AlarmRuleId alarmRuleId);

    void deleteTenant(TenantId tenantId);
}
