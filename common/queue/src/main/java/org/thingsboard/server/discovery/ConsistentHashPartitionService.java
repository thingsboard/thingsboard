package org.thingsboard.server.discovery;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ServiceInfo;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;

@Service
@Slf4j
public class ConsistentHashPartitionService implements PartitionService {

    @Value("${queue.core.topic}")
    private String coreTopic;
    @Value("${queue.core.partitions:100}")
    private Integer corePartitions;
    @Value("${queue.rule_engine.topic}")
    private String ruleEngineTopic;
    @Value("${queue.rule_engine.partitions:100}")
    private Integer ruleEnginePartitions;
    @Value("${queue.partitions.hash_function_name:murmur3_32}")
    private String hashFunctionName;
    @Value("${queue.partitions.virtual_nodes_size:16}")
    private Integer virtualNodesSize;

    private final TbServiceInfoProvider serviceInfoProvider;
    private final ConcurrentMap<ServiceType, String> partitionTopics = new ConcurrentHashMap<>();
    private final ConcurrentMap<ServiceType, Integer> partitionSizes = new ConcurrentHashMap<>();
    private ConcurrentMap<ServiceKey, List<Integer>> myPartitions = new ConcurrentHashMap<>();
    //TODO: Fetch this from the database, together with size of partitions for each service for each tenant.
    private ConcurrentMap<TenantId, Set<ServiceType>> isolatedTenants = new ConcurrentHashMap<>();

    private HashFunction hashFunction;

    public ConsistentHashPartitionService(TbServiceInfoProvider serviceInfoProvider) {
        this.serviceInfoProvider = serviceInfoProvider;
    }

    @PostConstruct
    public void init() {
        this.hashFunction = forName(hashFunctionName);
        partitionSizes.put(ServiceType.TB_CORE, corePartitions);
        partitionSizes.put(ServiceType.TB_RULE_ENGINE, ruleEnginePartitions);
        partitionTopics.put(ServiceType.TB_CORE, coreTopic);
        partitionTopics.put(ServiceType.TB_RULE_ENGINE, ruleEngineTopic);
    }

    @Override
    public List<TopicPartitionInfo> getCurrentPartitions(ServiceType serviceType) {
        ServiceInfo currentService = serviceInfoProvider.getServiceInfo();
        TenantId tenantId = getTenantId(currentService);
        ServiceKey serviceKey = new ServiceKey(serviceType, tenantId);
        List<Integer> partitions = myPartitions.get(serviceKey);
        List<TopicPartitionInfo> topicPartitions = new ArrayList<>();
        for (Integer partition : partitions) {
            TopicPartitionInfo.TopicPartitionInfoBuilder tpi = TopicPartitionInfo.builder();
            tpi.topic(partitionTopics.get(serviceType));
            tpi.partition(partition);
            if (!tenantId.isNullUid()) {
                tpi.tenantId(tenantId);
            }
            topicPartitions.add(tpi.build());
        }
        return topicPartitions;
    }

    @Override
    public TopicPartitionInfo resolve(ServiceType serviceType, TenantId tenantId, EntityId entityId) {
        boolean isolated = isolatedTenants.get(tenantId) != null && isolatedTenants.get(tenantId).contains(serviceType);
        int hash = hashFunction.newHasher()
                .putLong(entityId.getId().getMostSignificantBits())
                .putLong(entityId.getId().getLeastSignificantBits()).hash().asInt();
        int partition = Math.abs(hash % partitionSizes.get(serviceType));
        TopicPartitionInfo.TopicPartitionInfoBuilder tpi = TopicPartitionInfo.builder();
        tpi.topic(partitionTopics.get(serviceType));
        tpi.partition(partition);
        if (isolated) {
            tpi.tenantId(tenantId);
        }
        return tpi.build();
    }

