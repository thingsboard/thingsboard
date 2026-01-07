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
package org.thingsboard.server.dao.aspect;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Map;

@Data
@Builder
public class DbCallStatsSnapshot {

    private final TenantId tenantId;
    private final int totalSuccess;
    private final int totalFailure;
    private final long totalTiming;
    private final Map<String, MethodCallStatsSnapshot> methodStats;

    public int getTotalCalls() {
        return totalSuccess + totalFailure;
    }

}
