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
