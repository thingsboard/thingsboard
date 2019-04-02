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
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

@Slf4j
@RuleNode(type = ComponentType.ENRICHMENT,
        name = "tenant details",
        configClazz = TbGetTenantDetailsNodeConfiguration.class,
        nodeDescription = "Node fetch current Tenant details that selected from the drop-down list and add them to the message if they exist.",
        nodeDetails = "If selected checkbox: <b>Add selected details to the message metadata</b>, existing fields will add to the message metadata instead of message data.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbEnrichmentNodeEntityDetailsConfig")
public class TbGetTenantDetailsNode extends TbAbstractGetEntityDetailsNode<TbGetTenantDetailsNodeConfiguration> {


    private static final String TENANT_PREFIX = "tenant_";

    @Override
    protected TbGetTenantDetailsNodeConfiguration loadGetEntityDetailsNodeConfiguration(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbGetTenantDetailsNodeConfiguration.class);
    }

    @Override
    protected TbMsg getDetails(TbContext ctx, TbMsg msg) {
        return getTenantTbMsg(ctx, msg, getDataAsJson(msg));
    }

    private TbMsg getTenantTbMsg(TbContext ctx, TbMsg msg, MessageData messageData) {
        JsonElement resultObject = null;
        Tenant tenant = ctx.getTenantService().findTenantById(ctx.getTenantId());
        if (!config.getDetailsList().isEmpty()) {
            for (EntityDetails entityDetails : config.getDetailsList()) {
                resultObject = addTenantProperties(messageData.getData(), tenant, entityDetails);
            }
            return transformMsg(ctx, msg, resultObject, messageData);
        } else {
            return msg;
        }
    }

    private JsonElement addTenantProperties(JsonElement data, Tenant tenant, EntityDetails entityDetails) {
        JsonObject dataAsObject = data.getAsJsonObject();
        switch (entityDetails) {
            case ADDRESS:
                if (tenant.getAddress() != null)
                    dataAsObject.addProperty(TENANT_PREFIX + "address", tenant.getAddress());
                break;
            case ADDRESS2:
                if (tenant.getAddress2() != null)
                    dataAsObject.addProperty(TENANT_PREFIX + "address2", tenant.getAddress2());
                break;
            case CITY:
                if (tenant.getCity() != null) dataAsObject.addProperty(TENANT_PREFIX + "city", tenant.getCity());
                break;
            case COUNTRY:
                if (tenant.getCountry() != null)
                    dataAsObject.addProperty(TENANT_PREFIX + "country", tenant.getCountry());
                break;
            case STATE:
                if (tenant.getState() != null) dataAsObject.addProperty(TENANT_PREFIX + "state", tenant.getState());
                break;
            case EMAIL:
                if (tenant.getEmail() != null) dataAsObject.addProperty(TENANT_PREFIX + "email", tenant.getEmail());
                break;
            case PHONE:
                if (tenant.getPhone() != null) dataAsObject.addProperty(TENANT_PREFIX + "phone", tenant.getPhone());
                break;
            case ZIP:
                if (tenant.getZip() != null) dataAsObject.addProperty(TENANT_PREFIX + "zip", tenant.getZip());
                break;
            case ADDITIONAL_INFO:
                if (tenant.getAdditionalInfo().hasNonNull("description")) {
                    dataAsObject.addProperty(TENANT_PREFIX + "additionalInfo", tenant.getAdditionalInfo().get("description").asText());
                }
                break;
        }
        return dataAsObject;
    }
}
