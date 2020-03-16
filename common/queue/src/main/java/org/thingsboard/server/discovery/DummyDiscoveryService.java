package org.thingsboard.server.discovery;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@ConditionalOnProperty(prefix = "zk", value = "enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
@DependsOn("environmentLogService")
public class DummyDiscoveryService implements PartitionDiscoveryService {

    private final TbServiceInfoProvider serviceInfoProvider;
    private final PartitionService partitionService;


    public DummyDiscoveryService(TbServiceInfoProvider serviceInfoProvider, PartitionService partitionService) {
        this.serviceInfoProvider = serviceInfoProvider;
        this.partitionService = partitionService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        partitionService.recalculatePartitions(serviceInfoProvider.getServiceInfo(), Collections.emptyList());
    }

}
