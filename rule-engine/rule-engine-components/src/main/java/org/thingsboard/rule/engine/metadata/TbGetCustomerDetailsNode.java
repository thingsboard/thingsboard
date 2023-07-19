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
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

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
public class TbGetCustomerDetailsNode extends TbAbstractGetEntityDetailsNode<TbGetCustomerDetailsNodeConfiguration> {

    private static final String CUSTOMER_PREFIX = "customer_";

    @Override
    protected TbGetCustomerDetailsNodeConfiguration loadGetEntityDetailsNodeConfiguration(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbGetCustomerDetailsNodeConfiguration.class);
    }

    @Override
    protected ListenableFuture<TbMsg> getDetails(TbContext ctx, TbMsg msg) {
        return getTbMsgListenableFuture(ctx, msg, getDataAsJson(msg), CUSTOMER_PREFIX);
    }

    @Override
    protected ListenableFuture<? extends ContactBased> getContactBasedListenableFuture(TbContext ctx, TbMsg msg) {
        return getCustomer(ctx, msg);
    }

    private ListenableFuture<Customer> getCustomer(TbContext ctx, TbMsg msg) {
        ListenableFuture<? extends HasCustomerId> entityFuture;
        switch (msg.getOriginator().getEntityType()) { // TODO: use EntityServiceRegistry
            case DEVICE:
                entityFuture = Futures.immediateFuture(ctx.getDeviceService().findDeviceById(ctx.getTenantId(), (DeviceId) msg.getOriginator()));
                break;
            case ASSET:
                entityFuture = ctx.getAssetService().findAssetByIdAsync(ctx.getTenantId(), (AssetId) msg.getOriginator());
                break;
            case ENTITY_VIEW:
                entityFuture = ctx.getEntityViewService().findEntityViewByIdAsync(ctx.getTenantId(), (EntityViewId) msg.getOriginator());
                break;
            case USER:
                entityFuture = ctx.getUserService().findUserByIdAsync(ctx.getTenantId(), (UserId) msg.getOriginator());
                break;
            case EDGE:
                entityFuture = ctx.getEdgeService().findEdgeByIdAsync(ctx.getTenantId(), (EdgeId) msg.getOriginator());
                break;
            default:
                throw new RuntimeException(msg.getOriginator().getEntityType().getNormalName() + " entities not supported");
        }
        return Futures.transformAsync(entityFuture, entity -> {
            if (entity != null) {
                if (!entity.getCustomerId().isNullUid()) {
                    return ctx.getCustomerService().findCustomerByIdAsync(ctx.getTenantId(), entity.getCustomerId());
                } else {
                    throw new RuntimeException(msg.getOriginator().getEntityType().getNormalName() +
                            (entity instanceof HasName ? " with name '" + ((HasName) entity).getName() + "'" : "")
                            + " is not assigned to Customer");
                }
            } else {
                return Futures.immediateFuture(null);
            }
        }, MoreExecutors.directExecutor());
    }

}
