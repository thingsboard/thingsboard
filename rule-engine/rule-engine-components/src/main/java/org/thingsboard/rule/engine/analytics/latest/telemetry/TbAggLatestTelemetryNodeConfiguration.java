// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.latest.telemetry;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.rule.engine.analytics.incoming.MathFunction;
import org.thingsboard.rule.engine.analytics.latest.ParentEntitiesGroup;
import org.thingsboard.rule.engine.analytics.latest.TbAbstractLatestNodeConfiguration;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.msg.TbMsgType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Data
@EqualsAndHashCode(callSuper = true)
public class TbAggLatestTelemetryNodeConfiguration extends TbAbstractLatestNodeConfiguration implements NodeConfiguration<TbAggLatestTelemetryNodeConfiguration> {

    private List<AggLatestMapping> aggMappings;

    @Override
    public TbAggLatestTelemetryNodeConfiguration defaultConfiguration() {
        TbAggLatestTelemetryNodeConfiguration configuration = new TbAggLatestTelemetryNodeConfiguration();

        configuration.setParentEntitiesQuery(new ParentEntitiesGroup());

        List<AggLatestMapping> aggMappings = new ArrayList<>();
        AggLatestMapping aggMapping = new AggLatestMapping();
        aggMapping.setSource("temperature");
        aggMapping.setSourceScope("LATEST_TELEMETRY");
        aggMapping.setAggFunction(MathFunction.AVG);
        aggMapping.setDefaultValue(0);
        aggMapping.setTarget("latestAvgTemperature");
        aggMappings.add(aggMapping);

        configuration.setAggMappings(aggMappings);

        configuration.setPeriodTimeUnit(TimeUnit.MINUTES);
        configuration.setPeriodValue(5);
        configuration.setOutMsgType(TbMsgType.POST_TELEMETRY_REQUEST.name());
        return configuration;
    }
}
