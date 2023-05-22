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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.util.EntitiesRelatedDeviceIdAsyncLoader;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;

@Slf4j
@RuleNode(type = ComponentType.ENRICHMENT,
        name = "related device attributes",
        configClazz = TbGetDeviceAttrNodeConfiguration.class,
        nodeDescription = "Add originators related device attributes and latest telemetry values into message or message metadata",
        nodeDetails = "Related device lookup based on the configured relation query. " +
                "If multiple related devices are found, only first device is used for message enrichment, other entities are discarded.<br><br>" +
                "If Attributes enrichment configured, <i>CLIENT/SHARED/SERVER</i> attributes are added into message or message metadata " +
                "with specific prefix: <i>cs/shared/ss</i>. Latest telemetry value adds into message or message metadata without prefix. <br><br>" +
                "See the following examples of accessing those attributes in other nodes:<br>" +
                "<code>metadata.cs_serialNumber</code> - to access client side attribute 'serialNumber' that was fetched to message metadata.<br>" +
                "<code>metadata.shared_limit</code> - to access shared side attribute 'limit' that was fetched to message metadata.<br>" +
                "<code>msg.temperature</code> - to access latest telemetry 'temperature' that was fetched to message.<br>",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbEnrichmentNodeDeviceAttributesConfig")
public class TbGetDeviceAttrNode extends TbAbstractGetAttributesNode<TbGetDeviceAttrNodeConfiguration, DeviceId> {

    private static final String RELATED_DEVICE_NOT_FOUND_MESSAGE = "Failed to find related device to message originator using relation query specified in the configuration!";

    @Override
    protected TbGetDeviceAttrNodeConfiguration loadNodeConfiguration(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbGetDeviceAttrNodeConfiguration.class);
    }

    @Override
    protected ListenableFuture<DeviceId> findEntityIdAsync(TbContext ctx, TbMsg msg) {
        return Futures.transformAsync(
                EntitiesRelatedDeviceIdAsyncLoader.findDeviceAsync(ctx, msg.getOriginator(), config.getDeviceRelationsQuery()),
                checkIfEntityIsPresentOrThrow(RELATED_DEVICE_NOT_FOUND_MESSAGE),
                ctx.getDbCallbackExecutor());
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        return fromVersion == 0 ?
                upgradeRuleNodesWithOldPropertyToUseFetchTo(
                        oldConfiguration,
                        "fetchToData",
                        FetchTo.DATA.name(),
                        FetchTo.METADATA.name()) :
                new TbPair<>(false, oldConfiguration);
    }

}
