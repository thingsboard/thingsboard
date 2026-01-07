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
package org.thingsboard.server.queue.edqs;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class EdqsConfig {

    @Value("${queue.edqs.partitions:12}")
    private int partitions;
    @Value("${service.edqs.label:}")
    private String label;
    @Value("#{'${queue.edqs.partitioning_strategy:tenant}'.toUpperCase()}")
    private EdqsPartitioningStrategy partitioningStrategy;

    @Value("${queue.edqs.events_topic:edqs.events}")
    private String eventsTopic;
    @Value("${queue.edqs.state_topic:edqs.state}")
    private String stateTopic;
    @Value("${queue.edqs.requests_topic:edqs.requests}")
    private String requestsTopic;
    @Value("${queue.edqs.responses_topic:edqs.responses}")
    private String responsesTopic;
    @Value("${queue.edqs.poll_interval:25}")
    private long pollInterval;
    @Value("${queue.edqs.max_pending_requests:10000}")
    private int maxPendingRequests;
    @Value("${queue.edqs.max_request_timeout:20000}")
    private int maxRequestTimeout;
    @Value("${queue.edqs.request_executor_size:50}")
    private int requestExecutorSize;
    @Value("${queue.edqs.versions_cache_ttl:60}")
    private int versionsCacheTtl;

    public String getLabel() {
        if (partitioningStrategy == EdqsPartitioningStrategy.NONE) {
            label = "all"; // single set for all instances, so that each instance has all partitions
        }
        return label;
    }

    public enum EdqsPartitioningStrategy {
        TENANT, NONE
    }

}
