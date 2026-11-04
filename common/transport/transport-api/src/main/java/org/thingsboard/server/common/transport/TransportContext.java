// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.transport;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.cache.ota.OtaPackageDataCache;
import org.thingsboard.server.common.transport.limits.TransportRateLimitService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.scheduler.SchedulerComponent;

import java.util.concurrent.ExecutorService;

@Slf4j
@Data
public abstract class TransportContext {

    @Autowired
    protected TransportService transportService;

    @Autowired
    private TbServiceInfoProvider serviceInfoProvider;

    @Autowired
    private SchedulerComponent scheduler;

    @Getter
    private ExecutorService executor;

    @Getter
    @Autowired
    private OtaPackageDataCache otaPackageDataCache;

    @Autowired
    private TransportResourceCache transportResourceCache;

    @Autowired
    protected TransportRateLimitService rateLimitService;

    @PostConstruct
    public void init() {
        executor = ThingsBoardExecutors.newWorkStealingPool(50, getClass());
    }

    @PreDestroy
    public void stop() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    public String getNodeId() {
        return serviceInfoProvider.getServiceId();
    }

}
