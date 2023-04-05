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
package org.thingsboard.server.service.apiusage;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.data.ApiUsageRecordKey;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
public class ApiStatsKey {

    private final ApiUsageRecordKey recordKey;
    private final UUID entityId;

    private static final String HOURLY = "Hourly";

    private static final Map<ApiUsageRecordKey, ApiStatsKey> keys;
    static {
        keys = Stream.of(ApiUsageRecordKey.values()).collect(Collectors.toMap(key -> key, key -> of(key, null)));
    }

    public static ApiStatsKey of(ApiUsageRecordKey recordKey) {
        return keys.get(recordKey);
    }

    public static ApiStatsKey of(ApiUsageRecordKey recordKey, UUID entityId) {
        return new ApiStatsKey(recordKey, entityId);
    }

    public static UUID getEntityId(String entryKey) {
        String uuid = StringUtils.substringAfterLast(entryKey, "_");
        if (!uuid.isEmpty()) {
            try {
                return UUID.fromString(uuid);
            } catch (Exception ignored) {}
        }
        return null;
    }

    public String getEntryKey(boolean hourly) {
        String entryKey = recordKey.getApiCountKey();
        if (hourly) entryKey += HOURLY;
        if (entityId != null) entryKey += "_" + entityId;
        return entryKey;
    }

    public static boolean isHourly(ApiUsageRecordKey recordKey, String entryKey) {
        return entryKey.startsWith(recordKey.getApiCountKey() + HOURLY);
    }

    public static boolean isMonthly(ApiUsageRecordKey recordKey, String entryKey) {
        return entryKey.startsWith(recordKey.getApiCountKey()) && !entryKey.contains(HOURLY);
    }

}
