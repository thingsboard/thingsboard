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
package org.thingsboard.rule.engine.telemetry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonParser;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.telemetry.settings.TimeseriesProcessingSettings;
import org.thingsboard.rule.engine.telemetry.strategy.ProcessingStrategy;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.rule.engine.telemetry.settings.TimeseriesProcessingSettings.Advanced;
import static org.thingsboard.rule.engine.telemetry.settings.TimeseriesProcessingSettings.Deduplicate;
import static org.thingsboard.rule.engine.telemetry.settings.TimeseriesProcessingSettings.OnEveryMessage;
import static org.thingsboard.rule.engine.telemetry.settings.TimeseriesProcessingSettings.WebSocketsOnly;
import static org.thingsboard.server.common.data.msg.TbMsgType.POST_TELEMETRY_REQUEST;
import static org.thingsboard.server.dao.util.KvUtils.toTsKvEntryList;

@RuleNode(
        type = ComponentType.ACTION,
        name = "save time series",
        configClazz = TbMsgTimeseriesNodeConfiguration.class,
        nodeDescription = """
                Saves time series data with a configurable TTL and according to configured processing strategies.
                """,
        nodeDetails = """
                Node performs four <strong>actions:</strong>
                <ul>
                  <li><strong>Time series:</strong> save time series data to a <code>ts_kv</code> table in a DB.</li>
                  <li><strong>Latest values:</strong> save time series data to a <code>ts_kv_latest</code> table in a DB.</li>
                  <li><strong>WebSockets:</strong> notify WebSockets subscriptions about time series data updates.</li>
                  <li><strong>Calculated fields:</strong> notify calculated fields about time series data updates.</li>
                </ul>
                
                For each <em>action</em>, three <strong>processing strategies</strong> are available:
                <ul>
                  <li><strong>On every message:</strong> perform the action for every message.</li>
                  <li><strong>Deduplicate:</strong> perform the action only for the first message from a particular originator within a configurable interval.</li>
                  <li><strong>Skip:</strong> never perform the action.</li>
                </ul>
                
                <strong>Processing strategies</strong> are configured using <em>processing settings</em>, which support two modes:
                <ul>
                  <li><strong>Basic</strong>
                    <ul>
                      <li><strong>On every message:</strong> applies the "On every message" strategy to all actions.</li>
                      <li><strong>Deduplicate:</strong> applies the "Deduplicate" strategy (with a specified interval) to all actions.</li>
                      <li><strong>WebSockets only:</strong> for all actions except WebSocket notifications, the "Skip" strategy is applied, while WebSocket notifications use the "On every message" strategy.</li>
                    </ul>
                  </li>
                  <li><strong>Advanced:</strong> configure each action’s strategy independently.</li>
                </ul>
                
                By default, the timestamp is taken from <code>metadata.ts</code>. You can enable
                <em>Use server timestamp</em> to always use the current server time instead. This is particularly
                useful in sequential processing scenarios where messages may arrive with out-of-order timestamps from
                multiple sources. Note that the DB layer may ignore "outdated" records for attributes and latest values,
                so enabling <em>Use server timestamp</em> can ensure correct ordering.
                <br><br>
                The TTL is taken first from <code>metadata.TTL</code>. If absent, the node configuration’s default
                TTL is used. If neither is set, the tenant profile default applies.
                <br><br>
                This node expects messages of type <code>POST_TELEMETRY_REQUEST</code>.
                <br><br>
                Output connections: <code>Success</code>, <code>Failure</code>.
                """,
        configDirective = "tbActionNodeTimeseriesConfig",
        icon = "file_upload",
        version = 1,
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/action/save-timeseries/"
)
public class TbMsgTimeseriesNode implements TbNode {

    private TbMsgTimeseriesNodeConfiguration config;
    private TbContext ctx;
    private long tenantProfileDefaultStorageTtl;

