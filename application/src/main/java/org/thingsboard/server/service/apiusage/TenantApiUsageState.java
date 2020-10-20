/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.service.apiusage;

import lombok.Getter;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TenantApiUsageState {

    private final Map<ApiUsageRecordKey, Long> values = new ConcurrentHashMap<>();
    @Getter
    private final EntityId entityId;
    @Getter
    private volatile long currentMonthTs;

    public TenantApiUsageState(long currentMonthTs, EntityId entityId) {
        this.entityId = entityId;
        this.currentMonthTs = currentMonthTs;
    }

    public void put(ApiUsageRecordKey key, Long value) {
        values.put(key, value);
    }

    public long add(ApiUsageRecordKey key, long value) {
        long result = values.getOrDefault(key, 0L) + value;
        values.put(key, result);
        return result;
    }
}
