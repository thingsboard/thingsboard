// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.security;

import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.blob.BlobEntityInfo;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.AgentAppUnitId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.ApiUsageStateId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.controller.HttpValidationCallback;
import org.thingsboard.server.dao.agent.AgentAppUnitService;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.blob.BlobEntityService;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.rpc.RpcService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.exception.ToErrorResponseEntity;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.AccessControlService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

/**
 * Created by ashvayka on 27.03.18.
 */
@Component
public class AccessValidator {

    public static final String ONLY_SYSTEM_ADMINISTRATOR_IS_ALLOWED_TO_PERFORM_THIS_OPERATION = "Only system administrator is allowed to perform this operation!";
    public static final String CUSTOMER_USER_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION = "Customer user is not allowed to perform this operation!";
    public static final String SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION = "System administrator is not allowed to perform this operation!";
    public static final String DEVICE_WITH_REQUESTED_ID_NOT_FOUND = "Device with requested id wasn't found!";
    public static final String USER_WITH_REQUESTED_ID_NOT_FOUND = "User with requested id wasn't found!";
    public static final String AGENT_WITH_REQUESTED_ID_NOT_FOUND = "Agent with requested id wasn't found!";
    public static final String AGENT_APP_UNIT_WITH_REQUESTED_ID_NOT_FOUND = "Agent app unit with requested id wasn't found!";
    public static final String AGENT_APPLICATION_WITH_REQUESTED_ID_NOT_FOUND = "Agent application with requested id wasn't found!";
    public static final String ENTITY_VIEW_WITH_REQUESTED_ID_NOT_FOUND = "Entity-view with requested id wasn't found!";
    public static final String ROLE_WITH_REQUESTED_ID_NOT_FOUND = "Role with requested id wasn't found!";
    public static final String DASHBOARD_WITH_REQUESTED_ID_NOT_FOUND = "Dashboard with requested id wasn't found!";
    public static final String EDGE_WITH_REQUESTED_ID_NOT_FOUND = "Edge with requested id wasn't found!";

    @Autowired
    protected TenantService tenantService;

    @Autowired
    protected CustomerService customerService;

    @Autowired
    protected UserService userService;

    @Autowired
    protected DeviceService deviceService;

    @Autowired
    protected DeviceProfileService deviceProfileService;

    @Autowired
    protected AssetProfileService assetProfileService;

    @Autowired
    protected AssetService assetService;

    @Autowired
    protected AlarmService alarmService;

    @Autowired
    protected RuleChainService ruleChainService;

    @Autowired
    protected ConverterService converterService;

    @Autowired
    protected IntegrationService integrationService;

    @Autowired
    protected SchedulerEventService schedulerEventService;

    @Autowired
    protected BlobEntityService blobEntityService;

    @Autowired
    protected EntityGroupService entityGroupService;

    @Autowired
    protected EntityViewService entityViewService;

    @Autowired
    protected RoleService roleService;

    @Autowired
    protected DashboardService dashboardService;

    @Autowired(required = false)
    protected EdgeService edgeService;

    @Autowired(required = false)
    protected AgentService agentService;

    @Autowired(required = false)
    protected AgentAppUnitService agentAppUnitService;

    @Autowired(required = false)
    protected AgentApplicationService agentApplicationService;

    @Autowired
    protected AccessControlService accessControlService;

    @Autowired
    protected ApiUsageStateService apiUsageStateService;

    @Autowired
    protected ResourceService resourceService;

    @Autowired
    protected OtaPackageService otaPackageService;

    @Autowired
    protected RpcService rpcService;

    private ExecutorService executor;

    @PostConstruct
    public void initExecutor() {
        executor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("access-validator"));
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    public DeferredResult<ResponseEntity> validateEntityAndCallback(SecurityUser currentUser, Operation operation, String entityType, String entityIdStr,
                                                                    ThreeConsumer<DeferredResult<ResponseEntity>, TenantId, EntityId> onSuccess) throws ThingsboardException {
        return validateEntityAndCallback(currentUser, operation, entityType, entityIdStr, onSuccess, (result, t) -> handleError(t, result, HttpStatus.INTERNAL_SERVER_ERROR));
    }

