// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.housekeeper.processor;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTaskType;
import org.thingsboard.server.common.data.housekeeper.TsHistoryDeletionHousekeeperTask;
import org.thingsboard.server.common.data.kv.BaseDeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TsHistoryDeletionTaskProcessor extends HousekeeperTaskProcessor<TsHistoryDeletionHousekeeperTask> {

    private final TimeseriesService timeseriesService;

    @Override
    public void process(TsHistoryDeletionHousekeeperTask task) throws Exception {
        wait(processAsync(task));
    }

    @Override
    public ListenableFuture<Void> processAsync(TsHistoryDeletionHousekeeperTask task) {
        DeleteTsKvQuery deleteQuery = new BaseDeleteTsKvQuery(task.getKey(), 0, System.currentTimeMillis(), false, false);
        ListenableFuture<?> future = timeseriesService.remove(task.getTenantId(), task.getEntityId(), List.of(deleteQuery));
        return Futures.transform(future, _ -> {
            log.debug("[{}][{}][{}] Deleted timeseries history for key '{}'", task.getTenantId(), task.getEntityId().getEntityType(), task.getEntityId(), task.getKey());
            return null;
        }, MoreExecutors.directExecutor());
    }

    @Override
    public boolean supportsAsyncProcessing() {
        return true;
    }

    @Override
    public HousekeeperTaskType getTaskType() {
        return HousekeeperTaskType.DELETE_TS_HISTORY;
    }

}
