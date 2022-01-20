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

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.kv.BaseDeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
        configDirective = "tbActionNodeDeleteTimeseriesConfig",
        icon = "remove_circle"
)
public class TbMsgDeleteTimeseriesNode implements TbNode {

    private TbMsgDeleteTimeseriesNodeConfiguration config;
    private long startTs;
    private long endTs;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgDeleteTimeseriesNodeConfiguration.class);
        if (CollectionUtils.isEmpty(config.getKeysPatterns())) {
            throw new IllegalStateException("Telemetry keys list is not selected!");
        }
        this.startTs = TimeUnit.valueOf(config.getStartTsTimeUnit()).toMillis(config.getStartTs());
        this.endTs = TimeUnit.valueOf(config.getEndTsTimeUnit()).toMillis(config.getEndTs());
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        List<String> keysPatterns = config.getKeysPatterns();
        long deleteFromTs;
        long deleteToTs;
        long currentTimeMillis = System.currentTimeMillis();
        try {
            if (config.getDeleteAllDataForKeys()) {
                deleteFromTs = 0L;
                deleteToTs = currentTimeMillis;
            } else {
                if (config.isUseMetadataIntervalPatterns()) {
                    deleteFromTs = setDeleteTsIntervalFromPattern(msg, config.getStartTsIntervalPattern());
                    deleteToTs = setDeleteTsIntervalFromPattern(msg, config.getEndTsIntervalPattern());
                } else {
                    deleteFromTs = currentTimeMillis - startTs;
                    deleteToTs = currentTimeMillis - endTs;
                }
            }
            List<String> keysToDelete = keysPatterns.stream()
                    .map(keyPattern -> TbNodeUtils.processPattern(keyPattern, msg))
                    .distinct()
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList());
            if (keysToDelete.isEmpty()) {
                throw new RuntimeException("Selected keys patterns have invalid values!");
            }
            List<DeleteTsKvQuery> deleteTsKvQueries = keysToDelete.stream()
                    .map(key -> new BaseDeleteTsKvQuery(key, deleteFromTs, deleteToTs, config.getRewriteLatestIfDeleted()))
                    .collect(Collectors.toList());
            ctx.getTelemetryService().deleteTimeseriesAndNotify(ctx.getTenantId(), msg.getOriginator(),
                    keysToDelete, deleteTsKvQueries, new TelemetryNodeCallback(ctx, msg));
        } catch (Exception e) {
            ctx.tellFailure(msg, e);
        }
    }

    @Override
    public void destroy() {

    }

    private long setDeleteTsIntervalFromPattern(TbMsg msg, String pattern) {
        String patternValue = TbNodeUtils.processPattern(pattern, msg);
        return validateIntervalPatternValue(pattern, patternValue);
    }

    private long validateIntervalPatternValue(String pattern, String substitution) {
        try {
            return Long.parseLong(substitution);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Failed to parse message value for '" + pattern + "' pattern due to: ", e);
        }
    }

}
