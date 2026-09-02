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
package org.thingsboard.rule.engine.telemetry;

import com.google.gson.JsonParser;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.rule.engine.api.EmptyNodeConfiguration;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.StringUtils;
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

@RuleNode(
        type = ComponentType.ACTION,
        name = "calculated fields and alarm rules",
        configClazz = EmptyNodeConfiguration.class,
        nodeDescription = "Pushes incoming messages to calculated fields and alarm rules services",
        nodeDetails = "Node enables the processing of calculated fields and alarm rules without persisting incoming messages to the database. " +
                "By default, the processing of calculated fields and alarm rules is triggered by the <b>save attributes</b> and <b>save time series</b> nodes. " +
                "This rule node accepts the same messages as these nodes but allows you to trigger the processing of calculated " +
                "fields or alarm rules independently, ensuring that derived data can be computed and utilized in real time without storing the original message in the database.",
        configDirective = "tbNodeEmptyConfig",
        icon = "published_with_changes",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/action/calculated-fields/"
)
public class TbCalculatedFieldsNode implements TbNode {

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) {}

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        switch (msg.getInternalType()) {
            case POST_TELEMETRY_REQUEST -> processPostTelemetryRequest(ctx, msg);
            case POST_ATTRIBUTES_REQUEST -> processPostAttributesRequest(ctx, msg);
            default -> ctx.tellFailure(msg, new IllegalArgumentException("Unsupported msg type: " + msg.getType()));
        }
    }

    private void processPostTelemetryRequest(TbContext ctx, TbMsg msg) {
        Map<Long, List<KvEntry>> tsKvMap = JsonConverter.convertToTelemetry(JsonParser.parseString(msg.getData()), System.currentTimeMillis());

        if (tsKvMap.isEmpty()) {
            ctx.tellSuccess(msg);
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
                .strategy(TimeseriesSaveRequest.Strategy.CF_ONLY)
                .previousCalculatedFieldIds(msg.getPreviousCalculatedFieldIds())
                .tbMsgId(msg.getId())
                .tbMsgType(msg.getInternalType())
                .callback(new TelemetryNodeCallback(ctx, msg))
                .build();

        ctx.getTelemetryService().saveTimeseries(timeseriesSaveRequest);
    }

    private void processPostAttributesRequest(TbContext ctx, TbMsg msg) {
        List<AttributeKvEntry> newAttributes = JsonConverter.convertToAttributes(JsonParser.parseString(msg.getData()));
        if (newAttributes.isEmpty()) {
            ctx.tellSuccess(msg);
            return;
        }
        AttributeScope scope = resolveScope(ctx, msg);
        if (scope == null) {
            return;
        }

        AttributesSaveRequest attributesSaveRequest = AttributesSaveRequest.builder()
                .tenantId(ctx.getTenantId())
                .entityId(msg.getOriginator())
                .scope(scope)
                .entries(newAttributes)
                .strategy(AttributesSaveRequest.Strategy.CF_ONLY)
                .previousCalculatedFieldIds(msg.getPreviousCalculatedFieldIds())
                .tbMsgId(msg.getId())
                .tbMsgType(msg.getInternalType())
                .callback(new TelemetryNodeCallback(ctx, msg))
                .build();
        ctx.getTelemetryService().saveAttributes(attributesSaveRequest);
    }

    private AttributeScope resolveScope(TbContext ctx, TbMsg msg) {
        String scopeStr = msg.getMetaData().getValue(SCOPE);

        if (StringUtils.isEmpty(scopeStr)) {
            ctx.tellFailure(msg, new IllegalArgumentException("Attribute scope is missing"));
            return null;
        }

        try {
            return AttributeScope.valueOf(scopeStr);
        } catch (IllegalArgumentException e) {
            ctx.tellFailure(msg, new IllegalArgumentException("Invalid attribute scope: " + scopeStr));
            return null;
        }
    }

}
