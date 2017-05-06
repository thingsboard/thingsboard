package org.thingsboard.server.dao.sql.event;

import com.datastax.driver.core.utils.UUIDs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.event.EventDao;
import org.thingsboard.server.dao.model.sql.EventEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTimeDao;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.data.jpa.domain.Specifications.where;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

/**
 * Created by Valerii Sosliuk on 5/3/2017.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "sql", value = "enabled", havingValue = "true", matchIfMissing = false)
public class JpaBaseEventDao extends JpaAbstractSearchTimeDao<EventEntity, Event> implements EventDao {

    private final UUID systemTenantId = NULL_UUID;

    @Autowired
    private EventRepository eventRepository;

    @Override
    protected Class<EventEntity> getEntityClass() {
        return EventEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return EVENT_COLUMN_FAMILY_NAME;
    }

    @Override
    protected CrudRepository<EventEntity, UUID> getCrudRepository() {
        return eventRepository;
    }

    @Override
    public Event save(Event event) {
        if (StringUtils.isEmpty(event.getUid())) {
            event.setUid(event.getId().toString());
        }
        return save(new EventEntity(event), false).orElse(null);
    }

    @Override
    public Optional<Event> saveIfNotExists(Event event) {
        return save(new EventEntity(event), true);
    }

    @Override
    public Event findEvent(UUID tenantId, EntityId entityId, String eventType, String eventUid) {
        return DaoUtil.getData(eventRepository.findByTenantIdAndEntityTypeAndEntityIdAndEventTypeAndEventUid(
                tenantId, entityId.getEntityType(), entityId.getId(), eventType, eventUid));
    }

    @Override
    public List<Event> findEvents(UUID tenantId, EntityId entityId, TimePageLink pageLink) {
        return findEvents(tenantId, entityId, null, pageLink);
    }
    @Override
    public List<Event> findEvents(UUID tenantId, EntityId entityId, String eventType, TimePageLink pageLink) {
        Specification<EventEntity> timeSearchSpec = getTimeSearchPageSpec(pageLink);
        Specification<EventEntity> fieldsSpec = getEntityFieldsSpec(tenantId, entityId, eventType);
        Sort.Direction sortDirection = pageLink.isAscOrder() ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = new PageRequest(0, pageLink.getLimit(), sortDirection, ID_PROPERTY);
        return DaoUtil.convertDataList(eventRepository.findAll(where(timeSearchSpec).and(fieldsSpec), pageable).getContent());
    }

    public Optional<Event> save(EventEntity entity, boolean ifNotExists) {
        log.debug("Save event [{}] ", entity);
        if (entity.getTenantId() == null) {
            log.trace("Save system event with predefined id {}", systemTenantId);
            entity.setTenantId(systemTenantId);
        }
        if (entity.getId() == null) {
            entity.setId(UUIDs.timeBased());
        }
        if (StringUtils.isEmpty(entity.getEventUid())) {
            entity.setEventUid(entity.getId().toString());
        }
        if (ifNotExists && findById(entity.getId()) != null) {
            return Optional.empty();
        }
        return Optional.of(DaoUtil.getData(eventRepository.save(entity)));
    }

    private Specification<EventEntity> getEntityFieldsSpec(UUID tenantId, EntityId entityId, String eventType) {
        return new Specification<EventEntity>() {
            @Override
            public Predicate toPredicate(Root<EventEntity> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicates = new ArrayList<Predicate>();
                if (tenantId != null) {
                    Predicate tenantIdPredicate = criteriaBuilder.equal(root.get("tenantId"), tenantId);
                    predicates.add(tenantIdPredicate);
                }
                if (entityId != null) {
                    Predicate entityTypePredicate = criteriaBuilder.equal(root.get("entityType"), entityId.getEntityType());
                    Predicate entityIdPredicate = criteriaBuilder.equal(root.get("entityId"), entityId.getId());
                    predicates.add(entityTypePredicate);
                    predicates.add(entityIdPredicate);
                }
                if (eventType != null) {
                    Predicate eventTypePredicate = criteriaBuilder.equal(root.get("eventType"), eventType);
                    predicates.add(eventTypePredicate);
                }
                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            }
        };
    }
}
