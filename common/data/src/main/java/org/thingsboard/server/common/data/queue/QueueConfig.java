// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.queue;

import lombok.Data;

public interface QueueConfig {

    boolean isConsumerPerPartition();

    int getPollInterval();

    static QueueConfig of(boolean consumerPerPartition, long pollInterval) {
        return new BasicQueueConfig(consumerPerPartition, (int) pollInterval);
    }

    @Data
    class BasicQueueConfig implements QueueConfig {
        private final boolean consumerPerPartition;
        private final int pollInterval;
    }

}
