/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.service.attributes;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.queue.TbClusterService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Primary
@Slf4j
public class CachedAttributesService implements AttributesService {
    private static final List<EntityType> LOCAL_ENTITIES = Arrays.asList(EntityType.DEVICE, EntityType.ASSET);
    private static final List<EntityType> GLOBAL_ENTITIES = Arrays.asList(EntityType.CUSTOMER, EntityType.TENANT);

    private final Map<TenantId, Cache<AttributesKey, Optional<AttributeKvEntry>>> tenantsCache = new ConcurrentHashMap<>();

    @Autowired
    @Qualifier("daoAttributesService")
    private AttributesService daoAttributesService;

    @Autowired
    private TbClusterService clusterService;

    @Autowired
    private PartitionService partitionService;

    @Autowired
    private AttributesCacheConfiguration cacheConfiguration;

    private Cache<AttributesKey, Optional<AttributeKvEntry>> getTenantCache(TenantId tenantId) {
        return tenantsCache.computeIfAbsent(tenantId,
                id -> CacheBuilder.newBuilder()
                        .maximumSize(cacheConfiguration.getMaxSize())
                        .expireAfterAccess(cacheConfiguration.getExpireAfterAccessInMinutes(), TimeUnit.MINUTES)
                        .build()
        );
    }

    public void evict(TenantId tenantId, EntityId entityId, String scope, List<String> attributeKeys) {
        List<AttributesKey> keys = attributeKeys.stream().map(key -> new AttributesKey(scope, entityId, key)).collect(Collectors.toList());
        getTenantCache(tenantId).invalidateAll(keys);
    }

    @Override
    public ListenableFuture<Optional<AttributeKvEntry>> find(TenantId tenantId, EntityId entityId, String scope, String attributeKey) {
        validate(entityId, scope);
        Validator.validateString(attributeKey, "Incorrect attribute key " + attributeKey);

        if (LOCAL_ENTITIES.contains(entityId.getEntityType())) {
            TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, "Main", tenantId, entityId);
            if (!tpi.isMyPartition()) {
                return daoAttributesService.find(tenantId, entityId, scope, attributeKey);
            } else {
                return findAndPopulateCache(tenantId, entityId, scope, attributeKey);
            }
        } else if (GLOBAL_ENTITIES.contains(entityId.getEntityType())) {
            return findAndPopulateCache(tenantId, entityId, scope, attributeKey);
            // TODO add clear cache on changes from other nodes
        } else {
            return daoAttributesService.find(tenantId, entityId, scope, attributeKey);
        }

