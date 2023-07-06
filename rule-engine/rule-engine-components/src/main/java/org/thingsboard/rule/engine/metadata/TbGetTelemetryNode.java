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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by mshvayka on 04.09.18.
 */
@Slf4j
@RuleNode(type = ComponentType.ENRICHMENT,
        name = "originator telemetry",
        configClazz = TbGetTelemetryNodeConfiguration.class,
        nodeDescription = "Adds message originator telemetry for selected time range into message metadata",
        nodeDetails = "Useful when you need to get telemetry data set from the message originator for a specific time range " +
                "instead of fetching just the latest telemetry or if you need to get the closest telemetry to the fetch interval start or end. " +
                "Also, this node can be used for telemetry aggregation within configured fetch interval.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbEnrichmentNodeGetTelemetryFromDatabase")
public class TbGetTelemetryNode implements TbNode {

    private static final String DESC_ORDER = "DESC";
    private static final String ASC_ORDER = "ASC";

    private TbGetTelemetryNodeConfiguration config;
    private List<String> tsKeyNames;
    private int limit;
    private String fetchMode;
    private String orderByFetchAll;
    private Aggregation aggregation;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbGetTelemetryNodeConfiguration.class);
        tsKeyNames = config.getLatestTsKeyNames();
        limit = config.getFetchMode().equals(TbGetTelemetryNodeConfiguration.FETCH_MODE_ALL) ? validateLimit(config.getLimit()) : 1;
        fetchMode = config.getFetchMode();
        orderByFetchAll = config.getOrderBy();
        if (StringUtils.isEmpty(orderByFetchAll)) {
            orderByFetchAll = ASC_ORDER;
        }
        aggregation = parseAggregationConfig(config.getAggregation());
    }

    Aggregation parseAggregationConfig(String aggName) {
        if (StringUtils.isEmpty(aggName) || !fetchMode.equals(TbGetTelemetryNodeConfiguration.FETCH_MODE_ALL)) {
            return Aggregation.NONE;
        }
        return Aggregation.valueOf(aggName);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        if (tsKeyNames.isEmpty()) {
            ctx.tellFailure(msg, new IllegalStateException("Telemetry is not selected!"));
        } else {
            try {
                Interval interval = getInterval(msg);
                List<String> keys = TbNodeUtils.processPatterns(tsKeyNames, msg);
                ListenableFuture<List<TsKvEntry>> list = ctx.getTimeseriesService().findAll(ctx.getTenantId(), msg.getOriginator(), buildQueries(interval, keys));
                DonAsynchron.withCallback(list, data -> {
                    var metaData = updateMetadata(data, msg, keys);
                    ctx.tellSuccess(TbMsg.transformMsg(msg, metaData));
                }, error -> ctx.tellFailure(msg, error), ctx.getDbCallbackExecutor());
            } catch (Exception e) {
                ctx.tellFailure(msg, e);
            }
        }
    }

    private List<ReadTsKvQuery> buildQueries(Interval interval, List<String> keys) {
        final long aggIntervalStep = Aggregation.NONE.equals(aggregation) ? 1 :
                // exact how it validates on BaseTimeseriesService.validate()
                // see CassandraBaseTimeseriesDao.findAllAsync()
                interval.getEndTs() - interval.getStartTs();

        return keys.stream()
                .map(key -> new BaseReadTsKvQuery(key, interval.getStartTs(), interval.getEndTs(), aggIntervalStep, limit, aggregation, getOrderBy()))
                .collect(Collectors.toList());
    }

    private String getOrderBy() {
        switch (fetchMode) {
            case TbGetTelemetryNodeConfiguration.FETCH_MODE_ALL:
                return orderByFetchAll;
            case TbGetTelemetryNodeConfiguration.FETCH_MODE_FIRST:
                return ASC_ORDER;
            default:
                return DESC_ORDER;
        }
    }

    private TbMsgMetaData updateMetadata(List<TsKvEntry> entries, TbMsg msg, List<String> keys) {
        ObjectNode resultNode = JacksonUtil.newObjectNode(JacksonUtil.ALLOW_UNQUOTED_FIELD_NAMES_MAPPER);
        if (TbGetTelemetryNodeConfiguration.FETCH_MODE_ALL.equals(fetchMode)) {
            entries.forEach(entry -> processArray(resultNode, entry));
        } else {
            entries.forEach(entry -> processSingle(resultNode, entry));
        }
        var copy = msg.getMetaData().copy();
        for (String key : keys) {
            if (resultNode.has(key)) {
                copy.putValue(key, resultNode.get(key).toString());
            }
        }
        return copy;
    }

    private void processSingle(ObjectNode node, TsKvEntry entry) {
        node.put(entry.getKey(), entry.getValueAsString());
    }

    private void processArray(ObjectNode node, TsKvEntry entry) {
        if (node.has(entry.getKey())) {
            ArrayNode arrayNode = (ArrayNode) node.get(entry.getKey());
            arrayNode.add(buildNode(entry));
        } else {
            ArrayNode arrayNode = JacksonUtil.ALLOW_UNQUOTED_FIELD_NAMES_MAPPER.createArrayNode();
            arrayNode.add(buildNode(entry));
            node.set(entry.getKey(), arrayNode);
        }
    }

    private ObjectNode buildNode(TsKvEntry entry) {
        ObjectNode obj = JacksonUtil.newObjectNode(JacksonUtil.ALLOW_UNQUOTED_FIELD_NAMES_MAPPER);
        obj.put("ts", entry.getTs());
        JacksonUtil.addKvEntry(obj, entry, "value", JacksonUtil.ALLOW_UNQUOTED_FIELD_NAMES_MAPPER);
        return obj;
    }

    private Interval getInterval(TbMsg msg) {
        if (config.isUseMetadataIntervalPatterns()) {
            return getIntervalFromPatterns(msg);
        } else {
            Interval interval = new Interval();
            long ts = System.currentTimeMillis();
            interval.setStartTs(ts - TimeUnit.valueOf(config.getStartIntervalTimeUnit()).toMillis(config.getStartInterval()));
            interval.setEndTs(ts - TimeUnit.valueOf(config.getEndIntervalTimeUnit()).toMillis(config.getEndInterval()));
            return interval;
        }
    }

    private Interval getIntervalFromPatterns(TbMsg msg) {
        Interval interval = new Interval();
        interval.setStartTs(checkPattern(msg, config.getStartIntervalPattern()));
        interval.setEndTs(checkPattern(msg, config.getEndIntervalPattern()));
        return interval;
    }

    private long checkPattern(TbMsg msg, String pattern) {
        String value = getValuePattern(msg, pattern);
        if (value == null) {
            throw new IllegalArgumentException("Message value: '" +
                    replaceRegex(pattern) + "' is undefined");
        }
        boolean parsable = NumberUtils.isParsable(value);
        if (!parsable) {
            throw new IllegalArgumentException("Message value: '" +
                    replaceRegex(pattern) + "' has invalid format");
        }
        return Long.parseLong(value);
    }

    private String getValuePattern(TbMsg msg, String pattern) {
        String value = TbNodeUtils.processPattern(pattern, msg);
        return value.equals(pattern) ? null : value;
    }

    private String replaceRegex(String pattern) {
        return pattern.replaceAll("[$\\[{}\\]]", "");
    }

    private int validateLimit(int limit) {
        if (limit != 0) {
            return limit;
        } else {
            return TbGetTelemetryNodeConfiguration.MAX_FETCH_SIZE;
        }
    }

    @Data
    @NoArgsConstructor
    private static class Interval {
        private Long startTs;
        private Long endTs;
    }

}
