package org.thingsboard.server.service.telemetry.cmd.v2;

import java.util.List;

public class TimeSeriesCmd {

    private List<String> keys;
    private long startTs;
    private long timeWindow;
    private long interval;
    private int limit;
    private String agg;

}
