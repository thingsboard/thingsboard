package org.thingsboard.server.queue.discovery;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.thingsboard.server.common.data.exception.TenantNotFoundException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.util.AfterStartUp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.DataConstants.MAIN_QUEUE_NAME;

@Slf4j
public abstract class AbstractPartitionService implements PartitionService {

    @Value("${queue.core.topic}")
    private String coreTopic;
    @Value("${queue.core.partitions:100}")
    private Integer corePartitions;
    @Value("${queue.vc.topic:tb_version_control}")
    private String vcTopic;
    @Value("${queue.vc.partitions:10}")
    private Integer vcPartitions;
    @Value("${queue.partitions.hash_function_name:murmur3_128}")
    private String hashFunctionName;

    protected final ApplicationEventPublisher applicationEventPublisher;
    protected final TbServiceInfoProvider serviceInfoProvider;
    protected final TenantRoutingInfoService tenantRoutingInfoService;
    private final QueueRoutingInfoService queueRoutingInfoService;
    private final TopicService topicService;

    protected volatile ConcurrentMap<QueueKey, List<Integer>> myPartitions = new ConcurrentHashMap<>();

    protected final ConcurrentMap<QueueKey, String> partitionTopicsMap = new ConcurrentHashMap<>();
    protected final ConcurrentMap<QueueKey, Integer> partitionSizesMap = new ConcurrentHashMap<>();

    private final ConcurrentMap<TenantId, TenantRoutingInfo> tenantRoutingInfoMap = new ConcurrentHashMap<>();

    protected List<TransportProtos.ServiceInfo> currentOtherServices;
    protected final Map<String, List<TransportProtos.ServiceInfo>> tbTransportServicesByType = new HashMap<>();
    protected volatile Map<TenantProfileId, List<TransportProtos.ServiceInfo>> responsibleServices = Collections.emptyMap();

    private HashFunction hashFunction;

    public AbstractPartitionService(TbServiceInfoProvider serviceInfoProvider,
                                TenantRoutingInfoService tenantRoutingInfoService,
                                ApplicationEventPublisher applicationEventPublisher,
                                QueueRoutingInfoService queueRoutingInfoService,
                                TopicService topicService) {
        this.serviceInfoProvider = serviceInfoProvider;
        this.tenantRoutingInfoService = tenantRoutingInfoService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.queueRoutingInfoService = queueRoutingInfoService;
        this.topicService = topicService;
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

    @Override
    public List<Integer> getMyPartitions(QueueKey queueKey) {
        return myPartitions.get(queueKey);
    }

    @Override
    public void updateQueues(List<TransportProtos.QueueUpdateMsg> queueUpdateMsgs) {
        for (TransportProtos.QueueUpdateMsg queueUpdateMsg : queueUpdateMsgs) {
            TenantId tenantId = TenantId.fromUUID(new UUID(queueUpdateMsg.getTenantIdMSB(), queueUpdateMsg.getTenantIdLSB()));
            QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, queueUpdateMsg.getQueueName(), tenantId);
            partitionTopicsMap.put(queueKey, queueUpdateMsg.getQueueTopic());
            partitionSizesMap.put(queueKey, queueUpdateMsg.getPartitions());
            if (!tenantId.isSysTenantId()) {
                tenantRoutingInfoMap.remove(tenantId);
            }
        }
    }

    @Override
    public void removeQueues(List<TransportProtos.QueueDeleteMsg> queueDeleteMsgs) {
        List<QueueKey> queueKeys = queueDeleteMsgs.stream()
                .map(queueDeleteMsg -> {
                    TenantId tenantId = TenantId.fromUUID(new UUID(queueDeleteMsg.getTenantIdMSB(), queueDeleteMsg.getTenantIdLSB()));
                    return new QueueKey(ServiceType.TB_RULE_ENGINE, queueDeleteMsg.getQueueName(), tenantId);
                })
                .collect(Collectors.toList());
        queueKeys.forEach(queueKey -> {
            myPartitions.remove(queueKey);
            partitionTopicsMap.remove(queueKey);
            partitionSizesMap.remove(queueKey);
            evictTenantInfo(queueKey.getTenantId());
        });
        if (serviceInfoProvider.isService(ServiceType.TB_RULE_ENGINE)) {
            publishPartitionChangeEvent(ServiceType.TB_RULE_ENGINE, queueKeys.stream()
                    .collect(Collectors.toMap(k -> k, k -> Collections.emptySet())));
        }
    }

