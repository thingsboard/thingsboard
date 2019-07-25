/**
 * Copyright © 2016-2019 The Thingsboard Authors
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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;
import static org.thingsboard.rule.engine.metadata.TbGetTelemetryNodeConfiguration.FETCH_MODE_ALL;
import static org.thingsboard.rule.engine.metadata.TbGetTelemetryNodeConfiguration.FETCH_MODE_FIRST;
import static org.thingsboard.rule.engine.metadata.TbGetTelemetryNodeConfiguration.MAX_FETCH_SIZE;
import static org.thingsboard.server.common.data.kv.Aggregation.NONE;

/**
 * Created by mshvayka on 04.09.18.
 */
@Slf4j
@RuleNode(type = ComponentType.ENRICHMENT,
        name = "originator telemetry",
        configClazz = TbGetTelemetryNodeConfiguration.class,
        nodeDescription = "Add Message Originator Telemetry for selected time range into Message Metadata\n",
        nodeDetails = "The node allows you to select fetch mode: <b>FIRST/LAST/ALL</b> to fetch telemetry of certain time range that are added into Message metadata without any prefix. " +
                "If selected fetch mode <b>ALL</b> Telemetry will be added like array into Message Metadata where <b>key</b> is Timestamp and <b>value</b> is value of Telemetry.</br>" +
                "If selected fetch mode <b>FIRST</b> or <b>LAST</b> Telemetry will be added like string without Timestamp.</br>" +
                "Also, the rule node allows you to select telemetry sampling order: <b>ASC</b> or <b>DESC</b>. </br>" +
                "<b>Note</b>: The maximum size of the fetched array is 1000 records.\n ",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbEnrichmentNodeGetTelemetryFromDatabase")
public class TbGetTelemetryNode implements TbNode {

    private static final String DESC_ORDER = "DESC";
    private static final String ASC_ORDER = "ASC";

    private TbGetTelemetryNodeConfiguration config;
    private List<String> tsKeyNames;
    private int limit;
    private ObjectMapper mapper;
    private String fetchMode;
    private String orderByFetchAll;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbGetTelemetryNodeConfiguration.class);
        tsKeyNames = config.getLatestTsKeyNames();
        limit = config.getFetchMode().equals(FETCH_MODE_ALL) ? MAX_FETCH_SIZE : 1;
        fetchMode = config.getFetchMode();
        orderByFetchAll = config.getOrderBy();
        if (StringUtils.isEmpty(orderByFetchAll)) {
            orderByFetchAll = ASC_ORDER;
        }
        mapper = new ObjectMapper();
        mapper.configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, false);
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        if (tsKeyNames.isEmpty()) {
            ctx.tellFailure(msg, new IllegalStateException("Telemetry is not selected!"));
        } else {
            try {
                if (config.isUseMetadataIntervalPatterns()) {
                    checkMetadataKeyPatterns(msg);
                }
                ListenableFuture<List<TsKvEntry>> list = ctx.getTimeseriesService().findAll(ctx.getTenantId(), msg.getOriginator(), buildQueries(msg));
                DonAsynchron.withCallback(list, data -> {
                    process(data, msg);
                    TbMsg newMsg = ctx.transformMsg(msg, msg.getType(), msg.getOriginator(), msg.getMetaData(), msg.getData());
                    ctx.tellNext(newMsg, SUCCESS);
                }, error -> ctx.tellFailure(msg, error), ctx.getDbCallbackExecutor());
            } catch (Exception e) {
                ctx.tellFailure(msg, e);
            }
        }
    }

    @Override
    public void destroy() {
    }

    private List<ReadTsKvQuery> buildQueries(TbMsg msg) {
        return tsKeyNames.stream()
                .map(key -> new BaseReadTsKvQuery(key, getInterval(msg).getStartTs(), getInterval(msg).getEndTs(), 1, limit, NONE, getOrderBy()))
                .collect(Collectors.toList());
    }

    private String getOrderBy() {
        switch (fetchMode) {
            case FETCH_MODE_ALL:
                return orderByFetchAll;
            case FETCH_MODE_FIRST:
                return ASC_ORDER;
            default:
                return DESC_ORDER;
        }
    }

    private void process(List<TsKvEntry> entries, TbMsg msg) {
        ObjectNode resultNode = mapper.createObjectNode();
        if (limit == MAX_FETCH_SIZE) {
            entries.forEach(entry -> processArray(resultNode, entry));
        } else {
            entries.forEach(entry -> processSingle(resultNode, entry));
        }

        for (String key : tsKeyNames) {
            if (resultNode.has(key)) {
                msg.getMetaData().putValue(key, resultNode.get(key).toString());
            }
        }
    }

    private void processSingle(ObjectNode node, TsKvEntry entry) {
        node.put(entry.getKey(), entry.getValueAsString());
    }

    private void processArray(ObjectNode node, TsKvEntry entry) {
        if (node.has(entry.getKey())) {
            ArrayNode arrayNode = (ArrayNode) node.get(entry.getKey());
            ObjectNode obj = buildNode(entry);
            arrayNode.add(obj);
        } else {
            ArrayNode arrayNode = mapper.createArrayNode();
            ObjectNode obj = buildNode(entry);
            arrayNode.add(obj);
            node.set(entry.getKey(), arrayNode);
        }
    }

    private ObjectNode buildNode(TsKvEntry entry) {
        ObjectNode obj = mapper.createObjectNode()
                .put("ts", entry.getTs());
        switch (entry.getDataType()) {
            case STRING:
                obj.put("value", entry.getValueAsString());
                break;
            case LONG:
                obj.put("value", entry.getLongValue().get());
                break;
            case BOOLEAN:
                obj.put("value", entry.getBooleanValue().get());
                break;
            case DOUBLE:
                obj.put("value", entry.getDoubleValue().get());
                break;
        }
        return obj;
    }

    private Interval getInterval(TbMsg msg) {
        Interval interval = new Interval();
        if (config.isUseMetadataIntervalPatterns()) {
            if (isParsable(msg, config.getStartIntervalPattern())) {
                interval.setStartTs(Long.parseLong(TbNodeUtils.processPattern(config.getStartIntervalPattern(), msg.getMetaData())));
            }
            if (isParsable(msg, config.getEndIntervalPattern())) {
                interval.setEndTs(Long.parseLong(TbNodeUtils.processPattern(config.getEndIntervalPattern(), msg.getMetaData())));
            }
        } else {
            long ts = System.currentTimeMillis();
            interval.setStartTs(ts - TimeUnit.valueOf(config.getStartIntervalTimeUnit()).toMillis(config.getStartInterval()));
            interval.setEndTs(ts - TimeUnit.valueOf(config.getEndIntervalTimeUnit()).toMillis(config.getEndInterval()));
        }
        return interval;
    }

    private boolean isParsable(TbMsg msg, String pattern) {
        return NumberUtils.isParsable(TbNodeUtils.processPattern(pattern, msg.getMetaData()));
    }

    private void checkMetadataKeyPatterns(TbMsg msg) {
        isUndefined(msg, config.getStartIntervalPattern(), config.getEndIntervalPattern());
        isInvalid(msg, config.getStartIntervalPattern(), config.getEndIntervalPattern());
    }

    private void isUndefined(TbMsg msg, String startIntervalPattern, String endIntervalPattern) {
        if (getMetadataValue(msg, startIntervalPattern) == null && getMetadataValue(msg, endIntervalPattern) == null) {
            throw new IllegalArgumentException("Message metadata values: '" +
                    replaceRegex(startIntervalPattern) + "' and '" +
                    replaceRegex(endIntervalPattern) + "' are undefined");
        } else {
            if (getMetadataValue(msg, startIntervalPattern) == null) {
                throw new IllegalArgumentException("Message metadata value: '" +
                        replaceRegex(startIntervalPattern) + "' is undefined");
            }
            if (getMetadataValue(msg, endIntervalPattern) == null) {
                throw new IllegalArgumentException("Message metadata value: '" +
                        replaceRegex(endIntervalPattern) + "' is undefined");
            }
        }
    }

    private void isInvalid(TbMsg msg, String startIntervalPattern, String endIntervalPattern) {
        if (getInterval(msg).getStartTs() == null && getInterval(msg).getEndTs() == null) {
            throw new IllegalArgumentException("Message metadata values: '" +
                    replaceRegex(startIntervalPattern) + "' and '" +
                    replaceRegex(endIntervalPattern) + "' have invalid format");
        } else {
            if (getInterval(msg).getStartTs() == null) {
                throw new IllegalArgumentException("Message metadata value: '" +
                        replaceRegex(startIntervalPattern) + "' has invalid format");
            }
            if (getInterval(msg).getEndTs() == null) {
                throw new IllegalArgumentException("Message metadata value: '" +
                        replaceRegex(endIntervalPattern) + "' has invalid format");
            }
        }
    }

    private String getMetadataValue(TbMsg msg, String pattern) {
        return msg.getMetaData().getValue(replaceRegex(pattern));
    }

    private String replaceRegex(String pattern) {
        return pattern.replaceAll("[${}]", "");
    }

    @Data
    @NoArgsConstructor
    private static class Interval {
        private Long startTs;
        private Long endTs;
    }

}
