package org.thingsboard.server.dao.sql.timeseries;

import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvQuery;
import org.thingsboard.server.dao.timeseries.TimeseriesDao;

import java.util.List;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "sql", value = "enabled", havingValue = "true")
public class JpaTimeseriesDao implements TimeseriesDao {

    @Override
    public long toPartitionTs(long ts) {
        return 0;
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllAsync(EntityId entityId, List<TsKvQuery> queries) {
        return null;
    }

    @Override
    public ResultSetFuture findLatest(EntityId entityId, String key) {
        return null;
    }

    @Override
    public ResultSetFuture findAllLatest(EntityId entityId) {
        return null;
    }

    @Override
    public ResultSetFuture save(EntityId entityId, long partition, TsKvEntry tsKvEntry) {
        return null;
    }

    @Override
    public ResultSetFuture savePartition(EntityId entityId, long partition, String key) {
        return null;
    }

    @Override
    public ResultSetFuture saveLatest(EntityId entityId, TsKvEntry tsKvEntry) {
        return null;
    }

    @Override
    public TsKvEntry convertResultToTsKvEntry(Row row) {
        return null;
    }

    @Override
    public List<TsKvEntry> convertResultToTsKvEntryList(List<Row> rows) {
        return null;
    }
}
