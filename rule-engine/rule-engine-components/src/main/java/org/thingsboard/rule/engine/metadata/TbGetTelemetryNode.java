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
package org.thingsboard.rule.engine.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.lang3.math.NumberUtils;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.SortOrder.Direction;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RuleNode(type = ComponentType.ENRICHMENT,
        name = "originator telemetry",
        configClazz = TbGetTelemetryNodeConfiguration.class,
        version = 2,
        nodeDescription = "Adds message originator telemetry for selected time range into message metadata",
        nodeDetails = "Useful when you need to get telemetry data set from the message originator for a specific time range " +
                "instead of fetching just the latest telemetry or if you need to get the closest telemetry to the fetch interval start or end. " +
                "Also, this node can be used for telemetry aggregation within configured fetch interval.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        configDirective = "tbEnrichmentNodeGetTelemetryFromDatabase")
public class TbGetTelemetryNode implements TbNode {

    private TbGetTelemetryNodeConfiguration config;
    private List<String> tsKeyNames;
    private int limit;
    private FetchMode fetchMode;
    private Direction orderBy;
    private Aggregation aggregation;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbGetTelemetryNodeConfiguration.class);
        tsKeyNames = config.getLatestTsKeyNames();
        if (tsKeyNames.isEmpty()) {
            throw new TbNodeException("Telemetry should be specified!", true);
        }
        fetchMode = config.getFetchMode();
        if (fetchMode == null) {
            throw new TbNodeException("FetchMode should be specified!", true);
        }
        switch (fetchMode) {
            case ALL:
                limit = validateLimit(config.getLimit());
                if (config.getOrderBy() == null) {
                    throw new TbNodeException("OrderBy should be specified!", true);
                }
                orderBy = config.getOrderBy();
                if (config.getAggregation() == null) {
                    throw new TbNodeException("Aggregation should be specified!", true);
                }
                aggregation = config.getAggregation();
                break;
            case FIRST:
                limit = 1;
                orderBy = Direction.ASC;
                aggregation = Aggregation.NONE;
                break;
            case LAST:
                limit = 1;
                orderBy = Direction.DESC;
                aggregation = Aggregation.NONE;
                break;
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        Interval interval = getInterval(msg);
        if (interval.startTs() > interval.endTs()) {
            throw new RuntimeException("Interval start should be less than Interval end");
        }
        List<String> keys = TbNodeUtils.processPatterns(tsKeyNames, msg);
        ListenableFuture<List<TsKvEntry>> list = ctx.getTimeseriesService().findAll(ctx.getTenantId(), msg.getOriginator(), buildQueries(interval, keys));
        DonAsynchron.withCallback(list, data -> {
            var metaData = updateMetadata(data, msg, keys);
            ctx.tellSuccess(msg.transform()
                    .metaData(metaData)
                    .build());
        }, error -> ctx.tellFailure(msg, error), ctx.getDbCallbackExecutor());
    }

    private List<ReadTsKvQuery> buildQueries(Interval interval, List<String> keys) {
        final long aggIntervalStep = Aggregation.NONE.equals(aggregation) ? 1 :
                // exact how it validates on BaseTimeseriesService.validate()
                // see CassandraBaseTimeseriesDao.findAllAsync()
                interval.endTs() - interval.startTs();

        return keys.stream()
                .map(key -> new BaseReadTsKvQuery(key, interval.startTs(), interval.endTs(), aggIntervalStep, limit, aggregation, orderBy.name()))
                .collect(Collectors.toList());
    }

    private TbMsgMetaData updateMetadata(List<TsKvEntry> entries, TbMsg msg, List<String> keys) {
        ObjectNode resultNode = JacksonUtil.newObjectNode(JacksonUtil.ALLOW_UNQUOTED_FIELD_NAMES_MAPPER);
        if (FetchMode.ALL.equals(fetchMode)) {
            entries.forEach(entry -> processArray(resultNode, entry));
        } else {
            entries.forEach(entry -> processSingle(resultNode, entry));
        }
        var copy = msg.getMetaData().copy();
        for (String key : keys) {
            JsonNode value = resultNode.get(key);
            if (value != null) {
                copy.putValue(key, value.isValueNode() ? value.asText() : value.toString());
            }
        }
        return copy;
    }

    private static void processSingle(ObjectNode node, TsKvEntry entry) {
        node.put(entry.getKey(), entry.getValueAsString());
    }

    private static void processArray(ObjectNode node, TsKvEntry entry) {
        if (node.has(entry.getKey())) {
            ArrayNode arrayNode = (ArrayNode) node.get(entry.getKey());
            arrayNode.add(buildNode(entry));
        } else {
            ArrayNode arrayNode = JacksonUtil.ALLOW_UNQUOTED_FIELD_NAMES_MAPPER.createArrayNode();
            arrayNode.add(buildNode(entry));
            node.set(entry.getKey(), arrayNode);
        }
    }

    private static ObjectNode buildNode(TsKvEntry entry) {
        ObjectNode obj = JacksonUtil.newObjectNode(JacksonUtil.ALLOW_UNQUOTED_FIELD_NAMES_MAPPER);
        obj.put("ts", entry.getTs());
        JacksonUtil.addKvEntry(obj, entry, "value", JacksonUtil.ALLOW_UNQUOTED_FIELD_NAMES_MAPPER);
        return obj;
    }

    private Interval getInterval(TbMsg msg) {
        if (config.isUseMetadataIntervalPatterns()) {
            return getIntervalFromPatterns(msg);
        } else {
            long nowTs = getCurrentTimeMillis();
            long startTs = nowTs - TimeUnit.valueOf(config.getStartIntervalTimeUnit()).toMillis(config.getStartInterval());
            long endTs = nowTs - TimeUnit.valueOf(config.getEndIntervalTimeUnit()).toMillis(config.getEndInterval());
            return new Interval(startTs, endTs);
        }
    }

    private Interval getIntervalFromPatterns(TbMsg msg) {
        return new Interval(checkPattern(msg, config.getStartIntervalPattern()), checkPattern(msg, config.getEndIntervalPattern()));
    }

    private static long checkPattern(TbMsg msg, String pattern) {
        String value = getValuePattern(msg, pattern);
        if (value == null) {
            throw new IllegalArgumentException("Message value: '" + replaceRegex(pattern) + "' is undefined");
        }
        boolean parsable = NumberUtils.isParsable(value);
        if (!parsable) {
            throw new IllegalArgumentException("Message value: '" + replaceRegex(pattern) + "' has invalid format");
        }
        return Long.parseLong(value);
    }

    private static String getValuePattern(TbMsg msg, String pattern) {
        String value = TbNodeUtils.processPattern(pattern, msg);
        return value.equals(pattern) ? null : value;
    }

    private static String replaceRegex(String pattern) {
        return pattern.replaceAll("[$\\[{}\\]]", "");
    }

    private int validateLimit(int limit) throws TbNodeException {
        if (limit < 2 || limit > TbGetTelemetryNodeConfiguration.MAX_FETCH_SIZE) {
            throw new TbNodeException("Limit should be in a range from 2 to 1000.", true);
        }
        return limit;
    }

    long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    private record Interval(long startTs, long endTs) {}

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        boolean hasChanges = false;
        switch (fromVersion) {
            case 0: {
                if (oldConfiguration.hasNonNull("fetchMode")) {
                    String fetchMode = oldConfiguration.get("fetchMode").asText();
                    switch (fetchMode) {
                        case "FIRST" -> {
                            ((ObjectNode) oldConfiguration).put("orderBy", Direction.ASC.name());
                            ((ObjectNode) oldConfiguration).put("aggregation", Aggregation.NONE.name());
                            hasChanges = true;
                        }
                        case "LAST" -> {
                            ((ObjectNode) oldConfiguration).put("orderBy", Direction.DESC.name());
                            ((ObjectNode) oldConfiguration).put("aggregation", Aggregation.NONE.name());
                            hasChanges = true;
                        }
                        case "ALL" -> {
                            if (oldConfiguration.has("orderBy") &&
                                    (oldConfiguration.get("orderBy").isNull() || oldConfiguration.get("orderBy").asText().isEmpty())) {
                                ((ObjectNode) oldConfiguration).put("orderBy", Direction.ASC.name());
                                hasChanges = true;
                            }
                            if (oldConfiguration.has("aggregation") &&
                                    (oldConfiguration.get("aggregation").isNull() || oldConfiguration.get("aggregation").asText().isEmpty())) {
                                ((ObjectNode) oldConfiguration).put("aggregation", Aggregation.NONE.name());
                                hasChanges = true;
                            }
                        }
                        default -> {
                            ((ObjectNode) oldConfiguration).put("fetchMode", FetchMode.LAST.name());
                            ((ObjectNode) oldConfiguration).put("orderBy", Direction.DESC.name());
                            ((ObjectNode) oldConfiguration).put("aggregation", Aggregation.NONE.name());
                            hasChanges = true;
                        }
                    }
                }
            }
            case 1: {
                if (!oldConfiguration.hasNonNull("limit")) {
                    ((ObjectNode) oldConfiguration).put("limit", 1000);
                    hasChanges = true;
                }
                if (oldConfiguration.has("fetchMode") && oldConfiguration.get("fetchMode").asText().equals("ALL")) {
                    if (!oldConfiguration.hasNonNull("aggregation")) {
                        ((ObjectNode) oldConfiguration).put("aggregation", Aggregation.NONE.name());
                        hasChanges = true;
                    }
                    if (!oldConfiguration.hasNonNull("orderBy")) {
                        ((ObjectNode) oldConfiguration).put("orderBy", Direction.ASC.name());
                        hasChanges = true;
                    }
                }
                break;
            }
        }
        return new TbPair<>(hasChanges, oldConfiguration);
    }

}
