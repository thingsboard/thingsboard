// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trendz;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.trendz.TrendzSyncService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@TbCoreComponent
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "trendz", name = "enabled", havingValue = "true")
public class TrendzStartupSynchronizer {

    private final TrendzSyncService trendzSyncService;
    private final PartitionService partitionService;

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    private void startSyncProcess() {
        if (!partitionService.isSystemPartitionMine(ServiceType.TB_CORE)) {
            return;
        }
        ExecutorService executor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("trendz-startup-sync"));
        executor.submit(() -> {
            try {
                trendzSyncService.performSyncIfNeeded();
            } catch (Exception e) {
                log.error("Failed to perform Trendz startup synchronization", e);
            } finally {
                executor.shutdown();
            }
        });
    }
}
