/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.service.importing;

import com.google.common.util.concurrent.FutureCallback;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.controller.BaseController;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.service.action.EntityActionService;
import org.thingsboard.server.service.importing.BulkImportRequest.ColumnMapping;
import org.thingsboard.server.service.queue.TbClusterService;
import org.thingsboard.server.service.security.AccessValidator;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;
import org.thingsboard.server.utils.CsvUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
public abstract class AbstractBulkImportService<E extends BaseData<? extends EntityId>> {
    protected final TelemetrySubscriptionService tsSubscriptionService;
    protected final TbTenantProfileCache tenantProfileCache;
    protected final AccessControlService accessControlService;
    protected final AccessValidator accessValidator;
    protected final EntityActionService entityActionService;
    protected final TbClusterService clusterService;

    public final BulkImportResult<E> processBulkImport(BulkImportRequest request, SecurityUser user, Consumer<ImportedEntityInfo<E>> onEntityImported) throws Exception {
        BulkImportResult<E> result = new BulkImportResult<>();

        AtomicInteger i = new AtomicInteger(0);
        if (request.getMapping().getHeader()) {
            i.incrementAndGet();
        }

        parseData(request).forEach(entityData -> {
            i.incrementAndGet();
            try {
                ImportedEntityInfo<E> importedEntityInfo = saveEntity(request, entityData, user);
                onEntityImported.accept(importedEntityInfo);

                E entity = importedEntityInfo.getEntity();

                saveKvs(user, entity, entityData);

                if (importedEntityInfo.getRelatedError() != null) {
                    throw new RuntimeException(importedEntityInfo.getRelatedError());
                }

                if (importedEntityInfo.isUpdated()) {
                    result.setUpdated(result.getUpdated() + 1);
                } else {
                    result.setCreated(result.getCreated() + 1);
                }
            } catch (Exception e) {
                result.setErrors(result.getErrors() + 1);
                result.getErrorsList().add(String.format("Line %d: %s", i.get(), e.getMessage()));
            }
        });

        return result;
    }

    protected abstract ImportedEntityInfo<E> saveEntity(BulkImportRequest importRequest, Map<ColumnMapping, String> entityData, SecurityUser user);

    /*
     * Attributes' values are firstly added to JsonObject in order to then make some type cast,
     * because we get all values as strings from CSV
     * */
    private void saveKvs(SecurityUser user, E entity, Map<ColumnMapping, String> data) {
        Stream.of(BulkImportColumnType.SHARED_ATTRIBUTE, BulkImportColumnType.SERVER_ATTRIBUTE, BulkImportColumnType.TIMESERIES)
                .map(kvType -> {
                    JsonObject kvs = new JsonObject();
                    data.entrySet().stream()
                            .filter(dataEntry -> dataEntry.getKey().getType() == kvType &&
                                    StringUtils.isNotEmpty(dataEntry.getKey().getKey()))
                            .forEach(dataEntry -> kvs.add(dataEntry.getKey().getKey(), new JsonPrimitive(dataEntry.getValue())));
                    return Map.entry(kvType, kvs);
                })
                .filter(kvsEntry -> kvsEntry.getValue().entrySet().size() > 0)
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
        List<TsKvEntry> timeseries = JsonConverter.convertToTelemetry(kvsEntry.getValue(), System.currentTimeMillis()).entrySet().stream()
                .flatMap(entry -> entry.getValue().stream().map(kvEntry -> new BasicTsKvEntry(entry.getKey(), kvEntry)))
                .collect(Collectors.toList());

        accessValidator.validateEntityAndCallback(user, Operation.WRITE_TELEMETRY, entity.getId(), (result, tenantId, entityId) -> {
            TenantProfile tenantProfile = tenantProfileCache.get(tenantId);
            long tenantTtl = TimeUnit.DAYS.toSeconds(((DefaultTenantProfileConfiguration) tenantProfile.getProfileData().getConfiguration()).getDefaultStorageTtlDays());
            tsSubscriptionService.saveAndNotify(tenantId, user.getCustomerId(), entityId, timeseries, tenantTtl, new FutureCallback<Void>() {
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
            });
        });
    }

    @SneakyThrows
    private void saveAttributes(SecurityUser user, E entity, Map.Entry<BulkImportColumnType, JsonObject> kvsEntry, BulkImportColumnType kvType) {
        String scope = kvType.getKey();
        List<AttributeKvEntry> attributes = new ArrayList<>(JsonConverter.convertToAttributes(kvsEntry.getValue()));

        accessValidator.validateEntityAndCallback(user, Operation.WRITE_ATTRIBUTES, entity.getId(), (result, tenantId, entityId) -> {
            tsSubscriptionService.saveAndNotify(tenantId, entityId, scope, attributes, new FutureCallback<>() {

                @Override
                public void onSuccess(Void unused) {
                    entityActionService.logEntityAction(user, (UUIDBased & EntityId) entityId, null,
                            null, ActionType.ATTRIBUTES_UPDATED, null, scope, attributes);
                }

                @Override
                public void onFailure(Throwable throwable) {
                    entityActionService.logEntityAction(user, (UUIDBased & EntityId) entityId, null,
                            null, ActionType.ATTRIBUTES_UPDATED, BaseController.toException(throwable),
                            scope, attributes);
                    throw new RuntimeException(throwable);
                }

            });
        });
    }

    protected final String getByColumnType(BulkImportColumnType bulkImportColumnType, Map<ColumnMapping, String> data) {
        return data.entrySet().stream().filter(entry -> entry.getKey().getType() == bulkImportColumnType).findFirst().map(Map.Entry::getValue).orElse(null);
    }

    private List<Map<ColumnMapping, String>> parseData(BulkImportRequest request) throws Exception {
        List<List<String>> records = CsvUtils.parseCsv(request.getFile(), request.getMapping().getDelimiter());
        if (request.getMapping().getHeader()) {
            records.remove(0);
        }

        List<ColumnMapping> columnsMappings = request.getMapping().getColumns();

        return records.stream()
                .map(record -> Stream.iterate(0, i -> i < record.size(), i -> i + 1)
                        .map(i -> Map.entry(columnsMappings.get(i), record.get(i)))
                        .filter(entry -> StringUtils.isNotEmpty(entry.getValue()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                .collect(Collectors.toList());
    }

}
