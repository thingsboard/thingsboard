// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.edge;

import com.google.common.hash.Hashing;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.EdgeProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.edge.EdgeProfileDao;
import org.thingsboard.server.dao.model.sql.EdgeProfileEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

@Component
@SqlDao
@RequiredArgsConstructor
public class JpaEdgeProfileDao extends JpaAbstractDao<EdgeProfileEntity, EdgeProfile> implements EdgeProfileDao {

    private final EdgeProfileRepository repository;

    @Override
    protected Class<EdgeProfileEntity> getEntityClass() {
        return EdgeProfileEntity.class;
    }

    @Override
    protected JpaRepository<EdgeProfileEntity, UUID> getRepository() {
        return repository;
    }

    @Override
    public PageData<EdgeProfile> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(repository.findByTenantId(tenantId, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public EdgeProfile findByTenantIdAndName(UUID tenantId, String name) {
        return DaoUtil.getData(repository.findByTenantIdAndName(tenantId, name));
    }

    @Override
    public void lockNameForCreate(UUID tenantId, String name) {
        long key = Hashing.murmur3_128().hashUnencodedChars(tenantId.toString() + name).asLong();
        jdbcTemplate.queryForList("SELECT pg_advisory_xact_lock(?)", key);
    }

    @Override
    public EdgeProfile findDefaultEdgeProfile(UUID tenantId) {
        return DaoUtil.getData(repository.findByTenantIdAndDefaultTrue(tenantId));
    }

    @Override
    public List<EdgeProfile> findByTenantIdAndReferencedEntityId(UUID tenantId, UUID entityId) {
        return DaoUtil.convertDataList(repository.findByTenantIdAndReferencedEntityId(tenantId, entityId.toString()));
    }

    @Override
    public boolean existsByTenantIdAndRootRuleChainId(UUID tenantId, UUID ruleChainId) {
        return repository.existsByTenantIdAndRootRuleChainId(tenantId, ruleChainId);
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return repository.countByTenantId(tenantId.getId());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.EDGE_PROFILE;
    }

}