        // todo think about isolated tenants
    }

    @Override
    public ListenableFuture<List<AttributeKvEntry>> find(TenantId tenantId, EntityId entityId, String scope, Collection<String> attributeKeys) {
        validate(entityId, scope);
        attributeKeys.forEach(attributeKey -> Validator.validateString(attributeKey, "Incorrect attribute key " + attributeKey));

        if (LOCAL_ENTITIES.contains(entityId.getEntityType())) {
            TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, "Main", tenantId, entityId);
            if (!tpi.isMyPartition()) {
                return daoAttributesService.find(tenantId, entityId, scope, attributeKeys);
            } else {
                return findAndPopulateCache(tenantId, entityId, scope, attributeKeys);
            }
        } else if (GLOBAL_ENTITIES.contains(entityId.getEntityType())) {
            return findAndPopulateCache(tenantId, entityId, scope, attributeKeys);
        } else {
            return daoAttributesService.find(tenantId, entityId, scope, attributeKeys);
        }
    }

    private ListenableFuture<Optional<AttributeKvEntry>> findAndPopulateCache(TenantId tenantId, EntityId entityId, String scope, String attributeKey) {
        Cache<AttributesKey, Optional<AttributeKvEntry>> cache = getTenantCache(tenantId);
        AttributesKey cacheKey = new AttributesKey(scope, entityId, attributeKey);
        Optional<AttributeKvEntry> cachedAttribute = cache.getIfPresent(cacheKey);
        if (cachedAttribute != null) {
            return Futures.immediateFuture(cachedAttribute);
        } else {
            ListenableFuture<Optional<AttributeKvEntry>> result = daoAttributesService.find(tenantId, entityId, scope, attributeKey);
            return Futures.transform(result, foundAttrKvEntry -> {
                cache.put(cacheKey, foundAttrKvEntry);
                return foundAttrKvEntry;
            }, MoreExecutors.directExecutor());
        }
    }

    private ListenableFuture<List<AttributeKvEntry>> findAndPopulateCache(TenantId tenantId, EntityId entityId, String scope, Collection<String> attributeKeys) {
        Cache<AttributesKey, Optional<AttributeKvEntry>> cache = getTenantCache(tenantId);
        Collection<String> notFoundInCacheAttributeKeys = new HashSet<>();
        List<AttributeKvEntry> foundInCacheAttributes = new ArrayList<>();
        attributeKeys.forEach(attributeKey -> {
            AttributesKey cacheKey = new AttributesKey(scope, entityId, attributeKey);
            Optional<AttributeKvEntry> attributeKvEntryOpt = cache.getIfPresent(cacheKey);
            if (attributeKvEntryOpt != null) {
                if (attributeKvEntryOpt.isPresent()) {
                    foundInCacheAttributes.add(attributeKvEntryOpt.get());
                }
            } else {
                notFoundInCacheAttributeKeys.add(attributeKey);
            }
        });
        if (notFoundInCacheAttributeKeys.isEmpty()) {
            return Futures.immediateFuture(foundInCacheAttributes);
        } else {
            ListenableFuture<List<AttributeKvEntry>> result = daoAttributesService.find(tenantId, entityId, scope, notFoundInCacheAttributeKeys);
            return Futures.transform(result, foundInDbAttributes -> {
                for (AttributeKvEntry foundInDbAttribute : foundInDbAttributes) {
                    cache.put(new AttributesKey(scope, entityId, foundInDbAttribute.getKey()), Optional.of(foundInDbAttribute));
                    notFoundInCacheAttributeKeys.remove(foundInDbAttribute.getKey());
                }
                for (String key : notFoundInCacheAttributeKeys){
                    cache.put(new AttributesKey(scope, entityId, key), Optional.empty());
                }
                List<AttributeKvEntry> mergedAttributes = new ArrayList<>(foundInCacheAttributes);
                mergedAttributes.addAll(foundInDbAttributes);
                return mergedAttributes;
            }, MoreExecutors.directExecutor());
        }
    }

    @Override
    public ListenableFuture<List<AttributeKvEntry>> findAll(TenantId tenantId, EntityId entityId, String scope) {
        validate(entityId, scope);
        return daoAttributesService.findAll(tenantId, entityId, scope);
    }

    @Override
    public ListenableFuture<List<Void>> save(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes) {
        validate(entityId, scope);
        attributes.forEach(attribute -> validate(attribute));

        try {
            TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, "Main", tenantId, entityId);
            if (LOCAL_ENTITIES.contains(entityId.getEntityType()) && tpi.isMyPartition()) {
                updateCache(tenantId, entityId, scope, attributes);
            } else if (GLOBAL_ENTITIES.contains(entityId.getEntityType())) {
                if (tpi.getTenantId().isPresent()) {
                    updateCache(tenantId, entityId, scope, attributes);
                } else {
                    // TODO maybe first check if it's already in cache
                    clusterService.onAttributesCacheUpdated(tenantId, entityId, scope, attributes.stream()
                            .map(KvEntry::getKey)
                            .collect(Collectors.toList())
                    );
                }
            }
        } catch (Exception e) {
            log.error("[{}][{}] Failed to update cache.", tenantId, entityId, e);
        }
        return daoAttributesService.save(tenantId, entityId, scope, attributes);
    }

    private void updateCache(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes) {
        attributes.forEach(attributeKvEntry -> {
            AttributesKey attributesKey = new AttributesKey(scope, entityId, attributeKvEntry.getKey());
            Cache<AttributesKey, Optional<AttributeKvEntry>> cache = getTenantCache(tenantId);
            if (cache.getIfPresent(attributesKey) != null) {
                cache.put(attributesKey, Optional.of(attributeKvEntry));
            }
        });
    }

    @Override
    public ListenableFuture<List<Void>> removeAll(TenantId tenantId, EntityId entityId, String scope, List<String> attributeKeys) {
        validate(entityId, scope);
        try {
            TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, "Main", tenantId, entityId);
            Cache<AttributesKey, Optional<AttributeKvEntry>> cache = getTenantCache(tenantId);
            if (LOCAL_ENTITIES.contains(entityId.getEntityType())) {
                cache.invalidateAll(attributeKeys.stream().map(attributeKey -> new AttributesKey(scope, entityId, attributeKey)).collect(Collectors.toList()));
            } else if (GLOBAL_ENTITIES.contains(entityId.getEntityType())) {
                if (tpi.getTenantId().isPresent()) {
                    cache.invalidateAll(attributeKeys.stream().map(attributeKey -> new AttributesKey(scope, entityId, attributeKey)).collect(Collectors.toList()));
                } else {
                    // TODO push notification about cache change to every node
                }
            }
        } catch (Exception e) {
            log.error("[{}][{}] Failed to remove values from cache.", tenantId, entityId, e);
        }
        return daoAttributesService.removeAll(tenantId, entityId, scope, attributeKeys);
    }

    private static void validate(EntityId id, String scope) {
        Validator.validateId(id.getId(), "Incorrect id " + id);
        Validator.validateString(scope, "Incorrect scope " + scope);
    }

    private static void validate(AttributeKvEntry kvEntry) {
        if (kvEntry == null) {
            throw new IncorrectParameterException("Key value entry can't be null");
        } else if (kvEntry.getDataType() == null) {
            throw new IncorrectParameterException("Incorrect kvEntry. Data type can't be null");
        } else {
            Validator.validateString(kvEntry.getKey(), "Incorrect kvEntry. Key can't be empty");
            Validator.validatePositiveNumber(kvEntry.getLastUpdateTs(), "Incorrect last update ts. Ts should be positive");
        }
    }
}
