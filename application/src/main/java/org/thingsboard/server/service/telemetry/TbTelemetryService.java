// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.telemetry;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.IntervalType;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;

public interface TbTelemetryService {

    ListenableFuture<List<TsKvEntry>> getTimeseries(EntityId entityId,
                                                   List<String> keys,
                                                   Long startTs,
                                                   Long endTs,
                                                   IntervalType intervalType,
                                                   Long interval,
                                                   String timeZone,
                                                   Integer limit,
                                                   Aggregation agg,
                                                   String orderBy,
                                                   Boolean useStrictDataTypes,
                                                   SecurityUser currentUser) throws ThingsboardException;

    ListenableFuture<List<ReadTsKvQueryResult>> getTimeseriesByReadQueries(EntityId entityId,
                                                                           List<BaseReadTsKvQuery> queries,
                                                                           SecurityUser currentUser);
}
