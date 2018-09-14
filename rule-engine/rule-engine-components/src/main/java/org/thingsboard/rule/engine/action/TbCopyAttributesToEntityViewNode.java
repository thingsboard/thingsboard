/**
 * Copyright © 2016-2018 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.EmptyNodeConfiguration;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TbRelationTypes;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.rule.engine.api.util.DonAsynchron.withCallback;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "copy attributes",
        configClazz = EmptyNodeConfiguration.class,
        nodeDescription = "Copy attributes from asset/device to entity view",
        nodeDetails = "Copy attributes from asset/device to related entity view according to entity view configuration. \n " +
                "Copy will be done only for attributes that are between start and end dates and according to attribute keys configuration",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbNodeEmptyConfig",
        icon = "content_copy"
)
public class TbCopyAttributesToEntityViewNode implements TbNode {

    EmptyNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, EmptyNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        if (msg.getType().equals(SessionMsgType.POST_ATTRIBUTES_REQUEST.name()) ||
                msg.getType().equals(DataConstants.ATTRIBUTES_DELETED) ||
                msg.getType().equals(DataConstants.ATTRIBUTES_UPDATED)) {
            long now = System.currentTimeMillis();
            String scope;
            if (msg.getType().equals(SessionMsgType.POST_ATTRIBUTES_REQUEST.name())) {
                scope = DataConstants.CLIENT_SCOPE;
            } else {
                scope = msg.getMetaData().getValue("scope");
            }
            ListenableFuture<List<EntityView>> entityViewsFuture =
                    ctx.getEntityViewService().findEntityViewsByTenantIdAndEntityIdAsync(ctx.getTenantId(), msg.getOriginator());
            withCallback(entityViewsFuture,
                    entityViews -> {
                        List<ListenableFuture<List<Void>>> saveFutures = new ArrayList<>();
                        for (EntityView entityView : entityViews) {
                            if ((entityView.getEndTimeMs() != 0  && entityView.getEndTimeMs() > now && entityView.getStartTimeMs() < now) ||
                                    (entityView.getEndTimeMs() == 0 && entityView.getStartTimeMs() < now)) {
                                Set<AttributeKvEntry> attributes = JsonConverter.convertToAttributes(new JsonParser().parse(msg.getData())).getAttributes();
                                List<AttributeKvEntry> filteredAttributes =
                                        attributes.stream()
                                                .filter(attr -> {
                                                    switch (scope) {
                                                        case DataConstants.CLIENT_SCOPE:
                                                            if (entityView.getKeys().getAttributes().getCs().isEmpty()) {
                                                                return true;
                                                            }
                                                            return entityView.getKeys().getAttributes().getCs().contains(attr.getKey());
                                                        case DataConstants.SERVER_SCOPE:
                                                            if (entityView.getKeys().getAttributes().getSs().isEmpty()) {
                                                                return true;
                                                            }
                                                            return entityView.getKeys().getAttributes().getSs().contains(attr.getKey());
                                                        case  DataConstants.SHARED_SCOPE:
                                                            if (entityView.getKeys().getAttributes().getSh().isEmpty()) {
                                                                return true;
                                                            }
                                                            return entityView.getKeys().getAttributes().getSh().contains(attr.getKey());
                                                    }
                                                    return false;
                                                })
                                                .collect(Collectors.toList());
                                saveFutures.add(ctx.getAttributesService().save(entityView.getId(), scope, new ArrayList<>(filteredAttributes)));
                            }
                        }
                        Futures.transform(Futures.allAsList(saveFutures), new Function<List<List<Void>>, Object>() {
                            @Nullable
                            @Override
                            public Object apply(@Nullable List<List<Void>> lists) {
                                ctx.tellNext(msg, TbRelationTypes.SUCCESS);
                                return null;
                            }
                        });
                    },
                    t -> ctx.tellFailure(msg, t));
        } else {
            ctx.tellNext(msg, TbRelationTypes.FAILURE);
        }
    }

    @Override
    public void destroy() {

    }
}
