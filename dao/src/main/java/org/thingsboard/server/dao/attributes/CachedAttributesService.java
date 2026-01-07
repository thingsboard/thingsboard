/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.TbCacheValueWrapper;
import org.thingsboard.server.cache.VersionedTbCache;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edqs.AttributeKv;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.AttributesSaveResult;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.edqs.EdqsService;
import org.thingsboard.server.common.stats.DefaultCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.cache.CacheExecutorService;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.sql.JpaExecutorService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.thingsboard.server.dao.attributes.AttributeUtils.validate;

@Service
@ConditionalOnProperty(prefix = "cache.attributes", value = "enabled", havingValue = "true")
@Primary
@Slf4j
public class CachedAttributesService implements AttributesService {
    private static final String STATS_NAME = "attributes.cache";
    public static final String LOCAL_CACHE_TYPE = "caffeine";

    private final AttributesDao attributesDao;
    private final JpaExecutorService jpaExecutorService;
    private final CacheExecutorService cacheExecutorService;
    private final EdqsService edqsService;
    private final DefaultCounter hitCounter;
    private final DefaultCounter missCounter;
    private final VersionedTbCache<AttributeCacheKey, AttributeKvEntry> cache;
    private ListeningExecutorService cacheExecutor;

    @Value("${cache.type:caffeine}")
    private String cacheType;
    @Value("${sql.attributes.value_no_xss_validation:false}")
    private boolean valueNoXssValidation;

    public CachedAttributesService(AttributesDao attributesDao,
                                   JpaExecutorService jpaExecutorService,
                                   @Lazy EdqsService edqsService, StatsFactory statsFactory,
                                   CacheExecutorService cacheExecutorService,
                                   VersionedTbCache<AttributeCacheKey, AttributeKvEntry> cache) {
        this.attributesDao = attributesDao;
        this.jpaExecutorService = jpaExecutorService;
        this.edqsService = edqsService;
        this.cacheExecutorService = cacheExecutorService;
        this.cache = cache;

        this.hitCounter = statsFactory.createDefaultCounter(STATS_NAME, "result", "hit");
        this.missCounter = statsFactory.createDefaultCounter(STATS_NAME, "result", "miss");
    }

    @PostConstruct
    public void init() {
        this.cacheExecutor = getExecutor(cacheType, cacheExecutorService);
    }

    /**
     * Will return:
     * - for the <b>local</b> cache type (cache.type="coffeine"): directExecutor (run callback immediately in the same thread)
     * - for the <b>remote</b> cache: dedicated thread pool for the cache IO calls to unblock any caller thread
     */
    ListeningExecutorService getExecutor(String cacheType, CacheExecutorService cacheExecutorService) {
        if (StringUtils.isEmpty(cacheType) || LOCAL_CACHE_TYPE.equals(cacheType)) {
            log.info("Going to use directExecutor for the local cache type {}", cacheType);
            return MoreExecutors.newDirectExecutorService();
        }
        log.info("Going to use cacheExecutorService for the remote cache type {}", cacheType);
        return cacheExecutorService.executor();
    }

    @Override
    public ListenableFuture<Optional<AttributeKvEntry>> find(TenantId tenantId, EntityId entityId, AttributeScope scope, String attributeKey) {
        validate(entityId, scope);
        Validator.validateString(attributeKey, k -> "Incorrect attribute key " + k);

        return cacheExecutor.submit(() -> {
            AttributeCacheKey attributeCacheKey = new AttributeCacheKey(scope, entityId, attributeKey);
            TbCacheValueWrapper<AttributeKvEntry> cachedAttributeValue = cache.get(attributeCacheKey);
            if (cachedAttributeValue != null) {
                hitCounter.increment();
                AttributeKvEntry cachedAttributeKvEntry = cachedAttributeValue.get();
                return Optional.ofNullable(cachedAttributeKvEntry);
            } else {
                missCounter.increment();
                Optional<AttributeKvEntry> result = attributesDao.find(tenantId, entityId, scope, attributeKey);
                cache.put(attributeCacheKey, result.orElse(null));
                return result;
            }
        });
    }

