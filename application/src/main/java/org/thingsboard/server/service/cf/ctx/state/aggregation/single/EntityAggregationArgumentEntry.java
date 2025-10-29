package org.thingsboard.server.service.cf.ctx.state.aggregation.single;

import lombok.Data;
import org.thingsboard.script.api.tbel.TbelCfArg;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntryType;
import org.thingsboard.server.service.cf.ctx.state.SingleValueArgumentEntry;

import java.util.Map;

@Data
public class EntityAggregationArgumentEntry implements ArgumentEntry {

    private Map<AggIntervalEntry, AggIntervalEntryStatus> aggIntervals;

    private boolean forceResetPrevious;

    public EntityAggregationArgumentEntry(Map<AggIntervalEntry, AggIntervalEntryStatus> aggIntervals) {
        this.aggIntervals = aggIntervals;
    }

    @Override
    public ArgumentEntryType getType() {
        return ArgumentEntryType.ENTITY_AGGREGATION;
    }

    @Override
    public Object getValue() {
        return aggIntervals;
    }

    @Override
    public boolean updateEntry(ArgumentEntry entry) {
        if (entry instanceof EntityAggregationArgumentEntry entityAggEntry) {
            aggIntervals.putAll(entityAggEntry.getAggIntervals());
        } else if (entry instanceof SingleValueArgumentEntry singleValueArgEntry) {
            long entryTs = singleValueArgEntry.getTs();
            for (Map.Entry<AggIntervalEntry, AggIntervalEntryStatus> aggIntervalEntry : aggIntervals.entrySet()) {
                if (aggIntervalEntry.getKey().belongsToInterval(entryTs)) {
                    aggIntervalEntry.getValue().setLastArgsRefreshTs(System.currentTimeMillis());
                    aggIntervals.put(aggIntervalEntry.getKey(), aggIntervalEntry.getValue());
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public TbelCfArg toTbelCfArg() {
        return null;
    }

}
