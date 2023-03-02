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
package org.thingsboard.server.service.alarm.rule.store;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.service.alarm.rule.state.PersistedEntityState;

import java.util.Collections;
import java.util.List;

@Service
@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "caffeine", matchIfMissing = true)
public class DummyAlarmRuleEntityStateStore implements AlarmRuleEntityStateStore {

    @Override
    public void put(PersistedEntityState entityState) {
    }

    @Override
    public void remove(TenantId tenantId, EntityId entityId) {
    }

    @Override
    public List<PersistedEntityState> getAll(TopicPartitionInfo tpi) {
        return Collections.emptyList();
    }
}
