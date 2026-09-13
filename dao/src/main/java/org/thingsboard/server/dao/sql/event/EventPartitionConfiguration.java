// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.event;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.event.EventType;

import java.util.concurrent.TimeUnit;

@Component
public class EventPartitionConfiguration {

    @Getter
    @Value("${sql.events.partition_size:168}")
    private int regularPartitionSizeInHours;
    @Getter
    @Value("${sql.events.debug_partition_size:1}")
    private int debugPartitionSizeInHours;

    private long regularPartitionSizeInMs;
    private long debugPartitionSizeInMs;

    @PostConstruct
    public void init() {
        regularPartitionSizeInMs = TimeUnit.HOURS.toMillis(regularPartitionSizeInHours);
        debugPartitionSizeInMs = TimeUnit.HOURS.toMillis(debugPartitionSizeInHours);
    }

    public long getPartitionSizeInMs(EventType eventType) {
        return eventType.isDebug() ? debugPartitionSizeInMs : regularPartitionSizeInMs;
    }
}
