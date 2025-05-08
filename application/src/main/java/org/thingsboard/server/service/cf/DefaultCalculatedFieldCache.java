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
import org.springframework.stereotype.Service;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.msg.cf.CalculatedFieldInitMsg;
import org.thingsboard.server.common.msg.cf.CalculatedFieldLinkInitMsg;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultCalculatedFieldCache implements CalculatedFieldCache {

    private static final Integer UNKNOWN_PARTITION = -1;

    private final Lock calculatedFieldFetchLock = new ReentrantLock();

    private final CalculatedFieldService calculatedFieldService;
    private final TbelInvokeService tbelInvokeService;
    private final ActorSystemContext actorSystemContext;
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
        //TODO: move to separate place to avoid circular references with the ActorSystemContext (@Lazy for tsSubService)
        PageDataIterable<CalculatedField> cfs = new PageDataIterable<>(calculatedFieldService::findAllCalculatedFields, initFetchPackSize);
        cfs.forEach(cf -> {
            calculatedFields.putIfAbsent(cf.getId(), cf);
            actorSystemContext.tell(new CalculatedFieldInitMsg(cf.getTenantId(), cf));
        });
        calculatedFields.values().forEach(cf -> {
            entityIdCalculatedFields.computeIfAbsent(cf.getEntityId(), id -> new CopyOnWriteArrayList<>()).add(cf);
        });
        PageDataIterable<CalculatedFieldLink> cfls = new PageDataIterable<>(calculatedFieldService::findAllCalculatedFieldLinks, initFetchPackSize);
        cfls.forEach(link -> {
            calculatedFieldLinks.computeIfAbsent(link.getCalculatedFieldId(), id -> new CopyOnWriteArrayList<>()).add(link);
            actorSystemContext.tell(new CalculatedFieldLinkInitMsg(link.getTenantId(), link));
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
        return entityIdCalculatedFields.getOrDefault(entityId, new CopyOnWriteArrayList<>());
    }

    @Override
    public List<CalculatedFieldLink> getCalculatedFieldLinksByEntityId(EntityId entityId) {
        return entityIdCalculatedFieldLinks.getOrDefault(entityId, new CopyOnWriteArrayList<>());
    }

    @Override
    public CalculatedFieldCtx getCalculatedFieldCtx(CalculatedFieldId calculatedFieldId) {
        CalculatedFieldCtx ctx = calculatedFieldsCtx.get(calculatedFieldId);
        if (ctx == null) {
            calculatedFieldFetchLock.lock();
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
                calculatedFieldFetchLock.unlock();
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
        calculatedFieldFetchLock.lock();
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
            calculatedFieldFetchLock.unlock();
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

}
