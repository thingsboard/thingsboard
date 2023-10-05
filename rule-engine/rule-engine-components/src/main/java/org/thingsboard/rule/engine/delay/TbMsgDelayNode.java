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
package org.thingsboard.rule.engine.delay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.time.Duration;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "delay (deprecated)",
        configClazz = TbMsgDelayNodeConfiguration.class,
        nodeDescription = "Delays incoming message (deprecated)",
        nodeDetails = "Delays messages for a configurable period. " +
                "Please note, this node acknowledges the message from the current queue (message will be removed from queue). " +
                "Deprecated because the acknowledged message still stays in memory (to be delayed) and this " +
                "does not guarantee that message will be processed even if the \"retry failures and timeouts\" processing strategy will be chosen.",
        icon = "pause",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeMsgDelayConfig",
        version = 1
)
public class TbMsgDelayNode implements TbNode {

    private TbMsgDelayNodeConfiguration config;
    private Map<UUID, TbMsg> pendingMsgs;
    private final Set<TimeUnit> supportedTimeUnits = EnumSet.of(TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS);

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgDelayNodeConfiguration.class);
        this.pendingMsgs = new HashMap<>();
        if (config.getMaxPendingMsgs() < 1 || config.getMaxPendingMsgs() > 100000) {
            throw new TbNodeException("Invalid maximum pending messages limit [" + config.getMaxPendingMsgs() + "], " +
                    "should be in range from 1 to 100000 (inclusive)!", true);
        }
        if (!supportedTimeUnits.contains(config.getPeriodTimeUnit())) {
            throw new TbNodeException("Unsupported time unit: " + config.getPeriodTimeUnit(), true);
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (msg.isTypeOf(TbMsgType.DELAY_TIMEOUT_SELF_MSG)) {
            TbMsg pendingMsg = pendingMsgs.remove(UUID.fromString(msg.getData()));
            if (pendingMsg != null) {
                ctx.enqueueForTellNext(
                        TbMsg.newMsg(pendingMsg, pendingMsg.getQueueName(), null, null),
                        TbNodeConnectionType.SUCCESS
                );
            }
        } else {
            if (pendingMsgs.size() < config.getMaxPendingMsgs()) {
                pendingMsgs.put(msg.getId(), msg);
                TbMsg tickMsg = ctx.newMsg(null, TbMsgType.DELAY_TIMEOUT_SELF_MSG, ctx.getSelfId(), msg.getCustomerId(), TbMsgMetaData.EMPTY, msg.getId().toString());
                long periodValue = getDelayPeriodValue(msg);
                TimeUnit periodTimeUnit = getDelayPeriodTimeUnit(msg);
                ctx.tellSelf(tickMsg, Duration.of(periodValue, periodTimeUnit.toChronoUnit()).toMillis());
                ctx.ack(msg);
            } else {
                ctx.tellFailure(msg, new RuntimeException("Max limit of pending messages reached!"));
            }
        }
    }

    private long getDelayPeriodValue(TbMsg msg) {
        if (!config.isUsePeriodValuePattern()) {
            return config.getPeriodValue();
        }
        try {
            return Integer.parseInt(TbNodeUtils.processPattern(config.getPeriodValuePattern(), msg));
        } catch (NumberFormatException e) {
            throw new RuntimeException("Can't parse period value using pattern: " + config.getPeriodValuePattern());
        }
    }

    private TimeUnit getDelayPeriodTimeUnit(TbMsg msg) {
        if (!config.isUsePeriodTimeUnitPattern()) {
            return config.getPeriodTimeUnit();
        }
        TimeUnit periodTimeUnit;
        try {
            periodTimeUnit = TimeUnit.valueOf(TbNodeUtils.processPattern(config.getPeriodTimeUnitPattern(), msg));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new RuntimeException("Can't parse period time unit using pattern: " + config.getPeriodTimeUnitPattern());
        }
        if (!supportedTimeUnits.contains(periodTimeUnit)) {
            throw new RuntimeException("Unsupported time unit: " + periodTimeUnit);
        }
        return periodTimeUnit;
    }

    @Override
    public void destroy() {
        pendingMsgs.clear();
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) {
        boolean hasChanges = false;
        switch (fromVersion) {
            case 0: {
                var oldConfigurationNode = (ObjectNode) oldConfiguration;

                final String periodInSecondsPropertyName = "periodInSeconds";
                final String periodValuePropertyName = "periodValue";

                if (oldConfigurationNode.has(periodInSecondsPropertyName)) {
                    if (!oldConfigurationNode.hasNonNull(periodValuePropertyName)) {
                        JsonNode periodInSecondsNode = oldConfigurationNode.get(periodInSecondsPropertyName);
                        oldConfigurationNode.put(periodValuePropertyName, periodInSecondsNode.intValue());
                    }
                    oldConfigurationNode.remove(periodInSecondsPropertyName);
                    hasChanges = true;
                }

                final String periodTimeUnitPropertyName = "periodTimeUnit";

                if (!oldConfigurationNode.hasNonNull(periodTimeUnitPropertyName)) {
                    oldConfigurationNode.put(periodTimeUnitPropertyName, "SECONDS");
                    hasChanges = true;
                }

                final String periodInSecondsPatternPropertyName = "periodInSecondsPattern";
                final String periodValuePatternPropertyName = "periodValuePattern";

                if (oldConfigurationNode.has(periodInSecondsPatternPropertyName)) {
                    if (!oldConfigurationNode.hasNonNull(periodValuePatternPropertyName)) {
                        JsonNode periodInSecondsPatternNode = oldConfigurationNode.get(periodInSecondsPatternPropertyName);
                        oldConfigurationNode.put(periodValuePatternPropertyName, periodInSecondsPatternNode.isNull() ? null : periodInSecondsPatternNode.asText());
                    }
                    oldConfigurationNode.remove(periodInSecondsPatternPropertyName);
                    hasChanges = true;
                }

                final String useMetadataPeriodInSecondsPatternsPropertyName = "useMetadataPeriodInSecondsPatterns";
                final String usePeriodValuePatternPropertyName = "usePeriodValuePattern";

                if (oldConfigurationNode.has(useMetadataPeriodInSecondsPatternsPropertyName)) {
                    if (!oldConfigurationNode.hasNonNull(usePeriodValuePatternPropertyName)) {
                        JsonNode useMetadataPeriodInSecondsPatternsNode = oldConfigurationNode.get(useMetadataPeriodInSecondsPatternsPropertyName);
                        oldConfigurationNode.put(usePeriodValuePatternPropertyName, useMetadataPeriodInSecondsPatternsNode.booleanValue());
                    }
                    oldConfigurationNode.remove(useMetadataPeriodInSecondsPatternsPropertyName);
                    hasChanges = true;
                }

                oldConfigurationNode.set("periodTimeUnitPattern", null);
                oldConfigurationNode.put("usePeriodTimeUnitPattern", false);
            }
            default:
                break;
        }
        return new TbPair<>(hasChanges, oldConfiguration);
    }

}
