package org.thingsboard.server.dao.sql.entityview;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.entityview.EntityViewDao;
import org.thingsboard.server.dao.model.sql.EntityViewEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUID;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID_STR;

@Component
@SqlDao
public class JpaEntityViewDao extends JpaAbstractSearchTextDao<EntityViewEntity, EntityView>
        implements EntityViewDao {

    @Autowired
    EntityViewRepository entityViewRepository;

    @Override
    protected Class<EntityViewEntity> getEntityClass() {
        return EntityViewEntity.class;
    }

    @Override
    protected CrudRepository<EntityViewEntity, String> getCrudRepository() {
        return entityViewRepository;
    }

    @Override
    public List<EntityView> findEntityViewByTenantId(UUID tenantId, TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                entityViewRepository.findByTenantId(
                        fromTimeUUID(tenantId),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public Optional<EntityView> findEntityViewByTenantIdAndName(UUID tenantId, String name) {
        return Optional.ofNullable(
                DaoUtil.getData(entityViewRepository.findByTenantIdAndName(fromTimeUUID(tenantId), name)));
    }

    @Override
    public List<EntityView> findEntityViewByTenantIdAndEntityId(UUID tenantId,
                                                                UUID entityId,
                                                                TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                entityViewRepository.findByTenantIdAndEntityId(
                        fromTimeUUID(tenantId),
                        fromTimeUUID(entityId),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public List<EntityView> findEntityViewsByTenantIdAndCustomerId(UUID tenantId,
                                                                   UUID customerId,
                                                                   TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                entityViewRepository.findByTenantIdAndCustomerId(
                        fromTimeUUID(tenantId),
                        fromTimeUUID(customerId),
                        Objects.toString(pageLink, ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())
                ));
    }

    @Override
    public List<EntityView> findEntityViewsByTenantIdAndCustomerIdAndEntityId(UUID tenantId,
                                                                              UUID customerId,
                                                                              UUID entityId,
                                                                              TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                entityViewRepository.findByTenantIdAndCustomerIdAndEntityId(
                        fromTimeUUID(tenantId),
                        fromTimeUUID(customerId),
                        fromTimeUUID(entityId),
                        Objects.toString(pageLink, ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())
                ));
    }
}
