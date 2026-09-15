// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.telemetry;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.AggregationParams;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.IntervalType;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.service.security.AccessValidator;
import org.thingsboard.server.service.security.ValidationResult;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultTbTelemetryService implements TbTelemetryService {

    private final TimeseriesService tsService;
    private final AccessValidator accessValidator;

    @Override
    public ListenableFuture<List<TsKvEntry>> getTimeseries(EntityId entityId, List<String> keys, Long startTs, Long endTs, IntervalType intervalType,
                                                           Long interval, String timeZone, Integer limit, Aggregation agg, String orderBy,
                                                           Boolean useStrictDataTypes, SecurityUser currentUser) {
        SettableFuture<List<TsKvEntry>> future = SettableFuture.create();
        accessValidator.validate(currentUser, Operation.READ_TELEMETRY, entityId, new FutureCallback<>() {
            @Override
            public void onSuccess(ValidationResult validationResult) {
                try {
                    AggregationParams params;
                    if (Aggregation.NONE.equals(agg)) {
                        params = AggregationParams.none();
                    } else if (intervalType == null || IntervalType.MILLISECONDS.equals(intervalType)) {
                        params = interval == 0L ? AggregationParams.none() : AggregationParams.milliseconds(agg, interval);
                    } else {
                        params = AggregationParams.calendar(agg, intervalType, timeZone);
                    }
                    List<ReadTsKvQuery> queries = keys.stream().map(key -> new BaseReadTsKvQuery(key, startTs, endTs, params, limit, orderBy)).collect(Collectors.toList());
                    Futures.addCallback(tsService.findAll(currentUser.getTenantId(), entityId, queries), new FutureCallback<>() {
                        @Override
                        public void onSuccess(List<TsKvEntry> result) {
                            future.set(result);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            future.setException(t);
                        }
                    }, MoreExecutors.directExecutor());
                } catch (Throwable e) {
                    onFailure(e);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                future.setException(t);
            }
        });
        return future;
    }

}
