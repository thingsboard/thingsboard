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
package org.thingsboard.rule.engine.action;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.DonAsynchron;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.kv.*;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;

/**
 * @author mircopz 03.09.19
 */
@Slf4j
@RuleNode(type = ComponentType.ACTION,
        name = "delete timeseries",
        configClazz = TbDeleteTimeseriesNodeConfiguration.class,
        nodeDescription = "Delete Timeseries from database\n",
        nodeDetails = "The node allows you to remove entity timeseries from database.</br>" +
                "<b>Note</b>: The maximum size of the removed array is 1000 records.\n ",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeDeleteTimeseriesFromDatabase",
        icon = "delete"
)
public class TbDeleteTimeseriesNode implements TbNode {

    private TbDeleteTimeseriesNodeConfiguration config;
    private List<String> tsKeyNames;
    private ObjectMapper mapper;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbDeleteTimeseriesNodeConfiguration.class);
        tsKeyNames = config.getLatestTsKeyNames();
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
                ListenableFuture<List<Void>> future = ctx.getTimeseriesService().remove(ctx.getTenantId(), msg.getOriginator(), buildQueries(msg));
                DonAsynchron.withCallback(future, data -> {
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

    private List<DeleteTsKvQuery> buildQueries(TbMsg msg) {
        return tsKeyNames.stream()
                .map(key -> new BaseDeleteTsKvQuery(key, getInterval(msg).getStartTs(), getInterval(msg).getEndTs(), false))
                .collect(Collectors.toList());
    }

    private Interval getInterval(TbMsg msg) {
        Interval interval = new Interval();
        if (config.isDeleteAllDataForKeys()) {
            long ts = System.currentTimeMillis();
            interval.setStartTs(0L);
            interval.setEndTs(ts);
        } else if (config.isUseMetadataIntervalPatterns()) {
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
