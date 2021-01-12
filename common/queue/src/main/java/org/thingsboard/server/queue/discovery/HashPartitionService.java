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
package org.thingsboard.server.queue.discovery;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceQueue;
import org.thingsboard.server.common.msg.queue.ServiceQueueKey;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ServiceInfo;

import javax.annotation.PostConstruct;
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
    @Value("${queue.partitions.hash_function_name:murmur3_128}")
    private String hashFunctionName;

    private final ApplicationEventPublisher applicationEventPublisher;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TenantRoutingInfoService tenantRoutingInfoService;
    private final QueueRoutingInfoService queueRoutingInfoService;
    private final ConcurrentMap<TenantId, ConcurrentMap<ServiceQueue, String>> partitionTopicsMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, ConcurrentMap<ServiceQueue, Integer>> partitionSizesMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, TenantRoutingInfo> tenantRoutingInfoMap = new ConcurrentHashMap<>();

    private ConcurrentMap<ServiceQueueKey, List<Integer>> myPartitions = new ConcurrentHashMap<>();
    private ConcurrentMap<TopicPartitionInfoKey, TopicPartitionInfo> tpiCache = new ConcurrentHashMap<>();

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
        partitionsInit();
    }

    private void partitionsInit() {
        addPartitionSizeToMap(TenantId.SYS_TENANT_ID, new ServiceQueue(ServiceType.TB_CORE, "Main"), corePartitions);
        addPartitionTopicToMap(TenantId.SYS_TENANT_ID, new ServiceQueue(ServiceType.TB_CORE, "Main"), coreTopic);

        List<QueueRoutingInfo> queueRoutingInfoList;

        String serviceType = serviceInfoProvider.getServiceType();

        if ("tb-rule-engine".equals(serviceType)) {
            queueRoutingInfoList = queueRoutingInfoService.getQueuesRoutingInfo(serviceInfoProvider.getIsolatedTenant().orElse(TenantId.SYS_TENANT_ID));
        } else if ("monolith".equals(serviceType)) {
            queueRoutingInfoList = queueRoutingInfoService.getAllQueuesRoutingInfo();
        } else if ("tb-core".equals(serviceType)) {
            queueRoutingInfoList = queueRoutingInfoService.getMainQueuesRoutingInfo();
        } else {
            int getQueuesRetries = 10;
            while (true) {
                if (getQueuesRetries > 0) {
                    log.info("Try to get queue routing info.");
                    try {
                        queueRoutingInfoList = queueRoutingInfoService.getMainQueuesRoutingInfo();
                        break;
                    } catch (Exception e) {
                        log.info("Failed to get queues routing info!");
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
        }

        queueRoutingInfoList.forEach(queue -> {
            addPartitionTopicToMap(queue.getTenantId(), new ServiceQueue(ServiceType.TB_RULE_ENGINE, queue.getQueueName()), queue.getQueueTopic());
            addPartitionSizeToMap(queue.getTenantId(), new ServiceQueue(ServiceType.TB_RULE_ENGINE, queue.getQueueName()), queue.getPartitions());
        });
    }

    @Override
    public void addNewQueue(TransportProtos.QueueUpdateMsg queueUpdateMsg) {
        TenantId tenantId = new TenantId(new UUID(queueUpdateMsg.getTenantIdMSB(), queueUpdateMsg.getTenantIdLSB()));
        addPartitionTopicToMap(tenantId, new ServiceQueue(ServiceType.TB_RULE_ENGINE, queueUpdateMsg.getQueueName()), queueUpdateMsg.getQueueTopic());
        addPartitionSizeToMap(tenantId, new ServiceQueue(ServiceType.TB_RULE_ENGINE, queueUpdateMsg.getQueueName()), queueUpdateMsg.getPartitions());
    }

    @Override
    public void removeQueue(TransportProtos.QueueDeleteMsg queueDeleteMsg) {
        TenantId tenantId = new TenantId(new UUID(queueDeleteMsg.getTenantIdMSB(), queueDeleteMsg.getTenantIdLSB()));
        ServiceQueue serviceQueue = new ServiceQueue(ServiceType.TB_RULE_ENGINE, queueDeleteMsg.getQueueName());
        partitionTopicsMap.get(tenantId).remove(serviceQueue);
        partitionSizesMap.get(tenantId).remove(serviceQueue);
        myPartitions.remove(new ServiceQueueKey(serviceQueue, tenantId));
    }

    private void addPartitionSizeToMap(TenantId tenantId, ServiceQueue serviceQueue, int partitions) {
        partitionSizesMap.computeIfAbsent(tenantId, id -> new ConcurrentHashMap<>()).put(serviceQueue, partitions);
    }

    private void addPartitionTopicToMap(TenantId tenantId, ServiceQueue serviceQueue, String topic) {
        partitionTopicsMap.computeIfAbsent(tenantId, id -> new ConcurrentHashMap<>()).put(serviceQueue, topic);
    }

    @Override
    public TopicPartitionInfo resolve(ServiceType serviceType, TenantId tenantId, EntityId entityId) {
        return resolve(new ServiceQueue(serviceType), tenantId, entityId);
    }

    @Override
    public TopicPartitionInfo resolve(ServiceType serviceType, String queueName, TenantId tenantId, EntityId entityId) {
        return resolve(new ServiceQueue(serviceType, queueName), tenantId, entityId);
    }

    private TopicPartitionInfo resolve(ServiceQueue serviceQueue, TenantId tenantId, EntityId entityId) {
        int hash = hashFunction.newHasher()
                .putLong(entityId.getId().getMostSignificantBits())
                .putLong(entityId.getId().getLeastSignificantBits()).hash().asInt();

        boolean isolatedTenant = isIsolated(serviceQueue, tenantId);
        Integer partitionSize = partitionSizesMap.get(isolatedTenant ? tenantId : TenantId.SYS_TENANT_ID).get(serviceQueue);
        int partition = Math.abs(hash % partitionSize);

        TopicPartitionInfoKey cacheKey = new TopicPartitionInfoKey(serviceQueue, isolatedTenant ? tenantId : null, partition);
        return tpiCache.computeIfAbsent(cacheKey, key -> buildTopicPartitionInfo(serviceQueue, tenantId, partition));
    }

    @Override
    public void recalculatePartitions(ServiceInfo currentService, List<ServiceInfo> otherServices) {
        logServiceInfo(currentService);
        otherServices.forEach(this::logServiceInfo);
        Map<ServiceQueueKey, List<ServiceInfo>> queueServicesMap = new HashMap<>();
        addNode(queueServicesMap, currentService);
        for (ServiceInfo other : otherServices) {
            addNode(queueServicesMap, other);
        }
        queueServicesMap.values().forEach(list -> list.sort(Comparator.comparing(ServiceInfo::getServiceId)));

        ConcurrentMap<ServiceQueueKey, List<Integer>> oldPartitions = myPartitions;
        TenantId myIsolatedOrSystemTenantId = getSystemOrIsolatedTenantId(currentService);
        myPartitions = new ConcurrentHashMap<>();
        partitionSizesMap.get(myIsolatedOrSystemTenantId).forEach((serviceQueue, size) -> {
            ServiceQueueKey myServiceQueueKey = new ServiceQueueKey(serviceQueue, myIsolatedOrSystemTenantId);
            for (int i = 0; i < size; i++) {
                ServiceInfo serviceInfo = resolveByPartitionIdx(queueServicesMap.get(myServiceQueueKey), i);
                if (currentService.equals(serviceInfo)) {
                    ServiceQueueKey serviceQueueKey = new ServiceQueueKey(serviceQueue, getSystemOrIsolatedTenantId(serviceInfo));
                    myPartitions.computeIfAbsent(serviceQueueKey, key -> new ArrayList<>()).add(i);
                }
            }
        });

        oldPartitions.forEach((serviceQueueKey, partitions) -> {
            if (!myPartitions.containsKey(serviceQueueKey)) {
                log.info("[{}] NO MORE PARTITIONS FOR CURRENT KEY", serviceQueueKey);
                applicationEventPublisher.publishEvent(new PartitionChangeEvent(this, serviceQueueKey, Collections.emptySet()));
            }
        });

        myPartitions.forEach((serviceQueueKey, partitions) -> {
            if (!partitions.equals(oldPartitions.get(serviceQueueKey))) {
                log.info("[{}] NEW PARTITIONS: {}", serviceQueueKey, partitions);
                Set<TopicPartitionInfo> tpiList = partitions.stream()
                        .map(partition -> buildTopicPartitionInfo(serviceQueueKey, partition))
                        .collect(Collectors.toSet());
                applicationEventPublisher.publishEvent(new PartitionChangeEvent(this, serviceQueueKey, tpiList));
            }
        });
        tpiCache.clear();

        if (currentOtherServices == null) {
            currentOtherServices = new ArrayList<>(otherServices);
        } else {
            Set<ServiceQueueKey> changes = new HashSet<>();
            Map<ServiceQueueKey, List<ServiceInfo>> currentMap = getServiceKeyListMap(currentOtherServices);
            Map<ServiceQueueKey, List<ServiceInfo>> newMap = getServiceKeyListMap(otherServices);
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
        // TODO: check and remove this 'if' if do not need (check update api usage state after restart TB of the end of a month)
        if (currentOtherServices != null) {
            for (ServiceInfo serviceInfo : currentOtherServices) {
                if (serviceInfo.getServiceTypesList().contains(serviceType.name())) {
                    result.add(serviceInfo);
                }
            }
        }
        return result;
    }

    private Map<ServiceQueueKey, List<ServiceInfo>> getServiceKeyListMap(List<ServiceInfo> services) {
        final Map<ServiceQueueKey, List<ServiceInfo>> currentMap = new HashMap<>();
        services.forEach(serviceInfo -> {
            for (String serviceTypeStr : serviceInfo.getServiceTypesList()) {
                ServiceType serviceType = ServiceType.valueOf(serviceTypeStr.toUpperCase());
//                if (ServiceType.TB_RULE_ENGINE.equals(serviceType)) {
//                    for (TransportProtos.QueueInfo queue : serviceInfo.getRuleEngineQueuesList()) {
//                        ServiceQueueKey serviceQueueKey = new ServiceQueueKey(new ServiceQueue(serviceType, queue.getName()), getSystemOrIsolatedTenantId(serviceInfo));
//                        currentMap.computeIfAbsent(serviceQueueKey, key -> new ArrayList<>()).add(serviceInfo);
//                    }
//                } else {
//                    ServiceQueueKey serviceQueueKey = new ServiceQueueKey(new ServiceQueue(serviceType), getSystemOrIsolatedTenantId(serviceInfo));
//                    currentMap.computeIfAbsent(serviceQueueKey, key -> new ArrayList<>()).add(serviceInfo);
//                }
                if (ServiceType.TB_CORE.equals(serviceType)) {
                    ServiceQueueKey serviceQueueKey = new ServiceQueueKey(new ServiceQueue(serviceType), getSystemOrIsolatedTenantId(serviceInfo));
                    currentMap.computeIfAbsent(serviceQueueKey, key -> new ArrayList<>()).add(serviceInfo);
                }
            }
        });
        return currentMap;
    }

    private TopicPartitionInfo buildTopicPartitionInfo(ServiceQueueKey serviceQueueKey, int partition) {
        return buildTopicPartitionInfo(serviceQueueKey.getServiceQueue(), serviceQueueKey.getTenantId(), partition);
    }

    private TopicPartitionInfo buildTopicPartitionInfo(ServiceQueue serviceQueue, TenantId tenantId, int partition) {
        boolean isolatedTenant = isIsolated(serviceQueue, tenantId);

        TopicPartitionInfo.TopicPartitionInfoBuilder tpi = TopicPartitionInfo.builder();
        tpi.topic(partitionTopicsMap.get(isolatedTenant ? tenantId : TenantId.SYS_TENANT_ID).get(serviceQueue));
        tpi.partition(partition);
        ServiceQueueKey myPartitionsSearchKey;
        if (isolatedTenant) {
            tpi.tenantId(tenantId);
            myPartitionsSearchKey = new ServiceQueueKey(serviceQueue, tenantId);
        } else {
            myPartitionsSearchKey = new ServiceQueueKey(serviceQueue, new TenantId(TenantId.NULL_UUID));
        }
        List<Integer> partitions = myPartitions.get(myPartitionsSearchKey);
        if (partitions != null) {
            tpi.myPartition(partitions.contains(partition));
        } else {
            tpi.myPartition(false);
        }
        return tpi.build();
    }

    private boolean isIsolated(ServiceQueue serviceQueue, TenantId tenantId) {
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
            throw new RuntimeException("Tenant not found!");
        }
        switch (serviceQueue.getType()) {
            case TB_CORE:
                return routingInfo.isIsolatedTbCore();
            case TB_RULE_ENGINE:
                return routingInfo.isIsolatedTbRuleEngine();
            default:
                return false;
        }
    }

    private void logServiceInfo(ServiceInfo server) {
        TenantId tenantId = getSystemOrIsolatedTenantId(server);
        if (tenantId.isNullUid()) {
            log.info("[{}] Found common server: [{}]", server.getServiceId(), server.getServiceTypesList());
        } else {
            log.info("[{}][{}] Found specific server: [{}]", server.getServiceId(), tenantId, server.getServiceTypesList());
        }
    }

    private TenantId getSystemOrIsolatedTenantId(ServiceInfo serviceInfo) {
        return new TenantId(new UUID(serviceInfo.getTenantIdMSB(), serviceInfo.getTenantIdLSB()));
    }

    private void addNode(Map<ServiceQueueKey, List<ServiceInfo>> queueServiceList, ServiceInfo instance) {
        TenantId tenantId = getSystemOrIsolatedTenantId(instance);
        for (String serviceTypeStr : instance.getServiceTypesList()) {
            ServiceType serviceType = ServiceType.valueOf(serviceTypeStr.toUpperCase());
            if (ServiceType.TB_RULE_ENGINE.equals(serviceType)) {
                partitionTopicsMap.get(tenantId).forEach((serviceQueue, topic) -> {
                    if (serviceQueue.getType().equals(ServiceType.TB_RULE_ENGINE)) {
                        ServiceQueueKey serviceQueueKey = new ServiceQueueKey(serviceQueue, tenantId);
                        queueServiceList.computeIfAbsent(serviceQueueKey, key -> new ArrayList<>()).add(instance);
                    }
                });
            } else {
                ServiceQueueKey serviceQueueKey = new ServiceQueueKey(new ServiceQueue(serviceType), tenantId);
                queueServiceList.computeIfAbsent(serviceQueueKey, key -> new ArrayList<>()).add(instance);
            }
        }
    }

    private ServiceInfo resolveByPartitionIdx(List<ServiceInfo> servers, Integer partitionIdx) {
        if (servers == null || servers.isEmpty()) {
            return null;
        }
        return servers.get(partitionIdx % servers.size());
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
