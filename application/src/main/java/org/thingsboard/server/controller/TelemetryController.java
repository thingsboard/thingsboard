package org.thingsboard.server.controller;

import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.actors.plugin.ValidationResult;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.exception.ThingsboardException;
import org.thingsboard.server.extensions.api.exception.ToErrorResponseEntity;
import org.thingsboard.server.extensions.api.plugins.PluginConstants;
import org.thingsboard.server.extensions.core.plugin.telemetry.AttributeData;
import org.thingsboard.server.service.security.model.SecurityUser;

import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Created by ashvayka on 22.03.18.
 */
@RestController
@RequestMapping(PluginConstants.TELEMETRY_URL_PREFIX)
@Slf4j
public class TelemetryController extends BaseController {

    public static final String CUSTOMER_USER_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION = "Customer user is not allowed to perform this operation!";
    public static final String SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION = "System administrator is not allowed to perform this operation!";
    public static final String DEVICE_WITH_REQUESTED_ID_NOT_FOUND = "Device with requested id wasn't found!";

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private TimeseriesService tsService;

    private ExecutorService executor;

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
    @RequestMapping(value = "/{entityType}/{entityId}/keys/ATTRIBUTES", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<ResponseEntity> getAttributeKeys(
            @PathVariable("entityType") String entityType, @PathVariable("entityId") String entityIdStr) throws ThingsboardException {
        return validateEntityAndCallback(entityType, entityIdStr,
                this::getAttributeKeysCallback,
                (result, t) -> handleError(t, result, HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/keys/ATTRIBUTES/{scope}", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<ResponseEntity> getAttributeKeysByScope(
            @PathVariable("entityType") String entityType, @PathVariable("entityId") String entityIdStr
            , @PathVariable("scope") String scope) throws ThingsboardException {
        return validateEntityAndCallback(entityType, entityIdStr,
                (result, entityId) -> getAttributeKeysCallback(result, entityId, scope),
                (result, t) -> handleError(t, result, HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/values/ATTRIBUTES", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<ResponseEntity> getAttributes(
            @PathVariable("entityType") String entityType, @PathVariable("entityId") String entityIdStr,
            @RequestParam(name = "keys", required = false) String keysStr) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        return validateEntityAndCallback(entityType, entityIdStr,
                (result, entityId) -> getAttributeValuesCallback(result, user, entityId, null, keysStr),
                (result, t) -> handleError(t, result, HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/values/ATTRIBUTES/{scope}", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<ResponseEntity> getAttributesByScope(
            @PathVariable("entityType") String entityType, @PathVariable("entityId") String entityIdStr,
            @PathVariable("scope") String scope,
            @RequestParam(name = "keys", required = false) String keysStr) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        return validateEntityAndCallback(entityType, entityIdStr,
                (result, entityId) -> getAttributeValuesCallback(result, user, entityId, scope, keysStr),
                (result, t) -> handleError(t, result, HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/keys/TIMESERIES", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<ResponseEntity> getTimeseriesKeys(
            @PathVariable("entityType") String entityType, @PathVariable("entityId") String entityIdStr) throws ThingsboardException {
        return validateEntityAndCallback(entityType, entityIdStr,
                (result, entityId) -> {
                    Futures.addCallback(tsService.findAllLatest(entityId), getTsKeysToResponseCallback(result));
                },
                (result, t) -> handleError(t, result, HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/values/TIMESERIES", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<ResponseEntity> getLatestTimeseries(
            @PathVariable("entityType") String entityType, @PathVariable("entityId") String entityIdStr,
            @PathVariable("scope") String scope,
            @RequestParam(name = "keys", required = false) String keysStr) throws ThingsboardException {
        SecurityUser user = getCurrentUser();

        return validateEntityAndCallback(entityType, entityIdStr,
                (result, entityId) -> getAttributeValuesCallback(result, user, entityId, scope, keysStr),
                (result, t) -> handleError(t, result, HttpStatus.INTERNAL_SERVER_ERROR));
    }


    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/values/TIMESERIES", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<ResponseEntity> getLatestTimeseries(
            @PathVariable("entityType") String entityType, @PathVariable("entityId") String entityIdStr,
            @PathVariable("scope") String scope,
            @RequestParam(name = "keys", required = false) String keysStr) throws ThingsboardException {
        SecurityUser user = getCurrentUser();

        return validateEntityAndCallback(entityType, entityIdStr,
                (result, entityId) -> getAttributeValuesCallback(result, user, entityId, scope, keysStr),
                (result, t) -> handleError(t, result, HttpStatus.INTERNAL_SERVER_ERROR));
    }

    private DeferredResult<ResponseEntity> validateEntityAndCallback(String entityType, String entityIdStr,
                                                                     BiConsumer<DeferredResult<ResponseEntity>, EntityId> onSuccess, BiConsumer<DeferredResult<ResponseEntity>, Throwable> onFailure) throws ThingsboardException {
        final DeferredResult<ResponseEntity> response = new DeferredResult<>();
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);

        validate(getCurrentUser(), entityId, new ValidationCallback(response,
                new FutureCallback<DeferredResult<ResponseEntity>>() {
                    @Override
                    public void onSuccess(@Nullable DeferredResult<ResponseEntity> result) {
                        onSuccess.accept(response, entityId);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        onFailure.accept(response, t);
                    }
                }));

        return response;
    }

    private void getAttributeValuesCallback(@Nullable DeferredResult<ResponseEntity> result, SecurityUser user, EntityId entityId, String scope, String keys) {
        List<String> keyList = null;
        if (!StringUtils.isEmpty(keys)) {
            keyList = Arrays.asList(keys.split(","));
        }
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
                handleError(e, response, HttpStatus.INTERNAL_SERVER_ERROR);
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
                handleError(e, response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        };
    }

    private FutureCallback<List<AttributeKvEntry>> getAttributeValuesToResponseCallback(final DeferredResult<ResponseEntity> response, final SecurityUser user, final String scope,
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
                handleError(e, response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        };
    }

    private void logAttributesRead(SecurityUser user, EntityId entityId, String scope, List<String> keys, Throwable e) {
        auditLogService.logEntityAction(
                user.getTenantId(),
                user.getCustomerId(),
                user.getId(),
                user.getName(),
                (UUIDBased & EntityId) entityId,
                null,
                ActionType.ATTRIBUTES_READ,
                toException(e),
                scope,
                keys);
    }

    private void handleError(Throwable e, final DeferredResult<ResponseEntity> response, HttpStatus defaultErrorStatus) {
        ResponseEntity responseEntity;
        if (e != null && e instanceof ToErrorResponseEntity) {
            responseEntity = ((ToErrorResponseEntity) e).toErrorResponseEntity();
        } else if (e != null && e instanceof IllegalArgumentException) {
            responseEntity = new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } else {
            responseEntity = new ResponseEntity<>(defaultErrorStatus);
        }
        response.setResult(responseEntity);
    }

    private void validate(SecurityUser currentUser, EntityId entityId, ValidationCallback callback) {
        switch (entityId.getEntityType()) {
            case DEVICE:
                validateDevice(currentUser, entityId, callback);
                return;
            case ASSET:
                validateAsset(currentUser, entityId, callback);
                return;
            case RULE_CHAIN:
                validateRuleChain(currentUser, entityId, callback);
                return;
            case CUSTOMER:
                validateCustomer(currentUser, entityId, callback);
                return;
            case TENANT:
                validateTenant(currentUser, entityId, callback);
                return;
            default:
                //TODO: add support of other entities
                throw new IllegalStateException("Not Implemented!");
        }
    }

    private void validateDevice(final SecurityUser currentUser, EntityId entityId, ValidationCallback callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<Device> deviceFuture = deviceService.findDeviceByIdAsync(new DeviceId(entityId.getId()));
            Futures.addCallback(deviceFuture, getCallback(callback, device -> {
                if (device == null) {
                    return ValidationResult.entityNotFound(DEVICE_WITH_REQUESTED_ID_NOT_FOUND);
                } else {
                    if (!device.getTenantId().equals(currentUser.getTenantId())) {
                        return ValidationResult.accessDenied("Device doesn't belong to the current Tenant!");
                    } else if (currentUser.isCustomerUser() && !device.getCustomerId().equals(currentUser.getCustomerId())) {
                        return ValidationResult.accessDenied("Device doesn't belong to the current Customer!");
                    } else {
                        return ValidationResult.ok();
                    }
                }
            }));
        }
    }

    private void validateAsset(final SecurityUser currentUser, EntityId entityId, ValidationCallback callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<Asset> assetFuture = assetService.findAssetByIdAsync(new AssetId(entityId.getId()));
            Futures.addCallback(assetFuture, getCallback(callback, asset -> {
                if (asset == null) {
                    return ValidationResult.entityNotFound("Asset with requested id wasn't found!");
                } else {
                    if (!asset.getTenantId().equals(currentUser.getTenantId())) {
                        return ValidationResult.accessDenied("Asset doesn't belong to the current Tenant!");
                    } else if (currentUser.isCustomerUser() && !asset.getCustomerId().equals(currentUser.getCustomerId())) {
                        return ValidationResult.accessDenied("Asset doesn't belong to the current Customer!");
                    } else {
                        return ValidationResult.ok();
                    }
                }
            }));
        }
    }


    private void validateRuleChain(final SecurityUser currentUser, EntityId entityId, ValidationCallback callback) {
        if (currentUser.isCustomerUser()) {
            callback.onSuccess(ValidationResult.accessDenied(CUSTOMER_USER_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<RuleChain> ruleChainFuture = ruleChainService.findRuleChainByIdAsync(new RuleChainId(entityId.getId()));
            Futures.addCallback(ruleChainFuture, getCallback(callback, ruleChain -> {
                if (ruleChain == null) {
                    return ValidationResult.entityNotFound("Rule chain with requested id wasn't found!");
                } else {
                    if (currentUser.isTenantAdmin() && !ruleChain.getTenantId().equals(currentUser.getTenantId())) {
                        return ValidationResult.accessDenied("Rule chain doesn't belong to the current Tenant!");
                    } else if (currentUser.isSystemAdmin() && !ruleChain.getTenantId().isNullUid()) {
                        return ValidationResult.accessDenied("Rule chain is not in system scope!");
                    } else {
                        return ValidationResult.ok();
                    }
                }
            }));
        }
    }

    private void validateCustomer(final SecurityUser currentUser, EntityId entityId, ValidationCallback callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<Customer> customerFuture = customerService.findCustomerByIdAsync(new CustomerId(entityId.getId()));
            Futures.addCallback(customerFuture, getCallback(callback, customer -> {
                if (customer == null) {
                    return ValidationResult.entityNotFound("Customer with requested id wasn't found!");
                } else {
                    if (!customer.getTenantId().equals(currentUser.getTenantId())) {
                        return ValidationResult.accessDenied("Customer doesn't belong to the current Tenant!");
                    } else if (currentUser.isCustomerUser() && !customer.getId().equals(currentUser.getCustomerId())) {
                        return ValidationResult.accessDenied("Customer doesn't relate to the currently authorized customer user!");
                    } else {
                        return ValidationResult.ok();
                    }
                }
            }));
        }
    }

    private void validateTenant(final SecurityUser currentUser, EntityId entityId, ValidationCallback callback) {
        if (currentUser.isCustomerUser()) {
            callback.onSuccess(ValidationResult.accessDenied(CUSTOMER_USER_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.ok());
        } else {
            ListenableFuture<Tenant> tenantFuture = tenantService.findTenantByIdAsync(new TenantId(entityId.getId()));
            Futures.addCallback(tenantFuture, getCallback(callback, tenant -> {
                if (tenant == null) {
                    return ValidationResult.entityNotFound("Tenant with requested id wasn't found!");
                } else if (!tenant.getId().equals(currentUser.getTenantId())) {
                    return ValidationResult.accessDenied("Tenant doesn't relate to the currently authorized user!");
                } else {
                    return ValidationResult.ok();
                }
            }));
        }
    }

    private <T> FutureCallback<T> getCallback(ValidationCallback callback, Function<T, ValidationResult> transformer) {
        return new FutureCallback<T>() {
            @Override
            public void onSuccess(@Nullable T result) {
                callback.onSuccess(transformer.apply(result));
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        };
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

}
