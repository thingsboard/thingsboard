// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.partition;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Slf4j
@TbCoreComponent
@Service
@RequiredArgsConstructor
public class TbCoreStartupService {

    private final PartitionService partitionService;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TbClusterService clusterService;

    @AfterStartUp(order = AfterStartUp.STARTUP_SERVICE)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        var myPartitions = partitionService.getMyPartitions(new QueueKey(ServiceType.TB_CORE));
        if (myPartitions != null && !myPartitions.isEmpty()) {
            TransportProtos.ToCoreNotificationMsg toCoreMsg = TransportProtos.ToCoreNotificationMsg.newBuilder()
                    .setCoreStartupMsg(TransportProtos.CoreStartupMsg.newBuilder()
                            .setServiceId(serviceInfoProvider.getServiceId()).addAllPartitions(myPartitions).build()).build();
            clusterService.broadcastToCore(toCoreMsg);
        }
    }

}
