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
package org.thingsboard.server.dao.sql.edge;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.dao.model.sql.EdgeEventEntity;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;


@Repository
@Transactional
public class EdgeEventInsertRepository {

    private static final String INSERT =
            "INSERT INTO edge_event (id, created_time, edge_id, edge_event_type, edge_event_uid, entity_id, edge_event_action, body, tenant_id, ts) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT DO NOTHING;";

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    protected void save(List<EdgeEventEntity> entities) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                jdbcTemplate.batchUpdate(INSERT, new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        EdgeEventEntity edgeEvent = entities.get(i);
                        ps.setObject(1, edgeEvent.getId());
                        ps.setLong(2, edgeEvent.getCreatedTime());
                        ps.setObject(3, edgeEvent.getEdgeId());
                        ps.setString(4, edgeEvent.getEdgeEventType().name());
                        ps.setString(5, edgeEvent.getEdgeEventUid());
                        ps.setObject(6, edgeEvent.getEntityId());
                        ps.setString(7, edgeEvent.getEdgeEventAction().name());
                        ps.setString(8, edgeEvent.getEntityBody() != null
                                ? edgeEvent.getEntityBody().toString()
                                : null);
                        ps.setObject(9, edgeEvent.getTenantId());
                        ps.setLong(10, edgeEvent.getTs());
                    }

                    @Override
                    public int getBatchSize() {
                        return entities.size();
                    }
                });
            }
        });
    }
}
