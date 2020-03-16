package org.thingsboard.server.discovery;

import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.List;

public interface PartitionService {

    List<TopicPartitionInfo> getCurrentPartitions(ServiceType serviceType);

    TopicPartitionInfo resolve(ServiceType serviceType, TenantId tenantId, EntityId entityId);

    void recalculatePartitions(TransportProtos.ServiceInfo currentService, List<TransportProtos.ServiceInfo> otherServices);
}