    @Override
    public void removeTenant(TenantId tenantId) {
        List<QueueKey> queueKeys = partitionSizesMap.keySet().stream()
                .filter(queueKey -> tenantId.equals(queueKey.getTenantId()))
                .collect(Collectors.toList());
        queueKeys.forEach(queueKey -> {
            myPartitions.remove(queueKey);
            partitionTopicsMap.remove(queueKey);
            partitionSizesMap.remove(queueKey);
        });
        evictTenantInfo(tenantId);
    }

    @Override
    public boolean isManagedByCurrentService(TenantId tenantId) {
        if (serviceInfoProvider.isService(ServiceType.TB_CORE) || !serviceInfoProvider.isService(ServiceType.TB_RULE_ENGINE)) {
            return true;
        }

        boolean isManaged;
        Set<UUID> assignedTenantProfiles = serviceInfoProvider.getAssignedTenantProfiles();
        boolean isRegular = assignedTenantProfiles.isEmpty();
        if (tenantId.isSysTenantId()) {
            // All system queues are always processed on regular rule engines.
            return isRegular;
        }
        TenantRoutingInfo routingInfo = getRoutingInfo(tenantId);
        if (isRegular) {
            if (routingInfo.isIsolated()) {
                isManaged = hasDedicatedService(routingInfo.getProfileId());
            } else {
                isManaged = true;
            }
        } else {
            if (routingInfo.isIsolated()) {
                isManaged = assignedTenantProfiles.contains(routingInfo.getProfileId().getId());
            } else {
                isManaged = false;
            }
        }
        log.trace("[{}] Tenant {} managed by this service", tenantId, isManaged ? "is" : "is not");
        return isManaged;
    }

    private boolean hasDedicatedService(TenantProfileId profileId) {
        return CollectionsUtil.isEmpty(responsibleServices.get(profileId));
    }

    @Override
    public TopicPartitionInfo resolve(ServiceType serviceType, String queueName, TenantId tenantId, EntityId entityId) {
        TenantId isolatedOrSystemTenantId = getIsolatedOrSystemTenantId(serviceType, tenantId);
        if (queueName == null) {
            queueName = MAIN_QUEUE_NAME;
        }
        QueueKey queueKey = new QueueKey(serviceType, queueName, isolatedOrSystemTenantId);
        if (!partitionSizesMap.containsKey(queueKey)) {
            if (isolatedOrSystemTenantId.isSysTenantId()) {
                queueKey = new QueueKey(serviceType, TenantId.SYS_TENANT_ID);
            } else {
                queueKey = new QueueKey(serviceType, queueName, TenantId.SYS_TENANT_ID);
                if (!MAIN_QUEUE_NAME.equals(queueName) && !partitionSizesMap.containsKey(queueKey)) {
                    queueKey = new QueueKey(serviceType, TenantId.SYS_TENANT_ID);
                }
                log.warn("Using queue {} instead of isolated {} for tenant {}", queueKey, queueName, isolatedOrSystemTenantId);
            }
        }
        return resolve(queueKey, entityId);
    }

    @Override
    public TopicPartitionInfo resolve(ServiceType serviceType, TenantId tenantId, EntityId entityId) {
        return resolve(serviceType, null, tenantId, entityId);
    }

    @Override
    public boolean isMyPartition(ServiceType serviceType, TenantId tenantId, EntityId entityId) {
        try {
            return resolve(serviceType, tenantId, entityId).isMyPartition();
        } catch (TenantNotFoundException e) {
            log.warn("Tenant with id {} not found", tenantId, new RuntimeException("stacktrace"));
            return false;
        }
    }

    protected void publishPartitionChangeEvent(ServiceType serviceType, Map<QueueKey, Set<TopicPartitionInfo>> partitionsMap) {
        log.info("Partitions changed: {}", System.lineSeparator() + partitionsMap.entrySet().stream()
                .map(entry -> "[" + entry.getKey() + "] - [" + entry.getValue().stream()
                        .map(tpi -> tpi.getPartition().orElse(-1).toString()).sorted()
                        .collect(Collectors.joining(", ")) + "]")
                .collect(Collectors.joining(System.lineSeparator())));
        PartitionChangeEvent event = new PartitionChangeEvent(this, serviceType, partitionsMap);
        try {
            applicationEventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish partition change event {}", event, e);
        }
    }

