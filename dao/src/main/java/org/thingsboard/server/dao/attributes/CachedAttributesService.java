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
package org.thingsboard.server.dao.attributes;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.stats.DefaultCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.service.Validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.CacheConstants.ATTRIBUTES_CACHE;
import static org.thingsboard.server.dao.attributes.AttributeUtils.validate;

@Service
@ConditionalOnProperty(prefix = "cache.attributes", value = "enabled", havingValue = "true")
@Primary
@Slf4j
public class CachedAttributesService implements AttributesService {
    private static final String STATS_NAME = "attributes.cache";

    private final AttributesDao attributesDao;
    private final Cache attributesCache;

    private final DefaultCounter hitCounter;
    private final DefaultCounter missCounter;

    public CachedAttributesService(AttributesDao attributesDao,
                                   CacheManager cacheManager,
                                   StatsFactory statsFactory) {
        this.attributesDao = attributesDao;
        this.attributesCache = cacheManager.getCache(ATTRIBUTES_CACHE);

        this.hitCounter = statsFactory.createDefaultCounter(STATS_NAME, "result", "hit");
        this.missCounter = statsFactory.createDefaultCounter(STATS_NAME, "result", "miss");
    }

    @Override
    public ListenableFuture<Optional<AttributeKvEntry>> find(TenantId tenantId, EntityId entityId, String scope, String attributeKey) {
        validate(entityId, scope);
        Validator.validateString(attributeKey, "Incorrect attribute key " + attributeKey);

        AttributeCacheKey attributeCacheKey = new AttributeCacheKey(scope, entityId, attributeKey);
        Cache.ValueWrapper cachedAttributeValue = attributesCache.get(attributeCacheKey);
        if (cachedAttributeValue != null) {
            hitCounter.increment();
            AttributeKvEntry cachedAttributeKvEntry = (AttributeKvEntry) cachedAttributeValue.get();
            return Futures.immediateFuture(Optional.ofNullable(cachedAttributeKvEntry));
        } else {
            missCounter.increment();
            ListenableFuture<Optional<AttributeKvEntry>> result = attributesDao.find(tenantId, entityId, scope, attributeKey);
            return Futures.transform(result, foundAttrKvEntry -> {
                // TODO: think if it's a good idea to store 'empty' attributes
                attributesCache.put(attributeKey, foundAttrKvEntry.orElse(null));
                return foundAttrKvEntry;
            }, MoreExecutors.directExecutor());
        }
    }

    @Override
    public ListenableFuture<List<AttributeKvEntry>> find(TenantId tenantId, EntityId entityId, String scope, Collection<String> attributeKeys) {
        validate(entityId, scope);
        attributeKeys.forEach(attributeKey -> Validator.validateString(attributeKey, "Incorrect attribute key " + attributeKey));

        Map<String, Cache.ValueWrapper> wrappedCachedAttributes = findCachedAttributes(entityId, scope, attributeKeys);

        List<AttributeKvEntry> cachedAttributes = wrappedCachedAttributes.values().stream()
                .map(wrappedCachedAttribute -> (AttributeKvEntry) wrappedCachedAttribute.get())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (wrappedCachedAttributes.size() == attributeKeys.size()) {
            return Futures.immediateFuture(cachedAttributes);
        }

        Set<String> notFoundAttributeKeys = new HashSet<>(attributeKeys);
        notFoundAttributeKeys.removeAll(wrappedCachedAttributes.keySet());

        ListenableFuture<List<AttributeKvEntry>> result = attributesDao.find(tenantId, entityId, scope, notFoundAttributeKeys);
        return Futures.transform(result, foundInDbAttributes -> mergeDbAndCacheAttributes(entityId, scope, cachedAttributes, notFoundAttributeKeys, foundInDbAttributes), MoreExecutors.directExecutor());

    }

