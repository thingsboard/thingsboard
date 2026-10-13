// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.incoming;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.rule.engine.analytics.incoming.state.StatePersistPolicy;
import org.thingsboard.rule.engine.analytics.latest.ParentEntitiesGroup;
import org.thingsboard.rule.engine.analytics.latest.TbAbstractLatestNodeConfiguration;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.msg.TbMsgType;

import java.util.concurrent.TimeUnit;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TbSimpleAggMsgNodeConfiguration extends TbAbstractLatestNodeConfiguration implements NodeConfiguration<TbSimpleAggMsgNodeConfiguration> {

    private String mathFunction;

    private AggIntervalType aggIntervalType;
    private String timeZoneId;
    //For Static Intervals
    private String aggIntervalTimeUnit;
    private int aggIntervalValue;

    private boolean autoCreateIntervals;

    private String intervalPersistencePolicy;
    private String intervalCheckTimeUnit;
    private int intervalCheckValue;

    private String inputValueKey;
    private String outputValueKey;

    private String statePersistencePolicy;
    private String statePersistenceTimeUnit;
    private int statePersistenceValue;

    @Override
    public TbSimpleAggMsgNodeConfiguration defaultConfiguration() {
        TbSimpleAggMsgNodeConfiguration configuration = new TbSimpleAggMsgNodeConfiguration();

        configuration.setMathFunction(MathFunction.AVG.name());
        configuration.setAggIntervalType(AggIntervalType.HOUR);
        configuration.setAggIntervalTimeUnit(TimeUnit.HOURS.name());
        configuration.setAggIntervalValue(1);

        configuration.setAutoCreateIntervals(false);

        configuration.setParentEntitiesQuery(new ParentEntitiesGroup());

        configuration.setPeriodTimeUnit(TimeUnit.MINUTES);
        configuration.setPeriodValue(5);

        configuration.setIntervalPersistencePolicy(IntervalPersistPolicy.ON_EACH_CHECK_AFTER_INTERVAL_END.name());
        configuration.setIntervalCheckTimeUnit(TimeUnit.MINUTES.name());
        configuration.setIntervalCheckValue(1);

        configuration.setInputValueKey("temperature");
        configuration.setOutputValueKey("avgHourlyTemperature");

        configuration.setStatePersistencePolicy(StatePersistPolicy.ON_EACH_CHANGE.name());
        configuration.setStatePersistenceTimeUnit(TimeUnit.MINUTES.name());
        configuration.setStatePersistenceValue(1);
        configuration.setOutMsgType(TbMsgType.POST_TELEMETRY_REQUEST.name());

        return configuration;
    }

}
