package org.thingsboard.rule.engine.analytics;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

import java.util.concurrent.TimeUnit;

@Data
public class TbGeoSensorXCheckDuplicateMsgsNodeConfiguration implements NodeConfiguration<TbGeoSensorXCheckDuplicateMsgsNodeConfiguration> {

    private int failuresPollingInterval;
    private TimeUnit failuresPollingIntervalTimeUnit;

    @Override
    public TbGeoSensorXCheckDuplicateMsgsNodeConfiguration defaultConfiguration() {
        TbGeoSensorXCheckDuplicateMsgsNodeConfiguration configuration = new TbGeoSensorXCheckDuplicateMsgsNodeConfiguration();
        configuration.setFailuresPollingInterval(1);
        configuration.setFailuresPollingIntervalTimeUnit(TimeUnit.HOURS);
        return configuration;
    }
}

