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

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.stereotype.Repository;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.dao.model.sql.RpcEntity;
import org.thingsboard.server.dao.sqlts.insert.AbstractInsertRepository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class RpcInsertRepository extends AbstractInsertRepository {

    // Used only for the initial persistence of a new RPC (RpcStatus.QUEUED / already-EXPIRED on arrival).
    // ON CONFLICT keeps the create idempotent (e.g. on actor re-processing); COALESCE never clobbers an
    // existing response with NULL.
    private static final String INSERT =
            "INSERT INTO rpc (id, created_time, tenant_id, device_id, expiration_time, request, response, additional_info, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, response = COALESCE(EXCLUDED.response, rpc.response);";

    // Used for every subsequent status change. WHERE id = ? means a row deleted in the meantime
    // (TTL cleanup / manual delete) is NOT resurrected - it matches the old findById-null skip.
    // COALESCE preserves a previously stored response when a status update carries none.
    private static final String UPDATE =
            "UPDATE rpc SET status = ?, response = COALESCE(?, response) WHERE id = ?;";

    @FunctionalInterface
    private interface ColumnBinder {
        void bind(PreparedStatement ps, RpcEntity rpc) throws SQLException;
    }

    public List<Boolean> saveOrUpdate(List<RpcQueueEntry> entries) {
        Map<Boolean, List<RpcEntity>> byIntent = entries.stream().collect(Collectors.partitioningBy(
                RpcQueueEntry::insert, Collectors.mapping(RpcQueueEntry::entity, Collectors.toList())));
        List<RpcEntity> inserts = byIntent.get(true);
        List<RpcEntity> updates = byIntent.get(false);
        int[] updateCounts = transactionTemplate.execute(status -> {
            // Inserts run first so a create and a status update for the same rpcId coalesced into one
            // batch still apply in create -> update order.
            if (!inserts.isEmpty()) {
                batch(INSERT, inserts, (ps, rpc) -> {
                    ps.setObject(1, rpc.getUuid());
                    ps.setLong(2, rpc.getCreatedTime());
                    ps.setObject(3, rpc.getTenantId());
                    ps.setObject(4, rpc.getDeviceId());
                    ps.setLong(5, rpc.getExpirationTime());
                    ps.setString(6, toJsonStr(rpc.getRequest()));
                    ps.setString(7, toJsonStr(rpc.getResponse()));
                    ps.setString(8, toJsonStr(rpc.getAdditionalInfo()));
                    ps.setString(9, rpc.getStatus().name());
                });
            }
            if (!updates.isEmpty()) {
                return batch(UPDATE, updates, (ps, rpc) -> {
                    ps.setString(1, rpc.getStatus().name());
                    ps.setString(2, toJsonStr(rpc.getResponse()));
                    ps.setObject(3, rpc.getUuid());
                });
            }
            return new int[0];
        });

        // Result is aligned to the submission order of entries: an insert always persists
        // (INSERT ... ON CONFLICT), an update persists only if its WHERE id = ? matched a still-existing
        // row. updateCounts keeps the same relative order as the filtered updates, so a single cursor
        // walks it as we encounter update entries; insert entries short-circuit and don't advance it.
        List<Boolean> persisted = new ArrayList<>(entries.size());
        int updateIdx = 0;
        for (RpcQueueEntry entry : entries) {
            persisted.add(entry.insert() || updateCounts[updateIdx++] > 0);
        }
        return persisted;
    }

    private int[] batch(String sql, List<RpcEntity> entities, ColumnBinder binder) {
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

    private String toJsonStr(JsonNode node) {
        return node == null ? null : replaceNullChars(JacksonUtil.toString(node));
    }
}
