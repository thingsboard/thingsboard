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
package org.thingsboard.server.dao.attributes;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edqs.AttributeKv;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.edqs.EdqsService;
import org.thingsboard.server.dao.service.Validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.attributes.AttributeUtils.validate;

/**
 * @author Andrew Shvayka
 */
@Service
@ConditionalOnProperty(prefix = "cache.attributes", value = "enabled", havingValue = "false", matchIfMissing = true)
@Primary
@Slf4j
@RequiredArgsConstructor
public class BaseAttributesService implements AttributesService {

    private final AttributesDao attributesDao;
    private final EdqsService edqsService;

    @Value("${sql.attributes.value_no_xss_validation:false}")
    private boolean valueNoXssValidation;

    @Override
    public ListenableFuture<Optional<AttributeKvEntry>> find(TenantId tenantId, EntityId entityId, AttributeScope scope, String attributeKey) {
        validate(entityId, scope);
        Validator.validateString(attributeKey, k -> "Incorrect attribute key " + k);
        return Futures.immediateFuture(attributesDao.find(tenantId, entityId, scope, attributeKey));
    }

    @Override
    public ListenableFuture<List<AttributeKvEntry>> find(TenantId tenantId, EntityId entityId, AttributeScope scope, Collection<String> attributeKeys) {
        validate(entityId, scope);
        attributeKeys.forEach(attributeKey -> Validator.validateString(attributeKey, k -> "Incorrect attribute key " + k));
        return Futures.immediateFuture(attributesDao.find(tenantId, entityId, scope, attributeKeys));
    }

    @Override
    public ListenableFuture<List<AttributeKvEntry>> findAll(TenantId tenantId, EntityId entityId, AttributeScope scope) {
        validate(entityId, scope);
        return Futures.immediateFuture(attributesDao.findAll(tenantId, entityId, scope));
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
    public ListenableFuture<Long> save(TenantId tenantId, EntityId entityId, AttributeScope scope, AttributeKvEntry attribute) {
        validate(entityId, scope);
        AttributeUtils.validate(attribute, valueNoXssValidation);
        return doSave(tenantId, entityId, scope, attribute);
    }

    @Override
    public ListenableFuture<List<Long>> save(TenantId tenantId, EntityId entityId, AttributeScope scope, List<AttributeKvEntry> attributes) {
        validate(entityId, scope);
        AttributeUtils.validate(attributes, valueNoXssValidation);
        List<ListenableFuture<Long>> saveFutures = attributes.stream().map(attribute -> doSave(tenantId, entityId, scope, attribute)).collect(Collectors.toList());
        return Futures.allAsList(saveFutures);
    }

    private ListenableFuture<Long> doSave(TenantId tenantId, EntityId entityId, AttributeScope scope, AttributeKvEntry attribute) {
        ListenableFuture<Long> future = attributesDao.save(tenantId, entityId, scope, attribute);
        return Futures.transform(future, version -> {
            edqsService.onUpdate(tenantId, ObjectType.ATTRIBUTE_KV, new AttributeKv(entityId, scope, attribute, version));
            return version;
        }, MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<List<String>> removeAll(TenantId tenantId, EntityId entityId, AttributeScope scope, List<String> attributeKeys) {
        validate(entityId, scope);
        List<ListenableFuture<TbPair<String, Long>>> futures = attributesDao.removeAllWithVersions(tenantId, entityId, scope, attributeKeys);
        return Futures.transform(Futures.allAsList(futures), result -> {
            List<String> keys = new ArrayList<>();
            for (TbPair<String, Long> keyVersionPair : result) {
                String key = keyVersionPair.getFirst();
                Long version = keyVersionPair.getSecond();
                if (version != null) {
                    edqsService.onDelete(tenantId, ObjectType.ATTRIBUTE_KV, new AttributeKv(entityId, scope, key, version));
                }
                keys.add(key);
            }
            return keys;
        }, MoreExecutors.directExecutor());
    }

    @Override
    public int removeAllByEntityId(TenantId tenantId, EntityId entityId) {
        List<Pair<AttributeScope, String>> deleted = attributesDao.removeAllByEntityId(tenantId, entityId);
        deleted.forEach(attribute -> {
            AttributeScope scope = attribute.getKey();
            String key = attribute.getValue();
            if (scope != null && key != null) {
                edqsService.onDelete(tenantId, ObjectType.ATTRIBUTE_KV, new AttributeKv(entityId, scope, key, Long.MAX_VALUE));
            }
        });
        return deleted.size();
    }

}
