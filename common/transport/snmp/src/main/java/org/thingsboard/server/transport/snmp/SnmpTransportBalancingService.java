/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.transport.snmp;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.TransportName;
import org.thingsboard.server.gen.transport.TransportProtos.ServiceInfo;
import org.thingsboard.server.queue.discovery.HashPartitionService;
import org.thingsboard.server.queue.discovery.event.ServiceListChangedEvent;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SnmpTransportBalancingService {
    private final HashPartitionService hashPartitionService;
    private final TransportName transportName;

    private int snmpTransportsCount = 1;
    private Integer currentTransportPartitionIndex = 0;

    @EventListener(value = ServiceListChangedEvent.class)
    public void onSnmpTransportListChanged(ServiceListChangedEvent event) {
        if (event.getChangedService() == null || event.getChangedService().getTransportsList().contains(transportName.getName())) {
            recalculatePartitions(event.getServiceList(), event.getCurrentService());
        }
    }

    public boolean isAssignedToCurrentTransport(UUID entityId) {
        return resolvePartitionIndexForEntity(entityId) == currentTransportPartitionIndex;
    }

    public int resolvePartitionIndexForEntity(UUID entityId) {
        return hashPartitionService.resolvePartitionIndex(entityId, snmpTransportsCount);
    }

    private void recalculatePartitions(List<ServiceInfo> serviceList, ServiceInfo currentService) {
        List<ServiceInfo> snmpTransports = serviceList.stream()
                .filter(service -> service.getTransportsList().contains(transportName.getName()))
                .sorted(Comparator.comparing(ServiceInfo::getServiceId))
                .collect(Collectors.toList());

        if (!snmpTransports.isEmpty()) {
            for (int i = 0; i < snmpTransports.size(); i++) {
                if (snmpTransports.get(i).equals(currentService)) {
                    currentTransportPartitionIndex = i;
                }
            }
            snmpTransportsCount = snmpTransports.size();
        }
    }
}
