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
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.dao.model.sql.RpcEntity;
import org.thingsboard.server.dao.sqlts.insert.AbstractInsertRepository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Repository
public class RpcUpdateRepository extends AbstractInsertRepository {

    private static final String UPDATE =
            "UPDATE rpc SET status = ?, response = COALESCE(?, response) " +
            "WHERE id = ? AND status = ANY(?);";

    // Allowed-from arrays are a pure function of (target status, oneway), so precompute them once instead of
    // rebuilding an EnumSet + String[] for every row in every batch on the RPC status-write hot path.
    private static final Map<RpcStatus, String[]> ALLOWED_FROM_TWO_WAY = precomputeAllowedFrom(false);
    private static final Map<RpcStatus, String[]> ALLOWED_FROM_ONE_WAY = precomputeAllowedFrom(true);

    private static Map<RpcStatus, String[]> precomputeAllowedFrom(boolean oneway) {
        Map<RpcStatus, String[]> byStatus = new EnumMap<>(RpcStatus.class);
        for (RpcStatus status : RpcStatus.values()) {
            byStatus.put(status, status.getAllowedFromStatuses(oneway).stream().map(Enum::name).toArray(String[]::new));
        }
        return byStatus;
    }

    List<Boolean> update(List<RpcEntity> updates) {
        return transactionTemplate.execute(status -> {
            int[] updateCounts = jdbcTemplate.batchUpdate(UPDATE, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    RpcEntity rpc = updates.get(i);
                    ps.setString(1, rpc.getStatus().name());
                    ps.setString(2, toJsonStr(rpc.getResponse()));
                    ps.setObject(3, rpc.getUuid());
                    ps.setArray(4, ps.getConnection().createArrayOf("varchar", allowedFromArray(rpc)));
                }

                @Override
                public int getBatchSize() {
                    return updates.size();
                }
            });

            List<Boolean> persisted = new ArrayList<>(updateCounts.length);
            for (int updateCount : updateCounts) {
                persisted.add(updateCount > 0);
            }
            return persisted;
        });
    }

    private static String[] allowedFromArray(RpcEntity rpc) {
        Map<RpcStatus, String[]> byStatus = Boolean.TRUE.equals(rpc.getOneway()) ? ALLOWED_FROM_ONE_WAY : ALLOWED_FROM_TWO_WAY;
        return byStatus.get(rpc.getStatus());
    }

    private String toJsonStr(JsonNode node) {
        return node == null ? null : replaceNullChars(JacksonUtil.toString(node));
    }
}
