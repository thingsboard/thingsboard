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
package org.thingsboard.rule.engine.telemetry;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.util.CollectionUtils;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.kv.BaseDeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvLatestRemovingResult;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "delete timeseries",
        configClazz = TbMsgDeleteTimeseriesNodeConfiguration.class,
        nodeDescription = "Delete timeseries data for Message Originator.",
        nodeDetails = "In case that no telemetry keys are selected - message send via <b>Failure</b> chain. " +
                "In order to delete all data for keys checkbox <b>Delete all data for keys<b> should be selected, " +
                "otherwise will be deleted data that is in range of the selected time interval. " +
                "In order to rewrite the latest values for keys selected to delete, checkbox <b>Rewrite latest if deleted<b> " +
                "should be selected! If data for selected timeseries keys successfully deleted - " +
                "message send via <b>Success</b> chain, otherwise <b>Failure</b> chain will be used.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "",
        icon = "remove_circle"
)
public class TbMsgDeleteTimeseriesNode implements TbNode {

    private TbMsgDeleteTimeseriesNodeConfiguration config;
    private List<String> keys;
    private long deleteFromTs;
    private long deleteToTs;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgDeleteTimeseriesNodeConfiguration.class);
        this.keys = config.getKeys();
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        if (CollectionUtils.isEmpty(keys)) {
            ctx.tellFailure(msg, new IllegalStateException("Telemetry keys list is not selected!"));
        } else {
            long currentTimeMillis = System.currentTimeMillis();
            try {
                if (config.getDeleteAllDataForKeys()) {
                    deleteFromTs = 0L;
                    deleteToTs = currentTimeMillis;
                } else {
                    if (config.isUseMetadataIntervalPatterns()) {
                        setDeleteTsIntervalsFromPatterns(msg);
                    } else {
                        deleteFromTs = currentTimeMillis - TimeUnit.valueOf(config.getStartTsTimeUnit()).toMillis(config.getStartTs());
                        deleteToTs = currentTimeMillis - TimeUnit.valueOf(config.getEndTsTimeUnit()).toMillis(config.getEndTs());
                    }
                }
                List<DeleteTsKvQuery> deleteTsKvQueries = new ArrayList<>();
                List<String> keysToDelete = new ArrayList<>();
                keys.stream().map(key -> TbNodeUtils.processPattern(key, msg)).forEach(result -> {
                    keysToDelete.add(result);
                    deleteTsKvQueries.add(new BaseDeleteTsKvQuery(result, deleteFromTs, deleteToTs, config.getRewriteLatestIfDeleted()));
                });
                ctx.getTelemetryService().deleteTimeseriesAndNotify(ctx.getTenantId(), msg.getOriginator(),
                        keysToDelete, deleteTsKvQueries, new TelemetryNodeCallback(ctx, msg));
            } catch (Exception e) {
                ctx.tellFailure(msg, e);
            }
        }
    }

    @Override
    public void destroy() {

    }

    private void setDeleteTsIntervalsFromPatterns(TbMsg msg) {
        String startTsIntervalPattern = config.getStartTsIntervalPattern();
        String endTsIntervalPattern = config.getEndTsIntervalPattern();
        String startIntervalPatternValue = getPatternValue(msg, startTsIntervalPattern);
        String endIntervalPatternValue = getPatternValue(msg, endTsIntervalPattern);
        if (startIntervalPatternValue == null) {
            throw new IllegalArgumentException("Message value: '" +
                    replaceRegex(startTsIntervalPattern) + "' is undefined!");
        }
        if (endIntervalPatternValue == null) {
            throw new IllegalArgumentException("Message value: '" +
                    replaceRegex(endTsIntervalPattern) + "' is undefined!");
        }
        if (!NumberUtils.isParsable(startIntervalPatternValue)) {
            throw new IllegalArgumentException("Message value: '" +
                    replaceRegex(startTsIntervalPattern) + "' is invalid!");
        }
        if (!NumberUtils.isParsable(endIntervalPatternValue)) {
            throw new IllegalArgumentException("Message value: '" +
                    replaceRegex(endTsIntervalPattern) + "' is invalid!");
        }
        deleteFromTs = Long.parseLong(startIntervalPatternValue);
        deleteToTs = Long.parseLong(endIntervalPatternValue);
    }

    private String getPatternValue(TbMsg msg, String pattern) {
        return TbNodeUtils.processPattern(pattern, msg);
    }

    private String replaceRegex(String pattern) {
        return pattern.replaceAll("\\$\\{?\\[?", "").replaceAll("}?]?", "");
    }
}
