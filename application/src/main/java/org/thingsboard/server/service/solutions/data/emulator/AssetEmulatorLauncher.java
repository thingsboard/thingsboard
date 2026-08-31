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