    @Override
    public void recalculatePartitions(ServiceInfo currentService, List<ServiceInfo> otherServices) {
        logServiceInfo(currentService);
        otherServices.forEach(this::logServiceInfo);

        Map<ServiceType, ConsistentHashCircle<ServiceInfo>> newCircles = new HashMap<>(ServiceType.values().length);
        for (ServiceType serverType : ServiceType.values()) {
            newCircles.put(serverType, new ConsistentHashCircle<>());
        }
        addNode(newCircles, currentService);
        for (ServiceInfo other : otherServices) {
            addNode(newCircles, other);
            TenantId tenantId = getTenantId(other);
            if (!tenantId.isNullUid()) {
                isolatedTenants.putIfAbsent(tenantId, new HashSet<>());
                for (String serviceType : other.getServiceTypesList()) {
                    isolatedTenants.get(tenantId).add(ServiceType.valueOf(serviceType.toUpperCase()));
                }

            }
        }
        ConcurrentMap<ServiceKey, List<Integer>> oldPartitions = myPartitions;
        myPartitions = new ConcurrentHashMap<>();
        partitionSizes.forEach((type, size) -> {
            for (int i = 0; i < size; i++) {
                ServiceInfo serviceInfo = resolveByPartitionIdx(newCircles.get(type), i);
                if (currentService.equals(serviceInfo)) {
                    myPartitions.putIfAbsent(new ServiceKey(type, getTenantId(serviceInfo)), new ArrayList<>());
                }
            }
        });
        myPartitions.forEach((serviceKey, partitions) -> {
            if (!partitions.equals(oldPartitions.get(serviceKey))) {
                log.info("[{}] NEW PARTITIONS: {}", serviceKey, partitions);
            }
        });
    }

    private void logServiceInfo(TransportProtos.ServiceInfo server) {
        TenantId tenantId = getTenantId(server);
        if (tenantId.isNullUid()) {
            log.info("[{}] Found common server: [{}]", server.getServiceId(), server.getServiceTypesList());
        } else {
            log.info("[{}][{}] Found specific server: [{}]", server.getServiceId(), tenantId, server.getServiceTypesList());
        }
    }

    private TenantId getTenantId(TransportProtos.ServiceInfo serviceInfo) {
        return new TenantId(new UUID(serviceInfo.getTenantIdMSB(), serviceInfo.getTenantIdLSB()));
    }

    private void addNode(Map<ServiceType, ConsistentHashCircle<ServiceInfo>> circles, ServiceInfo instance) {
        for (String serviceTypeStr : instance.getServiceTypesList()) {
            ServiceType serviceType = ServiceType.valueOf(serviceTypeStr.toUpperCase());
            for (int i = 0; i < virtualNodesSize; i++) {
                circles.get(serviceType).put(hash(instance, i).asLong(), instance);
            }
        }
    }

    private ServiceInfo resolveByPartitionIdx(ConsistentHashCircle<ServiceInfo> circle, Integer partitionIdx) {
        if (circle.isEmpty()) {
            return null;
        }
        Long hash = hashFunction.newHasher().putInt(partitionIdx).hash().asLong();
        if (!circle.containsKey(hash)) {
            ConcurrentNavigableMap<Long, ServiceInfo> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ?
                    circle.firstKey() : tailMap.firstKey();
        }
        return circle.get(hash);
    }

    private HashCode hash(ServiceInfo instance, int i) {
        return hashFunction.newHasher().putString(instance.getServiceId(), StandardCharsets.UTF_8).putInt(i).hash();
    }

    private static class ServiceKey {
        @Getter
        private final ServiceType serviceType;
        @Getter
        private final TenantId tenantId;

        public ServiceKey(ServiceType serviceType, TenantId tenantId) {
            this.serviceType = serviceType;
            this.tenantId = tenantId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ServiceKey that = (ServiceKey) o;
            return serviceType == that.serviceType &&
                    Objects.equals(tenantId, that.tenantId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(serviceType, tenantId);
        }
    }

    public static HashFunction forName(String name) {
        switch (name) {
            case "murmur3_32":
                return Hashing.murmur3_32();
            case "murmur3_128":
                return Hashing.murmur3_128();
            case "crc32":
                return Hashing.crc32();
            case "md5":
                return Hashing.md5();
            default:
                throw new IllegalArgumentException("Can't find hash function with name " + name);
        }
    }
}
