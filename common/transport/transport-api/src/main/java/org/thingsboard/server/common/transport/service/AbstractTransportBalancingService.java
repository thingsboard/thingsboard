// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.transport.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.thingsboard.server.common.transport.TransportListChangedEvent;
import org.thingsboard.server.gen.transport.TransportProtos.ServiceInfo;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.discovery.event.ServiceListChangedEvent;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Splits the devices of a polling transport (SNMP, Modbus) between the nodes that run it.
 * Nodes running the transport are sorted by service id; each node owns the entities whose partition index equals its position.
 * When the node list changes, a {@link TransportListChangedEvent} is published for the transport to re-check its sessions.
 */
@Slf4j
public abstract class AbstractTransportBalancingService extends TbApplicationEventListener<ServiceListChangedEvent> {

    private final PartitionService partitionService;
    private final ApplicationEventPublisher eventPublisher;

    private volatile int transportsCount = 1;
    private volatile int currentTransportPartitionIndex = 0;

    protected AbstractTransportBalancingService(PartitionService partitionService, ApplicationEventPublisher eventPublisher) {
        this.partitionService = partitionService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Transport name as reported in {@link ServiceInfo#getTransportsList()}, e.g. {@code DataConstants.SNMP_TRANSPORT_NAME}.
     */
    protected abstract String getTransportName();

    public boolean isManagedByCurrentTransport(UUID entityId) {
        boolean isManaged = partitionService.resolvePartitionIndex(entityId, transportsCount) == currentTransportPartitionIndex;
        if (!isManaged) {
            log.trace("[{}] Entity {} is not managed by current transport node", getTransportName(), entityId);
        }
        return isManaged;
    }

    public boolean isManagedByCurrentTransport(String key) {
        boolean isManaged = partitionService.resolvePartitionIndex(key, transportsCount) == currentTransportPartitionIndex;
        if (!isManaged) {
            log.trace("[{}] Key {} is not managed by current transport node", getTransportName(), key);
        }
        return isManaged;
    }

    @Override
    protected void onTbApplicationEvent(ServiceListChangedEvent event) {
        log.trace("[{}] Got service list changed event: {}", getTransportName(), event);
        recalculatePartitions(event.getOtherServices(), event.getCurrentService());
    }

    private void recalculatePartitions(List<ServiceInfo> otherServices, ServiceInfo currentService) {
        String transportName = getTransportName();
        log.info("[{}] Recalculating partitions for transports", transportName);
        List<ServiceInfo> transports = Stream.concat(otherServices.stream(), Stream.of(currentService))
                .filter(service -> service.getTransportsList().contains(transportName))
                .sorted(Comparator.comparing(ServiceInfo::getServiceId))
                .toList();
        log.trace("[{}] Found transports: {}", transportName, transports);

        int previousCurrentTransportPartitionIndex = currentTransportPartitionIndex;
        int previousTransportsCount = transportsCount;

        if (!transports.isEmpty()) {
            for (int i = 0; i < transports.size(); i++) {
                if (transports.get(i).equals(currentService)) {
                    currentTransportPartitionIndex = i;
                    break;
                }
            }
            transportsCount = transports.size();
        }

        if (transportsCount != previousTransportsCount || currentTransportPartitionIndex != previousCurrentTransportPartitionIndex) {
            log.info("[{}] Transport partitions have changed: transports count = {}, current transport partition index = {}",
                    transportName, transportsCount, currentTransportPartitionIndex);
            eventPublisher.publishEvent(new TransportListChangedEvent(transportName));
        } else {
            log.info("[{}] Transport partitions have not changed", transportName);
        }
    }

}