    private TimeseriesProcessingSettings processingSettings;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgTimeseriesNodeConfiguration.class);
        this.ctx = ctx;
        ctx.addTenantProfileListener(this::onTenantProfileUpdate);
        onTenantProfileUpdate(ctx.getTenantProfile());
        processingSettings = config.getProcessingSettings();
    }

    private void onTenantProfileUpdate(TenantProfile tenantProfile) {
        DefaultTenantProfileConfiguration configuration = (DefaultTenantProfileConfiguration) tenantProfile.getProfileData().getConfiguration();
        tenantProfileDefaultStorageTtl = TimeUnit.DAYS.toSeconds(configuration.getDefaultStorageTtlDays());
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (!msg.isTypeOf(POST_TELEMETRY_REQUEST)) {
            ctx.tellFailure(msg, new IllegalArgumentException("Unsupported msg type: " + msg.getType()));
            return;
        }
        long ts = computeTs(msg, config.isUseServerTs());

        TimeseriesSaveRequest.Strategy strategy = determineSaveStrategy(ts, msg.getOriginator().getId());

        // short-circuit
        if (!strategy.saveTimeseries() && !strategy.saveLatest() && !strategy.sendWsUpdate() && !strategy.processCalculatedFields()) {
            ctx.tellSuccess(msg);
            return;
        }

        String src = msg.getData();
        Map<Long, List<KvEntry>> tsKvMap = JsonConverter.convertToTelemetry(JsonParser.parseString(src), ts);
        if (tsKvMap.isEmpty()) {
            ctx.tellFailure(msg, new IllegalArgumentException("Msg body is empty: " + src));
            return;
        }
        List<TsKvEntry> tsKvEntryList = toTsKvEntryList(tsKvMap);
        String ttlValue = msg.getMetaData().getValue("TTL");
        long ttl = !StringUtils.isEmpty(ttlValue) ? Long.parseLong(ttlValue) : config.getDefaultTTL();
        if (ttl == 0L) {
            ttl = tenantProfileDefaultStorageTtl;
        }
        ctx.getTelemetryService().saveTimeseries(TimeseriesSaveRequest.builder()
                .tenantId(ctx.getTenantId())
                .customerId(msg.getCustomerId())
                .entityId(msg.getOriginator())
                .entries(tsKvEntryList)
                .ttl(ttl)
                .strategy(strategy)
                .previousCalculatedFieldIds(msg.getPreviousCalculatedFieldIds())
                .tbMsgId(msg.getId())
                .tbMsgType(msg.getInternalType())
                .callback(new TelemetryNodeCallback(ctx, msg))
                .build());
    }

    public static long computeTs(TbMsg msg, boolean ignoreMetadataTs) {
        return ignoreMetadataTs ? System.currentTimeMillis() : msg.getMetaDataTs();
    }

    private TimeseriesSaveRequest.Strategy determineSaveStrategy(long ts, UUID originatorUuid) {
        if (processingSettings instanceof OnEveryMessage) {
            return TimeseriesSaveRequest.Strategy.PROCESS_ALL;
        }
        if (processingSettings instanceof WebSocketsOnly) {
            return TimeseriesSaveRequest.Strategy.WS_ONLY;
        }
        if (processingSettings instanceof Deduplicate deduplicate) {
            boolean isFirstMsgInInterval = deduplicate.getProcessingStrategy().shouldProcess(ts, originatorUuid);
            return isFirstMsgInInterval ? TimeseriesSaveRequest.Strategy.PROCESS_ALL : TimeseriesSaveRequest.Strategy.SKIP_ALL;
        }
        if (processingSettings instanceof Advanced advanced) {
            return new TimeseriesSaveRequest.Strategy(
                    advanced.timeseries().shouldProcess(ts, originatorUuid),
                    advanced.latest().shouldProcess(ts, originatorUuid),
                    advanced.webSockets().shouldProcess(ts, originatorUuid),
                    advanced.calculatedFields().shouldProcess(ts, originatorUuid)
            );
        }
        // should not happen
        throw new IllegalArgumentException("Unknown processing settings type: " + processingSettings.getClass().getSimpleName());
    }

    @Override
    public void destroy() {
        ctx.removeListeners();
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        boolean hasChanges = false;
        switch (fromVersion) {
            case 0:
                hasChanges = true;
                JsonNode skipLatestPersistence = oldConfiguration.get("skipLatestPersistence");
                if (skipLatestPersistence != null && "true".equals(skipLatestPersistence.asText())) {
                    var skipLatestProcessingSettings = new Advanced(
                            ProcessingStrategy.onEveryMessage(),
                            ProcessingStrategy.skip(),
                            ProcessingStrategy.onEveryMessage(),
                            ProcessingStrategy.onEveryMessage()
                    );
                    ((ObjectNode) oldConfiguration).set("processingSettings", JacksonUtil.valueToTree(skipLatestProcessingSettings));
                } else {
                    ((ObjectNode) oldConfiguration).set("processingSettings", JacksonUtil.valueToTree(new OnEveryMessage()));
                }
                ((ObjectNode) oldConfiguration).remove("skipLatestPersistence");
                break;
            default:
                break;
        }
        return new TbPair<>(hasChanges, oldConfiguration);
    }

}
