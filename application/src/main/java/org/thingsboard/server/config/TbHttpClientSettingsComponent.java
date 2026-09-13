// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.rule.engine.api.TbHttpClientSettings;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;

@TbRuleEngineComponent
@Component
public class TbHttpClientSettingsComponent implements TbHttpClientSettings {

    @Value("${actors.rule.external.http_client.max_parallel_requests:0}")
    private int maxParallelRequests;

    @Value("${actors.rule.external.http_client.max_pending_requests:0}")
    private int maxPendingRequests;

    @Value("${actors.rule.external.http_client.pool_max_connections:0}")
    private int poolMaxConnections;

    @Override
    public int getMaxParallelRequests() {
        return maxParallelRequests;
    }

    @Override
    public int getMaxPendingRequests() {
        return maxPendingRequests;
    }

    @Override
    public int getPoolMaxConnections() {
        return poolMaxConnections;
    }

}