    private Map<String, Cache.ValueWrapper> findCachedAttributes(EntityId entityId, String scope, Collection<String> attributeKeys) {
        Map<String, Cache.ValueWrapper> cachedAttributes = new HashMap<>();
        for (String attributeKey : attributeKeys) {
            Cache.ValueWrapper cachedAttributeValue = attributesCache.get(new AttributeCacheKey(scope, entityId, attributeKey));
            if (cachedAttributeValue != null) {
                hitCounter.increment();
                cachedAttributes.put(attributeKey, cachedAttributeValue);
            } else {
                missCounter.increment();
            }
        }
        return cachedAttributes;
    }

    private List<AttributeKvEntry> mergeDbAndCacheAttributes(EntityId entityId, String scope, List<AttributeKvEntry> cachedAttributes, Set<String> notFoundAttributeKeys, List<AttributeKvEntry> foundInDbAttributes) {
        for (AttributeKvEntry foundInDbAttribute : foundInDbAttributes) {
            AttributeCacheKey attributeCacheKey = new AttributeCacheKey(scope, entityId, foundInDbAttribute.getKey());
            attributesCache.put(attributeCacheKey, foundInDbAttribute);
            notFoundAttributeKeys.remove(foundInDbAttribute.getKey());
        }
        for (String key : notFoundAttributeKeys){
            attributesCache.put(new AttributeCacheKey(scope, entityId, key), null);
        }
        List<AttributeKvEntry> mergedAttributes = new ArrayList<>(cachedAttributes);
        mergedAttributes.addAll(foundInDbAttributes);
        return mergedAttributes;
    }

    @Override
    public ListenableFuture<List<AttributeKvEntry>> findAll(TenantId tenantId, EntityId entityId, String scope) {
        validate(entityId, scope);
        return attributesDao.findAll(tenantId, entityId, scope);
    }

    @Override
    public List<String> findAllKeysByDeviceProfileId(TenantId tenantId, DeviceProfileId deviceProfileId) {
        return attributesDao.findAllKeysByDeviceProfileId(tenantId, deviceProfileId);
    }

    @Override
    public List<String> findAllKeysByEntityIds(TenantId tenantId, EntityType entityType, List<EntityId> entityIds) {
        return attributesDao.findAllKeysByEntityIds(tenantId, entityType, entityIds);
    }

    @Override
    public ListenableFuture<List<Void>> save(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes) {
        validate(entityId, scope);
        attributes.forEach(AttributeUtils::validate);

        List<ListenableFuture<Void>> saveFutures = attributes.stream().map(attribute -> attributesDao.save(tenantId, entityId, scope, attribute)).collect(Collectors.toList());
        ListenableFuture<List<Void>> future = Futures.allAsList(saveFutures);

        // TODO: can do if (attributesCache.get() != null) attributesCache.put() instead, but will be more twice more requests to cache
        List<String> attributeKeys = attributes.stream().map(KvEntry::getKey).collect(Collectors.toList());
        future.addListener(() -> evictAttributesFromCache(tenantId, entityId, scope, attributeKeys), MoreExecutors.directExecutor());
        return future;
    }

    @Override
    public ListenableFuture<List<Void>> removeAll(TenantId tenantId, EntityId entityId, String scope, List<String> attributeKeys) {
        validate(entityId, scope);
        ListenableFuture<List<Void>> future = attributesDao.removeAll(tenantId, entityId, scope, attributeKeys);
        future.addListener(() -> evictAttributesFromCache(tenantId, entityId, scope, attributeKeys), MoreExecutors.directExecutor());
        return future;
    }

    private void evictAttributesFromCache(TenantId tenantId, EntityId entityId, String scope, List<String> attributeKeys) {
        try {
            for (String attributeKey : attributeKeys) {
                attributesCache.evict(new AttributeCacheKey(scope, entityId, attributeKey));
            }
        } catch (Exception e) {
            log.error("[{}][{}] Failed to remove values from cache.", tenantId, entityId, e);
        }
    }
}
