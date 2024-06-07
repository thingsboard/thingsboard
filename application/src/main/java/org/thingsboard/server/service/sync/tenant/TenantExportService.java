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
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKv;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.LatestTsKv;
import org.thingsboard.server.common.data.kv.TsKv;
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
import org.thingsboard.server.dao.timeseries.TimeseriesDao;
import org.thingsboard.server.dao.timeseries.TimeseriesLatestDao;
import org.thingsboard.server.service.sync.tenant.util.DataWrapper;
import org.thingsboard.server.service.sync.tenant.util.Result;
import org.thingsboard.server.service.sync.tenant.util.ResultStore;
import org.thingsboard.server.service.sync.tenant.util.Storage;
import org.thingsboard.server.service.sync.tenant.util.TenantExportConfig;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static org.thingsboard.server.common.data.ObjectType.ATTRIBUTE_KV;
import static org.thingsboard.server.common.data.ObjectType.AUDIT_LOG;
import static org.thingsboard.server.common.data.ObjectType.EVENT;
import static org.thingsboard.server.common.data.ObjectType.LATEST_TS_KV;
import static org.thingsboard.server.common.data.ObjectType.RELATION;
import static org.thingsboard.server.common.data.ObjectType.TENANT;
import static org.thingsboard.server.common.data.ObjectType.TS_KV;
import static org.thingsboard.server.common.data.ObjectType.values;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantExportService {

    private final Storage storage;
    private final EntityDaoRegistry entityDaoRegistry;
    private final TenantDao tenantDao;
    private final EventDao eventDao;
    private final AuditLogDao auditLogDao;
    private final AttributesDao attributesDao;
    private final RelationDao relationDao;
    private final TimeseriesLatestDao timeseriesLatestDao;
    private final TimeseriesDao timeseriesDao;
    private final SqlPartitioningRepository partitioningRepository;
    private final CacheManager cacheManager;
    @Value("${cache.specs.tenantExportResults.timeToLiveInMinutes:1440}")
    private int resultsTtl;
    private ResultStore<ObjectType> resultStore;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("tenant-export"));

    private Map<ObjectType, BiConsumer<TenantId, TenantExportConfig>> customExporters;
    private Map<ObjectType, BiConsumer<TenantId, EntityId>> relatedEntitiesExporters;

    private static final Set<ObjectType> RELATED = EnumSet.of(EVENT, RELATION, ATTRIBUTE_KV, LATEST_TS_KV);
    private static final Set<ObjectType> IGNORED = EnumSet.of(TENANT);
    private static final List<ObjectType> EXPORTABLE = Arrays.stream(values())
            .filter(type -> !RELATED.contains(type) && !IGNORED.contains(type))
            .sorted().toList();

    @PostConstruct
    private void init() {
        resultStore = ResultStore.<ObjectType>builder()
                .name("Tenant export")
                .ttlInMinutes(resultsTtl)
                .persistFrequency(100)
                .removalListener((tenantId, result) -> {
                    if (result.isDone() && result.isSuccess()) {
                        storage.cleanUpExportData(tenantId);
                    }
                })
                .cacheName(CacheConstants.TENANT_EXPORT_RESULT_CACHE)
                .cacheManager(cacheManager)
                .build();
        relatedEntitiesExporters = Map.of(
                RELATION, this::exportRelations,
                EVENT, this::exportEvents, // todo: query by tenant
                ATTRIBUTE_KV, this::exportAttributes,
                LATEST_TS_KV, this::exportLatestTelemetry
        );
        customExporters = Map.of(
                TS_KV, this::exportTelemetry,
                AUDIT_LOG, this::exportAuditLogs
        );
    }

    public UUID exportTenant(TenantExportConfig exportConfig) {
        UUID tenantId = exportConfig.getTenantId();
        log.info("[{}] Exporting tenant", tenantId);
        Tenant tenant = tenantDao.findById(TenantId.SYS_TENANT_ID, tenantId);
        if (tenant == null) {
            throw new IllegalArgumentException("Tenant with id " + tenantId + " not found");
        }
        executor.submit(() -> {
            try {
                exportTenant(tenant, exportConfig);
                resultStore.update(tenantId, result -> {
                    result.setSuccess(true);
                    result.setDone(true);
                });
            } catch (Throwable t) {
                log.error("Failed to export tenant {}", tenant, t);
                try {
                    resultStore.update(tenantId, result -> {
                        result.setError(ExceptionUtils.getStackTrace(t));
                        result.getStats().clear();
                        result.setDone(true);
                    });
                    storage.cleanUpExportData(tenantId);
                } catch (Exception e) {
                    log.error("Failed to handle export error", e);
                }
            }
        });
        return tenantId;
    }

    private void exportTenant(Tenant tenant, TenantExportConfig exportConfig) {
        TenantId tenantId = tenant.getId();
        storage.init(tenantId.getId());

        export(tenantId, TENANT, tenant, exportConfig);
        for (ObjectType type : EXPORTABLE) {
            if (exportConfig.getSkipped().contains(type)) {
                continue;
            }

            log.debug("[{}] Exporting {} entities", tenantId, type);
            if (!customExporters.containsKey(type)) {
                TenantEntityDao<?> dao = entityDaoRegistry.getTenantEntityDao(type);
                var entities = new PageDataIterable<>(pageLink -> dao.findAllByTenantId(tenantId, pageLink), 100);
                for (Object entity : entities) {
                    export(tenantId, type, entity, exportConfig);
                }
            } else {
                customExporters.get(type).accept(tenantId, exportConfig);
            }
            resultStore.flush(tenantId.getId(), type);
        }
        resultStore.flush(tenantId.getId(), RELATED.toArray(ObjectType[]::new));

        storage.archiveExportData(tenantId.getId());
    }

    private void export(TenantId tenantId, ObjectType type, Object entity, TenantExportConfig exportConfig) {
        save(tenantId, type, entity);
        if (entity instanceof HasId<?> hasId && hasId.getId() instanceof EntityId entityId) {
            relatedEntitiesExporters.forEach((relatedEntityType, exporter) -> {
                if (!exportConfig.getSkipped().contains(relatedEntityType)) {
                    exporter.accept(tenantId, entityId);
                }
            });
        }
    }

    private void save(TenantId tenantId, ObjectType type, Object entity) {
        DataWrapper dataWrapper = DataWrapper.of(entity);

        storage.save(tenantId.getId(), type, dataWrapper);
        resultStore.report(tenantId.getId(), type);
        log.trace("[{}][{}] Saved entity {}", tenantId, type, entity);
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

    public Result<ObjectType> getResult(UUID tenantId) {
        return resultStore.getStoredResult(tenantId);
    }

    public void cancelExport(UUID tenantId) {
        boolean removed = storage.cleanUpExportData(tenantId);
        if (!removed) {
            throw new IllegalArgumentException("Not found");
        }
    }

    @SneakyThrows
    public ResponseEntity<InputStreamResource> downloadResult(UUID tenantId) {
        var result = resultStore.getStoredResult(tenantId);
        if (result == null || !result.isDone()) {
            throw new IllegalStateException("Not ready yet");
        } else if (!result.isSuccess()) {
            throw new IllegalStateException("Tenant export failed: " + result.getError());
        }

        String fileName = "tenant_export_data_" + tenantId + ".tar";
        return ResponseEntity.ok()
                .header("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + fileName)
                .header("x-filename", fileName)
                .body(new InputStreamResource(storage.downloadExportData(tenantId)));
    }


    private void exportTelemetry(TenantId tenantId, TenantExportConfig exportConfig) {
        storage.readAndProcess(LATEST_TS_KV, tenantId.getId(), dataWrapper -> {
            LatestTsKv latestTsKv = (LatestTsKv) dataWrapper.getEntity();
            EntityId entityId = latestTsKv.getEntityId();
            String key = latestTsKv.getEntry().getKey();

            // todo: submit for 10 latest kv, wait
            try {
                timeseriesDao.findAllAsync(tenantId, entityId, key, tsKvEntry -> {
                    TsKv tsKv = new TsKv(entityId, tsKvEntry, exportConfig.getTsKvTtl());
                    save(tenantId, TS_KV, tsKv);
                }).get(60, TimeUnit.MINUTES);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void exportAuditLogs(TenantId tenantId, TenantExportConfig exportConfig) {
        Map<Long, Long> partitions = getPartitions(ModelConstants.AUDIT_LOG_TABLE_NAME);
        partitions.forEach((startTime, endTime) -> {
            PageDataIterable<AuditLog> auditLogs = new PageDataIterable<>(pageLink -> {
                return auditLogDao.findAuditLogsByTenantId(tenantId.getId(), null, new TimePageLink(pageLink, startTime, endTime));
            }, 512);
            for (AuditLog auditLog : auditLogs) {
                save(tenantId, AUDIT_LOG, auditLog);
            }
        });
    }

    private void exportAttributes(TenantId tenantId, EntityId entityId) {
        for (AttributeScope attributeScope : AttributeScope.values()) {
            List<AttributeKvEntry> attributes = attributesDao.findAll(tenantId, entityId, attributeScope);
            for (AttributeKvEntry entry : attributes) {
                AttributeKv attributeKv = new AttributeKv(entityId, attributeScope, entry);
                save(tenantId, ATTRIBUTE_KV, attributeKv);
            }
        }
    }

    private void exportRelations(TenantId tenantId, EntityId entityId) {
        List<EntityRelation> relations = relationDao.findAllByFrom(tenantId, entityId);
        for (EntityRelation relation : relations) {
            save(tenantId, RELATION, relation);
        }
    }

    @SneakyThrows
    private void exportLatestTelemetry(TenantId tenantId, EntityId entityId) {
        List<TsKvEntry> latestTelemetry = timeseriesLatestDao.findAllLatest(tenantId, entityId).get(30, TimeUnit.SECONDS);
        for (TsKvEntry tsKvEntry : latestTelemetry) {
            LatestTsKv latestTsKv = new LatestTsKv(entityId, tsKvEntry);
            save(tenantId, LATEST_TS_KV, latestTsKv);
        }
    }

    private void exportEvents(TenantId tenantId, EntityId entityId) {
        for (EventType eventType : EventType.values()) {
            Map<Long, Long> partitions = getPartitions(eventType.getTable());
            partitions.forEach((startTime, endTime) -> {
                PageDataIterable<? extends Event> events = new PageDataIterable<>(pageLink -> {
                    return eventDao.findEvents(tenantId.getId(), entityId.getId(), eventType, new TimePageLink(pageLink, startTime, endTime));
                }, 512);
                for (Event event : events) {
                    save(tenantId, EVENT, event);
                }
            });
        }
    }

}
