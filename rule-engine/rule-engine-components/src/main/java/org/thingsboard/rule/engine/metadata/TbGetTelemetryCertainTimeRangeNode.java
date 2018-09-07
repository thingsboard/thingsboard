/**
 * Copyright © 2016-2018 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.rule.engine.api.util.DonAsynchron;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.kv.BaseTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvQuery;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;
import static org.thingsboard.server.common.data.kv.Aggregation.NONE;

/**
 * Created by mshvayka on 04.09.18.
 */
@Slf4j
@RuleNode(type = ComponentType.ENRICHMENT,
        name = "get telemetry from certain time range",
        configClazz = TbGetTelemetryCertainTimeRangeNodeConfiguration.class,
        nodeDescription = "Fetch telemetry of certain time range based on the certain delay in the Message Metadata.\n",
        nodeDetails = "The node allows you to select fetch mode <b>FIRST/LAST/ALL</b> to fetch telemetry of certain time range that are added into Message metadata without any prefix. " +
                "If selected fetch mode <b>ALL</b> Telemetry will be added like array into Message Metadata where <b>key</b> is Timestamp and <b>value</b> is value of Telemetry. " +
                "If selected fetch mode <b>FIRST</b> or <b>LAST</b> Telemetry will be added like string without Timestamp",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbEnrichmentNodeGetTelemetryFromDatabase")
public class TbGetTelemetryCertainTimeRangeNode implements TbNode {

    private TbGetTelemetryCertainTimeRangeNodeConfiguration config;
    private List<String> tsKeyNames;
    private long startTsOffset;
    private long endTsOffset;
    private int limit;
    private ObjectMapper mapper;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbGetTelemetryCertainTimeRangeNodeConfiguration.class);
        tsKeyNames = config.getLatestTsKeyNames();
        startTsOffset = TimeUnit.valueOf(config.getStartIntervalTimeUnit()).toMillis(config.getStartInterval());
        endTsOffset = TimeUnit.valueOf(config.getEndIntervalTimeUnit()).toMillis(config.getEndInterval());
        limit = config.getFetchMode().equals(TbGetTelemetryCertainTimeRangeNodeConfiguration.FETCH_MODE_ALL)
                ? TbGetTelemetryCertainTimeRangeNodeConfiguration.MAX_FETCH_SIZE : 1;
        mapper = new ObjectMapper();
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        ObjectNode resultNode = mapper.createObjectNode();
        List<TsKvQuery> queries = new ArrayList<>();
        long ts = System.currentTimeMillis();
        long startTs = ts - startTsOffset;
        long endTs = ts - endTsOffset;
        if (tsKeyNames.isEmpty()) {
            ctx.tellFailure(msg, new Exception("Telemetry are not selected!"));
        } else {
            for (String key : tsKeyNames) {
                //TODO: handle direction;
                queries.add(new BaseTsKvQuery(key, startTs, endTs, 1, limit, NONE));
            }
            try {
                ListenableFuture<List<TsKvEntry>> list = ctx.getTimeseriesService().findAll(msg.getOriginator(), queries);
                DonAsynchron.withCallback(list, data -> {
                    for (TsKvEntry tsKvEntry : data) {
                        if (limit == TbGetTelemetryCertainTimeRangeNodeConfiguration.MAX_FETCH_SIZE) {
                            ArrayNode arrayNode;
                            if(resultNode.has(tsKvEntry.getKey())){
                                arrayNode = (ArrayNode) resultNode.get(tsKvEntry.getKey());
                                arrayNode.add(mapper.createObjectNode().put(String.valueOf(tsKvEntry.getTs()), tsKvEntry.getValueAsString()));
                            }else {
                                arrayNode =  mapper.createArrayNode();
                                arrayNode.add((mapper.createObjectNode().put(String.valueOf(tsKvEntry.getTs()), tsKvEntry.getValueAsString())));
                                resultNode.set(tsKvEntry.getKey(), arrayNode);
                            }
                        } else {
                            resultNode.put(tsKvEntry.getKey(), tsKvEntry.getValueAsString());
                        }
                    }
                    for (String key : tsKeyNames) {
                        if(resultNode.has(key)){
                            msg.getMetaData().putValue(key, resultNode.get(key).toString());
                        }
                    }
                    TbMsg newMsg = ctx.newMsg(msg.getType(), msg.getOriginator(), msg.getMetaData(), msg.getData());
                    ctx.tellNext(newMsg, SUCCESS);
                }, error -> ctx.tellFailure(msg, error));
            } catch (Exception e) {
                ctx.tellFailure(msg, e);
            }
        }
    }

    @Override
    public void destroy() {

    }
}
