package org.thingsboard.rule.engine.metadata;

import com.fasterxml.jackson.databind.JsonNode;
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
        name = "huy",
        configClazz = TbGetTelemetryCertainTimeRangeNodeConfiguration.class,
        nodeDescription = "",
        nodeDetails = "",
        uiResources = "", //{"static/rulenode/rulenode-core-config.js"},
        configDirective = "")
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
            ctx.tellFailure(msg, new Exception("Telemetry not found"));
        } else {
            for (String key : tsKeyNames) {
                //TODO: handle direction;
                queries.add(new BaseTsKvQuery(key, startTs, endTs, 1, limit, NONE));
                if (limit == TbGetTelemetryCertainTimeRangeNodeConfiguration.MAX_FETCH_SIZE) {
                    resultNode.set(key, mapper.createArrayNode());
                } else {
                    resultNode.putObject(key);
                }
            }
            try {
                ListenableFuture<List<TsKvEntry>> list = ctx.getTimeseriesService().findAll(msg.getOriginator(), queries);
                DonAsynchron.withCallback(list, data -> {
                    for (TsKvEntry tsKvEntry : data) {
                        JsonNode node = resultNode.get(tsKvEntry.getKey());
                        if (node.isArray()) {
                            ArrayNode arrayNode = (ArrayNode) node;
                            arrayNode.add(mapper.createObjectNode().put(String.valueOf(tsKvEntry.getTs()), tsKvEntry.getValueAsString()));
                        } else {
                            ObjectNode object = mapper.createObjectNode().put(String.valueOf(tsKvEntry.getTs()), tsKvEntry.getValueAsString());
                            resultNode.set(tsKvEntry.getKey(), object);
                        }
                    }
                    for (String key : tsKeyNames) {
                        msg.getMetaData().putValue(key, resultNode.get(key).toString());
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
