// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sqlts;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.AbstractTsKvEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDaoListeningExecutorService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public abstract class BaseAbstractSqlTimeseriesDao extends JpaAbstractDaoListeningExecutorService {

    protected ListenableFuture<ReadTsKvQueryResult> getReadTsKvQueryResultFuture(ReadTsKvQuery query, ListenableFuture<List<Optional<? extends AbstractTsKvEntity>>> future) {
        return Futures.transform(future, new Function<>() {
            @Nullable
            @Override
            public ReadTsKvQueryResult apply(@Nullable List<Optional<? extends AbstractTsKvEntity>> results) {
                if (results == null || results.isEmpty()) {
                    return null;
                }
                List<? extends AbstractTsKvEntity> data = results.stream().filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
                var lastTs = data.stream().map(AbstractTsKvEntity::getAggValuesLastTs).filter(Objects::nonNull).max(Long::compare);
                if (lastTs.isEmpty()) {
                    lastTs = data.stream().map(AbstractTsKvEntity::getTs).filter(Objects::nonNull).max(Long::compare);
                }
                return new ReadTsKvQueryResult(query.getId(), DaoUtil.convertDataList(data), lastTs.orElse(query.getStartTs()));
            }
        }, service);
    }

}
