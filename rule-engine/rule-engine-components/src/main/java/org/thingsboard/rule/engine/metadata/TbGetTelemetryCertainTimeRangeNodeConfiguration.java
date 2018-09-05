package org.thingsboard.rule.engine.metadata;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by mshvayka on 04.09.18.
 */
@Data
public class TbGetTelemetryCertainTimeRangeNodeConfiguration implements NodeConfiguration<TbGetTelemetryCertainTimeRangeNodeConfiguration> {

    public static final String FETCH_MODE_FIRST = "FIRST";
    public static final String FETCH_MODE_LAST = "LAST";
    public static final String FETCH_MODE_ALL = "ALL";
    public static final int MAX_FETCH_SIZE = 1000;

    private int startInterval;
    private int endInterval;
    private String startIntervalTimeUnit;
    private String endIntervalTimeUnit;
    private String fetchMode; //FIRST, LAST, LATEST

    private List<String> latestTsKeyNames;



    @Override
    public TbGetTelemetryCertainTimeRangeNodeConfiguration defaultConfiguration() {
        TbGetTelemetryCertainTimeRangeNodeConfiguration configuration = new TbGetTelemetryCertainTimeRangeNodeConfiguration();
        configuration.setLatestTsKeyNames(Collections.emptyList());
        configuration.setStartIntervalTimeUnit(TimeUnit.MINUTES.name());
        configuration.setStartInterval(1);
        configuration.setEndIntervalTimeUnit(TimeUnit.MINUTES.name());
        configuration.setEndInterval(2);
        return configuration;
    }
}
