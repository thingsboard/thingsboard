/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.sql.rpc;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.rpc.RpcKind;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.dao.model.sql.RpcEntity;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The only write path for the {@code rpc} table.
 */
@Repository
@RequiredArgsConstructor
public class RpcWriteRepository {

    private static final String INSERT_IF_ABSENT =
            "INSERT INTO rpc (id, created_time, tenant_id, device_id, expiration_time, request, response, " +
            "additional_info, status, request_id, oneway) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
            "ON CONFLICT (id) DO NOTHING;";

    private static final String UPDATE =
            "UPDATE rpc SET status = ?, response = COALESCE(?, response) " +
            "WHERE id = ? AND status = ANY(?);";

    // The status names bound into the guard are a pure function of (kind, target status), so precompute the
    // arrays once instead of mapping names for every row in every batch on the RPC status-write hot path.
    private static final Map<RpcKind, Map<RpcStatus, String[]>> ALLOWED_FROM = precomputeAllowedFrom();

    private static Map<RpcKind, Map<RpcStatus, String[]>> precomputeAllowedFrom() {
        Map<RpcKind, Map<RpcStatus, String[]>> byKind = new EnumMap<>(RpcKind.class);
        for (RpcKind kind : RpcKind.values()) {
            Map<RpcStatus, String[]> byStatus = new EnumMap<>(RpcStatus.class);
            for (RpcStatus status : RpcStatus.values()) {
                byStatus.put(status, status.getAllowedFromStatuses(kind).stream().map(Enum::name).toArray(String[]::new));
            }
            byKind.put(kind, byStatus);
        }
        return byKind;
    }

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    @FunctionalInterface
    private interface ColumnBinder {
        void bind(PreparedStatement ps, RpcEntity rpc) throws SQLException;
    }

    /**
     * One transaction per batch, all inserts before all updates: an update coalesced into the same flush as its
     * create would otherwise match no row and leave a stranded QUEUED row behind.
     * <p>
     * Must not be rebuilt on {@code AbstractVersionedInsertRepository}: that base updates first and inserts any
     * row whose update matched nothing, resurrecting an RPC deleted in the meantime.
     */
    List<Boolean> write(List<RpcWrite> writes) {
        return transactionTemplate.execute(status -> {
            List<RpcEntity> inserts = new ArrayList<>();
            List<RpcEntity> updates = new ArrayList<>();
            for (RpcWrite write : writes) {
                (write.op() == RpcWrite.Op.INSERT ? inserts : updates).add(write.entity());
            }

            int[] insertCounts = batch(INSERT_IF_ABSENT, inserts, RpcWriteRepository::bindInsert);
            int[] updateCounts = batch(UPDATE, updates, RpcWriteRepository::bindUpdate);

            List<Boolean> persisted = new ArrayList<>(writes.size());
            int insertIdx = 0;
            int updateIdx = 0;
            for (RpcWrite write : writes) {
                persisted.add(write.op() == RpcWrite.Op.INSERT
                        ? insertCounts[insertIdx++] > 0
                        : updateCounts[updateIdx++] > 0);
            }
            return persisted;
        });
    }

    private int[] batch(String sql, List<RpcEntity> entities, ColumnBinder binder) {
        if (entities.isEmpty()) {
            return new int[0];
        }
        return jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                binder.bind(ps, entities.get(i));
            }

            @Override
            public int getBatchSize() {
                return entities.size();
            }
        });
    }

    private static void bindInsert(PreparedStatement ps, RpcEntity rpc) throws SQLException {
        ps.setObject(1, rpc.getUuid());
        ps.setLong(2, rpc.getCreatedTime());
        ps.setObject(3, rpc.getTenantId());
        ps.setObject(4, rpc.getDeviceId());
        ps.setLong(5, rpc.getExpirationTime());
        // The json columns take a plain String and let PostgreSQL infer jsonb.
        ps.setString(6, JacksonUtil.toString(rpc.getRequest()));
        ps.setString(7, JacksonUtil.toString(rpc.getResponse()));
        ps.setString(8, JacksonUtil.toString(rpc.getAdditionalInfo()));
        ps.setString(9, rpc.getStatus().name());
        ps.setObject(10, rpc.getRequestId());
        ps.setObject(11, rpc.getOneway());
    }

    private static void bindUpdate(PreparedStatement ps, RpcEntity rpc) throws SQLException {
        ps.setString(1, rpc.getStatus().name());
        ps.setString(2, JacksonUtil.toString(rpc.getResponse()));
        ps.setObject(3, rpc.getUuid());
        ps.setArray(4, ps.getConnection().createArrayOf("varchar", allowedFromArray(rpc)));
    }

    private static String[] allowedFromArray(RpcEntity rpc) {
        return ALLOWED_FROM.get(RpcKind.of(rpc.getOneway())).get(rpc.getStatus());
    }

}
