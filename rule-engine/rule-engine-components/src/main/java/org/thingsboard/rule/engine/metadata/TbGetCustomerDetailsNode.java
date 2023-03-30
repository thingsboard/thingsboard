/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.rule.engine.metadata;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.ContactBased;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.NoSuchElementException;

@Slf4j
@RuleNode(type = ComponentType.ENRICHMENT,
        name = "customer details",
        configClazz = TbGetCustomerDetailsNodeConfiguration.class,
        nodeDescription = "Enrich the message body or metadata with the corresponding customer details: title, address, email, phone, etc.",
        nodeDetails = "If checkbox: <b>Add selected details to the message metadata</b> is selected, existing fields will be added to the message metadata instead of message data.<br><br>" +
                "<b>Note:</b> only Device, Asset, and Entity View type are allowed.<br><br>" +
                "If the originator of the message is not assigned to Customer, or originator type is not supported - Message will be forwarded to <b>Failure</b> chain, otherwise, <b>Success</b> chain will be used.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbEnrichmentNodeEntityDetailsConfig")
public class TbGetCustomerDetailsNode extends TbAbstractGetEntityDetailsNode<TbGetCustomerDetailsNodeConfiguration, CustomerId> {

    private static final String CUSTOMER_PREFIX = "customer_";

    @Override
    protected TbGetCustomerDetailsNodeConfiguration loadNodeConfiguration(TbNodeConfiguration configuration) throws TbNodeException {
        var config = TbNodeUtils.convert(configuration, TbGetCustomerDetailsNodeConfiguration.class);
        checkIfDetailsListIsNotEmptyOrThrow(config);
        return config;
    }

    @Override
    protected String getPrefix() {
        return CUSTOMER_PREFIX;
    }

    @Override
    protected ListenableFuture<? extends ContactBased<CustomerId>> getContactBasedFuture(TbContext ctx, TbMsg msg) {
        switch (msg.getOriginator().getEntityType()) {
            case DEVICE:
                return Futures.transformAsync(ctx.getDeviceService().findDeviceByIdAsync(ctx.getTenantId(), new DeviceId(msg.getOriginator().getId())),
                        device -> getCustomerFuture(ctx, device, msg.getOriginator()), MoreExecutors.directExecutor());
            case ASSET:
                return Futures.transformAsync(ctx.getAssetService().findAssetByIdAsync(ctx.getTenantId(), new AssetId(msg.getOriginator().getId())),
                        asset -> getCustomerFuture(ctx, asset, msg.getOriginator()), MoreExecutors.directExecutor());
            case ENTITY_VIEW:
                return Futures.transformAsync(ctx.getEntityViewService().findEntityViewByIdAsync(ctx.getTenantId(), new EntityViewId(msg.getOriginator().getId())),
                        entityView -> getCustomerFuture(ctx, entityView, msg.getOriginator()), MoreExecutors.directExecutor());
            case USER:
                return Futures.transformAsync(ctx.getUserService().findUserByIdAsync(ctx.getTenantId(), new UserId(msg.getOriginator().getId())),
                        user -> getCustomerFuture(ctx, user, msg.getOriginator()), MoreExecutors.directExecutor());
            case EDGE:
                return Futures.transformAsync(ctx.getEdgeService().findEdgeByIdAsync(ctx.getTenantId(), new EdgeId(msg.getOriginator().getId())),
                        edge -> getCustomerFuture(ctx, edge, msg.getOriginator()), MoreExecutors.directExecutor());
            default:
                return Futures.immediateFailedFuture(new NoSuchElementException("Entity with entityType '" + msg.getOriginator().getEntityType() + "' is not supported."));
        }
    }

    private ListenableFuture<Customer> getCustomerFuture(TbContext ctx, HasCustomerId hasCustomerId, EntityId originator) {
        if (hasCustomerId == null) {
            return Futures.immediateFuture(null);
        } else {
            if (hasCustomerId.getCustomerId().isNullUid()) {
                if (hasCustomerId instanceof HasName) {
                    var hasName = (HasName) hasCustomerId;
                    throw new RuntimeException(originator.getEntityType().getNormalName() + " with name '" + hasName.getName() + "' is not assigned to Customer.");
                }
                throw new RuntimeException(originator.getEntityType().getNormalName() + " with id '" + originator + "' is not assigned to Customer.");
            } else {
                return ctx.getCustomerService().findCustomerByIdAsync(ctx.getTenantId(), hasCustomerId.getCustomerId());
            }
        }
    }

}
