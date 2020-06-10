package org.thingsboard.server.service.telemetry.cmd.v2;

import org.thingsboard.server.common.data.kv.Aggregation;

import java.util.List;

public class EntityHistoryCmd {

    private List<String> keys;
    private long startTs;
    private long endTs;
    private long interval;
    private int limit;
    private Aggregation agg;

}
