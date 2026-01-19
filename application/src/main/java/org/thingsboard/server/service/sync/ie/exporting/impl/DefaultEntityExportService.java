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
package org.thingsboard.server.service.sync.ie.exporting.impl;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.configuration.AlarmCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.ArgumentsBasedCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.sync.ie.AttributeExportData;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.ie.exporting.EntityExportService;
import org.thingsboard.server.service.sync.ie.exporting.ExportableEntitiesService;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
@Primary
public class DefaultEntityExportService<I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> implements EntityExportService<I, E, D> {

    @Autowired
    @Lazy
    protected ExportableEntitiesService exportableEntitiesService;
    @Autowired
    private RelationDao relationDao;
    @Autowired
    private AttributesService attributesService;
    @Autowired
    private CalculatedFieldService calculatedFieldService;

    @Override
    public final D getExportData(EntitiesExportCtx<?> ctx, I entityId) throws ThingsboardException {
        D exportData = newExportData();

        E entity = exportableEntitiesService.findEntityByTenantIdAndId(ctx.getTenantId(), entityId);
        if (entity == null) {
            throw new IllegalArgumentException(entityId.getEntityType() + " [" + entityId.getId() + "] not found");
        }

        exportData.setEntity(entity);
        exportData.setEntityType(entityId.getEntityType());
        setAdditionalExportData(ctx, entity, exportData);
        if (entity instanceof HasVersion hasVersion) {
            hasVersion.setVersion(null);
        }

        var externalId = entity.getExternalId() != null ? entity.getExternalId() : entity.getId();
        ctx.putExternalId(entityId, externalId);
        entity.setId(externalId);
        entity.setTenantId(null);

        return exportData;
    }

    protected void setAdditionalExportData(EntitiesExportCtx<?> ctx, E entity, D exportData) throws ThingsboardException {
        var exportSettings = ctx.getSettings();
        if (exportSettings.isExportRelations()) {
            List<EntityRelation> relations = exportRelations(ctx, entity);
            relations.forEach(relation -> {
                relation.setFrom(getExternalIdOrElseInternal(ctx, relation.getFrom()));
                relation.setTo(getExternalIdOrElseInternal(ctx, relation.getTo()));
            });
            exportData.setRelations(relations);
        }
        if (exportSettings.isExportAttributes()) {
            Map<String, List<AttributeExportData>> attributes = exportAttributes(ctx, entity);
            exportData.setAttributes(attributes);
        }
        if (ctx.getSettings().isExportCalculatedFields()) {
            List<CalculatedField> calculatedFields = exportCalculatedFields(ctx, entity.getId());
            exportData.setCalculatedFields(calculatedFields);
        }
    }

    private List<EntityRelation> exportRelations(EntitiesExportCtx<?> ctx, E entity) throws ThingsboardException {
        List<EntityRelation> relations = new ArrayList<>();

        List<EntityRelation> inboundRelations = relationDao.findAllByTo(ctx.getTenantId(), entity.getId(), RelationTypeGroup.COMMON);
        relations.addAll(inboundRelations);

        List<EntityRelation> outboundRelations = relationDao.findAllByFrom(ctx.getTenantId(), entity.getId(), RelationTypeGroup.COMMON);
        relations.addAll(outboundRelations);
        return relations;
    }

