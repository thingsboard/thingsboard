// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
