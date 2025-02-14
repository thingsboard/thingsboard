/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.sync.tenant;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.edqs.AttributeKv;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.edqs.LatestTsKv;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.dao.TenantEntityDao;
import org.thingsboard.server.dao.attributes.AttributesDao;
import org.thingsboard.server.dao.audit.AuditLogDao;
import org.thingsboard.server.dao.entity.EntityDaoRegistry;
import org.thingsboard.server.dao.event.EventDao;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.dao.timeseries.TimeseriesLatestDao;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static org.thingsboard.server.common.data.ObjectType.ATTRIBUTE_KV;
import static org.thingsboard.server.common.data.ObjectType.AUDIT_LOG;
import static org.thingsboard.server.common.data.ObjectType.EVENT;
import static org.thingsboard.server.common.data.ObjectType.LATEST_TS_KV;
import static org.thingsboard.server.common.data.ObjectType.RELATION;
import static org.thingsboard.server.common.data.ObjectType.TENANT;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantExportService {

    private final EntityDaoRegistry entityDaoRegistry;
    private final TenantDao tenantDao;
    private final EventDao eventDao;
    private final AuditLogDao auditLogDao;
    private final AttributesDao attributesDao;
    private final RelationDao relationDao;
    private final TimeseriesLatestDao timeseriesLatestDao;
    private final SqlPartitioningRepository partitioningRepository;

    private Map<ObjectType, BiConsumer<TenantId, BiConsumer<ObjectType, Object>>> customExporters;
    private Map<ObjectType, Exporter> relatedEntitiesExporters;

    private static final Set<ObjectType> RELATED = EnumSet.of(EVENT, RELATION, ATTRIBUTE_KV, LATEST_TS_KV);

    @PostConstruct
    private void init() {
        relatedEntitiesExporters = Map.of(
                RELATION, this::exportRelations,
                EVENT, this::exportEvents, // todo: query by tenant
                ATTRIBUTE_KV, this::exportAttributes,
                LATEST_TS_KV, this::exportLatestTelemetry
        );
        customExporters = Map.of(
                AUDIT_LOG, this::exportAuditLogs
        );
    }

    public void exportTenant(TenantId tenantId, ExportConfig config, BiConsumer<ObjectType, Object> processor) {
        log.info("[{}] Exporting tenant", tenantId);
        Tenant tenant = tenantDao.findById(TenantId.SYS_TENANT_ID, tenantId.getId());
        if (tenant == null) {
            throw new IllegalArgumentException("Tenant with id " + tenantId + " not found");
        }

        Set<ObjectType> objectTypes = config.getIncludedObjectTypes();
        if (objectTypes.contains(TENANT)) {
            exportEntity(tenantId, TENANT, tenant, config, processor);
        }

        for (ObjectType type : objectTypes) {
            if (RELATED.contains(type) || type == TENANT) {
                continue;
            }
            log.debug("[{}] Exporting {} entities", tenantId, type);
            if (!customExporters.containsKey(type)) {
                TenantEntityDao<?> dao = entityDaoRegistry.getTenantEntityDao(type);
                var entities = new PageDataIterable<>(pageLink -> dao.findAllByTenantId(tenantId, pageLink), 100);
                for (Object entity : entities) {
                    exportEntity(tenantId, type, entity, config, processor);
                }
            } else {
                customExporters.get(type).accept(tenantId, processor);
            }
        }
    }

    private void exportEntity(TenantId tenantId, ObjectType type, Object entity, ExportConfig config, BiConsumer<ObjectType, Object> processor) {
        processor.accept(type, entity);
        if (entity instanceof HasId<?> hasId && hasId.getId() instanceof EntityId entityId) {
            relatedEntitiesExporters.forEach((relatedEntityType, exporter) -> {
                if (config.getIncludedObjectTypes().contains(relatedEntityType)) {
                    exporter.export(tenantId, entityId, processor);
                }
            });
        }
    }

    private Map<Long, Long> getPartitions(String table) {
        List<Long> partitionsStartTime = partitioningRepository.fetchPartitions(table).stream().sorted().toList();
        if (partitionsStartTime.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Long> partitions = new HashMap<>();
        for (int i = 0; i < partitionsStartTime.size(); i++) {
            Long startTime = partitionsStartTime.get(i);
            Long endTime;
            if (partitionsStartTime.size() - 1 == i) {
                endTime = System.currentTimeMillis();
            } else {
                endTime = partitionsStartTime.get(i + 1) - 1;
            }
            partitions.put(startTime, endTime);
        }
        return partitions;
    }

    private void exportAuditLogs(TenantId tenantId, BiConsumer<ObjectType, Object> processor) {
        Map<Long, Long> partitions = getPartitions(ModelConstants.AUDIT_LOG_TABLE_NAME);
        partitions.forEach((startTime, endTime) -> {
            PageDataIterable<AuditLog> auditLogs = new PageDataIterable<>(pageLink -> {
                return auditLogDao.findAuditLogsByTenantId(tenantId.getId(), null, new TimePageLink(pageLink, startTime, endTime));
            }, 512);
            for (AuditLog auditLog : auditLogs) {
                processor.accept(AUDIT_LOG, auditLog);
            }
        });
    }

    private void exportAttributes(TenantId tenantId, EntityId entityId, BiConsumer<ObjectType, Object> processor) {
        for (AttributeScope attributeScope : AttributeScope.values()) {
            List<AttributeKvEntry> attributes = attributesDao.findAll(tenantId, entityId, attributeScope);
            for (AttributeKvEntry entry : attributes) {
                AttributeKv attributeKv = new AttributeKv(entityId, attributeScope, entry, entry.getVersion());
                processor.accept(ATTRIBUTE_KV, attributeKv);
            }
        }
    }

    private void exportRelations(TenantId tenantId, EntityId entityId, BiConsumer<ObjectType, Object> processor) {
        List<EntityRelation> relations = relationDao.findAllByFrom(tenantId, entityId);
        for (EntityRelation relation : relations) {
            processor.accept(RELATION, relation);
        }
    }

    @SneakyThrows
    private void exportLatestTelemetry(TenantId tenantId, EntityId entityId, BiConsumer<ObjectType, Object> processor) {
        List<TsKvEntry> latestTelemetry = timeseriesLatestDao.findAllLatest(tenantId, entityId).get(30, TimeUnit.SECONDS);
        for (TsKvEntry tsKvEntry : latestTelemetry) {
            LatestTsKv latestTsKv = new LatestTsKv(entityId, tsKvEntry, tsKvEntry.getVersion());
            processor.accept(LATEST_TS_KV, latestTsKv);
        }
    }

    private void exportEvents(TenantId tenantId, EntityId entityId, BiConsumer<ObjectType, Object> processor) {
        for (EventType eventType : EventType.values()) {
            Map<Long, Long> partitions = getPartitions(eventType.getTable());
            partitions.forEach((startTime, endTime) -> {
                PageDataIterable<? extends Event> events = new PageDataIterable<>(pageLink -> {
                    return eventDao.findEvents(tenantId.getId(), entityId.getId(), eventType, new TimePageLink(pageLink, startTime, endTime));
                }, 512);
                for (Event event : events) {
                    processor.accept(EVENT, event);
                }
            });
        }
    }

    private interface Exporter {

        void export(TenantId tenantId, EntityId entityId, BiConsumer<ObjectType, Object> processor);

    }

    @Data
    public static class ExportConfig {

        private Set<ObjectType> includedObjectTypes;

    }

}
