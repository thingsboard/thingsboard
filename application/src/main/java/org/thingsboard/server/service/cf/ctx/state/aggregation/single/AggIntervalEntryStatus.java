package org.thingsboard.server.service.cf.ctx.state.aggregation.single;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
public class AggIntervalEntryStatus {

    @Setter
    private long lastArgsRefreshTs = -1;
    @Setter
    private long lastMetricsEvalTs = -1;

    public AggIntervalEntryStatus(long lastArgsRefreshTs) {
        this.lastArgsRefreshTs = lastArgsRefreshTs;
    }

    public boolean shouldRecalculate(long checkInterval) {
        boolean intervalPassed = lastMetricsEvalTs <= System.currentTimeMillis() - checkInterval;
        boolean argsUpdatedDuringInterval = lastArgsRefreshTs > lastMetricsEvalTs;
        return intervalPassed && argsUpdatedDuringInterval;
    }

}
