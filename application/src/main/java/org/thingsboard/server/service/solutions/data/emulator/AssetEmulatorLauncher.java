// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.emulator;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.service.solutions.data.definition.EmulatorDefinition;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.concurrent.ExecutorService;

@Slf4j
public class AssetEmulatorLauncher extends AbstractEmulatorLauncher<Asset> {

    @Builder
    public AssetEmulatorLauncher(Asset entity, EmulatorDefinition emulatorDefinition, ExecutorService oldTelemetryExecutor, TbClusterService tbClusterService,
                                 PartitionService partitionService,
                                 TbQueueProducerProvider tbQueueProducerProvider,
                                 TbServiceInfoProvider serviceInfoProvider,
                                 TelemetrySubscriptionService tsSubService) throws Exception {
        super(entity, emulatorDefinition, oldTelemetryExecutor, tbClusterService, partitionService, tbQueueProducerProvider, serviceInfoProvider, tsSubService);
    }

}
