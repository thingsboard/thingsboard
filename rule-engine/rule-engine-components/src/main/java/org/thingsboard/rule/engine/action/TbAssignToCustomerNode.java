/**
 * Copyright © 2016-2018 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.rule.engine.action;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.rule.engine.api.util.DonAsynchron;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.plugin.ComponentType;

import org.thingsboard.server.common.msg.TbMsg;

import java.util.UUID;

import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;


@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "assign to customer",
        configClazz = TbAssignToCustomerNodeConfiguration.class,
        nodeDescription = "",
        nodeDetails = "",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeAssignToCustomerConfig")
public class TbAssignToCustomerNode implements TbNode {

    private TbAssignToCustomerNodeConfiguration config;
    private CustomerId customerId;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbAssignToCustomerNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        EntityType type = msg.getOriginator().getEntityType();
        switch (type) {
            case DEVICE:
                processDevice(ctx, msg);
                break;
            case ASSET:
                processAsset(ctx, msg);
                break;
            case ENTITY_VIEW:
                processEntityView(ctx, msg);
                break;
            case DASHBOARD:

                break;
        }
    }

    private void processDevice(TbContext ctx, TbMsg msg) {
        ListenableFuture<Device> deviceListenableFuture = ctx.getDeviceService().findDeviceByIdAsync(ctx.getTenantId(), new DeviceId(msg.getOriginator().getId()));
        DonAsynchron.withCallback(deviceListenableFuture, device -> {
            if (!device.getCustomerId().isNullUid()) {
                customerId = device.getCustomerId();
                if (customerId.equals(new CustomerId(UUID.fromString(config.getCustomerId())))) {
                    ctx.tellNext(msg, SUCCESS, new Throwable("Device: " + device.getName() + " is already assign to Customer"));
                } else {
                    //ctx.getDeviceService().unassignDeviceFromCustomer(ctx.getTenantId(), new DeviceId(msg.getOriginator().getId()));
                    ctx.getDeviceService().assignDeviceToCustomer(ctx.getTenantId(), new DeviceId(msg.getOriginator().getId()), new CustomerId(UUID.fromString(config.getCustomerId())));
                    ctx.tellNext(msg, SUCCESS);
                }
            } else {
                ctx.getDeviceService().assignDeviceToCustomer(ctx.getTenantId(), new DeviceId(msg.getOriginator().getId()), new CustomerId(UUID.fromString(config.getCustomerId())));
                ctx.tellNext(msg, SUCCESS);
            }
        }, error -> ctx.tellFailure(msg, error), ctx.getDbCallbackExecutor());
    }


    private void processAsset(TbContext ctx, TbMsg msg) {
        ListenableFuture<Asset> assetListenableFuture = ctx.getAssetService().findAssetByIdAsync(ctx.getTenantId(), new AssetId(msg.getOriginator().getId()));
        DonAsynchron.withCallback(assetListenableFuture, asset -> {
            if (!asset.getCustomerId().isNullUid()) {
                customerId = asset.getCustomerId();
                if (customerId.equals(new CustomerId(UUID.fromString(config.getCustomerId())))) {
                    ctx.tellNext(msg, SUCCESS, new Throwable("Asset: " + asset.getName() + " is already assign to Customer"));
                } else {
                    //ctx.getAssetService().unassignAssetFromCustomer(ctx.getTenantId(), new AssetId(msg.getOriginator().getId()));
                    ctx.getAssetService().assignAssetToCustomer(ctx.getTenantId(), new AssetId(msg.getOriginator().getId()), new CustomerId(UUID.fromString(config.getCustomerId())));
                    ctx.tellNext(msg, SUCCESS);
                }
            } else {
                ctx.getAssetService().assignAssetToCustomer(ctx.getTenantId(), new AssetId(msg.getOriginator().getId()), new CustomerId(UUID.fromString(config.getCustomerId())));
                ctx.tellNext(msg, SUCCESS);
            }
        }, error -> ctx.tellFailure(msg, error), ctx.getDbCallbackExecutor());
    }

    private void processEntityView(TbContext ctx, TbMsg msg) {
        ListenableFuture<EntityView> entityViewListenableFuture = ctx.getEntityViewService().findEntityViewByIdAsync(ctx.getTenantId(), new EntityViewId(msg.getOriginator().getId()));
        DonAsynchron.withCallback(entityViewListenableFuture, entityView -> {
            if (!entityView.getCustomerId().isNullUid()) {
                customerId = entityView.getCustomerId();
                if (customerId.equals(new CustomerId(UUID.fromString(config.getCustomerId())))) {
                    ctx.tellNext(msg, SUCCESS, new Throwable("EntityView: " + entityView.getName() + " is already assign to Customer"));
                } else {
                    //ctx.getEntityViewService().unassignEntityViewFromCustomer(ctx.getTenantId(), new EntityViewId(msg.getOriginator().getId()));
                    ctx.getEntityViewService().assignEntityViewToCustomer(ctx.getTenantId(), new EntityViewId(msg.getOriginator().getId()), new CustomerId(UUID.fromString(config.getCustomerId())));
                    ctx.tellNext(msg, SUCCESS);
                }
            } else {
                ctx.getEntityViewService().assignEntityViewToCustomer(ctx.getTenantId(), new EntityViewId(msg.getOriginator().getId()), new CustomerId(UUID.fromString(config.getCustomerId())));
                ctx.tellNext(msg, SUCCESS);
            }
        }, error -> ctx.tellFailure(msg, error), ctx.getDbCallbackExecutor());
    }

    @Override
    public void destroy() {
    }


}
