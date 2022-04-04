/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.sync.importing.impl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.service.sync.exporting.ExportableEntitiesService;
import org.thingsboard.server.service.sync.exporting.data.EntityExportData;
import org.thingsboard.server.service.sync.importing.EntityImportResult;
import org.thingsboard.server.service.sync.importing.EntityImportService;
import org.thingsboard.server.service.sync.importing.EntityImportSettings;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class BaseEntityImportService<I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> implements EntityImportService<I, E, D> {

    @Autowired @Lazy
    private ExportableEntitiesService exportableEntitiesService;
    @Autowired
    private RelationService relationService;

    @Transactional
    @Override
    public EntityImportResult<E> importEntity(TenantId tenantId, D exportData, EntityImportSettings importSettings) {
        E entity = exportData.getEntity();
        E existingEntity = exportableEntitiesService.findEntityByExternalId(tenantId, entity.getId());

        entity.setExternalId(entity.getId());

        if (existingEntity == null) {
            entity.setId(null);
        } else {
            entity.setId(existingEntity.getId());
        }

        E savedEntity = prepareAndSave(tenantId, entity, exportData, new NewIdProvider(entity, existingEntity, importSettings));
        importRelations(tenantId, savedEntity, existingEntity, exportData, importSettings);

        EntityImportResult<E> importResult = new EntityImportResult<>();
        importResult.setSavedEntity(savedEntity);
        importResult.setOldEntity(existingEntity);
        return importResult;
    }

    protected abstract E prepareAndSave(TenantId tenantId, E entity, D exportData, NewIdProvider idProvider);


    private void importRelations(TenantId tenantId, E savedEntity, E existingEntity, D exportData, EntityImportSettings importSettings) {
        List<EntityRelation> newRelations = new LinkedList<>();

        if (importSettings.isImportInboundRelations() && CollectionUtils.isNotEmpty(exportData.getInboundRelations())) {
            newRelations.addAll(exportData.getInboundRelations().stream()
                    .peek(relation -> {
                        relation.setTo(savedEntity.getId());
                        relation.setFrom(getInternalId(tenantId, relation.getFrom()));
                    })
                    .collect(Collectors.toList()));
            if (importSettings.isRemoveExistingRelations() && existingEntity != null) {
                relationService.findByTo(tenantId, savedEntity.getId(), RelationTypeGroup.COMMON).forEach(existingRelation -> {
                    relationService.deleteRelation(tenantId, existingRelation);
                });
            }
        }
        if (importSettings.isImportOutboundRelations() && CollectionUtils.isNotEmpty(exportData.getOutboundRelations())) {
            newRelations.addAll(exportData.getOutboundRelations().stream()
                    .peek(relation -> {
                        relation.setTo(getInternalId(tenantId, relation.getTo()));
                        relation.setFrom(savedEntity.getId());
                    })
                    .collect(Collectors.toList()));
            if (importSettings.isRemoveExistingRelations() && existingEntity != null) {
                relationService.findByFrom(tenantId, savedEntity.getId(), RelationTypeGroup.COMMON).forEach(existingRelation -> {
                    relationService.deleteRelation(tenantId, existingRelation);
                });
            }
        }

        newRelations.forEach(relation -> {
            relationService.saveRelation(tenantId, relation);
        });
    }

    private <ID extends EntityId> ID getInternalId(TenantId tenantId, ID externalId) {
        if (externalId == null || externalId.isNullUid()) {
            return null;
        }
        HasId<ID> entity = exportableEntitiesService.findEntityByExternalId(tenantId, externalId);
        if (entity == null) {
            throw new IllegalArgumentException("Cannot find " + externalId.getEntityType() + " by external id " + externalId);
        }
        return entity.getId();
    }

    @RequiredArgsConstructor
    protected class NewIdProvider {
        private final E entity;
        private final E existingEntity;
        private final EntityImportSettings importSettings;

        private final Set<EntityType> ALWAYS_UPDATE_REFERENCED_IDS = Set.of(
                EntityType.RULE_CHAIN
        );

        public <ID extends EntityId> ID get(TenantId tenantId, Function<E, ID> idExtractor) {
            if (existingEntity == null || importSettings.isUpdateReferencesToOtherEntities()
                    || ALWAYS_UPDATE_REFERENCED_IDS.contains(getEntityType())) {
                return getInternalId(tenantId, idExtractor.apply(entity));
            } else {
                return idExtractor.apply(existingEntity);
            }
        }

        public <ID extends EntityId, T> Set<T> get(TenantId tenantId, Function<E, Set<T>> listExtractor, Function<T, ID> idGetter, BiConsumer<T, ID> idSetter) {
            if (existingEntity == null || importSettings.isUpdateReferencesToOtherEntities()) {
                return Optional.ofNullable(listExtractor.apply(entity)).orElse(Collections.emptySet()).stream()
                        .peek(t -> {
                            idSetter.accept(t, getInternalId(tenantId, idGetter.apply(t)));
                        })
                        .collect(Collectors.toSet());
            } else {
                return listExtractor.apply(existingEntity);
            }
        }
    }

}
