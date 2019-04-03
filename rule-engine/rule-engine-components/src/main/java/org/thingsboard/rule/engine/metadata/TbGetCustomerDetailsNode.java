/**
 * Copyright © 2016-2019 The Thingsboard Authors
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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.util.EntityDetails;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

@Slf4j
@RuleNode(type = ComponentType.ENRICHMENT,
        name = "customer details",
        configClazz = TbGetCustomerDetailsNodeConfiguration.class,
        nodeDescription = "Adds fields from Customer details to the message body or metadata",
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
    protected TbMsg getDetails(TbContext ctx, TbMsg msg) {
        return getCustomerTbMsg(ctx, msg, getDataAsJson(msg));
    }

    private TbMsg getCustomerTbMsg(TbContext ctx, TbMsg msg, MessageData messageData) {
        JsonElement resultObject = null;
        if (!config.getDetailsList().isEmpty()) {
            for (EntityDetails entityDetails : config.getDetailsList()) {
                resultObject = addContactProperties(messageData.getData(), getCustomer(ctx, msg), entityDetails, CUSTOMER_PREFIX);
            }
            return transformMsg(ctx, msg, resultObject, messageData);
        } else {
            return msg;
        }
    }

    private Customer getCustomer(TbContext ctx, TbMsg msg) {
        switch (msg.getOriginator().getEntityType()) {
            case DEVICE:
                Device device = ctx.getDeviceService().findDeviceById(ctx.getTenantId(), new DeviceId(msg.getOriginator().getId()));
                if (!device.getCustomerId().isNullUid()) {
                    return ctx.getCustomerService().findCustomerById(ctx.getTenantId(), device.getCustomerId());
                } else {
                    throw new RuntimeException("Device with name '" + device.getName() + "' is not assigned to Customer.");
                }
            case ASSET:
                Asset asset = ctx.getAssetService().findAssetById(ctx.getTenantId(), new AssetId(msg.getOriginator().getId()));
                if (!asset.getCustomerId().isNullUid()) {
                    return ctx.getCustomerService().findCustomerById(ctx.getTenantId(), asset.getCustomerId());
                } else {
                    throw new RuntimeException("Asset with name '" + asset.getName() + "' is not assigned to Customer.");
                }
            case ENTITY_VIEW:
                EntityView entityView = ctx.getEntityViewService().findEntityViewById(ctx.getTenantId(), new EntityViewId(msg.getOriginator().getId()));
                if (!entityView.getCustomerId().isNullUid()) {
                    return ctx.getCustomerService().findCustomerById(ctx.getTenantId(), entityView.getCustomerId());
                } else {
                    throw new RuntimeException("EntityView with name '" + entityView.getName() + "' is not assigned to Customer.");
                }
            default:
                throw new RuntimeException("Entity with entityType '" + msg.getOriginator().getEntityType() + "' is not supported.");
        }
    }

}
