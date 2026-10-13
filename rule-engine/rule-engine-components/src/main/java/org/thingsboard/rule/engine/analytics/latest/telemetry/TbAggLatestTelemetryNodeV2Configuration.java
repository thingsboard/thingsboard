// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.latest.telemetry;

import lombok.Data;
import org.thingsboard.rule.engine.analytics.incoming.MathFunction;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;

import java.util.ArrayList;
import java.util.List;

@Data
public class TbAggLatestTelemetryNodeV2Configuration implements NodeConfiguration<TbAggLatestTelemetryNodeV2Configuration> {

    private static final int MIN_DEDUPLICATION_IN_SEC = 10;

    private String outMsgType;

    private EntitySearchDirection direction;
    private String relationType;
    private long deduplicationInSec;
    private List<AggLatestMapping> aggMappings;

    @Override
    public TbAggLatestTelemetryNodeV2Configuration defaultConfiguration() {
        TbAggLatestTelemetryNodeV2Configuration configuration = new TbAggLatestTelemetryNodeV2Configuration();

        List<AggLatestMapping> aggMappings = new ArrayList<>();
        AggLatestMapping aggMapping = new AggLatestMapping();
        aggMapping.setSource("temperature");
        aggMapping.setSourceScope("LATEST_TELEMETRY");
        aggMapping.setAggFunction(MathFunction.AVG);
        aggMapping.setDefaultValue(0);
        aggMapping.setTarget("latestAvgTemperature");
        aggMappings.add(aggMapping);

        configuration.setAggMappings(aggMappings);
        configuration.setOutMsgType(TbMsgType.POST_TELEMETRY_REQUEST.name());
        return configuration;
    }

    public long getDeduplicationInSec() {
        return Math.max(MIN_DEDUPLICATION_IN_SEC, deduplicationInSec);
    }
}
