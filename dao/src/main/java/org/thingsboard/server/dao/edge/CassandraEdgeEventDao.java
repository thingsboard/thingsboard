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

import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.utils.UUIDs;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.EdgeEventId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.nosql.EdgeEventEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractSearchTimeDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.ttl;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_EVENT_BY_ID_VIEW_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_EVENT_COLUMN_FAMILY_NAME;

@Component
@Slf4j
@NoSqlDao
public class CassandraEdgeEventDao extends CassandraAbstractSearchTimeDao<EdgeEventEntity, EdgeEvent> implements EdgeEventDao {

    @Value("${edges.edge_events_ttl:0}")
    private int edgeEventsTtl;

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
        log.debug("Save edge event [{}] ", edgeEvent);
        if (edgeEvent.getId() == null) {
            edgeEvent.setId(new EdgeEventId(UUIDs.timeBased()));
        }
        if (StringUtils.isEmpty(edgeEvent.getUid())) {
            edgeEvent.setUid(edgeEvent.getId().toString());
        }
        ListenableFuture<Optional<EdgeEvent>> optionalSave = saveAsync(edgeEvent.getTenantId(), new EdgeEventEntity(edgeEvent), edgeEventsTtl);
        return Futures.transform(optionalSave, opt -> opt.orElse(null), MoreExecutors.directExecutor());
    }

    private ListenableFuture<Optional<EdgeEvent>> saveAsync(TenantId tenantId, EdgeEventEntity entity, int ttl) {
        if (entity.getUuid() == null) {
            entity.setUuid(UUIDs.timeBased());
        }
        Insert insert = QueryBuilder.insertInto(getColumnFamilyName())
                .value(ModelConstants.ID_PROPERTY, entity.getUuid())
                .value(ModelConstants.EDGE_EVENT_TENANT_ID_PROPERTY, entity.getTenantId())
                .value(ModelConstants.EDGE_EVENT_EDGE_ID_PROPERTY, entity.getEdgeId())
                .value(ModelConstants.EDGE_EVENT_TYPE_PROPERTY, entity.getEdgeEventType())
                .value(ModelConstants.EDGE_EVENT_UID_PROPERTY, entity.getEdgeEventUid())
                .value(ModelConstants.EDGE_EVENT_ENTITY_ID_PROPERTY, entity.getEntityId())
                .value(ModelConstants.EDGE_EVENT_ACTION_PROPERTY, entity.getEdgeEventAction())
                .value(ModelConstants.EDGE_EVENT_BODY_PROPERTY, entity.getBody());

        if (ttl > 0) {
            insert.using(ttl(ttl));
        }
        ResultSetFuture resultSetFuture = executeAsyncWrite(tenantId, insert);
        return Futures.transform(resultSetFuture, rs -> {
            if (rs.wasApplied()) {
                return Optional.of(DaoUtil.getData(entity));
            } else {
                return Optional.empty();
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    public List<EdgeEvent> findEdgeEvents(UUID tenantId, EdgeId edgeId, TimePageLink pageLink, boolean withTsUpdate) {
        log.trace("Try to find edge events by tenant [{}], edgeId [{}] and pageLink [{}]", tenantId, edgeId, pageLink);
        List<EdgeEventEntity> entities = findPageWithTimeSearch(new TenantId(tenantId), EDGE_EVENT_BY_ID_VIEW_NAME,
                Arrays.asList(eq(ModelConstants.EDGE_EVENT_TENANT_ID_PROPERTY, tenantId),
                        eq(ModelConstants.EDGE_EVENT_EDGE_ID_PROPERTY, edgeId.getId())),
                pageLink);
        log.trace("Found events by tenant [{}], edgeId [{}] and pageLink [{}]", tenantId, edgeId, pageLink);
        List<EdgeEvent> edgeEvents = DaoUtil.convertDataList(entities);
        if (!withTsUpdate) {
            return edgeEvents.stream()
                    .filter(edgeEvent -> !edgeEvent.getAction().equals(ActionType.TIMESERIES_UPDATED.name()))
                    .collect(Collectors.toList());
        } else {
            return edgeEvents;
        }
    }
}
