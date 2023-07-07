/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.queue.discovery;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.exception.TenantNotFoundException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ServiceInfo;
import org.thingsboard.server.queue.discovery.event.ClusterTopologyChangeEvent;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.discovery.event.ServiceListChangedEvent;
import org.thingsboard.server.queue.util.AfterStartUp;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HashPartitionService implements PartitionService {

    @Value("${queue.core.topic}")
    private String coreTopic;
    @Value("${queue.core.partitions:100}")
    private Integer corePartitions;
    @Value("${queue.vc.topic:tb_version_control}")
    private String vcTopic;
    @Value("${queue.vc.partitions:10}")
    private Integer vcPartitions;
    @Value("${queue.ar.topic:tb_alarm_rules}")
    private String arTopic;
    @Value("${queue.ar.partitions:10}")
    private Integer arPartitions;
    @Value("${queue.partitions.hash_function_name:murmur3_128}")
    private String hashFunctionName;

    private final ApplicationEventPublisher applicationEventPublisher;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TenantRoutingInfoService tenantRoutingInfoService;
    private final QueueRoutingInfoService queueRoutingInfoService;

    private volatile ConcurrentMap<QueueKey, List<Integer>> myPartitions = new ConcurrentHashMap<>();

    private final ConcurrentMap<QueueKey, String> partitionTopicsMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<QueueKey, Integer> partitionSizesMap = new ConcurrentHashMap<>();

    private final ConcurrentMap<TenantId, TenantRoutingInfo> tenantRoutingInfoMap = new ConcurrentHashMap<>();

    private Map<String, List<ServiceInfo>> tbTransportServicesByType = new HashMap<>();
    private List<ServiceInfo> currentOtherServices;

    private HashFunction hashFunction;

    public HashPartitionService(TbServiceInfoProvider serviceInfoProvider,
                                TenantRoutingInfoService tenantRoutingInfoService,
                                ApplicationEventPublisher applicationEventPublisher,
                                QueueRoutingInfoService queueRoutingInfoService) {
        this.serviceInfoProvider = serviceInfoProvider;
        this.tenantRoutingInfoService = tenantRoutingInfoService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.queueRoutingInfoService = queueRoutingInfoService;
    }

    @PostConstruct
    public void init() {
        this.hashFunction = forName(hashFunctionName);
        QueueKey coreKey = new QueueKey(ServiceType.TB_CORE);
        partitionSizesMap.put(coreKey, corePartitions);
        partitionTopicsMap.put(coreKey, coreTopic);

        QueueKey vcKey = new QueueKey(ServiceType.TB_VC_EXECUTOR);
        partitionSizesMap.put(vcKey, vcPartitions);
        partitionTopicsMap.put(vcKey, vcTopic);

        QueueKey arKey = new QueueKey(ServiceType.TB_ALARM_RULES_EXECUTOR);
        partitionSizesMap.put(arKey, arPartitions);
        partitionTopicsMap.put(arKey, arTopic);

        if (!isTransport(serviceInfoProvider.getServiceType())) {
            doInitRuleEnginePartitions();
        }
    }

    @AfterStartUp(order = AfterStartUp.QUEUE_INFO_INITIALIZATION)
    public void partitionsInit() {
        if (isTransport(serviceInfoProvider.getServiceType())) {
            doInitRuleEnginePartitions();
        }
    }

    private void doInitRuleEnginePartitions() {
        List<QueueRoutingInfo> queueRoutingInfoList = getQueueRoutingInfos();
        queueRoutingInfoList.forEach(queue -> {
            QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, queue);
            partitionTopicsMap.put(queueKey, queue.getQueueTopic());
            partitionSizesMap.put(queueKey, queue.getPartitions());
        });
    }

    private List<QueueRoutingInfo> getQueueRoutingInfos() {
        List<QueueRoutingInfo> queueRoutingInfoList;
        String serviceType = serviceInfoProvider.getServiceType();

        if (isTransport(serviceType)) {
            //If transport started earlier than tb-core
            int getQueuesRetries = 10;
            while (true) {
                if (getQueuesRetries > 0) {
                    log.info("Try to get queue routing info.");
                    try {
                        queueRoutingInfoList = queueRoutingInfoService.getAllQueuesRoutingInfo();
                        break;
                    } catch (Exception e) {
                        log.info("Failed to get queues routing info: {}!", e.getMessage());
                        getQueuesRetries--;
                    }
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        log.info("Failed to await queues routing info!", e);
                    }
                } else {
                    throw new RuntimeException("Failed to await queues routing info!");
                }
            }
        } else {
            queueRoutingInfoList = queueRoutingInfoService.getAllQueuesRoutingInfo();
        }
        return queueRoutingInfoList;
    }

    private boolean isTransport(String serviceType) {
        return "tb-transport".equals(serviceType);
    }

    @Override
    public void updateQueue(TransportProtos.QueueUpdateMsg queueUpdateMsg) {
        TenantId tenantId = new TenantId(new UUID(queueUpdateMsg.getTenantIdMSB(), queueUpdateMsg.getTenantIdLSB()));
        QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, queueUpdateMsg.getQueueName(), tenantId);
        partitionTopicsMap.put(queueKey, queueUpdateMsg.getQueueTopic());
        partitionSizesMap.put(queueKey, queueUpdateMsg.getPartitions());
        myPartitions.remove(queueKey);
    }

    @Override
    public void removeQueue(TransportProtos.QueueDeleteMsg queueDeleteMsg) {
        TenantId tenantId = new TenantId(new UUID(queueDeleteMsg.getTenantIdMSB(), queueDeleteMsg.getTenantIdLSB()));
        QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, queueDeleteMsg.getQueueName(), tenantId);
        myPartitions.remove(queueKey);
        partitionTopicsMap.remove(queueKey);
        partitionSizesMap.remove(queueKey);
        //TODO: remove after merging tb entity services
        removeTenant(tenantId);
    }

    @Override
    public TopicPartitionInfo resolve(ServiceType serviceType, String queueName, TenantId tenantId, EntityId entityId) {
        TenantId isolatedOrSystemTenantId = getIsolatedOrSystemTenantId(serviceType, tenantId);
        QueueKey queueKey = new QueueKey(serviceType, queueName, isolatedOrSystemTenantId);
        if (!partitionSizesMap.containsKey(queueKey)) {
            queueKey = new QueueKey(serviceType, isolatedOrSystemTenantId);
        }
        return resolve(queueKey, entityId);
    }

    @Override
    public TopicPartitionInfo resolve(ServiceType serviceType, TenantId tenantId, EntityId entityId) {
        return resolve(serviceType, null, tenantId, entityId);
    }

    @Override
    public boolean isMyPartition(ServiceType serviceType, TenantId tenantId, EntityId entityId) {
        return resolve(serviceType, tenantId, entityId).isMyPartition();
    }

    private TopicPartitionInfo resolve(QueueKey queueKey, EntityId entityId) {
        int hash = hashFunction.newHasher()
                .putLong(entityId.getId().getMostSignificantBits())
                .putLong(entityId.getId().getLeastSignificantBits()).hash().asInt();

        Integer partitionSize = partitionSizesMap.get(queueKey);
        int partition = Math.abs(hash % partitionSize);

        return buildTopicPartitionInfo(queueKey, partition);
    }

    @Override
    public synchronized void recalculatePartitions(ServiceInfo currentService, List<ServiceInfo> otherServices) {
        tbTransportServicesByType.clear();
        logServiceInfo(currentService);
        otherServices.forEach(this::logServiceInfo);

        Map<QueueKey, List<ServiceInfo>> queueServicesMap = new HashMap<>();
        addNode(queueServicesMap, currentService);
        for (ServiceInfo other : otherServices) {
            addNode(queueServicesMap, other);
        }
        queueServicesMap.values().forEach(list -> list.sort(Comparator.comparing(ServiceInfo::getServiceId)));

        final ConcurrentMap<QueueKey, List<Integer>> newPartitions = new ConcurrentHashMap<>();
        partitionSizesMap.forEach((queueKey, size) -> {
            for (int i = 0; i < size; i++) {
                ServiceInfo serviceInfo = resolveByPartitionIdx(queueServicesMap.get(queueKey), queueKey, i);
                if (currentService.equals(serviceInfo)) {
                    newPartitions.computeIfAbsent(queueKey, key -> new ArrayList<>()).add(i);
                }
            }
        });

        final ConcurrentMap<QueueKey, List<Integer>> oldPartitions = myPartitions;
        myPartitions = newPartitions;

        oldPartitions.forEach((queueKey, partitions) -> {
            if (!myPartitions.containsKey(queueKey)) {
                log.info("[{}] NO MORE PARTITIONS FOR CURRENT KEY", queueKey);
                applicationEventPublisher.publishEvent(new PartitionChangeEvent(this, queueKey, Collections.emptySet()));
            }
        });

        myPartitions.forEach((queueKey, partitions) -> {
            if (!partitions.equals(oldPartitions.get(queueKey))) {
                log.info("[{}] NEW PARTITIONS: {}", queueKey, partitions);
                Set<TopicPartitionInfo> tpiList = partitions.stream()
                        .map(partition -> buildTopicPartitionInfo(queueKey, partition))
                        .collect(Collectors.toSet());
                applicationEventPublisher.publishEvent(new PartitionChangeEvent(this, queueKey, tpiList));
            }
        });

        if (currentOtherServices == null) {
            currentOtherServices = new ArrayList<>(otherServices);
        } else {
            Set<QueueKey> changes = new HashSet<>();
            Map<QueueKey, List<ServiceInfo>> currentMap = getServiceKeyListMap(currentOtherServices);
            Map<QueueKey, List<ServiceInfo>> newMap = getServiceKeyListMap(otherServices);
            currentOtherServices = otherServices;
            currentMap.forEach((key, list) -> {
                if (!list.equals(newMap.get(key))) {
                    changes.add(key);
                }
            });
            currentMap.keySet().forEach(newMap::remove);
            changes.addAll(newMap.keySet());
            if (!changes.isEmpty()) {
                applicationEventPublisher.publishEvent(new ClusterTopologyChangeEvent(this, changes));
            }
        }

        applicationEventPublisher.publishEvent(new ServiceListChangedEvent(otherServices, currentService));
    }

    @Override
    public Set<String> getAllServiceIds(ServiceType serviceType) {
        return getAllServices(serviceType).stream().map(ServiceInfo::getServiceId).collect(Collectors.toSet());
    }

    @Override
    public Set<ServiceInfo> getAllServices(ServiceType serviceType) {
        Set<ServiceInfo> result = getOtherServices(serviceType);
        ServiceInfo current = serviceInfoProvider.getServiceInfo();
        if (current.getServiceTypesList().contains(serviceType.name())) {
            result.add(current);
        }
        return result;
    }

    @Override
    public Set<ServiceInfo> getOtherServices(ServiceType serviceType) {
        Set<ServiceInfo> result = new HashSet<>();
        if (currentOtherServices != null) {
            for (ServiceInfo serviceInfo : currentOtherServices) {
                if (serviceInfo.getServiceTypesList().contains(serviceType.name())) {
                    result.add(serviceInfo);
                }
            }
        }
        return result;
    }


    @Override
    public int resolvePartitionIndex(UUID entityId, int partitions) {
        int hash = hashFunction.newHasher()
                .putLong(entityId.getMostSignificantBits())
                .putLong(entityId.getLeastSignificantBits()).hash().asInt();
        return Math.abs(hash % partitions);
    }

    @Override
    public void removeTenant(TenantId tenantId) {
        tenantRoutingInfoMap.remove(tenantId);
    }

    @Override
    public int countTransportsByType(String type) {
        var list = tbTransportServicesByType.get(type);
        return list == null ? 0 : list.size();
    }

    private Map<QueueKey, List<ServiceInfo>> getServiceKeyListMap(List<ServiceInfo> services) {
        final Map<QueueKey, List<ServiceInfo>> currentMap = new HashMap<>();
        services.forEach(serviceInfo -> {
            for (String serviceTypeStr : serviceInfo.getServiceTypesList()) {
                ServiceType serviceType = ServiceType.of(serviceTypeStr);
                if (ServiceType.TB_RULE_ENGINE.equals(serviceType)) {
                    partitionTopicsMap.keySet().forEach(queueKey ->
                            currentMap.computeIfAbsent(queueKey, key -> new ArrayList<>()).add(serviceInfo));
                } else {
                    QueueKey queueKey = new QueueKey(serviceType);
                    currentMap.computeIfAbsent(queueKey, key -> new ArrayList<>()).add(serviceInfo);
                }
            }
        });
        return currentMap;
    }

    private TopicPartitionInfo buildTopicPartitionInfo(QueueKey queueKey, int partition) {
        TopicPartitionInfo.TopicPartitionInfoBuilder tpi = TopicPartitionInfo.builder();
        tpi.topic(partitionTopicsMap.get(queueKey));
        tpi.partition(partition);
        tpi.tenantId(queueKey.getTenantId());

        List<Integer> partitions = myPartitions.get(queueKey);
        if (partitions != null) {
            tpi.myPartition(partitions.contains(partition));
        } else {
            tpi.myPartition(false);
        }
        return tpi.build();
    }

    private boolean isIsolated(ServiceType serviceType, TenantId tenantId) {
        if (TenantId.SYS_TENANT_ID.equals(tenantId)) {
            return false;
        }
        TenantRoutingInfo routingInfo = tenantRoutingInfoMap.get(tenantId);
        if (routingInfo == null) {
            synchronized (tenantRoutingInfoMap) {
                routingInfo = tenantRoutingInfoMap.get(tenantId);
                if (routingInfo == null) {
                    routingInfo = tenantRoutingInfoService.getRoutingInfo(tenantId);
                    tenantRoutingInfoMap.put(tenantId, routingInfo);
                }
            }
        }
        if (routingInfo == null) {
            throw new TenantNotFoundException(tenantId);
        }
        switch (serviceType) {
            case TB_RULE_ENGINE:
                return routingInfo.isIsolatedTbRuleEngine();
            default:
                return false;
        }
    }

    protected TenantId getIsolatedOrSystemTenantId(ServiceType serviceType, TenantId tenantId) {
        return isIsolated(serviceType, tenantId) ? tenantId : TenantId.SYS_TENANT_ID;
    }

    private void logServiceInfo(TransportProtos.ServiceInfo server) {
        log.info("[{}] Found common server: [{}]", server.getServiceId(), server.getServiceTypesList());
    }

    private void addNode(Map<QueueKey, List<ServiceInfo>> queueServiceList, ServiceInfo instance) {
        for (String serviceTypeStr : instance.getServiceTypesList()) {
            ServiceType serviceType = ServiceType.of(serviceTypeStr);
            if (ServiceType.TB_RULE_ENGINE.equals(serviceType)) {
                partitionTopicsMap.keySet().forEach(key -> {
                    if (key.getType().equals(ServiceType.TB_RULE_ENGINE)) {
                        queueServiceList.computeIfAbsent(key, k -> new ArrayList<>()).add(instance);
                    }
                });
            } else if (ServiceType.TB_CORE.equals(serviceType)
                    || ServiceType.TB_VC_EXECUTOR.equals(serviceType)
                    || ServiceType.TB_ALARM_RULES_EXECUTOR.equals(serviceType)) {
                queueServiceList.computeIfAbsent(new QueueKey(serviceType), key -> new ArrayList<>()).add(instance);
            }
        }

        for (String transportType : instance.getTransportsList()) {
            tbTransportServicesByType.computeIfAbsent(transportType, t -> new ArrayList<>()).add(instance);
        }
    }

    protected ServiceInfo resolveByPartitionIdx(List<ServiceInfo> servers, QueueKey queueKey, int partition) {
        if (servers == null || servers.isEmpty()) {
            return null;
        }

        if (!ServiceType.TB_RULE_ENGINE.equals(queueKey.getType()) || TenantId.SYS_TENANT_ID.equals(queueKey.getTenantId())) {
            return servers.get(partition % servers.size());
        } else {
            int hash = hashFunction.newHasher().putLong(queueKey.getTenantId().getId().getMostSignificantBits())
                    .putLong(queueKey.getTenantId().getId().getLeastSignificantBits())
                    .putString(queueKey.getQueueName(), StandardCharsets.UTF_8)
                    .hash().asInt();

            return servers.get(Math.abs((hash + partition) % servers.size()));
        }
    }

    public static HashFunction forName(String name) {
        switch (name) {
            case "murmur3_32":
                return Hashing.murmur3_32();
            case "murmur3_128":
                return Hashing.murmur3_128();
            case "sha256":
                return Hashing.sha256();
            default:
                throw new IllegalArgumentException("Can't find hash function with name " + name);
        }
    }

}