    @Override
    public ListenableFuture<List<AttributeKvEntry>> find(TenantId tenantId, EntityId entityId, AttributeScope scope, final Collection<String> attributeKeysNonUnique) {
        validate(entityId, scope);
        final var attributeKeys = new LinkedHashSet<>(attributeKeysNonUnique); // deduplicate the attributes
        attributeKeys.forEach(attributeKey -> Validator.validateString(attributeKey, k -> "Incorrect attribute key " + k));

        //CacheExecutor for Redis or DirectExecutor for local Caffeine
        return Futures.transformAsync(cacheExecutor.submit(() -> findCachedAttributes(entityId, scope, attributeKeys)),
                wrappedCachedAttributes -> {

                    List<AttributeKvEntry> cachedAttributes = wrappedCachedAttributes.values().stream()
                            .map(TbCacheValueWrapper::get)
                            .filter(Objects::nonNull)
                            .toList();
                    if (wrappedCachedAttributes.size() == attributeKeys.size()) {
                        log.trace("[{}][{}] Found all attributes from cache: {}", entityId, scope, attributeKeys);
                        return Futures.immediateFuture(cachedAttributes);
                    }

                    Set<String> notFoundAttributeKeys = new HashSet<>(attributeKeys);
                    notFoundAttributeKeys.removeAll(wrappedCachedAttributes.keySet());

                    // DB call should run in DB executor, not in cache-related executor
                    return jpaExecutorService.submit(() -> {
                        log.trace("[{}][{}] Lookup attributes from db: {}", entityId, scope, notFoundAttributeKeys);
                        List<AttributeKvEntry> result = attributesDao.find(tenantId, entityId, scope, notFoundAttributeKeys);
                        for (AttributeKvEntry foundInDbAttribute : result) {
                            put(entityId, scope, foundInDbAttribute);
                            notFoundAttributeKeys.remove(foundInDbAttribute.getKey());
                        }
                        for (String key : notFoundAttributeKeys) {
                            cache.put(new AttributeCacheKey(scope, entityId, key), null);
                        }
                        List<AttributeKvEntry> mergedAttributes = new ArrayList<>(cachedAttributes);
                        mergedAttributes.addAll(result);
                        log.trace("[{}][{}] Commit cache transaction: {}", entityId, scope, notFoundAttributeKeys);
                        return mergedAttributes;
                    });

                }, MoreExecutors.directExecutor()); // cacheExecutor analyse and returns results or submit to DB executor
    }

    private Map<String, TbCacheValueWrapper<AttributeKvEntry>> findCachedAttributes(EntityId entityId, AttributeScope scope, Collection<String> attributeKeys) {
        Map<String, TbCacheValueWrapper<AttributeKvEntry>> cachedAttributes = new HashMap<>();
        for (String attributeKey : attributeKeys) {
            var cachedAttributeValue = cache.get(new AttributeCacheKey(scope, entityId, attributeKey));
            if (cachedAttributeValue != null) {
                hitCounter.increment();
                cachedAttributes.put(attributeKey, cachedAttributeValue);
            } else {
                missCounter.increment();
            }
        }
        return cachedAttributes;
    }

    @Override
    public ListenableFuture<List<AttributeKvEntry>> findAll(TenantId tenantId, EntityId entityId, AttributeScope scope) {
        validate(entityId, scope);
        // We can`t watch on cache because the keys are unknown.
        return jpaExecutorService.submit(() -> attributesDao.findAll(tenantId, entityId, scope));
    }

    @Override
    public List<String> findAllKeysByDeviceProfileId(TenantId tenantId, DeviceProfileId deviceProfileId) {
        return attributesDao.findAllKeysByDeviceProfileId(tenantId, deviceProfileId);
    }

    @Override
    public List<String> findAllKeysByEntityIds(TenantId tenantId, List<EntityId> entityIds) {
        return attributesDao.findAllKeysByEntityIds(tenantId, entityIds);
    }

    @Override
    public List<String> findAllKeysByEntityIds(TenantId tenantId, List<EntityId> entityIds, String scope) {
        if (StringUtils.isEmpty(scope)) {
            return attributesDao.findAllKeysByEntityIds(tenantId, entityIds);
        } else {
            return attributesDao.findAllKeysByEntityIdsAndAttributeType(tenantId, entityIds, scope);
        }
    }

