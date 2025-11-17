/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.cf;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultCalculatedFieldCache implements CalculatedFieldCache {

    private final ConcurrentReferenceHashMap<CalculatedFieldId, Lock> calculatedFieldFetchLocks = new ConcurrentReferenceHashMap<>();

    private final CalculatedFieldService calculatedFieldService;
    private final TbAssetProfileCache assetProfileCache;
    private final TbDeviceProfileCache deviceProfileCache;
    @Lazy
    private final ActorSystemContext systemContext;
    private final OwnerService ownerService;

    private final ConcurrentMap<CalculatedFieldId, CalculatedField> calculatedFields = new ConcurrentHashMap<>();
    private final ConcurrentMap<EntityId, List<CalculatedField>> entityIdCalculatedFields = new ConcurrentHashMap<>();
    private final ConcurrentMap<CalculatedFieldId, List<CalculatedFieldLink>> calculatedFieldLinks = new ConcurrentHashMap<>();
    private final ConcurrentMap<EntityId, List<CalculatedFieldLink>> entityIdCalculatedFieldLinks = new ConcurrentHashMap<>();
    private final ConcurrentMap<CalculatedFieldId, CalculatedFieldCtx> calculatedFieldsCtx = new ConcurrentHashMap<>();

    private final ConcurrentMap<EntityId, Set<EntityId>> ownerEntities = new ConcurrentHashMap<>();

    @Value("${queue.calculated_fields.init_fetch_pack_size:50000}")
    @Getter
    private int initFetchPackSize;

    @AfterStartUp(order = AfterStartUp.CF_READ_CF_SERVICE)
    public void init() {
        PageDataIterable<CalculatedField> cfs = new PageDataIterable<>(calculatedFieldService::findAllCalculatedFields, initFetchPackSize);
        cfs.forEach(cf -> {
            if (cf != null) {
                calculatedFields.putIfAbsent(cf.getId(), cf);
                List<CalculatedFieldLink> links = cf.getConfiguration().buildCalculatedFieldLinks(cf.getTenantId(), cf.getEntityId(), cf.getId());
                calculatedFieldLinks.put(cf.getId(), new CopyOnWriteArrayList<>(links));
            }
        });
        calculatedFields.values().forEach(cf -> {
            entityIdCalculatedFields.computeIfAbsent(cf.getEntityId(), id -> new CopyOnWriteArrayList<>()).add(cf);
        });
        calculatedFieldLinks.values().stream()
                .flatMap(List::stream)
                .forEach(link ->
                        entityIdCalculatedFieldLinks.computeIfAbsent(link.entityId(), id -> new CopyOnWriteArrayList<>()).add(link)
                );
    }

    @Override
    public CalculatedField getCalculatedField(CalculatedFieldId calculatedFieldId) {
        return calculatedFields.get(calculatedFieldId);
    }

    @Override
    public List<CalculatedField> getCalculatedFieldsByEntityId(EntityId entityId) {
        return entityIdCalculatedFields.getOrDefault(entityId, Collections.emptyList());
    }

    @Override
    public List<CalculatedFieldLink> getCalculatedFieldLinksByEntityId(EntityId entityId) {
        return entityIdCalculatedFieldLinks.getOrDefault(entityId, Collections.emptyList());
    }

    @Override
    public CalculatedFieldCtx getCalculatedFieldCtx(CalculatedFieldId calculatedFieldId) {
        CalculatedFieldCtx ctx = calculatedFieldsCtx.get(calculatedFieldId);
        if (ctx == null) {
            Lock lock = getFetchLock(calculatedFieldId);
            lock.lock();
            try {
                ctx = calculatedFieldsCtx.get(calculatedFieldId);
                if (ctx == null) {
                    CalculatedField calculatedField = getCalculatedField(calculatedFieldId);
                    if (calculatedField != null) {
                        ctx = new CalculatedFieldCtx(calculatedField, systemContext);
                        calculatedFieldsCtx.put(calculatedFieldId, ctx);
                        log.debug("[{}] Put calculated field ctx into cache: {}", calculatedFieldId, ctx);
                    }
                }
            } finally {
                lock.unlock();
            }
        }
        log.trace("[{}] Found calculated field ctx in cache: {}", calculatedFieldId, ctx);
        return ctx;
    }

    @Override
    public List<CalculatedFieldCtx> getCalculatedFieldCtxsByEntityId(EntityId entityId) {
        if (entityId == null) {
            return Collections.emptyList();
        }
        return getCalculatedFieldsByEntityId(entityId).stream()
                .map(cf -> getCalculatedFieldCtx(cf.getId()))
                .toList();
    }

    @Override
    public Stream<CalculatedFieldCtx> getCalculatedFieldCtxsByType(CalculatedFieldType cfType) {
        return calculatedFields.values().stream()
                .filter(cf -> cfType.equals(cf.getType()))
                .map(cf -> getCalculatedFieldCtx(cf.getId()));
    }

    @Override
    public boolean hasCalculatedFields(TenantId tenantId, EntityId entityId, Predicate<CalculatedFieldCtx> filter) {
        List<CalculatedFieldCtx> entityCfs = getCalculatedFieldCtxsByEntityId(entityId);
        for (CalculatedFieldCtx ctx : entityCfs) {
            if (filter.test(ctx)) {
                return true;
            }
        }

        return hasCalculatedFieldsByProfile(tenantId, entityId, filter);
    }

    public boolean hasCalculatedFieldsByProfile(TenantId tenantId, EntityId entityId, Predicate<CalculatedFieldCtx> filter) {
        EntityId profileId = getProfileId(tenantId, entityId);
        if (profileId != null) {
            List<CalculatedFieldCtx> profileCfs = getCalculatedFieldCtxsByEntityId(profileId);
            for (CalculatedFieldCtx ctx : profileCfs) {
                if (filter.test(ctx)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void addCalculatedField(TenantId tenantId, CalculatedFieldId calculatedFieldId) {
        Lock lock = getFetchLock(calculatedFieldId);
        lock.lock();
        try {
            CalculatedField calculatedField = calculatedFieldService.findById(tenantId, calculatedFieldId);
            if (calculatedField == null) {
                return;
            }
            EntityId cfEntityId = calculatedField.getEntityId();

            calculatedFields.put(calculatedFieldId, calculatedField);

            entityIdCalculatedFields.computeIfAbsent(cfEntityId, entityId -> new CopyOnWriteArrayList<>()).add(calculatedField);

            CalculatedFieldConfiguration configuration = calculatedField.getConfiguration();
            calculatedFieldLinks.put(calculatedFieldId, configuration.buildCalculatedFieldLinks(tenantId, cfEntityId, calculatedFieldId));

            configuration.getReferencedEntities().stream()
                    .filter(referencedEntityId -> !referencedEntityId.equals(cfEntityId))
                    .forEach(referencedEntityId -> {
                        entityIdCalculatedFieldLinks.computeIfAbsent(referencedEntityId, entityId -> new CopyOnWriteArrayList<>())
                                .add(configuration.buildCalculatedFieldLink(tenantId, referencedEntityId, calculatedFieldId));
                    });
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void updateCalculatedField(TenantId tenantId, CalculatedFieldId calculatedFieldId) {
        evict(calculatedFieldId);
        addCalculatedField(tenantId, calculatedFieldId);
    }

    @Override
    public void evict(CalculatedFieldId calculatedFieldId) {
        CalculatedField oldCalculatedField = calculatedFields.remove(calculatedFieldId);
        log.debug("[{}] evict calculated field from cache: {}", calculatedFieldId, oldCalculatedField);
        calculatedFieldLinks.remove(calculatedFieldId);
        log.debug("[{}] evict calculated field from cached calculated fields by entity id: {}", calculatedFieldId, oldCalculatedField);
        entityIdCalculatedFields.forEach((entityId, calculatedFields) -> calculatedFields.removeIf(cf -> cf.getId().equals(calculatedFieldId)));
        log.debug("[{}] evict calculated field links from cache: {}", calculatedFieldId, oldCalculatedField);
        calculatedFieldsCtx.remove(calculatedFieldId);
        log.debug("[{}] evict calculated field ctx from cache: {}", calculatedFieldId, oldCalculatedField);
        entityIdCalculatedFieldLinks.forEach((entityId, calculatedFieldLinks) -> calculatedFieldLinks.removeIf(link -> link.calculatedFieldId().equals(calculatedFieldId)));
        log.debug("[{}] evict calculated field links from cached links by entity id: {}", calculatedFieldId, oldCalculatedField);
    }

    @Override
    public void handleTenantProfileUpdate() {
        calculatedFieldsCtx.values().forEach(CalculatedFieldCtx::updateTenantProfileProperties);
    }

    @Override
    public EntityId getProfileId(TenantId tenantId, EntityId entityId) {
        return switch (entityId.getEntityType()) {
            case ASSET -> assetProfileCache.get(tenantId, (AssetId) entityId).getId();
            case DEVICE -> deviceProfileCache.get(tenantId, (DeviceId) entityId).getId();
            default -> null;
        };
    }

    @Override
    public Set<EntityId> getDynamicEntities(TenantId tenantId, EntityId entityId) {
        if (entityId != null && entityId.getEntityType().isOneOf(EntityType.CUSTOMER, EntityType.TENANT)) {
            return getOwnedEntities(tenantId, entityId);
        }
        return Collections.emptySet();
    }

    @Override
    public void addOwnerEntity(TenantId tenantId, EntityId entityId) {
        EntityId owner = ownerService.getOwner(tenantId, entityId);
        getOwnedEntities(tenantId, owner).add(entityId);
    }

    @Override
    public void updateOwnerEntity(TenantId tenantId, EntityId entityId) {
        evictEntity(entityId);
        addOwnerEntity(tenantId, entityId);
    }

    @Override
    public void evictEntity(EntityId entityId) {
        ownerEntities.values().forEach(entities -> entities.remove(entityId));
    }

    @Override
    public void evictOwner(EntityId owner) {
        ownerEntities.remove(owner);
    }

    private Set<EntityId> getOwnedEntities(TenantId tenantId, EntityId ownerId) {
        return ownerEntities.computeIfAbsent(ownerId, owner -> {
            Set<EntityId> entities = ConcurrentHashMap.newKeySet();
            entities.addAll(ownerService.getOwnedEntities(tenantId, ownerId));
            return entities;
        });
    }

    private Lock getFetchLock(CalculatedFieldId id) {
        return calculatedFieldFetchLocks.computeIfAbsent(id, __ -> new ReentrantLock());
    }

}
