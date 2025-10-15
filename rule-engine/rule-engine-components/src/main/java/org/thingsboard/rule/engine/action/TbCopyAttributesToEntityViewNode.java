/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import jakarta.annotation.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.rule.engine.api.AttributesDeleteRequest;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.rule.engine.api.EmptyNodeConfiguration;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.objects.AttributesEntityView;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.msg.TbMsgType.ACTIVITY_EVENT;
import static org.thingsboard.server.common.data.msg.TbMsgType.ATTRIBUTES_DELETED;
import static org.thingsboard.server.common.data.msg.TbMsgType.ATTRIBUTES_UPDATED;
import static org.thingsboard.server.common.data.msg.TbMsgType.INACTIVITY_EVENT;
import static org.thingsboard.server.common.data.msg.TbMsgType.POST_ATTRIBUTES_REQUEST;
import static org.thingsboard.server.common.data.msg.TbNodeConnectionType.SUCCESS;

@RuleNode(
        type = ComponentType.ACTION,
        name = "copy to view",
        configClazz = EmptyNodeConfiguration.class,
        nodeDescription = "Copy attributes from asset/device to entity view and changes message originator to related entity view",
        nodeDetails = "Copy attributes from asset/device to related entity view according to entity view configuration. \n " +
                "Copy will be done only for attributes that are between start and end dates and according to attribute keys configuration. \n" +
                "Changes message originator to related entity view and produces new messages according to count of updated entity views",
        configDirective = "tbNodeEmptyConfig",
        icon = "content_copy",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/action/copy-to-view/"
)
public class TbCopyAttributesToEntityViewNode implements TbNode {

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) {}

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (msg.isTypeOneOf(ATTRIBUTES_UPDATED, ATTRIBUTES_DELETED,
                ACTIVITY_EVENT, INACTIVITY_EVENT, POST_ATTRIBUTES_REQUEST)) {
            if (!msg.getMetaData().getData().isEmpty()) {
                long now = System.currentTimeMillis();
                AttributeScope scope = msg.isTypeOf(POST_ATTRIBUTES_REQUEST) ?
                        AttributeScope.CLIENT_SCOPE : AttributeScope.valueOf(msg.getMetaData().getValue(DataConstants.SCOPE));

                ListenableFuture<List<EntityView>> entityViewsFuture =
                        ctx.getEntityViewService().findEntityViewsByTenantIdAndEntityIdAsync(ctx.getTenantId(), msg.getOriginator());

                DonAsynchron.withCallback(entityViewsFuture,
                        entityViews -> {
                            for (EntityView entityView : entityViews) {
                                long startTime = entityView.getStartTimeMs();
                                long endTime = entityView.getEndTimeMs();
                                if ((endTime != 0 && endTime > now && startTime < now) || (endTime == 0 && startTime < now)) {
                                    if (msg.isTypeOf(ATTRIBUTES_DELETED)) {
                                        List<String> attributes = new ArrayList<>();
                                        for (JsonElement element : JsonParser.parseString(msg.getData()).getAsJsonObject().get("attributes").getAsJsonArray()) {
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
                                            ctx.getTelemetryService().deleteAttributes(AttributesDeleteRequest.builder()
                                                    .tenantId(ctx.getTenantId())
                                                    .entityId(entityView.getId())
                                                    .scope(scope)
                                                    .keys(filteredAttributes)
                                                    .callback(getFutureCallback(ctx, msg, entityView))
                                                    .build());
                                        }
                                    } else {
                                        List<AttributeKvEntry> attributes = JsonConverter.convertToAttributes(JsonParser.parseString(msg.getData())).stream()
                                                .filter(attr -> attributeContainsInEntityView(scope, attr.getKey(), entityView)).toList();
                                        ctx.getTelemetryService().saveAttributes(AttributesSaveRequest.builder()
                                                .tenantId(ctx.getTenantId())
                                                .entityId(entityView.getId())
                                                .scope(scope)
                                                .entries(attributes)
                                                .callback(getFutureCallback(ctx, msg, entityView))
                                                .build());
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

    private FutureCallback<Void> getFutureCallback(TbContext ctx, TbMsg msg, EntityView entityView) {
        return new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                transformAndTellNext(ctx, msg, entityView);
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                ctx.tellFailure(msg, t);
            }
        };
    }

    private void transformAndTellNext(TbContext ctx, TbMsg msg, EntityView entityView) {
        ctx.enqueueForTellNext(ctx.newMsg(msg.getQueueName(), msg.getType(), entityView.getId(), msg.getCustomerId(), msg.getMetaData(), msg.getData()), SUCCESS);
    }

    private boolean attributeContainsInEntityView(AttributeScope scope, String attrKey, EntityView entityView) {
        AttributesEntityView attributesEntityView = entityView.getKeys().getAttributes();
        List<String> keys = switch (scope) {
            case CLIENT_SCOPE -> attributesEntityView.getCs();
            case SERVER_SCOPE -> attributesEntityView.getSs();
            case SHARED_SCOPE -> attributesEntityView.getSh();
        };
        return CollectionsUtil.contains(keys, attrKey);
    }

}
