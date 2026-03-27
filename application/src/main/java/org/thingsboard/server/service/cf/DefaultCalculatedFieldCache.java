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
package org.thingsboard.server.service.cf;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultCalculatedFieldCache implements CalculatedFieldCache {

    private final ConcurrentReferenceHashMap<CalculatedFieldId, Lock> calculatedFieldFetchLocks = new ConcurrentReferenceHashMap<>();

    private final CalculatedFieldService calculatedFieldService;
    private final TbelInvokeService tbelInvokeService;
    private final ApiLimitService apiLimitService;

    private final ConcurrentMap<CalculatedFieldId, CalculatedField> calculatedFields = new ConcurrentHashMap<>();
    private final ConcurrentMap<EntityId, List<CalculatedField>> entityIdCalculatedFields = new ConcurrentHashMap<>();
    private final ConcurrentMap<CalculatedFieldId, List<CalculatedFieldLink>> calculatedFieldLinks = new ConcurrentHashMap<>();
    private final ConcurrentMap<EntityId, List<CalculatedFieldLink>> entityIdCalculatedFieldLinks = new ConcurrentHashMap<>();
    private final ConcurrentMap<CalculatedFieldId, CalculatedFieldCtx> calculatedFieldsCtx = new ConcurrentHashMap<>();

    @Value("${queue.calculated_fields.init_fetch_pack_size:50000}")
    @Getter
    private int initFetchPackSize;

    @AfterStartUp(order = AfterStartUp.CF_READ_CF_SERVICE)
    public void init() {
        PageDataIterable<CalculatedField> cfs = new PageDataIterable<>(calculatedFieldService::findAllCalculatedFields, initFetchPackSize);
        cfs.forEach(cf -> {
            if (cf != null) {
                calculatedFields.putIfAbsent(cf.getId(), cf);
            }
        });
        calculatedFields.values().forEach(cf -> {
            entityIdCalculatedFields.computeIfAbsent(cf.getEntityId(), id -> new CopyOnWriteArrayList<>()).add(cf);
        });
        PageDataIterable<CalculatedFieldLink> cfls = new PageDataIterable<>(calculatedFieldService::findAllCalculatedFieldLinks, initFetchPackSize);
        cfls.forEach(link -> {
            calculatedFieldLinks.computeIfAbsent(link.getCalculatedFieldId(), id -> new CopyOnWriteArrayList<>()).add(link);
        });
        calculatedFieldLinks.values().stream()
                .flatMap(List::stream)
                .forEach(link ->
                        entityIdCalculatedFieldLinks.computeIfAbsent(link.getEntityId(), id -> new CopyOnWriteArrayList<>()).add(link)
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
                        ctx = new CalculatedFieldCtx(calculatedField, tbelInvokeService, apiLimitService);
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
        entityIdCalculatedFieldLinks.forEach((entityId, calculatedFieldLinks) -> calculatedFieldLinks.removeIf(link -> link.getCalculatedFieldId().equals(calculatedFieldId)));
        log.debug("[{}] evict calculated field links from cached links by entity id: {}", calculatedFieldId, oldCalculatedField);
    }

    @EventListener(ComponentLifecycleMsg.class)
    public void onComponentLifecycleEvent(ComponentLifecycleMsg event) {
        switch (event.getEntityId().getEntityType()) {
            case TENANT:
                if (event.getEvent() == ComponentLifecycleEvent.DELETED) {
                    evictTenantCfs(event.getTenantId());
                }
                break;
            case DEVICE, ASSET, DEVICE_PROFILE, ASSET_PROFILE:
                if (event.getEvent() == ComponentLifecycleEvent.DELETED) {
                    evictEntityCfs(event.getEntityId());
                }
                break;
            case CALCULATED_FIELD:
                if (event.getEvent() == ComponentLifecycleEvent.CREATED) {
                    addCalculatedField(event.getTenantId(), (CalculatedFieldId) event.getEntityId());
                } else if (event.getEvent() == ComponentLifecycleEvent.UPDATED) {
                    updateCalculatedField(event.getTenantId(), (CalculatedFieldId) event.getEntityId());
                } else if (event.getEvent() == ComponentLifecycleEvent.DELETED) {
                    evict((CalculatedFieldId) event.getEntityId());
                }
                break;
        }
    }

    private void evictTenantCfs(TenantId tenantId) {
        var removedCfEntityIds = new HashSet<EntityId>();
        var removedLinkEntityIds = new HashSet<EntityId>();
        var toRemove = calculatedFields.entrySet().stream()
                .filter(e -> e.getValue().getTenantId().equals(tenantId))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        toRemove.forEach(cfId -> {
            CalculatedField cf = calculatedFields.remove(cfId);
            List<CalculatedFieldLink> links = calculatedFieldLinks.remove(cfId);
            if (links != null) {
                links.forEach(link -> removedLinkEntityIds.add(link.getEntityId()));
            }
            calculatedFieldsCtx.remove(cfId);
            if (cf != null) {
                removedCfEntityIds.add(cf.getEntityId());
            }
        });
        removedCfEntityIds.forEach(entityId -> {
            entityIdCalculatedFields.compute(entityId, (k, cfs) -> {
                if (cfs != null) {
                    cfs.removeIf(cf -> toRemove.contains(cf.getId()));
                    return cfs.isEmpty() ? null : cfs;
                }
                return null;
            });
        });
        removedLinkEntityIds.forEach(entityId -> {
            entityIdCalculatedFieldLinks.compute(entityId, ((entityId1, links) -> {
                if (links != null) {
                    links.removeIf(link -> toRemove.contains(link.getCalculatedFieldId()));
                    return links.isEmpty() ? null : links;
                }
                return null;
            }));
        });
    }

    private void evictEntityCfs(EntityId entityId) {
        List<CalculatedField> cfs = entityIdCalculatedFields.remove(entityId);
        if (cfs != null) {
            var cfIds = new HashSet<CalculatedFieldId>();
            cfs.forEach(cf -> {
                calculatedFields.remove(cf.getId());
                calculatedFieldLinks.remove(cf.getId());
                calculatedFieldsCtx.remove(cf.getId());
                cfIds.add(cf.getId());
                log.debug("[{}] evict calculated field from cache on entity deletion: {}", cf.getId(), cf);
            });
            entityIdCalculatedFieldLinks.values().forEach(list -> list.removeIf(link -> cfIds.contains(link.getCalculatedFieldId())));
        }
        entityIdCalculatedFieldLinks.remove(entityId);
    }

    private Lock getFetchLock(CalculatedFieldId id) {
        return calculatedFieldFetchLocks.computeIfAbsent(id, __ -> new ReentrantLock());
    }

}
