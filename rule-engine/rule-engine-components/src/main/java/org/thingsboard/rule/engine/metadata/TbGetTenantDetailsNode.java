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
package org.thingsboard.rule.engine.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;

@RuleNode(
        type = ComponentType.ENRICHMENT,
        name = "tenant details",
        configClazz = TbGetTenantDetailsNodeConfiguration.class,
        version = 1,
        nodeDescription = "Adds message originator tenant details into message or message metadata",
        nodeDetails = "Useful when we need to retrieve contact information from your tenant " +
                "such as email, phone, address, etc., for notifications via email, SMS, and other notification providers.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        configDirective = "tbEnrichmentNodeEntityDetailsConfig",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/enrichment/tenant-details/"
)
public class TbGetTenantDetailsNode extends TbAbstractGetEntityDetailsNode<TbGetTenantDetailsNodeConfiguration, TenantId> {

    private static final String TENANT_PREFIX = "tenant_";

    @Override
    protected TbGetTenantDetailsNodeConfiguration loadNodeConfiguration(TbNodeConfiguration configuration) throws TbNodeException {
        var config = TbNodeUtils.convert(configuration, TbGetTenantDetailsNodeConfiguration.class);
        checkIfDetailsListIsNotEmptyOrElseThrow(config.getDetailsList());
        return config;
    }

    @Override
    protected String getPrefix() {
        return TENANT_PREFIX;
    }

    @Override
    protected ListenableFuture<Tenant> getContactBasedFuture(TbContext ctx, TbMsg msg) {
        return ctx.getTenantService().findTenantByIdAsync(ctx.getTenantId(), ctx.getTenantId());
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        return fromVersion == 0 ?
                upgradeRuleNodesWithOldPropertyToUseFetchTo(
                        oldConfiguration,
                        "addToMetadata",
                        TbMsgSource.METADATA.name(),
                        TbMsgSource.DATA.name()) :
                new TbPair<>(false, oldConfiguration);
    }

}