    public DeferredResult<ResponseEntity> validateEntityAndCallback(SecurityUser currentUser, Operation operation, String entityType, String entityIdStr,
                                                                    ThreeConsumer<DeferredResult<ResponseEntity>, TenantId, EntityId> onSuccess,
                                                                    BiConsumer<DeferredResult<ResponseEntity>, Throwable> onFailure) throws ThingsboardException {
        return validateEntityAndCallback(currentUser, operation, EntityIdFactory.getByTypeAndId(entityType, entityIdStr),
                onSuccess, onFailure);
    }

    public DeferredResult<ResponseEntity> validateEntityAndCallback(SecurityUser currentUser, Operation operation, EntityId entityId,
                                                                    ThreeConsumer<DeferredResult<ResponseEntity>, TenantId, EntityId> onSuccess) throws ThingsboardException {
        return validateEntityAndCallback(currentUser, operation, entityId, onSuccess, (result, t) -> handleError(t, result, HttpStatus.INTERNAL_SERVER_ERROR));
    }

    public DeferredResult<ResponseEntity> validateEntityAndCallback(SecurityUser currentUser, Operation operation, EntityId entityId,
                                                                    ThreeConsumer<DeferredResult<ResponseEntity>, TenantId, EntityId> onSuccess,
                                                                    BiConsumer<DeferredResult<ResponseEntity>, Throwable> onFailure) throws ThingsboardException {

        final DeferredResult<ResponseEntity> response = new DeferredResult<>();

        validate(currentUser, operation, entityId, new HttpValidationCallback(response,
                new FutureCallback<DeferredResult<ResponseEntity>>() {
                    @Override
                    public void onSuccess(@Nullable DeferredResult<ResponseEntity> result) {
                        try {
                            onSuccess.accept(response, currentUser.getTenantId(), entityId);
                        } catch (Exception e) {
                            onFailure(e);
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        onFailure.accept(response, t);
                    }
                }));

        return response;
    }

    public void validate(SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        switch (entityId.getEntityType()) {
            case DEVICE -> validateDevice(currentUser, operation, entityId, callback);
            case DEVICE_PROFILE -> validateDeviceProfile(currentUser, operation, entityId, callback);
            case ASSET -> validateAsset(currentUser, operation, entityId, callback);
            case ASSET_PROFILE -> validateAssetProfile(currentUser, operation, entityId, callback);
            case RULE_CHAIN -> validateRuleChain(currentUser, operation, entityId, callback);
            case CUSTOMER -> validateCustomer(currentUser, operation, entityId, callback);
            case TENANT -> validateTenant(currentUser, operation, entityId, callback);
            case CONVERTER -> validateConverter(currentUser, operation, entityId, callback);
            case INTEGRATION -> validateIntegration(currentUser, operation, entityId, callback);
            case TENANT_PROFILE -> validateTenantProfile(currentUser, operation, entityId, callback);
            case USER -> validateUser(currentUser, operation, entityId, callback);
            case SCHEDULER_EVENT -> validateSchedulerEvent(currentUser, operation, entityId, callback);
            case BLOB_ENTITY -> validateBlobEntity(currentUser, operation, entityId, callback);
            case ENTITY_GROUP -> validateEntityGroup(currentUser, operation, entityId, callback);
            case ENTITY_VIEW -> validateEntityView(currentUser, operation, entityId, callback);
            case ROLE -> validateRole(currentUser, operation, entityId, callback);
            case DASHBOARD -> validateDashboard(currentUser, operation, entityId, callback);
            case EDGE -> validateEdge(currentUser, operation, entityId, callback);
            case AGENT -> validateAgent(currentUser, operation, entityId, callback);
            case AGENT_APPLICATION -> validateAgentApplication(currentUser, operation, entityId, callback);
            case AGENT_APP_UNIT -> validateAgentAppUnit(currentUser, operation, entityId, callback);
            case API_USAGE_STATE -> validateApiUsageState(currentUser, operation, entityId, callback);
            case TB_RESOURCE -> validateResource(currentUser, operation, entityId, callback);
            case OTA_PACKAGE -> validateOtaPackage(currentUser, operation, entityId, callback);
            case RPC -> validateRpc(currentUser, entityId, callback);
            default ->
                //TODO: add support of other entities
                    throw new IllegalStateException("Not Implemented!");
        }
    }

    private void validateUser(SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        ListenableFuture<User> userFuture = userService.findUserByIdAsync(currentUser.getTenantId(), new UserId(entityId.getId()));
        Futures.addCallback(userFuture, getCallback(callback, user -> {
            if (user == null) {
                return ValidationResult.entityNotFound(USER_WITH_REQUESTED_ID_NOT_FOUND);
            } else {
                try {
                    accessControlService.checkPermission(currentUser, Resource.USER, operation, entityId, user);
                } catch (ThingsboardException e) {
                    return ValidationResult.accessDenied(e.getMessage());
                }
                return ValidationResult.ok(user);
            }
        }), executor);
    }

    private void validateDevice(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            Futures.addCallback(Futures.immediateFuture(deviceService.findDeviceById(currentUser.getTenantId(), new DeviceId(entityId.getId()))), getCallback(callback, device -> {
                if (device == null) {
                    return ValidationResult.entityNotFound(DEVICE_WITH_REQUESTED_ID_NOT_FOUND);
                } else {
                    try {
                        accessControlService.checkPermission(currentUser, Resource.DEVICE, operation, entityId, device);
                    } catch (ThingsboardException e) {
                        return ValidationResult.accessDenied(e.getMessage());
                    }
                    return ValidationResult.ok(device);
                }
            }), executor);
        }
    }

