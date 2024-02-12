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
package org.thingsboard.rule.engine.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.EnumSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static org.thingsboard.common.util.DonAsynchron.withCallback;
import static org.thingsboard.server.common.data.msg.TbNodeConnectionType.FAILURE;
import static org.thingsboard.server.common.data.msg.TbNodeConnectionType.SUCCESS;

@Slf4j
public abstract class TbAbstractRelationActionNode<C extends TbAbstractRelationActionNodeConfiguration> implements TbNode {

    private final ConcurrentMap<EntityCreationLock, Object> entitiesCreationLocks = new ConcurrentReferenceHashMap<>();

    private static final Set<EntityType> supportedEntityTypes = EnumSet.of(EntityType.TENANT, EntityType.DEVICE,
            EntityType.ASSET, EntityType.CUSTOMER, EntityType.ENTITY_VIEW, EntityType.DASHBOARD, EntityType.EDGE, EntityType.USER);

    private static final String supportedEntityTypesStr = supportedEntityTypes.stream().map(Enum::name).collect(Collectors.joining(" ,"));

    protected C config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = loadEntityNodeActionConfig(configuration);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        withCallback(processEntityRelationAction(ctx, msg),
                actionResultAndMsg -> ctx.tellNext(actionResultAndMsg.getFirst(), actionResultAndMsg.getSecond() ? SUCCESS : FAILURE),
                t -> ctx.tellFailure(msg, t), MoreExecutors.directExecutor());
    }

    protected abstract ListenableFuture<TbPair<TbMsg, Boolean>> processEntityRelationAction(TbContext ctx, TbMsg msg);

    protected abstract boolean createEntityIfNotExists();

    protected abstract C loadEntityNodeActionConfig(TbNodeConfiguration configuration) throws TbNodeException;

    protected String processPattern(TbMsg msg, String pattern) {
        return TbNodeUtils.processPattern(pattern, msg);
    }

    protected ListenableFuture<EntityId> getTargetEntityId(TbContext ctx, TbMsg msg) {
        var entityType = config.getEntityType();
        var tenantId = ctx.getTenantId();
        if (EntityType.TENANT.equals(entityType)) {
            return ctx.getDbCallbackExecutor().executeAsync(() -> tenantId);
        }
        var targetEntityName = processPattern(msg, config.getEntityNamePattern());
        boolean createEntityIfNotExists = createEntityIfNotExists();
        switch (entityType) {
            case DEVICE:
                var deviceService = ctx.getDeviceService();
                if (createEntityIfNotExists) {
                    var entityCreationLock = new EntityCreationLock(tenantId, entityType, targetEntityName);
                    synchronized (entitiesCreationLocks.computeIfAbsent(entityCreationLock, k -> new Object())) {
                        return ctx.getDbCallbackExecutor().executeAsync(() -> {
                            var device = deviceService.findDeviceByTenantIdAndName(tenantId, targetEntityName);
                            if (device != null) {
                                return device.getId();
                            }
                            var deviceProfileName = processPattern(msg, config.getEntityTypePattern());
                            var newDevice = new Device();
                            newDevice.setName(targetEntityName);
                            newDevice.setType(deviceProfileName);
                            newDevice.setTenantId(tenantId);
                            var savedDevice = deviceService.saveDevice(newDevice);
                            ctx.getClusterService().onDeviceUpdated(savedDevice, null);
                            ctx.enqueue(ctx.deviceCreatedMsg(savedDevice, ctx.getSelfId()),
                                    () -> log.trace("Pushed Device Created message: {}", savedDevice),
                                    throwable -> log.warn("Failed to push Device Created message: {}", savedDevice, throwable));
                            return savedDevice.getId();
                        });
                    }
                }
                return ctx.getDbCallbackExecutor().executeAsync(() -> {
                    var device = deviceService.findDeviceByTenantIdAndName(tenantId, targetEntityName);
                    if (device == null) {
                        throw new NoSuchElementException("Device with name '" + targetEntityName + "' doesn't exist!");
                    }
                    return device.getId();
                });
            case ASSET:
                var assetService = ctx.getAssetService();
                if (createEntityIfNotExists) {
                    var entityCreationLock = new EntityCreationLock(tenantId, entityType, targetEntityName);
                    synchronized (entitiesCreationLocks.computeIfAbsent(entityCreationLock, k -> new Object())) {
                        return ctx.getDbCallbackExecutor().executeAsync(() -> {
                            var asset = assetService.findAssetByTenantIdAndName(tenantId, targetEntityName);
                            if (asset != null) {
                                return asset.getId();
                            }
                            var assetProfileName = processPattern(msg, config.getEntityTypePattern());
                            var newAsset = new Asset();
                            newAsset.setName(targetEntityName);
                            newAsset.setType(assetProfileName);
                            newAsset.setTenantId(tenantId);
                            var savedAsset = assetService.saveAsset(newAsset);
                            ctx.enqueue(ctx.assetCreatedMsg(savedAsset, ctx.getSelfId()),
                                    () -> log.trace("Pushed Asset Created message: {}", savedAsset),
                                    throwable -> log.warn("Failed to push Asset Created message: {}", savedAsset, throwable));
                            return savedAsset.getId();
                        });
                    }
                }
                return ctx.getDbCallbackExecutor().executeAsync(() -> {
                    var asset = assetService.findAssetByTenantIdAndName(tenantId, targetEntityName);
                    if (asset == null) {
                        throw new NoSuchElementException("Asset with name '" + targetEntityName + "' doesn't exist!");
                    }
                    return asset.getId();
                });
            case CUSTOMER:
                var customerService = ctx.getCustomerService();
                if (createEntityIfNotExists) {
                    var entityCreationLock = new EntityCreationLock(tenantId, entityType, targetEntityName);
                    synchronized (entitiesCreationLocks.computeIfAbsent(entityCreationLock, k -> new Object())) {
                        return ctx.getDbCallbackExecutor().executeAsync(() -> {
                            var customer = customerService.findCustomerByTenantIdAndTitleUsingCache(tenantId, targetEntityName);
                            if (customer != null) {
                                return customer.getId();
                            }
                            var newCustomer = new Customer();
                            newCustomer.setTitle(targetEntityName);
                            newCustomer.setTenantId(tenantId);
                            var savedCustomer = customerService.saveCustomer(newCustomer);
                            ctx.enqueue(ctx.customerCreatedMsg(savedCustomer, ctx.getSelfId()),
                                    () -> log.trace("Pushed Customer Created message: {}", savedCustomer),
                                    throwable -> log.warn("Failed to push Customer Created message: {}", savedCustomer, throwable));
                            return savedCustomer.getId();
                        });
                    }
                }
                return ctx.getDbCallbackExecutor().executeAsync(() -> {
                    var customer = customerService.findCustomerByTenantIdAndTitleUsingCache(tenantId, targetEntityName);
                    if (customer == null) {
                        throw new NoSuchElementException("Customer with title '" + targetEntityName + "' doesn't exist!");
                    }
                    return customer.getId();
                });
            case ENTITY_VIEW:
                return ctx.getDbCallbackExecutor().executeAsync(() -> {
                    var entityViewService = ctx.getEntityViewService();
                    var entityView = entityViewService.findEntityViewByTenantIdAndName(tenantId, targetEntityName);
                    if (entityView != null) {
                        return entityView.getId();
                    }
                    throw new NoSuchElementException("EntityView with name '" + targetEntityName + "' doesn't exist!");
                });
            case EDGE:
                return ctx.getDbCallbackExecutor().executeAsync(() -> {
                    var edgeService = ctx.getEdgeService();
                    var edge = edgeService.findEdgeByTenantIdAndName(tenantId, targetEntityName);
                    if (edge != null) {
                        return edge.getId();
                    }
                    throw new NoSuchElementException("Edge with name '" + targetEntityName + "' doesn't exist!");
                });
            case DASHBOARD:
                return ctx.getDbCallbackExecutor().executeAsync(() -> {
                    var dashboardService = ctx.getDashboardService();
                    var dashboardInfo = dashboardService.findFirstDashboardInfoByTenantIdAndName(tenantId, targetEntityName);
                    if (dashboardInfo != null) {
                        return dashboardInfo.getId();
                    }
                    throw new NoSuchElementException("Dashboard with title '" + targetEntityName + "' doesn't exist!");
                });
            case USER:
                return ctx.getDbCallbackExecutor().executeAsync(() -> {
                    var userService = ctx.getUserService();
                    var user = userService.findUserByTenantIdAndEmail(tenantId, targetEntityName);
                    if (user != null) {
                        return user.getId();
                    }
                    throw new NoSuchElementException("User with email '" + targetEntityName + "' doesn't exist!");
                });
            default:
                throw new IllegalArgumentException(unsupportedEntityTypeErrorMessage(entityType));
        }
    }

    protected void checkIfConfigEntityTypeIsSupported(EntityType entityType) throws TbNodeException {
        if (!supportedEntityTypes.contains(entityType)) {
            throw new TbNodeException(unsupportedEntityTypeErrorMessage(entityType), true);
        }
    }

    static String unsupportedEntityTypeErrorMessage(EntityType entityType) {
        return "Unsupported entity type '" + entityType +
                "'! Only " + supportedEntityTypesStr + " types are allowed.";
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        boolean hasChanges = false;
        var newConfigObjectNode = (ObjectNode) oldConfiguration;
        switch (fromVersion) {
            case 0: {
                if (!oldConfiguration.has("entityCacheExpiration")) {
                    break;
                }
                newConfigObjectNode.remove("entityCacheExpiration");

                var directionPropertyName = "direction";
                if (!newConfigObjectNode.has(directionPropertyName)) {
                    throw new TbNodeException("property to update: '" + directionPropertyName + "' doesn't exists in configuration!");
                }
                String direction = newConfigObjectNode.get(directionPropertyName).asText();
                if (EntitySearchDirection.TO.name().equals(direction)) {
                    newConfigObjectNode.put(directionPropertyName, EntitySearchDirection.FROM.name());
                    hasChanges = true;
                    break;
                }
                if (EntitySearchDirection.FROM.name().equals(direction)) {
                    newConfigObjectNode.put(directionPropertyName, EntitySearchDirection.TO.name());
                    hasChanges = true;
                    break;
                }
                throw new TbNodeException("property to update: '" + directionPropertyName + "' has invalid value!");
            }
        }
        return new TbPair<>(hasChanges, newConfigObjectNode);
    }

    @Data
    @RequiredArgsConstructor
    private static class EntityCreationLock {
        private final TenantId tenantId;
        private final EntityType entityType;
        private final String entityName;
    }

}
