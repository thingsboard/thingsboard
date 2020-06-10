package org.thingsboard.server.service.telemetry.cmd.v2;

import lombok.Data;
import org.thingsboard.server.common.data.query.EntityDataQuery;

@Data
public class EntityDataCmd {

    private final int cmdId;
    private final EntityDataQuery query;
    private final EntityHistoryCmd historyCmd;
    private final LatestValueCmd latestCmd;
    private final TimeSeriesCmd tsCmd;

}
