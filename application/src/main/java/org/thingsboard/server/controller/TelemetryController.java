/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.rule.engine.api.msg.DeviceAttributesEventNotificationMsg;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.AttributeKey;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.msg.cluster.SendToClusterMsg;
import org.thingsboard.server.common.msg.core.TelemetryUploadRequest;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.service.security.AccessValidator;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.telemetry.AttributeData;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;
import org.thingsboard.server.service.telemetry.TsData;
import org.thingsboard.server.service.telemetry.exception.InvalidParametersException;
import org.thingsboard.server.service.telemetry.exception.UncheckedApiException;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Created by ashvayka on 22.03.18.
 */
@RestController
@RequestMapping(TbUrlConstants.TELEMETRY_URL_PREFIX)
@Slf4j
public class TelemetryController extends BaseController {

    @Autowired
    private TimeseriesService tsService;

    @Autowired
    private AccessValidator accessValidator;

    private ExecutorService executor;

    @PostConstruct
    public void initExecutor() {
        executor = Executors.newSingleThreadExecutor();
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/keys/attributes", method = RequestMethod.GET)
    @ResponseBody
    public DeferredResult<ResponseEntity> getAttributeKeys(
            @PathVariable("entityType") String entityType, @PathVariable("entityId") String entityIdStr) throws ThingsboardException {
        return accessValidator.validateEntityAndCallback(getCurrentUser(), entityType, entityIdStr, this::getAttributeKeysCallback);
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/keys/attributes/{scope}", method = RequestMethod.GET)
    @ResponseBody
    public DeferredResult<ResponseEntity> getAttributeKeysByScope(
            @PathVariable("entityType") String entityType, @PathVariable("entityId") String entityIdStr
            , @PathVariable("scope") String scope) throws ThingsboardException {
        return accessValidator.validateEntityAndCallback(getCurrentUser(), entityType, entityIdStr,
                (result, entityId) -> getAttributeKeysCallback(result, entityId, scope));
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/values/attributes", method = RequestMethod.GET)
    @ResponseBody
    public DeferredResult<ResponseEntity> getAttributes(
            @PathVariable("entityType") String entityType, @PathVariable("entityId") String entityIdStr,
            @RequestParam(name = "keys", required = false) String keysStr) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        return accessValidator.validateEntityAndCallback(getCurrentUser(), entityType, entityIdStr,
                (result, entityId) -> getAttributeValuesCallback(result, user, entityId, null, keysStr));
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/values/attributes/{scope}", method = RequestMethod.GET)
    @ResponseBody
    public DeferredResult<ResponseEntity> getAttributesByScope(
            @PathVariable("entityType") String entityType, @PathVariable("entityId") String entityIdStr,
            @PathVariable("scope") String scope,
            @RequestParam(name = "keys", required = false) String keysStr) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        return accessValidator.validateEntityAndCallback(getCurrentUser(), entityType, entityIdStr,
                (result, entityId) -> getAttributeValuesCallback(result, user, entityId, scope, keysStr));
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/keys/timeseries", method = RequestMethod.GET)
    @ResponseBody
    public DeferredResult<ResponseEntity> getTimeseriesKeys(
            @PathVariable("entityType") String entityType, @PathVariable("entityId") String entityIdStr) throws ThingsboardException {
        return accessValidator.validateEntityAndCallback(getCurrentUser(), entityType, entityIdStr,
                (result, entityId) -> {
                    Futures.addCallback(tsService.findAllLatest(entityId), getTsKeysToResponseCallback(result));
                });
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/values/timeseries", method = RequestMethod.GET)
    @ResponseBody
    public DeferredResult<ResponseEntity> getLatestTimeseries(
            @PathVariable("entityType") String entityType, @PathVariable("entityId") String entityIdStr,
            @RequestParam(name = "keys", required = false) String keysStr) throws ThingsboardException {
        SecurityUser user = getCurrentUser();

        return accessValidator.validateEntityAndCallback(getCurrentUser(), entityType, entityIdStr,
                (result, entityId) -> getLatestTimeseriesValuesCallback(result, user, entityId, keysStr));
    }


    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/values/timeseries", method = RequestMethod.GET, params = {"keys", "startTs", "endTs"})
    @ResponseBody
    public DeferredResult<ResponseEntity> getTimeseries(
            @PathVariable("entityType") String entityType, @PathVariable("entityId") String entityIdStr,
            @RequestParam(name = "keys") String keys,
            @RequestParam(name = "startTs") Long startTs,
            @RequestParam(name = "endTs") Long endTs,
            @RequestParam(name = "interval", defaultValue = "0") Long interval,
            @RequestParam(name = "limit", defaultValue = "100") Integer limit,
            @RequestParam(name = "agg", defaultValue = "NONE") String aggStr
    ) throws ThingsboardException {
        return accessValidator.validateEntityAndCallback(getCurrentUser(), entityType, entityIdStr,
                (result, entityId) -> {
                    // If interval is 0, convert this to a NONE aggregation, which is probably what the user really wanted
                    Aggregation agg = interval == 0L ? Aggregation.valueOf(Aggregation.NONE.name()) : Aggregation.valueOf(aggStr);
                    List<ReadTsKvQuery> queries = toKeysList(keys).stream().map(key -> new BaseReadTsKvQuery(key, startTs, endTs, interval, limit, agg))
                            .collect(Collectors.toList());

                    Futures.addCallback(tsService.findAll(entityId, queries), getTsKvListCallback(result));
                });
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{deviceId}/{scope}", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> saveDeviceAttributes(@PathVariable("deviceId") String deviceIdStr, @PathVariable("scope") String scope,
                                                               @RequestBody JsonNode request) throws ThingsboardException {
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(EntityType.DEVICE, deviceIdStr);
        return saveAttributes(entityId, scope, request);
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/{scope}", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> saveEntityAttributesV1(@PathVariable("entityType") String entityType, @PathVariable("entityId") String entityIdStr,
                                                                 @PathVariable("scope") String scope,
                                                                 @RequestBody JsonNode request) throws ThingsboardException {
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
        return saveAttributes(entityId, scope, request);
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/attributes/{scope}", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> saveEntityAttributesV2(@PathVariable("entityType") String entityType, @PathVariable("entityId") String entityIdStr,
                                                                 @PathVariable("scope") String scope,
                                                                 @RequestBody JsonNode request) throws ThingsboardException {
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
        return saveAttributes(entityId, scope, request);
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/timeseries/{scope}", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> saveEntityTelemetry(@PathVariable("entityType") String entityType, @PathVariable("entityId") String entityIdStr,
                                                              @PathVariable("scope") String scope,
                                                              @RequestBody String requestBody) throws ThingsboardException {
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
        return saveTelemetry(entityId, requestBody, 0L);
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/timeseries/{scope}/{ttl}", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> saveEntityTelemetryWithTTL(@PathVariable("entityType") String entityType, @PathVariable("entityId") String entityIdStr,
                                                                     @PathVariable("scope") String scope, @PathVariable("ttl") Long ttl,
                                                                     @RequestBody String requestBody) throws ThingsboardException {
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
        return saveTelemetry(entityId, requestBody, ttl);
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{deviceId}/{scope}", method = RequestMethod.DELETE)
    @ResponseBody
    public DeferredResult<ResponseEntity> deleteEntityAttributes(@PathVariable("deviceId") String deviceIdStr,
                                                                 @PathVariable("scope") String scope,
                                                                 @RequestParam(name = "keys") String keysStr) throws ThingsboardException {
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(EntityType.DEVICE, deviceIdStr);
        return deleteAttributes(entityId, scope, keysStr);
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/{scope}", method = RequestMethod.DELETE)
    @ResponseBody
    public DeferredResult<ResponseEntity> deleteEntityAttributes(@PathVariable("entityType") String entityType, @PathVariable("entityId") String entityIdStr,
                                                                 @PathVariable("scope") String scope,
                                                                 @RequestParam(name = "keys") String keysStr) throws ThingsboardException {
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
        return deleteAttributes(entityId, scope, keysStr);
    }

    private DeferredResult<ResponseEntity> deleteAttributes(EntityId entityIdStr, String scope, String keysStr) throws ThingsboardException {
        List<String> keys = toKeysList(keysStr);
        if (keys.isEmpty()) {
            return getImmediateDeferredResult("Empty keys: " + keysStr, HttpStatus.BAD_REQUEST);
        }
        SecurityUser user = getCurrentUser();

        if (DataConstants.SERVER_SCOPE.equals(scope) ||
                DataConstants.SHARED_SCOPE.equals(scope) ||
                DataConstants.CLIENT_SCOPE.equals(scope)) {
            return accessValidator.validateEntityAndCallback(getCurrentUser(), entityIdStr, (result, entityId) -> {
                ListenableFuture<List<Void>> future = attributesService.removeAll(entityId, scope, keys);
                Futures.addCallback(future, new FutureCallback<List<Void>>() {
                    @Override
                    public void onSuccess(@Nullable List<Void> tmp) {
                        logAttributesDeleted(user, entityId, scope, keys, null);
                        if (entityId.getEntityType() == EntityType.DEVICE) {
                            DeviceId deviceId = new DeviceId(entityId.getId());
                            Set<AttributeKey> keysToNotify = new HashSet<>();
                            keys.forEach(key -> keysToNotify.add(new AttributeKey(scope, key)));
                            DeviceAttributesEventNotificationMsg notificationMsg = DeviceAttributesEventNotificationMsg.onDelete(
                                    user.getTenantId(), deviceId, keysToNotify);
                            actorService.onMsg(new SendToClusterMsg(deviceId, notificationMsg));
                        }
                        result.setResult(new ResponseEntity<>(HttpStatus.OK));
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        logAttributesDeleted(user, entityId, scope, keys, t);
                        result.setResult(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
                    }
                }, executor);
            });
        } else {
            return getImmediateDeferredResult("Invalid attribute scope: " + scope, HttpStatus.BAD_REQUEST);
        }
    }

    private DeferredResult<ResponseEntity> saveAttributes(EntityId entityIdSrc, String scope, JsonNode json) throws ThingsboardException {
        if (!DataConstants.SERVER_SCOPE.equals(scope) && !DataConstants.SHARED_SCOPE.equals(scope)) {
            return getImmediateDeferredResult("Invalid scope: " + scope, HttpStatus.BAD_REQUEST);
        }
        if (json.isObject()) {
            List<AttributeKvEntry> attributes = extractRequestAttributes(json);
            if (attributes.isEmpty()) {
                return getImmediateDeferredResult("No attributes data found in request body!", HttpStatus.BAD_REQUEST);
            }
            SecurityUser user = getCurrentUser();
            return accessValidator.validateEntityAndCallback(getCurrentUser(), entityIdSrc, (result, entityId) -> {
                tsSubService.saveAndNotify(entityId, scope, attributes, new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(@Nullable Void tmp) {
                        logAttributesUpdated(user, entityId, scope, attributes, null);
                        if (entityId.getEntityType() == EntityType.DEVICE) {
                            DeviceId deviceId = new DeviceId(entityId.getId());
                            DeviceAttributesEventNotificationMsg notificationMsg = DeviceAttributesEventNotificationMsg.onUpdate(
                                    user.getTenantId(), deviceId, scope, attributes);
                            actorService.onMsg(new SendToClusterMsg(deviceId, notificationMsg));
                        }
                        result.setResult(new ResponseEntity(HttpStatus.OK));
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        logAttributesUpdated(user, entityId, scope, attributes, t);
                        AccessValidator.handleError(t, result, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                });
            });
        } else {
            return getImmediateDeferredResult("Request is not a JSON object", HttpStatus.BAD_REQUEST);
        }
    }

    private DeferredResult<ResponseEntity> saveTelemetry(EntityId entityIdSrc, String requestBody, long ttl) throws ThingsboardException {
        TelemetryUploadRequest telemetryRequest;
        JsonElement telemetryJson;
        try {
            telemetryJson = new JsonParser().parse(requestBody);
        } catch (Exception e) {
            return getImmediateDeferredResult("Unable to parse timeseries payload: Invalid JSON body!", HttpStatus.BAD_REQUEST);
        }
        try {
            telemetryRequest = JsonConverter.convertToTelemetry(telemetryJson);
        } catch (Exception e) {
            return getImmediateDeferredResult("Unable to parse timeseries payload. Invalid JSON body: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        List<TsKvEntry> entries = new ArrayList<>();
        for (Map.Entry<Long, List<KvEntry>> entry : telemetryRequest.getData().entrySet()) {
            for (KvEntry kv : entry.getValue()) {
                entries.add(new BasicTsKvEntry(entry.getKey(), kv));
            }
        }
        if (entries.isEmpty()) {
            return getImmediateDeferredResult("No timeseries data found in request body!", HttpStatus.BAD_REQUEST);
        }
        SecurityUser user = getCurrentUser();
        return accessValidator.validateEntityAndCallback(getCurrentUser(), entityIdSrc, (result, entityId) -> {
            tsSubService.saveAndNotify(entityId, entries, ttl, new FutureCallback<Void>() {
                @Override
                public void onSuccess(@Nullable Void tmp) {
                    result.setResult(new ResponseEntity(HttpStatus.OK));
                }

                @Override
                public void onFailure(Throwable t) {
                    AccessValidator.handleError(t, result, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            });
        });
    }

    private void getLatestTimeseriesValuesCallback(@Nullable DeferredResult<ResponseEntity> result, SecurityUser user, EntityId entityId, String keys) {
        ListenableFuture<List<TsKvEntry>> future;
        if (StringUtils.isEmpty(keys)) {
            future = tsService.findAllLatest(entityId);
        } else {
            future = tsService.findLatest(entityId, toKeysList(keys));
        }
        Futures.addCallback(future, getTsKvListCallback(result));
    }

    private void getAttributeValuesCallback(@Nullable DeferredResult<ResponseEntity> result, SecurityUser user, EntityId entityId, String scope, String keys) {
        List<String> keyList = toKeysList(keys);
        FutureCallback<List<AttributeKvEntry>> callback = getAttributeValuesToResponseCallback(result, user, scope, entityId, keyList);
        if (!StringUtils.isEmpty(scope)) {
            if (keyList != null && !keyList.isEmpty()) {
                Futures.addCallback(attributesService.find(entityId, scope, keyList), callback);
            } else {
                Futures.addCallback(attributesService.findAll(entityId, scope), callback);
            }
        } else {
            List<ListenableFuture<List<AttributeKvEntry>>> futures = new ArrayList<>();
            for (String tmpScope : DataConstants.allScopes()) {
                if (keyList != null && !keyList.isEmpty()) {
                    futures.add(attributesService.find(entityId, tmpScope, keyList));
                } else {
                    futures.add(attributesService.findAll(entityId, tmpScope));
                }
            }

            ListenableFuture<List<AttributeKvEntry>> future = mergeAllAttributesFutures(futures);

            Futures.addCallback(future, callback);
        }
    }

    private void getAttributeKeysCallback(@Nullable DeferredResult<ResponseEntity> result, EntityId entityId, String scope) {
        Futures.addCallback(attributesService.findAll(entityId, scope), getAttributeKeysToResponseCallback(result));
    }

    private void getAttributeKeysCallback(@Nullable DeferredResult<ResponseEntity> result, EntityId entityId) {
        List<ListenableFuture<List<AttributeKvEntry>>> futures = new ArrayList<>();
        for (String scope : DataConstants.allScopes()) {
            futures.add(attributesService.findAll(entityId, scope));
        }

        ListenableFuture<List<AttributeKvEntry>> future = mergeAllAttributesFutures(futures);

        Futures.addCallback(future, getAttributeKeysToResponseCallback(result));
    }

    private FutureCallback<List<TsKvEntry>> getTsKeysToResponseCallback(final DeferredResult<ResponseEntity> response) {
        return new FutureCallback<List<TsKvEntry>>() {
            @Override
            public void onSuccess(List<TsKvEntry> values) {
                List<String> keys = values.stream().map(KvEntry::getKey).collect(Collectors.toList());
                response.setResult(new ResponseEntity<>(keys, HttpStatus.OK));
            }

            @Override
            public void onFailure(Throwable e) {
                log.error("Failed to fetch attributes", e);
                AccessValidator.handleError(e, response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        };
    }

    private FutureCallback<List<AttributeKvEntry>> getAttributeKeysToResponseCallback(final DeferredResult<ResponseEntity> response) {
        return new FutureCallback<List<AttributeKvEntry>>() {

            @Override
            public void onSuccess(List<AttributeKvEntry> attributes) {
                List<String> keys = attributes.stream().map(KvEntry::getKey).collect(Collectors.toList());
                response.setResult(new ResponseEntity<>(keys, HttpStatus.OK));
            }

            @Override
            public void onFailure(Throwable e) {
                log.error("Failed to fetch attributes", e);
                AccessValidator.handleError(e, response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        };
    }

    private FutureCallback<List<AttributeKvEntry>> getAttributeValuesToResponseCallback(final DeferredResult<ResponseEntity> response,
                                                                                        final SecurityUser user, final String scope,
                                                                                        final EntityId entityId, final List<String> keyList) {
        return new FutureCallback<List<AttributeKvEntry>>() {
            @Override
            public void onSuccess(List<AttributeKvEntry> attributes) {
                List<AttributeData> values = attributes.stream().map(attribute -> new AttributeData(attribute.getLastUpdateTs(),
                        attribute.getKey(), attribute.getValue())).collect(Collectors.toList());
                logAttributesRead(user, entityId, scope, keyList, null);
                response.setResult(new ResponseEntity<>(values, HttpStatus.OK));
            }

            @Override
            public void onFailure(Throwable e) {
                log.error("Failed to fetch attributes", e);
                logAttributesRead(user, entityId, scope, keyList, e);
                AccessValidator.handleError(e, response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        };
    }

    private FutureCallback<List<TsKvEntry>> getTsKvListCallback(final DeferredResult<ResponseEntity> response) {
        return new FutureCallback<List<TsKvEntry>>() {
            @Override
            public void onSuccess(List<TsKvEntry> data) {
                Map<String, List<TsData>> result = new LinkedHashMap<>();
                for (TsKvEntry entry : data) {
                    result.computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                            .add(new TsData(entry.getTs(), entry.getValueAsString()));
                }
                response.setResult(new ResponseEntity<>(result, HttpStatus.OK));
            }

            @Override
            public void onFailure(Throwable e) {
                log.error("Failed to fetch historical data", e);
                AccessValidator.handleError(e, response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        };
    }

    private void logAttributesDeleted(SecurityUser user, EntityId entityId, String scope, List<String> keys, Throwable e) {
        try {
            logEntityAction(user, (UUIDBased & EntityId) entityId, null, null, ActionType.ATTRIBUTES_DELETED, toException(e),
                    scope, keys);
        } catch (ThingsboardException te) {
            log.warn("Failed to log attributes delete", te);
        }
    }

    private void logAttributesUpdated(SecurityUser user, EntityId entityId, String scope, List<AttributeKvEntry> attributes, Throwable e) {
        try {
            logEntityAction(user, (UUIDBased & EntityId) entityId, null, null, ActionType.ATTRIBUTES_UPDATED, toException(e),
                    scope, attributes);
        } catch (ThingsboardException te) {
            log.warn("Failed to log attributes update", te);
        }
    }


    private void logAttributesRead(SecurityUser user, EntityId entityId, String scope, List<String> keys, Throwable e) {
        try {
            logEntityAction(user, (UUIDBased & EntityId) entityId, null, null, ActionType.ATTRIBUTES_READ, toException(e),
                    scope, keys);
        } catch (ThingsboardException te) {
            log.warn("Failed to log attributes read", te);
        }
    }

    private ListenableFuture<List<AttributeKvEntry>> mergeAllAttributesFutures(List<ListenableFuture<List<AttributeKvEntry>>> futures) {
        return Futures.transform(Futures.successfulAsList(futures),
                (Function<? super List<List<AttributeKvEntry>>, ? extends List<AttributeKvEntry>>) input -> {
                    List<AttributeKvEntry> tmp = new ArrayList<>();
                    if (input != null) {
                        input.forEach(tmp::addAll);
                    }
                    return tmp;
                }, executor);
    }

    private List<String> toKeysList(String keys) {
        List<String> keyList = null;
        if (!StringUtils.isEmpty(keys)) {
            keyList = Arrays.asList(keys.split(","));
        }
        return keyList;
    }

    private DeferredResult<ResponseEntity> getImmediateDeferredResult(String message, HttpStatus status) {
        DeferredResult<ResponseEntity> result = new DeferredResult<>();
        result.setResult(new ResponseEntity<>(message, status));
        return result;
    }

    private List<AttributeKvEntry> extractRequestAttributes(JsonNode jsonNode) {
        long ts = System.currentTimeMillis();
        List<AttributeKvEntry> attributes = new ArrayList<>();
        jsonNode.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            if (entry.getValue().isTextual()) {
                attributes.add(new BaseAttributeKvEntry(new StringDataEntry(key, value.textValue()), ts));
            } else if (entry.getValue().isBoolean()) {
                attributes.add(new BaseAttributeKvEntry(new BooleanDataEntry(key, value.booleanValue()), ts));
            } else if (entry.getValue().isDouble()) {
                attributes.add(new BaseAttributeKvEntry(new DoubleDataEntry(key, value.doubleValue()), ts));
            } else if (entry.getValue().isNumber()) {
                if (entry.getValue().isBigInteger()) {
                    throw new UncheckedApiException(new InvalidParametersException("Big integer values are not supported!"));
                } else {
                    attributes.add(new BaseAttributeKvEntry(new LongDataEntry(key, value.longValue()), ts));
                }
            }
        });
        return attributes;
    }
}
