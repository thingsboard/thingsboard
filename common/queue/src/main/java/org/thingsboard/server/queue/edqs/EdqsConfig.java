// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
