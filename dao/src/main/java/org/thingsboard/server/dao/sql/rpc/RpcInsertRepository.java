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
import java.util.List;

@Repository
public class RpcInsertRepository extends AbstractInsertRepository {

    private static final String INSERT_OR_UPDATE =
            "INSERT INTO rpc (id, created_time, tenant_id, device_id, expiration_time, request, response, additional_info, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, response = EXCLUDED.response;";

    public void saveOrUpdate(List<RpcEntity> entities) {
        transactionTemplate.execute(status -> {
            jdbcTemplate.batchUpdate(INSERT_OR_UPDATE, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    RpcEntity rpc = entities.get(i);
                    ps.setObject(1, rpc.getUuid());
                    ps.setLong(2, rpc.getCreatedTime());
                    ps.setObject(3, rpc.getTenantId());
                    ps.setObject(4, rpc.getDeviceId());
                    ps.setLong(5, rpc.getExpirationTime());
                    ps.setString(6, toJsonStr(rpc.getRequest()));
                    ps.setString(7, toJsonStr(rpc.getResponse()));
                    ps.setString(8, toJsonStr(rpc.getAdditionalInfo()));
                    ps.setString(9, rpc.getStatus().name());
                }

                @Override
                public int getBatchSize() {
                    return entities.size();
                }
            });
            return null;
        });
    }

    private String toJsonStr(JsonNode node) {
        return node == null ? null : replaceNullChars(JacksonUtil.toString(node));
    }
}