    private Map<String, List<AttributeExportData>> exportAttributes(EntitiesExportCtx<?> ctx, E entity) throws ThingsboardException {
        List<AttributeScope> scopes;
        if (entity.getId().getEntityType() == EntityType.DEVICE) {
            scopes = List.of(AttributeScope.SERVER_SCOPE, AttributeScope.SHARED_SCOPE);
        } else {
            scopes = Collections.singletonList(AttributeScope.SERVER_SCOPE);
        }
        Map<String, List<AttributeExportData>> attributes = new LinkedHashMap<>();
        scopes.forEach(scope -> {
            try {
                attributes.put(scope.name(), attributesService.findAll(ctx.getTenantId(), entity.getId(), scope).get().stream()
                        .map(attribute -> {
                            AttributeExportData attributeExportData = new AttributeExportData();
                            attributeExportData.setKey(attribute.getKey());
                            attributeExportData.setLastUpdateTs(attribute.getLastUpdateTs());
                            attributeExportData.setStrValue(attribute.getStrValue().orElse(null));
                            attributeExportData.setDoubleValue(attribute.getDoubleValue().orElse(null));
                            attributeExportData.setLongValue(attribute.getLongValue().orElse(null));
                            attributeExportData.setBooleanValue(attribute.getBooleanValue().orElse(null));
                            attributeExportData.setJsonValue(attribute.getJsonValue().orElse(null));
                            return attributeExportData;
                        })
                        .collect(Collectors.toList()));
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        return attributes;
    }

    private List<CalculatedField> exportCalculatedFields(EntitiesExportCtx<?> ctx, EntityId entityId) {
        List<CalculatedField> calculatedFields = calculatedFieldService.findCalculatedFieldsByEntityId(ctx.getTenantId(), entityId);
        calculatedFields.forEach(calculatedField -> {
            calculatedField.setEntityId(getExternalIdOrElseInternal(ctx, entityId));
            if (calculatedField.getConfiguration() instanceof ArgumentsBasedCalculatedFieldConfiguration argBasedConfig) {
                if (argBasedConfig instanceof GeofencingCalculatedFieldConfiguration geofencingCfg) {
                    geofencingCfg.getZoneGroups().values().forEach(zoneGroupConfiguration -> {
                        if (zoneGroupConfiguration.getRefEntityId() != null) {
                            zoneGroupConfiguration.setRefEntityId(getExternalIdOrElseInternal(ctx, zoneGroupConfiguration.getRefEntityId()));
                        }
                    });
                } else {
                    argBasedConfig.getArguments().values().forEach(argument -> {
                        if (argument.getRefEntityId() != null) {
                            argument.setRefEntityId(getExternalIdOrElseInternal(ctx, argument.getRefEntityId()));
                        }
                    });
                }
            }
            if (calculatedField.getConfiguration() instanceof AlarmCalculatedFieldConfiguration alarmCfConfig) {
                alarmCfConfig.getAllRules().map(Pair::getValue).forEach(rule -> {
                    if (rule.getDashboardId() != null) {
                        rule.setDashboardId(getExternalIdOrElseInternal(ctx, rule.getDashboardId()));
                    }
                });
            }
        });
        return calculatedFields;
    }

    protected <ID extends EntityId> ID getExternalIdOrElseInternal(EntitiesExportCtx<?> ctx, ID internalId) {
        if (internalId == null || internalId.isNullUid()) return internalId;
        var result = ctx.getExternalId(internalId);
        if (result == null) {
            result = Optional.ofNullable(exportableEntitiesService.getExternalIdByInternal(internalId))
                    .orElse(internalId);
            ctx.putExternalId(internalId, result);
        }
        return result;
    }

    protected UUID getExternalIdOrElseInternalByUuid(EntitiesExportCtx<?> ctx, UUID internalUuid) {
        for (EntityType entityType : EntityType.values()) {
            EntityId internalId;
            try {
                internalId = EntityIdFactory.getByTypeAndUuid(entityType, internalUuid);
            } catch (Exception e) {
                continue;
            }
            EntityId externalId = ctx.getExternalId(internalId);
            if (externalId != null) {
                return externalId.getId();
            }
        }
        for (EntityType entityType : EntityType.values()) {
            EntityId internalId;
            try {
                internalId = EntityIdFactory.getByTypeAndUuid(entityType, internalUuid);
            } catch (Exception e) {
                continue;
            }
            EntityId externalId = exportableEntitiesService.getExternalIdByInternal(internalId);
            if (externalId != null) {
                ctx.putExternalId(internalId, externalId);
                return externalId.getId();
            }
        }
        return internalUuid;
    }

    protected D newExportData() {
        return (D) new EntityExportData<E>();
    }

}
