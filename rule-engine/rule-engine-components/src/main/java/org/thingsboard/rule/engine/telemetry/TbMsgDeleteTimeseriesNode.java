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

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
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
                "To delete all data for keys checkbox <b>Delete all data for keys<b> should be selected, " +
                "otherwise will be deleted data that is in range of the selected time interval. If selected " +
                "timeseries data successfully deleted -  Message send via <b>Success</b> chain, otherwise <b>Failure</b> chain will be used.",
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
            try {
                if (config.getDeleteAllDataForKeys()) {
                    deleteFromTs = 0L;
                    deleteToTs = System.currentTimeMillis();
                } else {
                    if (config.isUseMetadataIntervalPatterns()) {
                        getDeleteTsIntervalFromPatterns(msg);
                    } else {
                        long ts = System.currentTimeMillis();
                        deleteFromTs = ts - TimeUnit.valueOf(config.getStartTsTimeUnit()).toMillis(config.getStartTs());
                        deleteToTs = ts - TimeUnit.valueOf(config.getEndTsTimeUnit()).toMillis(config.getEndTs());
                    }
                }
                List<DeleteTsKvQuery> deleteTsKvQueries = new ArrayList<>();
                for (String key : keys) {
                    deleteTsKvQueries.add(new BaseDeleteTsKvQuery(TbNodeUtils.processPattern(key, msg), deleteFromTs, deleteToTs, config.getRewriteLatestIfDeleted()));
                }
                ListenableFuture<List<Void>> removeFuture = ctx.getTimeseriesService().remove(ctx.getTenantId(), msg.getOriginator(), deleteTsKvQueries);
                DonAsynchron.withCallback(removeFuture, onSuccess -> ctx.tellSuccess(msg), throwable -> ctx.tellFailure(msg, throwable));
            } catch (Exception e) {
                ctx.tellFailure(msg, e);
            }
        }
    }

    @Override
    public void destroy() {

    }

    private void getDeleteTsIntervalFromPatterns(TbMsg msg) {
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