    private void validateRpc(final SecurityUser currentUser, EntityId entityId, FutureCallback<ValidationResult> callback) {
        ListenableFuture<Rpc> rpcFurure = rpcService.findRpcByIdAsync(currentUser.getTenantId(), new RpcId(entityId.getId()));
        Futures.addCallback(rpcFurure, getCallback(callback, rpc -> {
            if (rpc == null) {
                return ValidationResult.entityNotFound("Rpc with requested id wasn't found!");
            } else {
                try {
                    Device device = deviceService.findDeviceById(currentUser.getTenantId(), rpc.getDeviceId());
                    accessControlService.checkPermission(currentUser, Resource.DEVICE, Operation.RPC_CALL, device.getId(), device);
                } catch (ThingsboardException e) {
                    return ValidationResult.accessDenied(e.getMessage());
                }
                return ValidationResult.ok(rpc);
            }
        }), executor);
    }

    private void validateDeviceProfile(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(currentUser.getTenantId(), new DeviceProfileId(entityId.getId()));
            if (deviceProfile == null) {
                callback.onSuccess(ValidationResult.entityNotFound("Device profile with requested id wasn't found!"));
            } else {
                try {
                    accessControlService.checkPermission(currentUser, Resource.DEVICE_PROFILE, operation, entityId, deviceProfile);
                } catch (ThingsboardException e) {
                    callback.onSuccess(ValidationResult.accessDenied(e.getMessage()));
                }
                callback.onSuccess(ValidationResult.ok(deviceProfile));
            }
        }
    }

    private void validateAssetProfile(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            AssetProfile assetProfile = assetProfileService.findAssetProfileById(currentUser.getTenantId(), new AssetProfileId(entityId.getId()));
            if (assetProfile == null) {
                callback.onSuccess(ValidationResult.entityNotFound("Asset profile with requested id wasn't found!"));
            } else {
                try {
                    accessControlService.checkPermission(currentUser, Resource.ASSET_PROFILE, operation, entityId, assetProfile);
                } catch (ThingsboardException e) {
                    callback.onSuccess(ValidationResult.accessDenied(e.getMessage()));
                }
                callback.onSuccess(ValidationResult.ok(assetProfile));
            }
        }
    }

    private void validateApiUsageState(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            if (!operation.equals(Operation.READ_TELEMETRY)) {
                callback.onSuccess(ValidationResult.accessDenied("Allowed only READ_TELEMETRY operation!"));
            }
            ApiUsageState apiUsageState = apiUsageStateService.findApiUsageStateById(currentUser.getTenantId(), new ApiUsageStateId(entityId.getId()));
            if (apiUsageState == null) {
                callback.onSuccess(ValidationResult.entityNotFound("Api Usage State with requested id wasn't found!"));
            } else {
                try {
                    accessControlService.checkPermission(currentUser, Resource.API_USAGE_STATE, operation, entityId, apiUsageState);
                } catch (ThingsboardException e) {
                    callback.onSuccess(ValidationResult.accessDenied(e.getMessage()));
                }
                callback.onSuccess(ValidationResult.ok(apiUsageState));
            }
        }
    }

    private void validateOtaPackage(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            OtaPackageInfo otaPackage = otaPackageService.findOtaPackageInfoById(currentUser.getTenantId(), new OtaPackageId(entityId.getId()));
            if (otaPackage == null) {
                callback.onSuccess(ValidationResult.entityNotFound("OtaPackage with requested id wasn't found!"));
            } else {
                try {
                    accessControlService.checkPermission(currentUser, Resource.OTA_PACKAGE, operation, entityId, otaPackage);
                } catch (ThingsboardException e) {
                    callback.onSuccess(ValidationResult.accessDenied(e.getMessage()));
                }
                callback.onSuccess(ValidationResult.ok(otaPackage));
            }
        }
    }

    private void validateResource(SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        ListenableFuture<TbResourceInfo> resourceFuture = resourceService.findResourceInfoByIdAsync(currentUser.getTenantId(), new TbResourceId(entityId.getId()));
        Futures.addCallback(resourceFuture, getCallback(callback, resource -> {
            if (resource == null) {
                return ValidationResult.entityNotFound("Resource with requested id wasn't found!");
            } else {
                try {
                    accessControlService.checkPermission(currentUser, Resource.TB_RESOURCE, operation, entityId, resource);
                } catch (ThingsboardException e) {
                    return ValidationResult.accessDenied(e.getMessage());
                }
                return ValidationResult.ok(resource);
            }
        }), executor);
    }

    private void validateAsset(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<Asset> assetFuture = assetService.findAssetByIdAsync(currentUser.getTenantId(), new AssetId(entityId.getId()));
            Futures.addCallback(assetFuture, getCallback(callback, asset -> {
                if (asset == null) {
                    return ValidationResult.entityNotFound("Asset with requested id wasn't found!");
                } else {
                    try {
                        accessControlService.checkPermission(currentUser, Resource.ASSET, operation, entityId, asset);
                    } catch (ThingsboardException e) {
                        return ValidationResult.accessDenied(e.getMessage());
                    }
                    return ValidationResult.ok(asset);
                }
            }), executor);
        }
    }

    private void validateRuleChain(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isCustomerUser()) {
            callback.onSuccess(ValidationResult.accessDenied(CUSTOMER_USER_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<RuleChain> ruleChainFuture = ruleChainService.findRuleChainByIdAsync(currentUser.getTenantId(), new RuleChainId(entityId.getId()));
            Futures.addCallback(ruleChainFuture, getCallback(callback, ruleChain -> {
                if (ruleChain == null) {
                    return ValidationResult.entityNotFound("Rule chain with requested id wasn't found!");
                } else {
                    try {
                        accessControlService.checkPermission(currentUser, Resource.RULE_CHAIN, operation, entityId, ruleChain);
                    } catch (ThingsboardException e) {
                        return ValidationResult.accessDenied(e.getMessage());
                    }
                    return ValidationResult.ok(ruleChain);
                }
            }), executor);
        }
    }

    private void validateRule(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isCustomerUser()) {
            callback.onSuccess(ValidationResult.accessDenied(CUSTOMER_USER_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<RuleNode> ruleNodeFuture = ruleChainService.findRuleNodeByIdAsync(currentUser.getTenantId(), new RuleNodeId(entityId.getId()));
            Futures.addCallback(ruleNodeFuture, getCallback(callback, ruleNodeTmp -> {
                RuleNode ruleNode = ruleNodeTmp;
                if (ruleNode == null) {
                    return ValidationResult.entityNotFound("Rule node with requested id wasn't found!");
                } else if (ruleNode.getRuleChainId() == null) {
                    return ValidationResult.entityNotFound("Rule chain with requested node id wasn't found!");
                } else {
                    //TODO: make async
                    RuleChain ruleChain = ruleChainService.findRuleChainById(currentUser.getTenantId(), ruleNode.getRuleChainId());
                    try {
                        accessControlService.checkPermission(currentUser, Resource.RULE_CHAIN, operation, ruleNode.getRuleChainId(), ruleChain);
                    } catch (ThingsboardException e) {
                        return ValidationResult.accessDenied(e.getMessage());
                    }
                    return ValidationResult.ok(ruleNode);
                }
            }), executor);
        }
    }

    private void validateCustomer(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<Customer> customerFuture = customerService.findCustomerByIdAsync(currentUser.getTenantId(), new CustomerId(entityId.getId()));
            Futures.addCallback(customerFuture, getCallback(callback, customer -> {
                if (customer == null) {
                    return ValidationResult.entityNotFound("Customer with requested id wasn't found!");
                } else {
                    try {
                        accessControlService.checkPermission(currentUser, Resource.CUSTOMER, operation, entityId, customer);
                    } catch (ThingsboardException e) {
                        return ValidationResult.accessDenied(e.getMessage());
                    }
                    return ValidationResult.ok(customer);
                }
            }), executor);
        }
    }

    private void validateTenant(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isCustomerUser()) {
            callback.onSuccess(ValidationResult.accessDenied(CUSTOMER_USER_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.ok(null));
        } else {
            ListenableFuture<Tenant> tenantFuture = tenantService.findTenantByIdAsync(currentUser.getTenantId(), TenantId.fromUUID(entityId.getId()));
            Futures.addCallback(tenantFuture, getCallback(callback, tenant -> {
                if (tenant == null) {
                    return ValidationResult.entityNotFound("Tenant with requested id wasn't found!");
                }
                try {
                    accessControlService.checkPermission(currentUser, Resource.TENANT, operation, entityId, tenant);
                } catch (ThingsboardException e) {
                    return ValidationResult.accessDenied(e.getMessage());
                }
                return ValidationResult.ok(tenant);

            }), executor);
        }
    }

    private void validateConverter(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isCustomerUser()) {
            callback.onSuccess(ValidationResult.accessDenied(CUSTOMER_USER_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<Converter> converterFuture = converterService.findConverterByIdAsync(currentUser.getTenantId(), new ConverterId(entityId.getId()));
            Futures.addCallback(converterFuture, getCallback(callback, converter -> {
                if (converter == null) {
                    return ValidationResult.entityNotFound("Converter with requested id wasn't found!");
                } else {
                    try {
                        accessControlService.checkPermission(currentUser, Resource.CONVERTER, operation, entityId, converter);
                    } catch (ThingsboardException e) {
                        return ValidationResult.accessDenied(e.getMessage());
                    }
                    return ValidationResult.ok(converter);
                }
            }), executor);
        }
    }

    private void validateTenantProfile(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.ok(null));
        } else {
            callback.onSuccess(ValidationResult.accessDenied(ONLY_SYSTEM_ADMINISTRATOR_IS_ALLOWED_TO_PERFORM_THIS_OPERATION));
        }
    }

    private void validateIntegration(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isCustomerUser()) {
            callback.onSuccess(ValidationResult.accessDenied(CUSTOMER_USER_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<Integration> integrationFuture = integrationService.findIntegrationByIdAsync(currentUser.getTenantId(), new IntegrationId(entityId.getId()));
            Futures.addCallback(integrationFuture, getCallback(callback, integration -> {
                if (integration == null) {
                    return ValidationResult.entityNotFound("Integration with requested id wasn't found!");
                } else {
                    try {
                        accessControlService.checkPermission(currentUser, Resource.INTEGRATION, operation, entityId, integration);
                    } catch (ThingsboardException e) {
                        return ValidationResult.accessDenied(e.getMessage());
                    }
                    return ValidationResult.ok(integration);
                }
            }), executor);
        }
    }

    private void validateSchedulerEvent(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<SchedulerEventInfo> schedulerEventInfoFuture = schedulerEventService.findSchedulerEventInfoByIdAsync(
                    currentUser.getTenantId(), new SchedulerEventId(entityId.getId()));
            Futures.addCallback(schedulerEventInfoFuture, getCallback(callback, schedulerEventInfo -> {
                if (schedulerEventInfo == null) {
                    return ValidationResult.entityNotFound("Scheduler event with requested id wasn't found!");
                } else {
                    try {
                        accessControlService.checkPermission(currentUser, Resource.SCHEDULER_EVENT, operation, entityId, schedulerEventInfo);
                    } catch (ThingsboardException e) {
                        return ValidationResult.accessDenied(e.getMessage());
                    }
                    return ValidationResult.ok(schedulerEventInfo);
                }
            }), executor);
        }
    }

    private void validateBlobEntity(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<BlobEntityInfo> blobEntityInfoFuture = blobEntityService.findBlobEntityInfoByIdAsync(
                    currentUser.getTenantId(), new BlobEntityId(entityId.getId()));
            Futures.addCallback(blobEntityInfoFuture, getCallback(callback, blobEntityInfo -> {
                if (blobEntityInfo == null) {
                    return ValidationResult.entityNotFound("Blob entity with requested id wasn't found!");
                } else {
                    try {
                        accessControlService.checkPermission(currentUser, Resource.BLOB_ENTITY, operation, entityId, blobEntityInfo);
                    } catch (ThingsboardException e) {
                        return ValidationResult.accessDenied(e.getMessage());
                    }
                    return ValidationResult.ok(blobEntityInfo);
                }
            }), executor);
        }
    }

    private void validateEntityGroup(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<EntityGroupInfo> entityGroupFuture = entityGroupService.findEntityGroupInfoByIdAsync(currentUser.getTenantId(), new EntityGroupId(entityId.getId()));
            Futures.addCallback(entityGroupFuture, getCallback(callback, entityGroup -> {
                if (entityGroup == null) {
                    return ValidationResult.entityNotFound("Entity group with requested id wasn't found!");
                } else {
                    try {
                        accessControlService.checkEntityGroupInfoPermission(currentUser, operation, entityGroup);
                    } catch (ThingsboardException e) {
                        return ValidationResult.accessDenied(e.getMessage());
                    }
                    return ValidationResult.ok(entityGroup);
                }
            }), executor);
        }
    }

    private void validateEntityView(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<EntityView> entityViewFuture = entityViewService.findEntityViewByIdAsync(currentUser.getTenantId(), new EntityViewId(entityId.getId()));
            Futures.addCallback(entityViewFuture, getCallback(callback, entityView -> {
                if (entityView == null) {
                    return ValidationResult.entityNotFound(ENTITY_VIEW_WITH_REQUESTED_ID_NOT_FOUND);
                } else {
                    try {
                        accessControlService.checkPermission(currentUser, Resource.ENTITY_VIEW, operation, entityId, entityView);
                    } catch (ThingsboardException e) {
                        return ValidationResult.accessDenied(e.getMessage());
                    }
                    return ValidationResult.ok(entityView);
                }
            }), executor);
        }
    }

    private void validateRole(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<Role> roleFuture = roleService.findRoleByIdAsync(currentUser.getTenantId(), new RoleId(entityId.getId()));
            Futures.addCallback(roleFuture, getCallback(callback, role -> {
                if (role == null) {
                    return ValidationResult.entityNotFound(ROLE_WITH_REQUESTED_ID_NOT_FOUND);
                } else {

                    try {
                        accessControlService.checkPermission(currentUser, Resource.ROLE, operation, entityId, role);
                    } catch (ThingsboardException e) {
                        return ValidationResult.accessDenied(e.getMessage());
                    }
                    return ValidationResult.ok(role);
                }
            }), executor);
        }
    }

    private void validateEdge(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<Edge> edgeFuture = edgeService.findEdgeByIdAsync(currentUser.getTenantId(), new EdgeId(entityId.getId()));
            Futures.addCallback(edgeFuture, getCallback(callback, edge -> {
                if (edge == null) {
                    return ValidationResult.entityNotFound(EDGE_WITH_REQUESTED_ID_NOT_FOUND);
                } else {
                    try {
                        accessControlService.checkPermission(currentUser, Resource.EDGE, operation, entityId, edge);
                    } catch (ThingsboardException e) {
                        return ValidationResult.accessDenied(e.getMessage());
                    }
                    return ValidationResult.ok(edge);
                }
            }), executor);
        }
    }

    private void validateDashboard(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<DashboardInfo> dashboardFuture = dashboardService.findDashboardInfoByIdAsync(currentUser.getTenantId(), new DashboardId(entityId.getId()));
            Futures.addCallback(dashboardFuture, getCallback(callback, dashboard -> {
                if (dashboard == null) {
                    return ValidationResult.entityNotFound(DASHBOARD_WITH_REQUESTED_ID_NOT_FOUND);
                } else {
                    try {
                        accessControlService.checkPermission(currentUser, Resource.DASHBOARD, operation, entityId, dashboard);
                    } catch (ThingsboardException e) {
                        return ValidationResult.accessDenied(e.getMessage());
                    }
                    return ValidationResult.ok(dashboard);
                }
            }), executor);
        }
    }

    private void validateAgentApplication(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
            return;
        }
        AgentApplication app = agentApplicationService.findById(currentUser.getTenantId(), new AgentApplicationId(entityId.getId()));
        if (app == null) {
            callback.onSuccess(ValidationResult.entityNotFound(AGENT_APPLICATION_WITH_REQUESTED_ID_NOT_FOUND));
            return;
        }
        Agent agent = agentService.findAgentById(currentUser.getTenantId(), app.getAgentId());
        if (agent == null) {
            callback.onSuccess(ValidationResult.entityNotFound(AGENT_WITH_REQUESTED_ID_NOT_FOUND));
            return;
        }
        Futures.addCallback(Futures.immediateFuture(app), getCallback(callback, a -> {
            try {
                accessControlService.checkPermission(currentUser, Resource.AGENT, operation, agent.getId(), agent);
            } catch (ThingsboardException e) {
                return ValidationResult.accessDenied(e.getMessage());
            }
            return ValidationResult.ok(a);
        }), executor);
    }

    private void validateAgentAppUnit(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
            return;
        }
        AgentAppUnit unit = agentAppUnitService.findAgentAppUnitById(currentUser.getTenantId(), new AgentAppUnitId(entityId.getId()));
        if (unit == null) {
            callback.onSuccess(ValidationResult.entityNotFound(AGENT_APP_UNIT_WITH_REQUESTED_ID_NOT_FOUND));
            return;
        }
        AgentApplication app = agentApplicationService.findById(currentUser.getTenantId(), unit.getAgentApplicationId());
        if (app == null) {
            callback.onSuccess(ValidationResult.entityNotFound(AGENT_APPLICATION_WITH_REQUESTED_ID_NOT_FOUND));
            return;
        }
        Agent agent = agentService.findAgentById(currentUser.getTenantId(), app.getAgentId());
        if (agent == null) {
            callback.onSuccess(ValidationResult.entityNotFound(AGENT_WITH_REQUESTED_ID_NOT_FOUND));
            return;
        }
        Futures.addCallback(Futures.immediateFuture(unit), getCallback(callback, u -> {
            try {
                accessControlService.checkPermission(currentUser, Resource.AGENT, operation, agent.getId(), agent);
            } catch (ThingsboardException e) {
                return ValidationResult.accessDenied(e.getMessage());
            }
            return ValidationResult.ok(u);
        }), executor);
    }

    private void validateAgent(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            Agent agent = agentService.findAgentById(currentUser.getTenantId(), new AgentId(entityId.getId()));
            Futures.addCallback(Futures.immediateFuture(agent), getCallback(callback, a -> {
                if (a == null) {
                    return ValidationResult.entityNotFound(AGENT_WITH_REQUESTED_ID_NOT_FOUND);
                } else {
                    try {
                        accessControlService.checkPermission(currentUser, Resource.AGENT, operation, entityId, a);
                    } catch (ThingsboardException e) {
                        return ValidationResult.accessDenied(e.getMessage());
                    }
                    return ValidationResult.ok(a);
                }
            }), executor);
        }
    }

    private <T, V> FutureCallback<T> getCallback(FutureCallback<ValidationResult> callback, Function<T, ValidationResult<V>> transformer) {
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

    public static void handleError(Throwable e, final DeferredResult<ResponseEntity> response, HttpStatus defaultErrorStatus) {
        ResponseEntity responseEntity;
        if (e instanceof ToErrorResponseEntity) {
            responseEntity = ((ToErrorResponseEntity) e).toErrorResponseEntity();
        } else if (e instanceof IllegalArgumentException || e instanceof IncorrectParameterException || e instanceof DataValidationException) {
            responseEntity = new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } else {
            responseEntity = new ResponseEntity<>(defaultErrorStatus);
        }
        response.setResult(responseEntity);
    }

    public interface ThreeConsumer<A, B, C> {
        void accept(A a, B b, C c);
    }
}
