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
package org.thingsboard.server.dao.util;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.data.limit.LimitedApi;

@Getter
public enum BufferedRateExecutorType {

    READ(LimitedApi.CASSANDRA_READ_QUERIES_CORE, LimitedApi.CASSANDRA_READ_QUERIES_RULE_ENGINE, LimitedApi.CASSANDRA_READ_QUERIES_MONOLITH),
    WRITE(LimitedApi.CASSANDRA_WRITE_QUERIES_CORE, LimitedApi.CASSANDRA_WRITE_QUERIES_RULE_ENGINE, LimitedApi.CASSANDRA_WRITE_QUERIES_MONOLITH);

    private final LimitedApi coreLimitedApi;
    private final LimitedApi ruleEngineLimitedApi;
    private final LimitedApi monolithLimitedApi;

    private final String displayName = StringUtils.capitalize(name().toLowerCase());

    BufferedRateExecutorType(LimitedApi coreLimitedApi, LimitedApi ruleEngineLimitedApi, LimitedApi monolithLimitedApi) {
        this.coreLimitedApi = coreLimitedApi;
        this.ruleEngineLimitedApi = ruleEngineLimitedApi;
        this.monolithLimitedApi = monolithLimitedApi;
    }

}
