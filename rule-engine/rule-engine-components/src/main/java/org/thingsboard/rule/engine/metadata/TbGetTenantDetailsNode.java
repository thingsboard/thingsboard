/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

@Slf4j
@RuleNode(type = ComponentType.ENRICHMENT,
        name = "tenant details",
        configClazz = TbGetTenantDetailsNodeConfiguration.class,
        nodeDescription = "Adds fields from Tenant details to the message body or metadata",
        nodeDetails = "If checkbox: <b>Add selected details to the message metadata</b> is selected, existing fields will be added to the message metadata instead of message data.<br><br>" +
                "<b>Note:</b> only Device, Asset, and Entity View type are allowed.<br><br>" +
                "If the originator of the message is not assigned to Tenant, or originator type is not supported - Message will be forwarded to <b>Failure</b> chain, otherwise, <b>Success</b> chain will be used.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbEnrichmentNodeEntityDetailsConfig")
public class TbGetTenantDetailsNode extends TbAbstractGetEntityDetailsNode<TbGetTenantDetailsNodeConfiguration> {

    private static final String TENANT_PREFIX = "tenant_";

    @Override
    protected TbGetTenantDetailsNodeConfiguration loadGetEntityDetailsNodeConfiguration(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbGetTenantDetailsNodeConfiguration.class);
    }

    @Override
    protected ListenableFuture<TbMsg> getDetails(TbContext ctx, TbMsg msg) {
        return getTbMsgListenableFuture(ctx, msg, getDataAsJson(msg), TENANT_PREFIX);
    }

    @Override
    protected ListenableFuture<ContactBased> getContactBasedListenableFuture(TbContext ctx, TbMsg msg) {
        return Futures.transformAsync(ctx.getTenantService().findTenantByIdAsync(ctx.getTenantId(), ctx.getTenantId()), tenant -> {
            if (tenant != null) {
                return Futures.immediateFuture(tenant);
            } else {
                return Futures.immediateFuture(null);
            }
        }, MoreExecutors.directExecutor());
    }
}
