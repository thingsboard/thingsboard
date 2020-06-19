/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.EmptyNodeConfiguration;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "copy to view",
        configClazz = EmptyNodeConfiguration.class,
        nodeDescription = "Copy attributes from asset/device to entity view and changes message originator to related entity view",
        nodeDetails = "Copy attributes from asset/device to related entity view according to entity view configuration. \n " +
                "Copy will be done only for attributes that are between start and end dates and according to attribute keys configuration. \n" +
                "Changes message originator to related entity view and produces new messages according to count of updated entity views",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbNodeEmptyConfig",
        icon = "content_copy",
        ruleChainTypes = {RuleChainType.SYSTEM, RuleChainType.EDGE}
)
public class TbCopyAttributesToEntityViewNode implements TbNode {

    EmptyNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, EmptyNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (DataConstants.ATTRIBUTES_UPDATED.equals(msg.getType()) ||
                DataConstants.ATTRIBUTES_DELETED.equals(msg.getType()) ||
                DataConstants.ACTIVITY_EVENT.equals(msg.getType()) ||
                SessionMsgType.POST_ATTRIBUTES_REQUEST.name().equals(msg.getType())) {
            if (!msg.getMetaData().getData().isEmpty()) {
                long now = System.currentTimeMillis();
                String scope = msg.getType().equals(SessionMsgType.POST_ATTRIBUTES_REQUEST.name()) ?
                        DataConstants.CLIENT_SCOPE : msg.getMetaData().getValue("scope");

                ListenableFuture<List<EntityView>> entityViewsFuture =
                        ctx.getEntityViewService().findEntityViewsByTenantIdAndEntityIdAsync(ctx.getTenantId(), msg.getOriginator());

                DonAsynchron.withCallback(entityViewsFuture,
                        entityViews -> {
                            for (EntityView entityView : entityViews) {
                                long startTime = entityView.getStartTimeMs();
                                long endTime = entityView.getEndTimeMs();
                                if ((endTime != 0 && endTime > now && startTime < now) || (endTime == 0 && startTime < now)) {
                                    if (DataConstants.ATTRIBUTES_UPDATED.equals(msg.getType()) ||
                                            DataConstants.ACTIVITY_EVENT.equals(msg.getType()) ||
                                            SessionMsgType.POST_ATTRIBUTES_REQUEST.name().equals(msg.getType())) {
                                        Set<AttributeKvEntry> attributes = JsonConverter.convertToAttributes(new JsonParser().parse(msg.getData()));
                                        List<AttributeKvEntry> filteredAttributes =
                                                attributes.stream().filter(attr -> attributeContainsInEntityView(scope, attr.getKey(), entityView)).collect(Collectors.toList());
                                        ctx.getTelemetryService().saveAndNotify(ctx.getTenantId(), entityView.getId(), scope, filteredAttributes,
                                                new FutureCallback<Void>() {
                                                    @Override
                                                    public void onSuccess(@Nullable Void result) {
                                                        transformAndTellNext(ctx, msg, entityView);
                                                    }

                                                    @Override
                                                    public void onFailure(Throwable t) {
                                                        ctx.tellFailure(msg, t);
                                                    }
                                                });
                                    } else if (DataConstants.ATTRIBUTES_DELETED.equals(msg.getType())) {
                                        List<String> attributes = new ArrayList<>();
                                        for (JsonElement element : new JsonParser().parse(msg.getData()).getAsJsonObject().get("attributes").getAsJsonArray()) {
                                            if (element.isJsonPrimitive()) {
                                                JsonPrimitive value = element.getAsJsonPrimitive();
                                                if (value.isString()) {
                                                    attributes.add(value.getAsString());
                                                }
                                            }
                                        }
                                        List<String> filteredAttributes =
                                                attributes.stream().filter(attr -> attributeContainsInEntityView(scope, attr, entityView)).collect(Collectors.toList());
                                        if (!filteredAttributes.isEmpty()) {
                                            ctx.getAttributesService().removeAll(ctx.getTenantId(), entityView.getId(), scope, filteredAttributes);
                                            transformAndTellNext(ctx, msg, entityView);
                                        }
                                    }
                                }
                            }
                            ctx.ack(msg);
                        },
                        t -> ctx.tellFailure(msg, t));
            } else {
                ctx.tellFailure(msg, new IllegalArgumentException("Message metadata is empty"));
            }
        } else {
            ctx.tellFailure(msg, new IllegalArgumentException("Unsupported msg type [" + msg.getType() + "]"));
        }
    }

    private void transformAndTellNext(TbContext ctx, TbMsg msg, EntityView entityView) {
        ctx.enqueueForTellNext(ctx.newMsg(msg.getQueueName(), msg.getType(), entityView.getId(), msg.getMetaData(), msg.getData()), SUCCESS);
    }

    private boolean attributeContainsInEntityView(String scope, String attrKey, EntityView entityView) {
        switch (scope) {
            case DataConstants.CLIENT_SCOPE:
                return entityView.getKeys().getAttributes().getCs().contains(attrKey);
            case DataConstants.SERVER_SCOPE:
                return entityView.getKeys().getAttributes().getSs().contains(attrKey);
            case DataConstants.SHARED_SCOPE:
                return entityView.getKeys().getAttributes().getSh().contains(attrKey);
        }
        return false;
    }

    @Override
    public void destroy() {
    }
}