    @Override
    public Set<String> getAllServiceIds(ServiceType serviceType) {
        return getAllServices(serviceType).stream().map(TransportProtos.ServiceInfo::getServiceId).collect(Collectors.toSet());
    }

    @Override
    public Set<TransportProtos.ServiceInfo> getAllServices(ServiceType serviceType) {
        Set<TransportProtos.ServiceInfo> result = getOtherServices(serviceType);
        TransportProtos.ServiceInfo current = serviceInfoProvider.getServiceInfo();
        if (current.getServiceTypesList().contains(serviceType.name())) {
            result.add(current);
        }
        return result;
    }

    @Override
    public Set<TransportProtos.ServiceInfo> getOtherServices(ServiceType serviceType) {
        Set<TransportProtos.ServiceInfo> result = new HashSet<>();
        if (currentOtherServices != null) {
            for (TransportProtos.ServiceInfo serviceInfo : currentOtherServices) {
                if (serviceInfo.getServiceTypesList().contains(serviceType.name())) {
                    result.add(serviceInfo);
                }
            }
        }
        return result;
    }


    @Override
    public int resolvePartitionIndex(UUID entityId, int partitions) {
        int hash = hash(entityId);
        return Math.abs(hash % partitions);
    }

    @Override
    public void evictTenantInfo(TenantId tenantId) {
        tenantRoutingInfoMap.remove(tenantId);
    }

    @Override
    public int countTransportsByType(String type) {
        var list = tbTransportServicesByType.get(type);
        return list == null ? 0 : list.size();
    }

    protected Map<QueueKey, List<TransportProtos.ServiceInfo>> getServiceKeyListMap(List<TransportProtos.ServiceInfo> services) {
        final Map<QueueKey, List<TransportProtos.ServiceInfo>> currentMap = new HashMap<>();
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

    protected TopicPartitionInfo buildTopicPartitionInfo(QueueKey queueKey, int partition) {
        TopicPartitionInfo.TopicPartitionInfoBuilder tpi = TopicPartitionInfo.builder();
        tpi.topic(topicService.buildTopicName(partitionTopicsMap.get(queueKey)));
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
        TenantRoutingInfo routingInfo = getRoutingInfo(tenantId);
        if (routingInfo == null) {
            throw new TenantNotFoundException(tenantId);
        }
        return switch (serviceType) {
            case TB_RULE_ENGINE -> routingInfo.isIsolated();
            default -> false;
        };
    }

    private TenantRoutingInfo getRoutingInfo(TenantId tenantId) {
        return tenantRoutingInfoMap.computeIfAbsent(tenantId, tenantRoutingInfoService::getRoutingInfo);
    }

    protected TenantId getIsolatedOrSystemTenantId(ServiceType serviceType, TenantId tenantId) {
        return isIsolated(serviceType, tenantId) ? tenantId : TenantId.SYS_TENANT_ID;
    }

    protected void logServiceInfo(TransportProtos.ServiceInfo server) {
        log.info("[{}] Found common server: {}", server.getServiceId(), server.getServiceTypesList());
    }

    private TopicPartitionInfo resolve(QueueKey queueKey, EntityId entityId) {
        Integer partitionSize = partitionSizesMap.get(queueKey);
        if (partitionSize == null) {
            throw new IllegalStateException("Partitions info for queue " + queueKey + " is missing");
        }

        int hash = hash(entityId.getId());
        int partition = Math.abs(hash % partitionSize);

        return buildTopicPartitionInfo(queueKey, partition);
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

    protected int hash(UUID key) {
        return hashFunction.newHasher()
                .putLong(key.getMostSignificantBits())
                .putLong(key.getLeastSignificantBits())
                .hash().asInt();
    }

    public static HashFunction forName(String name) {
        return switch (name) {
            case "murmur3_32" -> Hashing.murmur3_32();
            case "murmur3_128" -> Hashing.murmur3_128();
            case "sha256" -> Hashing.sha256();
            default -> throw new IllegalArgumentException("Can't find hash function with name " + name);
        };
    }

    protected static List<String> toServiceIds(Collection<TransportProtos.ServiceInfo> serviceInfos) {
        return serviceInfos.stream().map(TransportProtos.ServiceInfo::getServiceId).collect(Collectors.toList());
    }

}
