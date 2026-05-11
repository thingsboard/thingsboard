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
package org.thingsboard.rule.engine.api;

/**
 * Server-level safety caps for the HTTP client used by the REST API Call rule node.
 * Values are read from {@code thingsboard.yml} (or the corresponding environment variables)
 * and applied as hard ceilings on top of the per-node tenant configuration.
 * A value of {@code 0} means no system-level restriction.
 */
public interface TbHttpClientSettings {

    /** System ceiling for {@code maxParallelRequestsCount}. 0 = no system limit. */
    int getMaxParallelRequests();

    /** System ceiling for the pending-request queue depth. 0 = no system limit. */
    int getMaxPendingRequests();

    /**
     * Maximum number of TCP connections in the reactor-netty pool per node instance.
     * 0 = use reactor-netty's default: {@code max(availableProcessors, 8) * 2}.
     */
    int getPoolMaxConnections();

    TbHttpClientSettings DEFAULT = new TbHttpClientSettings() {
        @Override
        public int getMaxParallelRequests() { return 0; }

        @Override
        public int getMaxPendingRequests() { return 0; }

        @Override
        public int getPoolMaxConnections() { return 0; }
    };

}
