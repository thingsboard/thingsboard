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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.actors.plugin.ValidationResult;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.exception.ThingsboardException;
import org.thingsboard.server.extensions.api.exception.ToErrorResponseEntity;
import org.thingsboard.server.extensions.api.plugins.PluginConstants;
import org.thingsboard.server.service.security.model.SecurityUser;

import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
        DeferredResult<ResponseEntity> response = new DeferredResult<ResponseEntity>();
        EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);

        validate(getCurrentUser(), entityId, new ValidationCallback(response,
                new FutureCallback<DeferredResult<ResponseEntity>>() {
                    @Override
                    public void onSuccess(@Nullable DeferredResult<ResponseEntity> result) {
                        List<ListenableFuture<List<AttributeKvEntry>>> futures = new ArrayList<>();
                        for (String scope : DataConstants.allScopes()) {
                            futures.add(attributesService.findAll(entityId, scope));
                        }

                        ListenableFuture<List<AttributeKvEntry>> future = Futures.transform(Futures.successfulAsList(futures),
                                (Function<? super List<List<AttributeKvEntry>>, ? extends List<AttributeKvEntry>>) input -> {
                                    List<AttributeKvEntry> tmp = new ArrayList<>();
                                    if (input != null) {
                                        input.forEach(tmp::addAll);
                                    }
                                    return tmp;
                                }, executor);

                        Futures.addCallback(future, getAttributeKeysPluginCallback(result));
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        handleError(t, response, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }));

        return response;
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/keys/ATTRIBUTES/{scope}", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<ResponseEntity> getAttributeKeysByScope() {
        return null;
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/values/ATTRIBUTES", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<ResponseEntity> getAttributeValues() {
        return null;
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/values/ATTRIBUTES", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<ResponseEntity> getAttributeValuesByScope() {
        return null;
    }

    private FutureCallback<List<AttributeKvEntry>> getAttributeKeysPluginCallback(final DeferredResult<ResponseEntity> response) {
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
//            case ASSET:
//                validateAsset(ctx, entityId, callback);
//                return;
//            case RULE:
//                validateRule(ctx, entityId, callback);
//                return;
//            case RULE_CHAIN:
//                validateRuleChain(ctx, entityId, callback);
//                return;
//            case PLUGIN:
//                validatePlugin(ctx, entityId, callback);
//                return;
//            case CUSTOMER:
//                validateCustomer(ctx, entityId, callback);
//                return;
//            case TENANT:
//                validateTenant(ctx, entityId, callback);
//                return;
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
//
//    private void validateAsset(final PluginApiCallSecurityContext ctx, EntityId entityId, ValidationCallback callback) {
//        if (ctx.isSystemAdmin()) {
//            callback.onSuccess(this, ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
//        } else {
//            ListenableFuture<Asset> assetFuture = pluginCtx.assetService.findAssetByIdAsync(new AssetId(entityId.getId()));
//            Futures.addCallback(assetFuture, getCallback(callback, asset -> {
//                if (asset == null) {
//                    return ValidationResult.entityNotFound("Asset with requested id wasn't found!");
//                } else {
//                    if (!asset.getTenantId().equals(ctx.getTenantId())) {
//                        return ValidationResult.accessDenied("Asset doesn't belong to the current Tenant!");
//                    } else if (ctx.isCustomerUser() && !asset.getCustomerId().equals(ctx.getCustomerId())) {
//                        return ValidationResult.accessDenied("Asset doesn't belong to the current Customer!");
//                    } else {
//                        return ValidationResult.ok();
//                    }
//                }
//            }));
//        }
//    }
//
//    private void validateRule(final PluginApiCallSecurityContext ctx, EntityId entityId, ValidationCallback callback) {
//        if (ctx.isCustomerUser()) {
//            callback.onSuccess(this, ValidationResult.accessDenied(CUSTOMER_USER_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
//        } else {
//            ListenableFuture<RuleMetaData> ruleFuture = pluginCtx.ruleService.findRuleByIdAsync(new RuleId(entityId.getId()));
//            Futures.addCallback(ruleFuture, getCallback(callback, rule -> {
//                if (rule == null) {
//                    return ValidationResult.entityNotFound("Rule with requested id wasn't found!");
//                } else {
//                    if (ctx.isTenantAdmin() && !rule.getTenantId().equals(ctx.getTenantId())) {
//                        return ValidationResult.accessDenied("Rule doesn't belong to the current Tenant!");
//                    } else if (ctx.isSystemAdmin() && !rule.getTenantId().isNullUid()) {
//                        return ValidationResult.accessDenied("Rule is not in system scope!");
//                    } else {
//                        return ValidationResult.ok();
//                    }
//                }
//            }));
//        }
//    }
//
//    private void validateRuleChain(final PluginApiCallSecurityContext ctx, EntityId entityId, ValidationCallback callback) {
//        if (ctx.isCustomerUser()) {
//            callback.onSuccess(this, ValidationResult.accessDenied(CUSTOMER_USER_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
//        } else {
//            ListenableFuture<RuleChain> ruleChainFuture = pluginCtx.ruleChainService.findRuleChainByIdAsync(new RuleChainId(entityId.getId()));
//            Futures.addCallback(ruleChainFuture, getCallback(callback, ruleChain -> {
//                if (ruleChain == null) {
//                    return ValidationResult.entityNotFound("Rule chain with requested id wasn't found!");
//                } else {
//                    if (ctx.isTenantAdmin() && !ruleChain.getTenantId().equals(ctx.getTenantId())) {
//                        return ValidationResult.accessDenied("Rule chain doesn't belong to the current Tenant!");
//                    } else if (ctx.isSystemAdmin() && !ruleChain.getTenantId().isNullUid()) {
//                        return ValidationResult.accessDenied("Rule chain is not in system scope!");
//                    } else {
//                        return ValidationResult.ok();
//                    }
//                }
//            }));
//        }
//    }
//
//
//    private void validatePlugin(final PluginApiCallSecurityContext ctx, EntityId entityId, ValidationCallback callback) {
//        if (ctx.isCustomerUser()) {
//            callback.onSuccess(this, ValidationResult.accessDenied(CUSTOMER_USER_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
//        } else {
//            ListenableFuture<PluginMetaData> pluginFuture = pluginCtx.pluginService.findPluginByIdAsync(new PluginId(entityId.getId()));
//            Futures.addCallback(pluginFuture, getCallback(callback, plugin -> {
//                if (plugin == null) {
//                    return ValidationResult.entityNotFound("Plugin with requested id wasn't found!");
//                } else {
//                    if (ctx.isTenantAdmin() && !plugin.getTenantId().equals(ctx.getTenantId())) {
//                        return ValidationResult.accessDenied("Plugin doesn't belong to the current Tenant!");
//                    } else if (ctx.isSystemAdmin() && !plugin.getTenantId().isNullUid()) {
//                        return ValidationResult.accessDenied("Plugin is not in system scope!");
//                    } else {
//                        return ValidationResult.ok();
//                    }
//                }
//            }));
//        }
//    }
//
//    private void validateCustomer(final PluginApiCallSecurityContext ctx, EntityId entityId, ValidationCallback callback) {
//        if (ctx.isSystemAdmin()) {
//            callback.onSuccess(this, ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
//        } else {
//            ListenableFuture<Customer> customerFuture = pluginCtx.customerService.findCustomerByIdAsync(new CustomerId(entityId.getId()));
//            Futures.addCallback(customerFuture, getCallback(callback, customer -> {
//                if (customer == null) {
//                    return ValidationResult.entityNotFound("Customer with requested id wasn't found!");
//                } else {
//                    if (!customer.getTenantId().equals(ctx.getTenantId())) {
//                        return ValidationResult.accessDenied("Customer doesn't belong to the current Tenant!");
//                    } else if (ctx.isCustomerUser() && !customer.getId().equals(ctx.getCustomerId())) {
//                        return ValidationResult.accessDenied("Customer doesn't relate to the currently authorized customer user!");
//                    } else {
//                        return ValidationResult.ok();
//                    }
//                }
//            }));
//        }
//    }
//
//    private void validateTenant(final PluginApiCallSecurityContext ctx, EntityId entityId, ValidationCallback callback) {
//        if (ctx.isCustomerUser()) {
//            callback.onSuccess(this, ValidationResult.accessDenied(CUSTOMER_USER_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
//        } else if (ctx.isSystemAdmin()) {
//            callback.onSuccess(this, ValidationResult.ok());
//        } else {
//            ListenableFuture<Tenant> tenantFuture = pluginCtx.tenantService.findTenantByIdAsync(new TenantId(entityId.getId()));
//            Futures.addCallback(tenantFuture, getCallback(callback, tenant -> {
//                if (tenant == null) {
//                    return ValidationResult.entityNotFound("Tenant with requested id wasn't found!");
//                } else if (!tenant.getId().equals(ctx.getTenantId())) {
//                    return ValidationResult.accessDenied("Tenant doesn't relate to the currently authorized user!");
//                } else {
//                    return ValidationResult.ok();
//                }
//            }));
//        }
//    }


}
