/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.dao.edge;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.model.nosql.EdgeEventEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractSearchTimeDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.EDGE_EVENT_COLUMN_FAMILY_NAME;

@Component
@Slf4j
@NoSqlDao
public class CassandraEdgeEventDao extends CassandraAbstractSearchTimeDao<EdgeEventEntity, EdgeEvent> implements EdgeEventDao {


    @Override
    protected Class<EdgeEventEntity> getColumnFamilyClass() {
        return EdgeEventEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return EDGE_EVENT_COLUMN_FAMILY_NAME;
    }


    @Override
    public ListenableFuture<EdgeEvent> saveAsync(EdgeEvent edgeEvent) {
        return null;
    }

    @Override
    public List<EdgeEvent> findEdgeEvents(UUID tenantId, EdgeId edgeId, TimePageLink pageLink) {
        return null;
    }
}
