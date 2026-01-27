/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
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
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.exception.DataValidationException;

import java.util.EnumSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
public abstract class TbAbstractRelationActionNode<C extends TbAbstractRelationActionNodeConfiguration> implements TbNode {

    private static final Set<EntityType> supportedEntityTypes = EnumSet.of(EntityType.TENANT, EntityType.DEVICE,
            EntityType.ASSET, EntityType.CUSTOMER, EntityType.ENTITY_VIEW, EntityType.DASHBOARD, EntityType.EDGE, EntityType.USER);

    private static final String supportedEntityTypesStr = supportedEntityTypes.stream().map(Enum::name).collect(Collectors.joining(" ,"));

    protected C config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = loadEntityNodeActionConfig(configuration);
    }

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
            case DEVICE -> {
                var deviceService = ctx.getDeviceService();
                var deviceByNameFuture = deviceService.findDeviceByTenantIdAndNameAsync(tenantId, targetEntityName);
                if (createEntityIfNotExists) {
                    return Futures.transform(deviceByNameFuture, device -> {
                        if (device != null) {
                            return device.getId();
                        }
                        try {
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
                        } catch (DataValidationException e) {
                            device = deviceService.findDeviceByTenantIdAndName(tenantId, targetEntityName);
                            if (device != null) {
                                return device.getId();
                            }
                            throw new RuntimeException("Failed to create device with name '" + targetEntityName + "' due to: ", e);
                        }
                    }, MoreExecutors.directExecutor());
                }
                return Futures.transform(deviceByNameFuture, device -> {
                    if (device == null) {
                        throw new NoSuchElementException("Device with name '" + targetEntityName + "' doesn't exist!");
                    }
                    return device.getId();
                }, MoreExecutors.directExecutor());
            }
            case ASSET -> {
                var assetService = ctx.getAssetService();
                var assetByNameFuture = assetService.findAssetByTenantIdAndNameAsync(tenantId, targetEntityName);
                if (createEntityIfNotExists) {
                    return Futures.transform(assetByNameFuture, asset -> {
                        if (asset != null) {
                            return asset.getId();
                        }
                        try {
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
                        } catch (DataValidationException e) {
                            asset = assetService.findAssetByTenantIdAndName(tenantId, targetEntityName);
                            if (asset != null) {
                                return asset.getId();
                            }
                            throw new RuntimeException("Failed to create asset with name '" + targetEntityName + "' due to: ", e);
                        }
                    }, MoreExecutors.directExecutor());
                }
                return Futures.transform(assetByNameFuture, asset -> {
                    if (asset == null) {
                        throw new NoSuchElementException("Asset with name '" + targetEntityName + "' doesn't exist!");
                    }
                    return asset.getId();
                }, MoreExecutors.directExecutor());
            }
            case CUSTOMER -> {
                var customerService = ctx.getCustomerService();
                var customerByTitleOptFuture = customerService.findCustomerByTenantIdAndTitleAsync(tenantId, targetEntityName);
                if (createEntityIfNotExists) {
                    return Futures.transform(customerByTitleOptFuture, customerOpt -> {
                        if (customerOpt.isPresent()) {
                            return customerOpt.get().getId();
                        }
                        try {
                            var newCustomer = new Customer();
                            newCustomer.setTitle(targetEntityName);
                            newCustomer.setTenantId(tenantId);
                            var savedCustomer = customerService.saveCustomer(newCustomer);
                            ctx.enqueue(ctx.customerCreatedMsg(savedCustomer, ctx.getSelfId()),
                                    () -> log.trace("Pushed Customer Created message: {}", savedCustomer),
                                    throwable -> log.warn("Failed to push Customer Created message: {}", savedCustomer, throwable));
                            return savedCustomer.getId();
                        } catch (DataValidationException e) {
                            customerOpt = customerService.findCustomerByTenantIdAndTitle(tenantId, targetEntityName);
                            if (customerOpt.isPresent()) {
                                return customerOpt.get().getId();
                            }
                            throw new RuntimeException("Failed to create customer with title '" + targetEntityName + "' due to: ", e);
                        }
                    }, MoreExecutors.directExecutor());
                }
                return Futures.transform(customerByTitleOptFuture, customerOpt -> {
                    if (customerOpt.isEmpty()) {
                        throw new NoSuchElementException("Customer with title '" + targetEntityName + "' doesn't exist!");
                    }
                    return customerOpt.get().getId();
                }, MoreExecutors.directExecutor());
            }
            case ENTITY_VIEW -> {
                var entityViewFuture = ctx.getEntityViewService().findEntityViewByTenantIdAndNameAsync(tenantId, targetEntityName);
                return Futures.transform(entityViewFuture, entityView -> {
                    if (entityView != null) {
                        return entityView.getId();
                    }
                    throw new NoSuchElementException("Entity View with name '" + targetEntityName + "' doesn't exist!");
                }, MoreExecutors.directExecutor());
            }
            case EDGE -> {
                var edgeFuture = ctx.getEdgeService().findEdgeByTenantIdAndNameAsync(tenantId, targetEntityName);
                return Futures.transform(edgeFuture, edge -> {
                    if (edge != null) {
                        return edge.getId();
                    }
                    throw new NoSuchElementException("Edge with name '" + targetEntityName + "' doesn't exist!");
                }, MoreExecutors.directExecutor());
            }
            case DASHBOARD -> {
                var dashboardInfoFuture = ctx.getDashboardService().findFirstDashboardInfoByTenantIdAndNameAsync(tenantId, targetEntityName);
                return Futures.transform(dashboardInfoFuture, dashboardInfo -> {
                    if (dashboardInfo != null) {
                        return dashboardInfo.getId();
                    }
                    throw new NoSuchElementException("Dashboard with title '" + targetEntityName + "' doesn't exist!");
                }, MoreExecutors.directExecutor());
            }
            case USER -> {
                var userFuture = ctx.getUserService().findUserByTenantIdAndEmailAsync(tenantId, targetEntityName);
                return Futures.transform(userFuture, user -> {
                    if (user != null) {
                        return user.getId();
                    }
                    throw new NoSuchElementException("User with email '" + targetEntityName + "' doesn't exist!");
                }, MoreExecutors.directExecutor());
            }
            default -> throw new IllegalArgumentException(unsupportedEntityTypeErrorMessage(entityType));
        }
    }

    protected ListenableFuture<Boolean> deleteRelationsByTypeAndDirection(TbContext ctx, TbMsg msg, Executor executor) {
        var relationType = processPattern(msg, config.getRelationType());
        return deleteRelationsByTypeAndDirection(ctx, msg, relationType, executor);
    }

    protected ListenableFuture<Boolean> deleteRelationsByTypeAndDirection(TbContext ctx, TbMsg msg, String relationType, Executor executor) {
        var tenantId = ctx.getTenantId();
        var originator = msg.getOriginator();
        var relationService = ctx.getRelationService();
        var originatorRelationsFuture = EntitySearchDirection.FROM.equals(config.getDirection()) ?
                relationService.findByFromAndTypeAsync(tenantId, originator, relationType, RelationTypeGroup.COMMON) :
                relationService.findByToAndTypeAsync(tenantId, originator, relationType, RelationTypeGroup.COMMON);
        return Futures.transformAsync(originatorRelationsFuture, originatorRelations -> {
            if (originatorRelations.isEmpty()) {
                return Futures.immediateFuture(true);
            }
            var deleteRelationFutures = originatorRelations.stream()
                    .map(entityRelation -> relationService.deleteRelationAsync(tenantId, entityRelation))
                    .collect(Collectors.toList());
            return Futures.transform(Futures.allAsList(deleteRelationFutures), deleteResults ->
                    deleteResults.stream().allMatch(Boolean::booleanValue), executor);
        }, executor);
    }

    protected void checkIfConfigEntityTypeIsSupported(EntityType entityType) throws TbNodeException {
        if (!supportedEntityTypes.contains(entityType)) {
            throw new TbNodeException(unsupportedEntityTypeErrorMessage(entityType), true);
        }
    }

    private static String unsupportedEntityTypeErrorMessage(EntityType entityType) {
        return "Unsupported entity type '" + entityType +
                "'! Only " + supportedEntityTypesStr + " types are allowed.";
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        boolean hasChanges = false;
        var config = (ObjectNode) oldConfiguration;
        switch (fromVersion) {
            case 0 -> {
                if (!config.has("entityCacheExpiration")) {
                    break;
                }
                config.remove("entityCacheExpiration");

                var directionPropertyName = "direction";
                if (!config.has(directionPropertyName)) {
                    throw new TbNodeException("property to update: '" + directionPropertyName + "' doesn't exists in configuration!");
                }
                String direction = config.get(directionPropertyName).asText();
                if (EntitySearchDirection.TO.name().equals(direction)) {
                    config.put(directionPropertyName, EntitySearchDirection.FROM.name());
                    hasChanges = true;
                    break;
                }
                if (EntitySearchDirection.FROM.name().equals(direction)) {
                    config.put(directionPropertyName, EntitySearchDirection.TO.name());
                    hasChanges = true;
                    break;
                }
                throw new TbNodeException("property to update: '" + directionPropertyName + "' has invalid value!");
            }
        }
        return new TbPair<>(hasChanges, config);
    }

}
