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
package org.thingsboard.server.dao.sql.edge;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.EdgeEventId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.edge.EdgeEventDao;
import org.thingsboard.server.dao.model.sql.EdgeEventEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;

import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@Slf4j
@Component
public class JpaBaseEdgeEventDao extends JpaAbstractSearchTextDao<EdgeEventEntity, EdgeEvent> implements EdgeEventDao {

    private final UUID systemTenantId = NULL_UUID;

    @Autowired
    private EdgeEventRepository edgeEventRepository;

    @Override
    protected Class<EdgeEventEntity> getEntityClass() {
        return EdgeEventEntity.class;
    }

    @Override
    protected CrudRepository<EdgeEventEntity, UUID> getCrudRepository() {
        return edgeEventRepository;
    }

    @Override
    public ListenableFuture<EdgeEvent> saveAsync(EdgeEvent edgeEvent) {
        log.debug("Save edge event [{}] ", edgeEvent);
        if (edgeEvent.getId() == null) {
            edgeEvent.setId(new EdgeEventId(Uuids.timeBased()));
        }
        return service.submit(() -> save(new EdgeEventEntity(edgeEvent)).orElse(null));
    }

    @Override
    public PageData<EdgeEvent> findEdgeEvents(UUID tenantId, EdgeId edgeId, TimePageLink pageLink, boolean withTsUpdate) {
        return DaoUtil.toPageData(
                edgeEventRepository
                        .findEdgeEventsByTenantIdAndEdgeId(
                                tenantId,
                                edgeId.getId(),
                                pageLink.getStartTime(),
                                pageLink.getEndTime(),
                                DaoUtil.toPageable(pageLink)));
        // TODO: voba
//        Specification<EdgeEventEntity> timeSearchSpec = JpaAbstractSearchTimeDao.getTimeSearchPageSpec(pageLink, "id");
//        Specification<EdgeEventEntity> fieldsSpec = getEntityFieldsSpec(tenantId, edgeId, withTsUpdate);
//        Sort.Direction sortDirection = pageLink.isAscOrder() ? Sort.Direction.ASC : Sort.Direction.DESC;
//        Pageable pageable = PageRequest.of(0, pageLink.getLimit(), sortDirection, ID_PROPERTY);
//        return DaoUtil.convertDataList(edgeEventRepository.findAll(Specification.where(timeSearchSpec).and(fieldsSpec), pageable).getContent());
    }

    public Optional<EdgeEvent> save(EdgeEventEntity entity) {
        log.debug("Save edge event [{}] ", entity);
        if (entity.getTenantId() == null) {
            log.trace("Save system edge event with predefined id {}", systemTenantId);
            entity.setTenantId(systemTenantId);
        }
        if (entity.getUuid() == null) {
            entity.setUuid(Uuids.timeBased());
        }
        return Optional.of(DaoUtil.getData(edgeEventRepository.save(entity)));
    }

    // TODO: voba
//    private Specification<EdgeEventEntity> getEntityFieldsSpec(UUID tenantId, EdgeId edgeId, boolean withTsUpdate) {
//        return (root, criteriaQuery, criteriaBuilder) -> {
//            List<Predicate> predicates = new ArrayList<>();
//            if (tenantId != null) {
//                Predicate tenantIdPredicate = criteriaBuilder.equal(root.get("tenantId"), UUIDConverter.fromTimeUUID(tenantId));
//                predicates.add(tenantIdPredicate);
//            }
//            if (edgeId != null) {
//                Predicate entityIdPredicate = criteriaBuilder.equal(root.get("edgeId"), UUIDConverter.fromTimeUUID(edgeId.getId()));
//                predicates.add(entityIdPredicate);
//            }
//            if (!withTsUpdate) {
//                Predicate edgeEventActionPredicate = criteriaBuilder.notEqual(root.get("edgeEventAction"), ActionType.TIMESERIES_UPDATED.name());
//                predicates.add(edgeEventActionPredicate);
//            }
//            return criteriaBuilder.and(predicates.toArray(new Predicate[]{}));
//        };
//    }
}
