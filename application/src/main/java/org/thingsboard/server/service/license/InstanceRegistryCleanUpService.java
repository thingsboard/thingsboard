// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.license;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.instance.registry.InstanceRegistryService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.discovery.event.ServiceListChangedEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "zk", value = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class InstanceRegistryCleanUpService extends TbApplicationEventListener<ServiceListChangedEvent> {

    private final InstanceRegistryService instanceRegistryService;
    private final PartitionService partitionService;

    private Set<String> currentIds = new HashSet<>();

    @Override
    protected void onTbApplicationEvent(ServiceListChangedEvent event) {
        List<TransportProtos.ServiceInfo> otherServices = event.getOtherServices().stream()
                .filter(serviceInfo -> serviceInfo.getServiceTypesList().contains("TB_CORE") || serviceInfo.getServiceTypesList().contains("TB_RULE_ENGINE"))
                .collect(Collectors.toList());

        Set<String> serviceTypes = otherServices.stream().flatMap(s -> s.getServiceTypesList().stream()).collect(Collectors.toSet());
        serviceTypes.addAll(event.getCurrentService().getServiceTypesList());

        ServiceType serviceType = serviceTypes.contains("TB_CORE") ? ServiceType.TB_CORE : ServiceType.TB_RULE_ENGINE;

        Set<String> newIds = otherServices.stream().map(TransportProtos.ServiceInfo::getServiceId).collect(Collectors.toSet());
        Set<String> toRemove = CollectionsUtil.diffSets(newIds, currentIds );

        currentIds = newIds;
        if (!toRemove.isEmpty() && partitionService.isSystemTenantPartitionMine(serviceType)) {
            log.debug("Going to remove outdated instance registries: {}", toRemove);
            toRemove.forEach(instanceRegistryService::deleteByServiceId);
        }
    }

}
