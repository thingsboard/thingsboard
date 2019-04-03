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
        nodeDescription = "Node find the customer of the message originator and fetch his details that selected from the drop-down list and add them to the message if they exist.",
        nodeDetails = "If selected checkbox: <b>Add selected details to the message metadata</b>, existing fields will add to the message metadata instead of message data.<br><br>" +
                "<b>Note:</b> only Device, Asset, and Entity View type are allowed.<br><br>" +
                "If the originator of the message didn't assign to Customer, or originator type is not supported - Message send via <b>Failure</b> chain, otherwise, <b>Success</b> chain will be used.",
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
                resultObject = addCustomerProperties(messageData.getData(), getCustomer(ctx, msg), entityDetails);
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
                    throw new RuntimeException("Device with name '" + device.getName() + "' didn't assign to Customer.");
                }
            case ASSET:
                Asset asset = ctx.getAssetService().findAssetById(ctx.getTenantId(), new AssetId(msg.getOriginator().getId()));
                if (!asset.getCustomerId().isNullUid()) {
                    return ctx.getCustomerService().findCustomerById(ctx.getTenantId(), asset.getCustomerId());
                } else {
                    throw new RuntimeException("Asset with name '" + asset.getName() + "' didn't assign to Customer.");
                }
            case ENTITY_VIEW:
                EntityView entityView = ctx.getEntityViewService().findEntityViewById(ctx.getTenantId(), new EntityViewId(msg.getOriginator().getId()));
                if (!entityView.getCustomerId().isNullUid()) {
                    return ctx.getCustomerService().findCustomerById(ctx.getTenantId(), entityView.getCustomerId());
                } else {
                    throw new RuntimeException("EntityView with name '" + entityView.getName() + "' didn't assign to Customer.");
                }
            default:
                throw new RuntimeException("Entity with entityType '" + msg.getOriginator().getEntityType() + "' can't be assigned to Customer.");
        }
    }

    private JsonElement addCustomerProperties(JsonElement data, Customer customer, EntityDetails entityDetails) {
        JsonObject dataAsObject = data.getAsJsonObject();
        switch (entityDetails) {
            case ADDRESS:
                if (customer.getAddress() != null)
                    dataAsObject.addProperty(CUSTOMER_PREFIX + "address", customer.getAddress());
                break;
            case ADDRESS2:
                if (customer.getAddress2() != null)
                    dataAsObject.addProperty(CUSTOMER_PREFIX + "address2", customer.getAddress2());
                break;
            case CITY:
                if (customer.getCity() != null) dataAsObject.addProperty(CUSTOMER_PREFIX + "city", customer.getCity());
                break;
            case COUNTRY:
                if (customer.getCountry() != null)
                    dataAsObject.addProperty(CUSTOMER_PREFIX + "country", customer.getCountry());
                break;
            case STATE:
                if (customer.getState() != null)
                    dataAsObject.addProperty(CUSTOMER_PREFIX + "state", customer.getState());
                break;
            case EMAIL:
                if (customer.getEmail() != null)
                    dataAsObject.addProperty(CUSTOMER_PREFIX + "email", customer.getEmail());
                break;
            case PHONE:
                if (customer.getPhone() != null)
                    dataAsObject.addProperty(CUSTOMER_PREFIX + "phone", customer.getPhone());
                break;
            case ZIP:
                if (customer.getZip() != null) dataAsObject.addProperty(CUSTOMER_PREFIX + "zip", customer.getZip());
                break;
            case ADDITIONAL_INFO:
                if (customer.getAdditionalInfo().hasNonNull("description")) {
                    dataAsObject.addProperty(CUSTOMER_PREFIX + "additionalInfo", customer.getAdditionalInfo().get("description").asText());
                }
                break;
        }
        return dataAsObject;
    }
}
