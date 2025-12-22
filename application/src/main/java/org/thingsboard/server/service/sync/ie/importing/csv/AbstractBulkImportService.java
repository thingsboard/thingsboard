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
package org.thingsboard.server.service.sync.ie.importing.csv;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasAdditionalInfo;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.sync.ie.importing.csv.BulkImportColumnType;
import org.thingsboard.server.common.data.sync.ie.importing.csv.BulkImportRequest;
import org.thingsboard.server.common.data.sync.ie.importing.csv.BulkImportResult;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.util.TypeCastUtil;
import org.thingsboard.server.controller.BaseController;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.service.action.EntityActionService;
import org.thingsboard.server.service.security.AccessValidator;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;
import org.thingsboard.server.utils.CsvUtils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractBulkImportService<E extends HasId<? extends EntityId> & HasTenantId> {
    @Autowired
    private TelemetrySubscriptionService tsSubscriptionService;
    @Autowired
    private TbTenantProfileCache tenantProfileCache;
    @Autowired
    private AccessControlService accessControlService;
    @Autowired
    private AccessValidator accessValidator;
    @Autowired
    private EntityActionService entityActionService;

    private ExecutorService executor;

    @PostConstruct
    private void initExecutor() {
        executor = ThingsBoardExecutors.newLimitedTasksExecutor(Runtime.getRuntime().availableProcessors(), 150_000, "bulk-import");
    }

    public final BulkImportResult<E> processBulkImport(BulkImportRequest request, SecurityUser user) throws Exception {
        List<EntityData> entitiesData = parseData(request);

        BulkImportResult<E> result = new BulkImportResult<>();
        CountDownLatch completionLatch = new CountDownLatch(entitiesData.size());

        SecurityContext securityContext = SecurityContextHolder.getContext();

        entitiesData.forEach(entityData -> DonAsynchron.submit(() -> {
                    SecurityContextHolder.setContext(securityContext);

                    ImportedEntityInfo<E> importedEntityInfo = saveEntity(entityData.getFields(), user);
                    E entity = importedEntityInfo.getEntity();

                    if (request.getMapping().getUpdate() || !importedEntityInfo.isUpdated()) {
                        saveKvs(user, entity, entityData.getKvs());
                    }
                    return importedEntityInfo;
                },
                importedEntityInfo -> {
                    if (importedEntityInfo.isUpdated()) {
                        result.getUpdated().incrementAndGet();
                    } else {
                        result.getCreated().incrementAndGet();
                    }
                    completionLatch.countDown();
                },
                throwable -> {
                    result.getErrors().incrementAndGet();
                    result.getErrorsList().add(String.format("Line %d: %s", entityData.getLineNumber(), ExceptionUtils.getRootCauseMessage(throwable)));
                    completionLatch.countDown();
                },
                executor));

        completionLatch.await();
        return result;
    }

    @SneakyThrows
    private ImportedEntityInfo<E> saveEntity(Map<BulkImportColumnType, String> fields, SecurityUser user) {
        ImportedEntityInfo<E> importedEntityInfo = new ImportedEntityInfo<>();

        E entity = findOrCreateEntity(user.getTenantId(), fields.get(BulkImportColumnType.NAME));
        if (entity.getId() != null) {
            importedEntityInfo.setOldEntity((E) entity.getClass().getConstructor(entity.getClass()).newInstance(entity));
            importedEntityInfo.setUpdated(true);
            if (entity instanceof HasVersion versionedEntity) {
                versionedEntity.setVersion(null); // to overwrite the entity regardless of concurrent changes
            }
        } else {
            setOwners(entity, user);
        }

        setEntityFields(entity, fields);
        accessControlService.checkPermission(user, Resource.of(getEntityType()), Operation.WRITE, entity.getId(), entity);

        E savedEntity = saveEntity(user, entity, fields);

        importedEntityInfo.setEntity(savedEntity);
        return importedEntityInfo;
    }


    protected abstract E findOrCreateEntity(TenantId tenantId, String name);

    protected abstract void setOwners(E entity, SecurityUser user);

    protected abstract void setEntityFields(E entity, Map<BulkImportColumnType, String> fields);

    protected abstract E saveEntity(SecurityUser user, E entity, Map<BulkImportColumnType, String> fields);

    protected abstract EntityType getEntityType();

    protected ObjectNode getOrCreateAdditionalInfoObj(HasAdditionalInfo entity) {
        return entity.getAdditionalInfo() == null || entity.getAdditionalInfo().isNull() ?
                JacksonUtil.newObjectNode() : (ObjectNode) entity.getAdditionalInfo();
    }

    private void saveKvs(SecurityUser user, E entity, Map<BulkImportRequest.ColumnMapping, ParsedValue> data) {
        Arrays.stream(BulkImportColumnType.values())
                .filter(BulkImportColumnType::isKv)
                .map(kvType -> {
                    JsonObject kvs = new JsonObject();
                    data.entrySet().stream()
                            .filter(dataEntry -> dataEntry.getKey().getType() == kvType &&
                                    StringUtils.isNotEmpty(dataEntry.getKey().getKey()))
                            .forEach(dataEntry -> kvs.add(dataEntry.getKey().getKey(), dataEntry.getValue().toJsonPrimitive()));
                    return Map.entry(kvType, kvs);
                })
                .filter(kvsEntry -> !kvsEntry.getValue().entrySet().isEmpty())
                .forEach(kvsEntry -> {
                    BulkImportColumnType kvType = kvsEntry.getKey();
                    if (kvType == BulkImportColumnType.SHARED_ATTRIBUTE || kvType == BulkImportColumnType.SERVER_ATTRIBUTE) {
                        saveAttributes(user, entity, kvsEntry, kvType);
                    } else {
                        saveTelemetry(user, entity, kvsEntry);
                    }
                });
    }

    @SneakyThrows
    private void saveTelemetry(SecurityUser user, E entity, Map.Entry<BulkImportColumnType, JsonObject> kvsEntry) {
        List<TsKvEntry> timeseries = JsonConverter.convertToTelemetry(kvsEntry.getValue(), System.currentTimeMillis())
                .entrySet().stream()
                .flatMap(entry -> entry.getValue().stream().map(kvEntry -> new BasicTsKvEntry(entry.getKey(), kvEntry)))
                .collect(Collectors.toList());

        accessValidator.validateEntityAndCallback(user, Operation.WRITE_TELEMETRY, entity.getId(), (result, tenantId, entityId) -> {
            TenantProfile tenantProfile = tenantProfileCache.get(tenantId);
            long tenantTtl = TimeUnit.DAYS.toSeconds(((DefaultTenantProfileConfiguration) tenantProfile.getProfileData().getConfiguration()).getDefaultStorageTtlDays());
            tsSubscriptionService.saveTimeseries(TimeseriesSaveRequest.builder()
                    .tenantId(tenantId)
                    .customerId(user.getCustomerId())
                    .entityId(entityId)
                    .entries(timeseries)
                    .ttl(tenantTtl)
                    .callback(new FutureCallback<>() {
                        @Override
                        public void onSuccess(@Nullable Void tmp) {
                            entityActionService.logEntityAction(user, (UUIDBased & EntityId) entityId, null, null,
                                    ActionType.TIMESERIES_UPDATED, null, timeseries);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            entityActionService.logEntityAction(user, (UUIDBased & EntityId) entityId, null, null,
                                    ActionType.TIMESERIES_UPDATED, BaseController.toException(t), timeseries);
                            throw new RuntimeException(t);
                        }
                    })
                    .build());
        });
    }

    @SneakyThrows
    private void saveAttributes(SecurityUser user, E entity, Map.Entry<BulkImportColumnType, JsonObject> kvsEntry, BulkImportColumnType kvType) {
        String scope = kvType.getKey();
        List<AttributeKvEntry> attributes = JsonConverter.convertToAttributes(kvsEntry.getValue());

        accessValidator.validateEntityAndCallback(user, Operation.WRITE_ATTRIBUTES, entity.getId(), (result, tenantId, entityId) -> {
            tsSubscriptionService.saveAttributes(AttributesSaveRequest.builder()
                    .tenantId(tenantId)
                    .entityId(entityId)
                    .scope(AttributeScope.valueOf(scope))
                    .entries(attributes)
                    .callback(new FutureCallback<>() {
                        @Override
                        public void onSuccess(Void unused) {
                            entityActionService.logEntityAction(user, (UUIDBased & EntityId) entityId, null,
                                    null, ActionType.ATTRIBUTES_UPDATED, null, AttributeScope.valueOf(scope), attributes);
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            entityActionService.logEntityAction(user, (UUIDBased & EntityId) entityId, null,
                                    null, ActionType.ATTRIBUTES_UPDATED, BaseController.toException(throwable),
                                    AttributeScope.valueOf(scope), attributes);
                            throw new RuntimeException(throwable);
                        }
                    })
                    .build());
        });
    }

    private List<EntityData> parseData(BulkImportRequest request) throws Exception {
        List<List<String>> records = CsvUtils.parseCsv(request.getFile(), request.getMapping().getDelimiter());
        AtomicInteger linesCounter = new AtomicInteger(0);

        if (request.getMapping().getHeader()) {
            records.remove(0);
            linesCounter.incrementAndGet();
        }

        List<BulkImportRequest.ColumnMapping> columnsMappings = request.getMapping().getColumns();
        return records.stream()
                .map(record -> {
                    EntityData entityData = new EntityData();
                    Stream.iterate(0, i -> i < record.size(), i -> i + 1)
                            .map(i -> Map.entry(columnsMappings.get(i), record.get(i)))
                            .filter(entry -> StringUtils.isNotEmpty(entry.getValue()))
                            .forEach(entry -> {
                                if (!entry.getKey().getType().isKv()) {
                                    entityData.getFields().put(entry.getKey().getType(), entry.getValue());
                                } else {
                                    Pair<DataType, Object> castResult = TypeCastUtil.castValue(entry.getValue());
                                    entityData.getKvs().put(entry.getKey(), new ParsedValue(castResult.getValue(), castResult.getKey()));
                                }
                            });
                    entityData.setLineNumber(linesCounter.incrementAndGet());
                    return entityData;
                })
                .collect(Collectors.toList());
    }

    @PreDestroy
    private void shutdownExecutor() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Data
    protected static class EntityData {
        private final Map<BulkImportColumnType, String> fields = new LinkedHashMap<>();
        private final Map<BulkImportRequest.ColumnMapping, ParsedValue> kvs = new LinkedHashMap<>();
        private int lineNumber;

    }

    @Data
    protected static class ParsedValue {
        private final Object value;
        private final DataType dataType;

        public JsonPrimitive toJsonPrimitive() {
            return switch (dataType) {
                case STRING -> new JsonPrimitive((String) value);
                case LONG -> new JsonPrimitive((Long) value);
                case DOUBLE -> new JsonPrimitive((Double) value);
                case BOOLEAN -> new JsonPrimitive((Boolean) value);
                default -> null;
            };
        }

        public String stringValue() {
            return value.toString();
        }

    }

}
