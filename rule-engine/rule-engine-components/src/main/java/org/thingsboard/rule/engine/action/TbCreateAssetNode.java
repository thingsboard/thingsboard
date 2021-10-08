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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.concurrent.ExecutionException;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "create asset",
        configClazz = TbCreateDeviceNodeConfiguration.class,
        nodeDescription = "Create or update asset.",
        nodeDetails = "Details - to create the asset should be indicated asset name and type, otherwise message send via " +
                "<b>Failure</b> chain. If the asset already exists or successfully created -  " +
                "Message send via <b>Success</b> chain, otherwise <b>Failure</b> chain will be used.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "",
        icon = "add_circle"
)
public class TbCreateAssetNode implements TbNode {

    private TbCreateAssetNodeConfiguration config;
    private String assetName;
    private String assetType;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbCreateAssetNodeConfiguration.class);
        this.assetName = config.getName();
        this.assetType = config.getType();
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        if (StringUtils.isEmpty(assetName)) {
            ctx.tellFailure(msg, new IllegalArgumentException("Asset name is null or empty "));
        } else {
            assetName = TbNodeUtils.processPattern(assetName, msg);
            if (StringUtils.isEmpty(assetType)) {
                ctx.tellFailure(msg, new IllegalArgumentException("Asset type is null or empty "));
            } else {
                try {
                    assetType = TbNodeUtils.processPattern(assetType, msg);
                    checkRegexValidation(assetName, assetType);
                    Asset asset = ctx.getAssetService().findAssetByTenantIdAndName(ctx.getTenantId(), assetName);

                    if (asset == null) {
                        asset = new Asset();
                        asset.setTenantId(ctx.getTenantId());
                    }
                    createOrUpdateAsset(asset, msg);
                    ctx.getAssetService().saveAsset(asset);
                    ctx.tellSuccess(msg);
                } catch (Exception e) {
                    ctx.tellFailure(msg, e);
                }
            }
        }
    }

    @Override
    public void destroy() {

    }

    private void createOrUpdateAsset(Asset asset, TbMsg msg) {
        asset.setName(assetName);
        asset.setType(assetType);
        if (!StringUtils.isEmpty(config.getLabel())) {
            asset.setLabel(TbNodeUtils.processPattern(config.getLabel(), msg));
        }
        asset.setAdditionalInfo(createAdditionalInfo(asset));
    }

    private JsonNode createAdditionalInfo(Asset asset) {
        JsonNode additionalInfo = asset.getAdditionalInfo();
        ObjectNode additionalInfoObjNode;
        if (additionalInfo == null) {
            additionalInfoObjNode = JacksonUtil.newObjectNode();
        } else {
            additionalInfoObjNode = (ObjectNode) additionalInfo;
        }
        additionalInfoObjNode.put("description", config.getDescription());
        return additionalInfoObjNode;
    }

    private void checkRegexValidation(String name, String type) {
        if (type.equals(config.getType())) {
            assetType = replaceRegex(type);
        }
        if (name.equals(config.getName())) {
            assetName = replaceRegex(name);
        }
    }

    private String replaceRegex(String pattern) {
        return pattern.replaceAll("\\$\\{?\\[?", "").replaceAll("}?]?", "");
    }
}
