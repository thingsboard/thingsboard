// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.role;

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.fields.RoleFields;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.RoleEntity;
import org.thingsboard.server.dao.role.RoleDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Component
@SqlDao
public class JpaRoleDao extends JpaAbstractDao<RoleEntity, Role> implements RoleDao {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    protected Class<RoleEntity> getEntityClass() {
        return RoleEntity.class;
    }

    @Override
    protected JpaRepository<RoleEntity, UUID> getRepository() {
        return roleRepository;
    }

    @Override
    public PageData<Role> findRolesByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                roleRepository.findByTenantIdAndCustomerId(
                        tenantId,
                        EntityId.NULL_UUID,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Role> findRolesByTenantIdAndType(UUID tenantId, RoleType type, PageLink pageLink) {
        return DaoUtil.toPageData(
                roleRepository.findByTenantIdAndCustomerIdAndType(
                        tenantId,
                        EntityId.NULL_UUID,
                        type,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public Optional<Role> findRoleByTenantIdAndName(UUID tenantId, String name) {
        return Optional.ofNullable(
                DaoUtil.getData(roleRepository.findByTenantIdAndCustomerIdAndName(tenantId, EntityId.NULL_UUID, name)));
    }

    @Override
    public Optional<Role> findRoleByByTenantIdAndCustomerIdAndName(UUID tenantId, UUID customerId, String name) {
        return Optional.ofNullable(
                DaoUtil.getData(roleRepository.findByTenantIdAndCustomerIdAndName(tenantId, customerId, name)));
    }

    @Override
    public PageData<Role> findRolesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(
                roleRepository.findByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Role> findRolesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, RoleType type, PageLink pageLink) {
        return DaoUtil.toPageData(
                roleRepository.findByTenantIdAndCustomerIdAndType(
                        tenantId,
                        customerId,
                        type,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public ListenableFuture<List<Role>> findRolesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> roleIds) {
        return service.submit(() -> DaoUtil.convertDataList(roleRepository.findRolesByTenantIdAndIdIn(tenantId, roleIds)));
    }

    @Override
    public Role findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(roleRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public PageData<Role> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(roleRepository.findByTenantId(tenantId, DaoUtil.toPageable(pageLink)));
    }

    @Override
    public RoleId getExternalIdByInternal(RoleId internalId) {
        return Optional.ofNullable(roleRepository.getExternalIdById(internalId.getId()))
                .map(RoleId::new).orElse(null);
    }

    @Override
    public PageData<Role> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return findByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public List<RoleFields> findNextBatch(UUID id, int batchSize) {
        return roleRepository.findNextBatch(id, Limit.of(batchSize));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ROLE;
    }

}
