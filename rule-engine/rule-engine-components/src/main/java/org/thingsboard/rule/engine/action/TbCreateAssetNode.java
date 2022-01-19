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
package org.thingsboard.rule.engine.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.concurrent.ExecutionException;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "get or create asset",
        configClazz = TbCreateAssetNodeConfiguration.class,
        nodeDescription = "Get or Create asset based on selected configuration",
        nodeDetails = "Try to find target asset by <b>Name pattern</b> or create asset if it doesn't exists.</br>" +
                "In case that asset already exists, a message with asset entity as message originator and msg type <b>ASSET_FETCHED</b> will be generated.</br>" +
                "In case that asset doesn't exists, rule node will create an asset based on selected configuration and generate a message with asset entity as message originator and msg type <b>ASSET_CREATED</b>.</br>" +
                "Additionally <b>ENTITY_CREATED</b> event will generate and push to Root Rule Chain</br>" +
                "In both cases for message with type <b>ASSET_CREATED</b> and type <b>ASSET_FETCHED</b> message content will be not changed from initial incoming message.</br>" +
                "In case that <b>Name pattern</b> will be not specified or any processing errors will occurred the result message send via <b>Failure</b> chain.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeSaveAssetConfig",
        icon = "add_circle"
)
public class TbCreateAssetNode extends TbAbstractCreateEntityNode<TbCreateAssetNodeConfiguration> {

    @Override
    protected TbCreateAssetNodeConfiguration initConfiguration(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbCreateAssetNodeConfiguration.class);
    }

    @Override
    protected void processOnMsg(TbContext ctx, TbMsg msg) throws TbNodeException {
        if (StringUtils.isEmpty(name)) {
            ctx.tellFailure(msg, new IllegalArgumentException("Asset name is null or empty!"));
        } else {
            try {
                String assetName = TbNodeUtils.processPattern(name, msg);
                Asset asset = ctx.getAssetService().findAssetByTenantIdAndName(ctx.getTenantId(), assetName);
                if (asset == null) {
                    Asset savedAsset = createAsset(ctx, msg, assetName);
                    ctx.enqueue(ctx.assetCreatedMsg(savedAsset, ctx.getSelfId()),
                            () -> log.trace("Pushed Asset Created message: {}", savedAsset),
                            throwable -> log.warn("Failed to push Asset Created message: {}", savedAsset, throwable));
                    ctx.transformMsg(msg, DataConstants.ASSET_CREATED, savedAsset.getId(), msg.getMetaData(), msg.getData());
                } else {
                    ctx.transformMsg(msg, DataConstants.ASSET_FETCHED, asset.getId(), msg.getMetaData(), msg.getData());
                }
            } catch (Exception e) {
                ctx.tellFailure(msg, e);
            }
        }
    }

    private Asset createAsset(TbContext ctx, TbMsg msg, String name) {
        Asset asset = new Asset();
        asset.setName(name);
        if (!StringUtils.isEmpty(type)) {
            asset.setType(TbNodeUtils.processPattern(type, msg));
        }
        if (!StringUtils.isEmpty(label)) {
            asset.setLabel(TbNodeUtils.processPattern(label, msg));
        }
        if (!StringUtils.isEmpty(description)) {
            ObjectNode additionalInfo = JacksonUtil.newObjectNode();
            additionalInfo.put("description", TbNodeUtils.processPattern(description, msg));
            asset.setAdditionalInfo(additionalInfo);
        }
        return ctx.getAssetService().saveAsset(asset);
    }

}
