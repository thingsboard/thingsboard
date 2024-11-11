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
package org.thingsboard.server.service.entitiy.cf;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.cf.BaseCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.cf.CalculatedFiledLinkConfiguration;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.service.Validator.validateEntityId;

@TbCoreComponent
@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultTbCalculatedFieldService extends AbstractTbEntityService implements TbCalculatedFieldService {

    private final CalculatedFieldService calculatedFieldService;
    private final TbDeviceProfileCache deviceProfileCache;
    private final TbAssetProfileCache assetProfileCache;
    private final AttributesService attributesService;
    private final TimeseriesService timeseriesService;
    private ListeningScheduledExecutorService scheduledExecutor;

    private ListeningExecutorService calculatedFieldExecutor;
    private ListeningExecutorService calculatedFieldCallbackExecutor;

    private final ConcurrentMap<CalculatedFieldId, CalculatedField> calculatedFields = new ConcurrentHashMap<>();
    private final ConcurrentMap<CalculatedFieldId, List<CalculatedFieldLink>> calculatedFieldLinks = new ConcurrentHashMap<>();
    private final ConcurrentMap<CalculatedFieldId, CalculatedFieldCtx> states = new ConcurrentHashMap<>();

    @Value("${state.initFetchPackSize:50000}")
    @Getter
    private int initFetchPackSize;

    @Value("10")
    @Getter
    private int defaultCalculatedFieldCheckIntervalInSec;

    @PostConstruct
    public void init() {
        // from AbstractPartitionBasedService
        scheduledExecutor = MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("calculated-field-scheduled")));
        ///
        calculatedFieldExecutor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(
                Math.max(4, Runtime.getRuntime().availableProcessors()), "calculated-field"));
        calculatedFieldCallbackExecutor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(
                Math.max(4, Runtime.getRuntime().availableProcessors()), "calculated-field-callback"));
        scheduledExecutor.scheduleWithFixedDelay(this::fetchCalculatedFields, new Random().nextInt(defaultCalculatedFieldCheckIntervalInSec), defaultCalculatedFieldCheckIntervalInSec, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void stop() {
        // from AbstractPartitionBasedService
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdown();
        }
        ///
        if (calculatedFieldExecutor != null) {
            calculatedFieldExecutor.shutdownNow();
        }
        if (calculatedFieldCallbackExecutor != null) {
            calculatedFieldCallbackExecutor.shutdownNow();
        }
    }

    private ListenableFuture<List<AttributeKvEntry>> fetchAttributesForEntity(TenantId tenantId, EntityId entityId, List<String> keys) {
        return attributesService.find(tenantId, entityId, AttributeScope.SERVER_SCOPE, keys);
    }

    private ListenableFuture<List<TsKvEntry>> fetchTimeSeries(TenantId tenantId, EntityId entityId, List<String> keys) {
        return timeseriesService.findLatest(tenantId, entityId, keys);
    }

    private ListenableFuture<Void> initializeStateFromFutures(TenantId tenantId, EntityId entityId, CalculatedField calculatedField, List<String> attributeKeys, List<String> timeSeriesKeys) {
        ListenableFuture<List<AttributeKvEntry>> attributesFuture = fetchAttributesForEntity(tenantId, entityId, attributeKeys);
        ListenableFuture<List<TsKvEntry>> timeSeriesFuture = fetchTimeSeries(tenantId, entityId, timeSeriesKeys);

        ListenableFuture<List<Object>> combinedFuture = Futures.allAsList(attributesFuture, timeSeriesFuture);

        return Futures.transform(combinedFuture, results -> {
            List<AttributeKvEntry> attributes = (List<AttributeKvEntry>) results.get(0);
            List<TsKvEntry> timeSeries = (List<TsKvEntry>) results.get(1);

            initializeState(calculatedField, attributes, timeSeries);

            return null;
        }, MoreExecutors.directExecutor());
    }

    private void initializeState(CalculatedField calculatedField, List<AttributeKvEntry> attributes, List<TsKvEntry> timeSeries) {
        CalculatedFieldCtx calculatedFieldCtx = states.computeIfAbsent(calculatedField.getId(),
                ctx -> new CalculatedFieldCtx(calculatedField.getId(), calculatedField.getEntityId(), null));

        CalculatedFieldState state = calculatedFieldCtx.getState();

        if (state != null) {
            String calculation = performCalculation(state.getArguments());

            Map<String, String> updatedArguments = state.getArguments();

            state = CalculatedFieldState.builder()
                    .arguments(updatedArguments)
                    .result(calculation)
                    .build();
        } else {
            // initial calculation
            Map<String, BaseCalculatedFieldConfiguration.Argument> arguments = calculatedField.getConfiguration().getArguments();

            Map<String, String> argumentValues = arguments.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> resolveArgumentValue(entry.getKey(), entry.getValue(), attributes, timeSeries)
                    ));

            String calculation = performCalculation(argumentValues);

            state = CalculatedFieldState.builder()
                    .arguments(argumentValues)
                    .result(calculation)
                    .build();
        }

        calculatedFieldCtx = new CalculatedFieldCtx(calculatedField.getId(), calculatedField.getEntityId(), state);
        states.put(calculatedField.getId(), calculatedFieldCtx);
    }

    private String resolveArgumentValue(String key, BaseCalculatedFieldConfiguration.Argument argument,
                                        List<AttributeKvEntry> attributes, List<TsKvEntry> timeSeries) {
        String type = argument.getType();
        String value = null;

        if ("ATTRIBUTES".equals(type)) {
            value = attributes.stream()
                    .filter(attribute -> attribute.getKey().equals(key))
                    .map(AttributeKvEntry::getValueAsString)
                    .findFirst()
                    .orElse(null);
        } else if ("TIME_SERIES".equals(type)) {
            value = timeSeries.stream()
                    .filter(tsKvEntry -> tsKvEntry.getKey().equals(key))
                    .map(TsKvEntry::getValueAsString)
                    .findFirst()
                    .orElse(null);
        }

        return value != null ? value : argument.getDefaultValue();
    }

    @Override
    public void onCalculatedFieldAdded(TransportProtos.CalculatedFieldAddMsgProto proto, TbCallback callback) {
        try {
            TenantId tenantId = TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB()));
            CalculatedFieldId calculatedFieldId = new CalculatedFieldId(new UUID(proto.getCalculatedFieldIdMSB(), proto.getCalculatedFieldIdLSB()));
            CalculatedField cf = calculatedFieldService.findById(tenantId, calculatedFieldId);
            List<CalculatedFieldLink> links = calculatedFieldService.findAllCalculatedFieldLinksById(tenantId, calculatedFieldId);
            if (cf != null) {
                EntityId entityId = cf.getEntityId();
                calculatedFields.put(calculatedFieldId, cf);
                calculatedFieldLinks.put(calculatedFieldId, links);
                switch (entityId.getEntityType()) {
                    case ASSET, DEVICE: {
                        for (CalculatedFieldLink link : links) {
                            CalculatedFiledLinkConfiguration configuration = link.getConfiguration();
                            initializeStateFromFutures(tenantId, link.getEntityId(), cf, configuration.getAttributes(), configuration.getTimeSeries());
                        }
                    }
                    case ASSET_PROFILE: {
                        PageDataIterable<AssetId> assetIds = new PageDataIterable<>(pageLink ->
                                assetService.findAssetIdsByTenantIdAndAssetProfileId(tenantId, (AssetProfileId) entityId, pageLink), initFetchPackSize);
                        for (AssetId assetId : assetIds) {
                            for (CalculatedFieldLink link : links) {
                                CalculatedFiledLinkConfiguration configuration = link.getConfiguration();
                                initializeStateFromFutures(tenantId, assetId, cf, configuration.getAttributes(), configuration.getTimeSeries());
                            }
                        }
                    }
                    case DEVICE_PROFILE: {
                        PageDataIterable<DeviceId> deviceIds = new PageDataIterable<>(pageLink ->
                                deviceService.findDeviceIdsByTenantIdAndDeviceProfileId(tenantId, (DeviceProfileId) entityId, pageLink), initFetchPackSize);
                        for (DeviceId deviceId : deviceIds) {
                            for (CalculatedFieldLink link : links) {
                                CalculatedFiledLinkConfiguration configuration = link.getConfiguration();
                                initializeStateFromFutures(tenantId, deviceId, cf, configuration.getAttributes(), configuration.getTimeSeries());
                            }
                        }
                    }
                    default: throw new IllegalArgumentException("Entity type '" + calculatedFieldId.getEntityType() + "' does not support calculated fields.");
                }
            } else {
                //Calculated field or entity was probably deleted while message was in queue;
                callback.onSuccess();
            }
        } catch (Exception e) {
            log.trace("Failed to process calculated field add msg: [{}]", proto, e);
            callback.onFailure(e);
        }
    }

    @Override
    public void onCalculatedFieldUpdated(TransportProtos.CalculatedFieldUpdateMsgProto proto, TbCallback callback) {
        try {
            TenantId tenantId = TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB()));
            CalculatedFieldId calculatedFieldId = new CalculatedFieldId(new UUID(proto.getCalculatedFieldIdMSB(), proto.getCalculatedFieldIdLSB()));
        } catch (Exception e) {
            log.trace("Failed to process calculated field update msg: [{}]", proto, e);
            callback.onFailure(e);
        }
    }

    @Override
    public void onCalculatedFieldDeleted(TransportProtos.CalculatedFieldDeleteMsgProto proto, TbCallback callback) {
        try {
            CalculatedFieldId calculatedFieldId = new CalculatedFieldId(new UUID(proto.getCalculatedFieldIdMSB(), proto.getCalculatedFieldIdLSB()));
            calculatedFieldLinks.remove(calculatedFieldId);
            calculatedFields.remove(calculatedFieldId);
            states.remove(calculatedFieldId);
        } catch (Exception e) {
            log.trace("Failed to process calculated field delete msg: [{}]", proto, e);
            callback.onFailure(e);
        }
    }

    @Override
    public CalculatedField save(CalculatedField calculatedField, SecurityUser user) throws ThingsboardException {
        ActionType actionType = calculatedField.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = calculatedField.getTenantId();
        try {
            checkEntityExistence(tenantId, calculatedField.getEntityId());
            checkReferencedEntities(calculatedField.getConfiguration(), user);
            CalculatedField savedCalculatedField = checkNotNull(calculatedFieldService.save(calculatedField));
            logEntityActionService.logEntityAction(tenantId, savedCalculatedField.getId(), savedCalculatedField, actionType, user);
            return savedCalculatedField;
        } catch (ThingsboardException e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.CALCULATED_FIELD), calculatedField, actionType, user, e);
            throw e;
        }
    }

    @Override
    public CalculatedField findById(CalculatedFieldId calculatedFieldId, SecurityUser user) {
        return calculatedFieldService.findById(user.getTenantId(), calculatedFieldId);
    }

    @Override
    @Transactional
    public void delete(CalculatedField calculatedField, SecurityUser user) {
        ActionType actionType = ActionType.DELETED;
        TenantId tenantId = calculatedField.getTenantId();
        CalculatedFieldId calculatedFieldId = calculatedField.getId();
        try {
            calculatedFieldService.deleteCalculatedField(tenantId, calculatedFieldId);
            logEntityActionService.logEntityAction(tenantId, calculatedFieldId, calculatedField, actionType, user, calculatedFieldId.toString());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.CALCULATED_FIELD), actionType, user, e, calculatedFieldId.toString());
            throw e;
        }
    }

    private void fetchCalculatedFields() {
        PageDataIterable<CalculatedField> cfs = new PageDataIterable<>(calculatedFieldService::findAllCalculatedFields, initFetchPackSize);
        cfs.forEach(cf -> calculatedFields.putIfAbsent(cf.getId(), cf));
        PageDataIterable<CalculatedFieldLink> cfls = new PageDataIterable<>(calculatedFieldService::findAllCalculatedFieldLinks, initFetchPackSize);
        cfls.forEach(link -> calculatedFieldLinks.computeIfAbsent(link.getCalculatedFieldId(), id -> new ArrayList<>()).add(link));
        // TODO:    read all states(CalculatedFieldCtx)
        states.keySet().removeIf(calculatedFieldId -> !calculatedFields.containsKey(calculatedFieldId));
    }

    private void checkEntityExistence(TenantId tenantId, EntityId entityId) {
        switch (entityId.getEntityType()) {
            case ASSET, DEVICE, ASSET_PROFILE, DEVICE_PROFILE -> Optional.ofNullable(entityService.fetchEntity(tenantId, entityId))
                    .orElseThrow(() -> new IllegalArgumentException(entityId.getEntityType().getNormalName() + " with id [" + entityId.getId() + "] does not exist."));
            default ->
                    throw new IllegalArgumentException("Entity type '" + entityId.getEntityType() + "' does not support calculated fields.");
        }
    }

    private <E extends HasId<I> & HasTenantId, I extends EntityId> void checkReferencedEntities(CalculatedFieldConfiguration calculatedFieldConfig, SecurityUser user) throws ThingsboardException {
        List<EntityId> referencedEntityIds = calculatedFieldConfig.getReferencedEntities();
        for (EntityId referencedEntityId : referencedEntityIds) {
            validateEntityId(referencedEntityId, id -> "Invalid entity id " + id);
            E entity = findEntity(user.getTenantId(), referencedEntityId);
            checkNotNull(entity);
            checkEntity(user, entity, Operation.READ);
        }

    }

    private <E extends HasId<I> & HasTenantId, I extends EntityId> E findEntity(TenantId tenantId, EntityId entityId) {
        return switch (entityId.getEntityType()) {
            case TENANT, CUSTOMER, ASSET, DEVICE -> (E) entityService.fetchEntity(tenantId, entityId).orElse(null);
            default -> throw new IllegalArgumentException("Calculated fields do not support entity type '" + entityId.getEntityType() + "' for referenced entities.");
        };
    }

    private String performCalculation(Map<String, String> argumentValues) {
        return "calculation";
    }

}
