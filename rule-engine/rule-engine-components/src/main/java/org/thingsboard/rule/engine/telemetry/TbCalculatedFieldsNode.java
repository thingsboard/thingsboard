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
package org.thingsboard.rule.engine.telemetry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.gson.JsonParser;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.AttributesDeleteRequest;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.rule.engine.api.EmptyNodeConfiguration;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TimeseriesDeleteRequest;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.thingsboard.server.common.data.DataConstants.SCOPE;
import static org.thingsboard.server.common.data.msg.TbMsgType.ATTRIBUTES_DELETED;
import static org.thingsboard.server.common.data.msg.TbMsgType.POST_ATTRIBUTES_REQUEST;
import static org.thingsboard.server.common.data.msg.TbMsgType.POST_TELEMETRY_REQUEST;
import static org.thingsboard.server.common.data.msg.TbMsgType.TIMESERIES_DELETED;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "push to calculated fields",
        configClazz = EmptyNodeConfiguration.class,
        nodeDescription = "Pushes messages to the calculated fields for further processing",
        nodeDetails = "Forwards incoming messages to the calculated fields, where they will be processed to compute values based on predefined calculation rules without persisting any data in the database.",
        configDirective = "tbNodeEmptyConfig",
        icon = "send"
)
public class TbCalculatedFieldsNode implements TbNode {

    private EmptyNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, EmptyNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (!msg.isTypeOneOf(POST_ATTRIBUTES_REQUEST, POST_TELEMETRY_REQUEST, ATTRIBUTES_DELETED, TIMESERIES_DELETED)) {
            ctx.tellFailure(msg, new IllegalArgumentException("Unsupported msg type: " + msg.getType()));
            return;
        }

        String src = msg.getData();

        if (msg.isTypeOf(POST_TELEMETRY_REQUEST)) {
            processPostTelemetryRequest(ctx, msg, src);
        } else if (msg.isTypeOf(POST_ATTRIBUTES_REQUEST)) {
            processPostAttributesRequest(ctx, msg, src);
        } else if (msg.isTypeOf(TIMESERIES_DELETED)) {
            processTimeSeriesDeleted(ctx, msg, src);
        } else {
            processAttributesDeleted(ctx, msg, src);
        }

    }

    private void processPostTelemetryRequest(TbContext ctx, TbMsg msg, String src) {
        Map<Long, List<KvEntry>> tsKvMap = JsonConverter.convertToTelemetry(JsonParser.parseString(src), System.currentTimeMillis());
        if (tsKvMap.isEmpty()) {
            ctx.tellFailure(msg, new IllegalArgumentException("Msg body is empty: " + src));
            return;
        }

        List<TsKvEntry> tsKvEntryList = new ArrayList<>();
        for (Map.Entry<Long, List<KvEntry>> tsKvEntry : tsKvMap.entrySet()) {
            for (KvEntry kvEntry : tsKvEntry.getValue()) {
                tsKvEntryList.add(new BasicTsKvEntry(tsKvEntry.getKey(), kvEntry));
            }
        }

        TimeseriesSaveRequest timeseriesSaveRequest = TimeseriesSaveRequest.builder()
                .tenantId(ctx.getTenantId())
                .customerId(msg.getCustomerId())
                .entityId(msg.getOriginator())
                .entries(tsKvEntryList)
                .previousCalculatedFieldIds(msg.getPreviousCalculatedFieldIds())
                .tbMsgId(msg.getId())
                .tbMsgType(msg.getInternalType())
                .callback(new TelemetryNodeCallback(ctx, msg))
                .build();

        ctx.getCalculatedFieldQueueService().pushRequestToQueue(timeseriesSaveRequest, timeseriesSaveRequest.getCallback());
    }

    private void processPostAttributesRequest(TbContext ctx, TbMsg msg, String src) {
        List<AttributeKvEntry> newAttributes = new ArrayList<>(JsonConverter.convertToAttributes(JsonParser.parseString(src)));

        if (newAttributes.isEmpty()) {
            ctx.tellSuccess(msg);
            return;
        }

        AttributesSaveRequest attributesSaveRequest = AttributesSaveRequest.builder()
                .tenantId(ctx.getTenantId())
                .entityId(msg.getOriginator())
                .scope(AttributeScope.valueOf(msg.getMetaData().getValue(SCOPE)))
                .entries(newAttributes)
                .previousCalculatedFieldIds(msg.getPreviousCalculatedFieldIds())
                .tbMsgId(msg.getId())
                .tbMsgType(msg.getInternalType())
                .callback(new TelemetryNodeCallback(ctx, msg))
                .build();
        ctx.getCalculatedFieldQueueService().pushRequestToQueue(attributesSaveRequest, attributesSaveRequest.getCallback());
    }

    private void processTimeSeriesDeleted(TbContext ctx, TbMsg msg, String src) {
        JsonNode dataJson = JacksonUtil.toJsonNode(msg.getData());
        List<String> keysToDelete = JacksonUtil.convertValue(dataJson.get("timeSeries"), new TypeReference<>() {
        });

        if (keysToDelete.isEmpty()) {
            ctx.tellSuccess(msg);
            return;
        }

        TimeseriesDeleteRequest timeseriesDeleteRequest = TimeseriesDeleteRequest.builder()
                .tenantId(ctx.getTenantId())
                .entityId(msg.getOriginator())
                .keys(keysToDelete)
                .previousCalculatedFieldIds(msg.getPreviousCalculatedFieldIds())
                .tbMsgId(msg.getId())
                .tbMsgType(msg.getInternalType())
                .callback(new FutureCallback<List<String>>() {
                    @Override
                    public void onSuccess(@Nullable List<String> tmp) {
                    }

                    @Override
                    public void onFailure(Throwable t) {
                    }
                })
                .build();

        ctx.getCalculatedFieldQueueService().pushRequestToQueue(timeseriesDeleteRequest, keysToDelete, getCalculatedFieldCallback(timeseriesDeleteRequest.getCallback(), keysToDelete));
    }

    private void processAttributesDeleted(TbContext ctx, TbMsg msg, String src) {
        JsonNode dataJson = JacksonUtil.toJsonNode(msg.getData());
        List<String> keysToDelete = JacksonUtil.convertValue(dataJson.get("attributes"), new TypeReference<>() {
        });

        if (keysToDelete.isEmpty()) {
            ctx.tellSuccess(msg);
            return;
        }

        AttributesDeleteRequest attributesDeleteRequest = AttributesDeleteRequest.builder()
                .tenantId(ctx.getTenantId())
                .entityId(msg.getOriginator())
                .scope(AttributeScope.valueOf(msg.getMetaData().getValue(SCOPE)))
                .keys(keysToDelete)
                .previousCalculatedFieldIds(msg.getPreviousCalculatedFieldIds())
                .tbMsgId(msg.getId())
                .tbMsgType(msg.getInternalType())
                .callback(new TelemetryNodeCallback(ctx, msg))
                .build();
        ctx.getCalculatedFieldQueueService().pushRequestToQueue(attributesDeleteRequest, keysToDelete, attributesDeleteRequest.getCallback());
    }

    private FutureCallback<Void> getCalculatedFieldCallback(FutureCallback<List<String>> originalCallback, List<String> keys) {
        return new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void unused) {
                originalCallback.onSuccess(keys);
            }

            @Override
            public void onFailure(Throwable t) {
                originalCallback.onFailure(t);
            }
        };
    }

}