    @Override
    public ListenableFuture<AttributesSaveResult> save(TenantId tenantId, EntityId entityId, AttributeScope scope, AttributeKvEntry attribute) {
        validate(entityId, scope);
        AttributeUtils.validate(attribute, valueNoXssValidation);
        return doSave(tenantId, entityId, scope, List.of(attribute));
    }

    @Override
    public ListenableFuture<AttributesSaveResult> save(TenantId tenantId, EntityId entityId, AttributeScope scope, List<AttributeKvEntry> attributes) {
        validate(entityId, scope);
        AttributeUtils.validate(attributes, valueNoXssValidation);
        return doSave(tenantId, entityId, scope, attributes);
    }

    private ListenableFuture<AttributesSaveResult> doSave(TenantId tenantId, EntityId entityId, AttributeScope scope, List<AttributeKvEntry> attributes) {
        List<ListenableFuture<Long>> futures = new ArrayList<>(attributes.size());
        for (var attribute : attributes) {
            ListenableFuture<Long> future = Futures.transform(attributesDao.save(tenantId, entityId, scope, attribute), version -> {
                BaseAttributeKvEntry attributeKvEntry = new BaseAttributeKvEntry(((BaseAttributeKvEntry) attribute).getKv(), attribute.getLastUpdateTs(), version);
                put(entityId, scope, attributeKvEntry);
                TenantId edqsTenantId = entityId.getEntityType() == EntityType.TENANT ? (TenantId) entityId : tenantId;
                edqsService.onUpdate(edqsTenantId, ObjectType.ATTRIBUTE_KV, new AttributeKv(entityId, scope, attributeKvEntry, version));
                return version;
            }, cacheExecutor);
            futures.add(future);
        }
        return Futures.transform(Futures.allAsList(futures), AttributesSaveResult::of, MoreExecutors.directExecutor());
    }

    private void put(EntityId entityId, AttributeScope scope, AttributeKvEntry attribute) {
        String key = attribute.getKey();
        log.trace("[{}][{}][{}] Before cache put: {}", entityId, scope, key, attribute);
        cache.put(new AttributeCacheKey(scope, entityId, key), attribute);
        log.trace("[{}][{}][{}] after cache put.", entityId, scope, key);
    }

    @Override
    public ListenableFuture<List<String>> removeAll(TenantId tenantId, EntityId entityId, AttributeScope scope, List<String> attributeKeys) {
        validate(entityId, scope);
        List<ListenableFuture<TbPair<String, Long>>> futures = attributesDao.removeAllWithVersions(tenantId, entityId, scope, attributeKeys);
        return Futures.allAsList(futures.stream().map(future -> Futures.transform(future, keyVersionPair -> {
            String key = keyVersionPair.getFirst();
            Long version = keyVersionPair.getSecond();
            cache.evict(new AttributeCacheKey(scope, entityId, key), version);
            if (version != null) {
                TenantId edqsTenantId = entityId.getEntityType() == EntityType.TENANT ? (TenantId) entityId : tenantId;
                edqsService.onDelete(edqsTenantId, ObjectType.ATTRIBUTE_KV, new AttributeKv(entityId, scope, key, version));
            }
            return key;
        }, cacheExecutor)).toList());
    }

    @Override
    public int removeAllByEntityId(TenantId tenantId, EntityId entityId) {
        List<Pair<AttributeScope, String>> result = attributesDao.removeAllByEntityId(tenantId, entityId);
        result.forEach(deleted -> {
            AttributeScope scope = deleted.getKey();
            String key = deleted.getValue();
            if (scope != null && key != null) {
                cache.evict(new AttributeCacheKey(scope, entityId, key));
                // using version as Long.MAX_VALUE because we expect that the entity is deleted and there won't be any attributes after this
                edqsService.onDelete(tenantId, ObjectType.ATTRIBUTE_KV, new AttributeKv(entityId, scope, key, Long.MAX_VALUE));
            }
        });
        return result.size();
    }

}
