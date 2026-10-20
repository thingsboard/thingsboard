// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Function;

@Component
@ConditionalOnProperty(prefix = "database.citus", name = "enabled", havingValue = "true")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class CitusQueuePartitioner {

    private final CitusSettings settings;
    private final CitusShardLocator shardLocator;

    public boolean isEnabled() {
        return settings.isEnabled();
    }

    public int queueCount() {
        return shardLocator.shardCount();
    }

    public <E> Function<E, Integer> bucketResolver(Function<E, UUID> entityIdExtractor) {
        return element -> shardLocator.bucket(entityIdExtractor.apply(element));
    }
}
