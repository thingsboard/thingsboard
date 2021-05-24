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
package org.thingsboard.server.transport.snmp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.thingsboard.server.gen.transport.TransportProtos.ServiceInfo;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.event.ServiceListChangedEvent;
import org.thingsboard.server.queue.util.TbSnmpTransportComponent;
import org.thingsboard.server.transport.snmp.event.SnmpTransportListChangedEvent;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@TbSnmpTransportComponent
@Service
@RequiredArgsConstructor
@Slf4j
public class SnmpTransportBalancingService {
    private final PartitionService partitionService;
    private final ApplicationEventPublisher eventPublisher;
    private final SnmpTransportService snmpTransportService;

    private int snmpTransportsCount = 1;
    private Integer currentTransportPartitionIndex = 0;

    public void onServiceListChanged(ServiceListChangedEvent event) {
        log.trace("Got service list changed event: {}", event);
        recalculatePartitions(event.getOtherServices(), event.getCurrentService());
    }

    public boolean isManagedByCurrentTransport(UUID entityId) {
        boolean isManaged = resolvePartitionIndexForEntity(entityId) == currentTransportPartitionIndex;
        if (!isManaged) {
            log.trace("Entity {} is not managed by current SNMP transport node", entityId);
        }
        return isManaged;
    }

    private int resolvePartitionIndexForEntity(UUID entityId) {
        return partitionService.resolvePartitionIndex(entityId, snmpTransportsCount);
    }

    private void recalculatePartitions(List<ServiceInfo> otherServices, ServiceInfo currentService) {
        log.info("Recalculating partitions for SNMP transports");
        List<ServiceInfo> snmpTransports = Stream.concat(otherServices.stream(), Stream.of(currentService))
                .filter(service -> service.getTransportsList().contains(snmpTransportService.getName()))
                .sorted(Comparator.comparing(ServiceInfo::getServiceId))
                .collect(Collectors.toList());
        log.trace("Found SNMP transports: {}", snmpTransports);

        int previousCurrentTransportPartitionIndex = currentTransportPartitionIndex;
        int previousSnmpTransportsCount = snmpTransportsCount;

        if (!snmpTransports.isEmpty()) {
            for (int i = 0; i < snmpTransports.size(); i++) {
                if (snmpTransports.get(i).equals(currentService)) {
                    currentTransportPartitionIndex = i;
                    break;
                }
            }
            snmpTransportsCount = snmpTransports.size();
        }

        if (snmpTransportsCount != previousSnmpTransportsCount || currentTransportPartitionIndex != previousCurrentTransportPartitionIndex) {
            log.info("SNMP transports partitions have changed: transports count = {}, current transport partition index = {}", snmpTransportsCount, currentTransportPartitionIndex);
            eventPublisher.publishEvent(new SnmpTransportListChangedEvent());
        } else {
            log.info("SNMP transports partitions have not changed");
        }
    }

}
