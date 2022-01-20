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
import org.thingsboard.server.dao.exception.DataValidationException;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "get or create asset",
        configClazz = TbCreateAssetNodeConfiguration.class,
        nodeDescription = "Get or Create asset based on selected configuration",
        nodeDetails = "Try to find target asset by <b>Name pattern</b> or create asset if it doesn't exists. " +
                "In both cases incoming message send via <b>Success</b> chain.<br></br>" +
                "In case that asset already exists, outgoing message type will be set to <b>ASSET_FETCHED</b> " +
                "and asset entity will be acts as message originator.<br></br>" +
                "In case that asset doesn't exists, rule node will create an asset based on selected configuration " +
                "and generate a message with msg type <b>ASSET_CREATED</b> and asset entity as message originator. " +
                "Additionally <b>ENTITY_CREATED</b> event will generate and push to rule chain marked as root<br></br>" +
                "In both cases for message with type <b>ASSET_CREATED</b> and type <b>ASSET_FETCHED</b> message content will be not changed from initial incoming message.</br>" +
                "In case that <b>Name pattern</b> or <b>Type pattern</b> will be not specified or any processing errors will occurred the result message send via <b>Failure</b> chain.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeGetOrCreateAssetConfig",
        icon = "add_circle"
)
public class TbCreateAssetNode extends TbAbstractCreateEntityNode<TbCreateAssetNodeConfiguration> {

    @Override
    protected TbCreateAssetNodeConfiguration initConfiguration(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbCreateAssetNodeConfiguration.class);
    }

    @Override
    protected void processOnMsg(TbContext ctx, TbMsg msg) throws TbNodeException {
        String namePattern = config.getNamePattern();
        if (StringUtils.isEmpty(namePattern)) {
            ctx.tellFailure(msg, new DataValidationException("Asset name should be specified!"));
            return;
        }
        String typePattern = config.getTypePattern();
        if (StringUtils.isEmpty(typePattern)) {
            ctx.tellFailure(msg, new DataValidationException("Asset type should be specified!"));
            return;
        }
        try {
            TbMsg result;
            String assetName = TbNodeUtils.processPattern(namePattern, msg);
            validatePatternSubstitution(namePattern, assetName);
            Asset asset = ctx.getAssetService().findAssetByTenantIdAndName(ctx.getTenantId(), assetName);
            if (asset == null) {
                String assetType = TbNodeUtils.processPattern(typePattern, msg);
                validatePatternSubstitution(typePattern, assetType);
                Asset savedAsset = createAsset(ctx, msg, assetName, assetType);
                ctx.enqueue(ctx.assetCreatedMsg(savedAsset, ctx.getSelfId()),
                        () -> log.trace("Pushed Asset Created message: {}", savedAsset),
                        throwable -> log.warn("Failed to push Asset Created message: {}", savedAsset, throwable));
                result = ctx.transformMsg(msg, DataConstants.ASSET_CREATED, savedAsset.getId(), msg.getMetaData(), msg.getData());
            } else {
                result = ctx.transformMsg(msg, DataConstants.ASSET_FETCHED, asset.getId(), msg.getMetaData(), msg.getData());
            }
            ctx.tellSuccess(result);
        } catch (Exception e) {
            ctx.tellFailure(msg, e);
        }
    }

    private Asset createAsset(TbContext ctx, TbMsg msg, String name, String type) {
        Asset asset = new Asset();
        asset.setTenantId(ctx.getTenantId());
        asset.setName(name);
        asset.setType(type);
        String labelPattern = config.getLabelPattern();
        if (!StringUtils.isEmpty(labelPattern)) {
            asset.setLabel(TbNodeUtils.processPattern(labelPattern, msg));
        }
        String descriptionPattern = config.getDescriptionPattern();
        if (!StringUtils.isEmpty(descriptionPattern)) {
            ObjectNode additionalInfo = JacksonUtil.newObjectNode();
            additionalInfo.put("description", TbNodeUtils.processPattern(descriptionPattern, msg));
            asset.setAdditionalInfo(additionalInfo);
        }
        return ctx.getAssetService().saveAsset(asset);
    }

}
