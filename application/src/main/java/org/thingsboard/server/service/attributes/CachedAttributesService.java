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

import com.google.common.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
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
import java.util.stream.Collectors;

@Service
@Primary
@Slf4j
public class CachedAttributesService implements AttributesService {
    private static final List<EntityType> LOCAL_ENTITIES = Arrays.asList(EntityType.DEVICE, EntityType.ASSET);
    private static final List<EntityType> GLOBAL_ENTITIES = Arrays.asList(EntityType.CUSTOMER, EntityType.TENANT);
    public static final String MAIN_QUEUE_NAME = "Main";

    private final AttributesService daoAttributesService;
    private final TbClusterService clusterService;
    private final PartitionService partitionService;
    private final TbAttributesCache attributesCache;

    public CachedAttributesService(@Qualifier("daoAttributesService") AttributesService daoAttributesService,
                                   TbClusterService clusterService, PartitionService partitionService,
                                   TbAttributesCache attributesCache) {
        this.daoAttributesService = daoAttributesService;
        this.clusterService = clusterService;
        this.partitionService = partitionService;
        this.attributesCache = attributesCache;
    }


    @Override
    public ListenableFuture<Optional<AttributeKvEntry>> find(TenantId tenantId, EntityId entityId, String scope, String attributeKey) {
        validate(entityId, scope);
        Validator.validateString(attributeKey, "Incorrect attribute key " + attributeKey);

        if (LOCAL_ENTITIES.contains(entityId.getEntityType())) {
            TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, MAIN_QUEUE_NAME, tenantId, entityId);
            if (!tpi.isMyPartition()) {
                return daoAttributesService.find(tenantId, entityId, scope, attributeKey);
            } else {
                return findAndPopulateCache(tenantId, entityId, scope, attributeKey);
            }
        } else if (GLOBAL_ENTITIES.contains(entityId.getEntityType())) {
            return findAndPopulateCache(tenantId, entityId, scope, attributeKey);
        } else {
            return daoAttributesService.find(tenantId, entityId, scope, attributeKey);
        }
    }

    @Override
    public ListenableFuture<List<AttributeKvEntry>> find(TenantId tenantId, EntityId entityId, String scope, Collection<String> attributeKeys) {
        validate(entityId, scope);
        attributeKeys.forEach(attributeKey -> Validator.validateString(attributeKey, "Incorrect attribute key " + attributeKey));

        if (LOCAL_ENTITIES.contains(entityId.getEntityType())) {
            TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, MAIN_QUEUE_NAME, tenantId, entityId);
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
        AttributeCacheEntry cachedEntry = attributesCache.find(tenantId, entityId, scope, attributeKey);
        if (cachedEntry != null) {
            return Futures.immediateFuture(Optional.of(cachedEntry.getAttributeKvEntry()));
        } else {
            ListenableFuture<Optional<AttributeKvEntry>> result = daoAttributesService.find(tenantId, entityId, scope, attributeKey);
            return Futures.transform(result, foundAttrKvEntry -> {
                attributesCache.put(tenantId, entityId, scope, attributeKey, foundAttrKvEntry.orElse(null));
                return foundAttrKvEntry;
            }, MoreExecutors.directExecutor());
        }
    }

    private ListenableFuture<List<AttributeKvEntry>> findAndPopulateCache(TenantId tenantId, EntityId entityId, String scope, Collection<String> attributeKeys) {
        Collection<String> notFoundInCacheAttributeKeys = new HashSet<>();
        List<AttributeKvEntry> foundInCacheAttributes = new ArrayList<>();
        for (String attributeKey : attributeKeys) {
            AttributeCacheEntry cachedEntry = attributesCache.find(tenantId, entityId, scope, attributeKey);
            if (cachedEntry != null) {
                if (cachedEntry.isPresent()) {
                    foundInCacheAttributes.add(cachedEntry.getAttributeKvEntry());
                }
            } else {
                notFoundInCacheAttributeKeys.add(attributeKey);
            }
        }
        if (notFoundInCacheAttributeKeys.isEmpty()) {
            return Futures.immediateFuture(foundInCacheAttributes);
        } else {
            ListenableFuture<List<AttributeKvEntry>> result = daoAttributesService.find(tenantId, entityId, scope, notFoundInCacheAttributeKeys);
            return Futures.transform(result, foundInDbAttributes -> {
                for (AttributeKvEntry foundInDbAttribute : foundInDbAttributes) {
                    attributesCache.put(tenantId, entityId, scope, foundInDbAttribute.getKey(), foundInDbAttribute);
                    notFoundInCacheAttributeKeys.remove(foundInDbAttribute.getKey());
                }
                for (String key : notFoundInCacheAttributeKeys){
                    attributesCache.put(tenantId, entityId, scope, key, null);
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

        return Futures.transform(daoAttributesService.save(tenantId, entityId, scope, attributes),
                result -> {
                    try {
                        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, MAIN_QUEUE_NAME, tenantId, entityId);
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
                    return result;
                },
                MoreExecutors.directExecutor());
    }

    private void updateCache(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes) {
        for (AttributeKvEntry attribute : attributes) {
            if (Objects.nonNull(attributesCache.find(tenantId, entityId, scope, attribute.getKey()))) {
                attributesCache.put(tenantId, entityId, scope, attribute.getKey(), attribute);
            }
        }
    }

    @Override
    public ListenableFuture<List<Void>> removeAll(TenantId tenantId, EntityId entityId, String scope, List<String> attributeKeys) {
        validate(entityId, scope);
        return Futures.transform(daoAttributesService.removeAll(tenantId, entityId, scope, attributeKeys),
                result -> {
                    try {
                        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, MAIN_QUEUE_NAME, tenantId, entityId);
                        if (LOCAL_ENTITIES.contains(entityId.getEntityType())) {
                            attributesCache.evict(tenantId, entityId, scope, attributeKeys);
                        } else if (GLOBAL_ENTITIES.contains(entityId.getEntityType())) {
                            if (tpi.getTenantId().isPresent()) {
                                attributesCache.evict(tenantId, entityId, scope, attributeKeys);
                            } else {
                                // TODO maybe first check if it's already in cache
                                clusterService.onAttributesCacheUpdated(tenantId, entityId, scope, attributeKeys);
                            }
                        }
                    } catch (Exception e) {
                        log.error("[{}][{}] Failed to remove values from cache.", tenantId, entityId, e);
                    }
                    return result;
                },
                MoreExecutors.directExecutor());
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
