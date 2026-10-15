// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sql.user;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.UserAuthDetails;
import org.thingsboard.server.common.data.edqs.fields.UserFields;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.UserEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.user.UserDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

/**
 * @author Valerii Sosliuk
 */
@Component
@Slf4j
@SqlDao
public class JpaUserDao extends JpaAbstractDao<UserEntity, User> implements UserDao {

    @Autowired
    private UserRepository userRepository;

    @Override
    protected Class<UserEntity> getEntityClass() {
        return UserEntity.class;
    }

    @Override
    protected JpaRepository<UserEntity, UUID> getRepository() {
        return userRepository;
    }

    @Override
    public User findByEmail(TenantId tenantId, String email) {
        return DaoUtil.getData(userRepository.findByEmail(email));
    }

    @Override
    public User findByTenantIdAndEmail(TenantId tenantId, String email) {
        return DaoUtil.getData(userRepository.findByTenantIdAndEmail(tenantId.getId(), email));
    }

    @Override
    public PageData<User> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                userRepository
                        .findByTenantId(
                                tenantId,
                                Objects.toString(pageLink.getTextSearch(), ""),
                                DaoUtil.toPageable(pageLink, UserEntity.userColumnMap)));
    }

    @Override
    public PageData<User> findTenantAdmins(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                userRepository
                        .findUsersByAuthority(
                                tenantId,
                                NULL_UUID,
                                pageLink.getTextSearch(),
                                Authority.TENANT_ADMIN,
                                DaoUtil.toPageable(pageLink, UserEntity.userColumnMap)));
    }


    @Override
    public PageData<User> findCustomerUsers(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(
                userRepository
                        .findUsersByAuthority(
                                tenantId,
                                customerId,
                                pageLink.getTextSearch(),
                                Authority.CUSTOMER_USER,
                                DaoUtil.toPageable(pageLink, UserEntity.userColumnMap)));

    }

    @Override
    public PageData<User> findAllCustomerUsers(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                userRepository
                        .findAllTenantUsersByAuthority(
                                tenantId,
                                pageLink.getTextSearch(),
                                Authority.CUSTOMER_USER,
                                DaoUtil.toPageable(pageLink, UserEntity.userColumnMap)));
    }

    @Override
    public ListenableFuture<List<User>> findUsersByTenantIdAndIdsAsync(UUID tenantId, List<UUID> userIds) {
        return service.submit(() -> DaoUtil.convertDataList(userRepository.findUsersByTenantIdAndIdIn(tenantId, userIds)));
    }

    @Override
    public List<User> findUsersByTenantIdAndIds(UUID tenantId, List<UUID> userIds) {
        return DaoUtil.convertDataList(userRepository.findUsersByTenantIdAndIdIn(tenantId, userIds));
    }

    @Override
    public PageData<User> findUsersByEntityGroupId(UUID groupId, PageLink pageLink) {
        return DaoUtil.toPageData(userRepository
                .findByEntityGroupId(
                        groupId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, UserEntity.userColumnMap)));
    }

    @Override
    public PageData<User> findUsersByEntityGroupIds(List<UUID> groupIds, PageLink pageLink) {
        return DaoUtil.toPageData(userRepository
                .findByEntityGroupIds(
                        groupIds,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, UserEntity.userColumnMap)));
    }

    @Override
    public PageData<User> findUsersByTenantIdAndRolesIds(TenantId tenantId, List<RoleId> rolesIds, PageLink pageLink) {
        return DaoUtil.toPageData(userRepository.findByTenantIdAndRolesIds(tenantId.getId(), DaoUtil.toUUIDs(rolesIds),
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<User> findUsersByTenantsIdsAndRoleId(List<TenantId> tenantsIds, RoleId roleId, PageLink pageLink) {
        return DaoUtil.toPageData(userRepository.findByTenantsIdsAndRoleId(DaoUtil.toUUIDs(tenantsIds), roleId.getId(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public boolean existsByTenantsIdsAndRoleIdAndUserId(List<TenantId> tenantsIds, RoleId roleId, UserId userId) {
        return userRepository.existsByIdAndTenantsIdsAndRoleId(userId.getId(), DaoUtil.toUUIDs(tenantsIds), roleId.getId());
    }

    @Override
    public PageData<User> findUsersByTenantProfilesIdsAndRoleId(List<TenantProfileId> tenantProfilesIds, RoleId roleId, PageLink pageLink) {
        return DaoUtil.toPageData(userRepository.findByTenantProfilesIdsAndRoleId(DaoUtil.toUUIDs(tenantProfilesIds), roleId.getId(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public boolean existsByTenantProfilesIdsAndRoleIdAndUserId(List<TenantProfileId> tenantProfilesIds, RoleId roleId, UserId userId) {
        return userRepository.existsByIdAndTenantProfilesIdsAndRoleId(userId.getId(), DaoUtil.toUUIDs(tenantProfilesIds), roleId.getId());
    }

    @Override
    public PageData<User> findAllUsersByRoleId(RoleId roleId, PageLink pageLink) {
        return DaoUtil.toPageData(userRepository.findByRoleId(roleId.getId(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public boolean existsByRoleIdAndUserId(RoleId roleId, UserId userId) {
        return userRepository.existsByIdAndRoleId(userId.getId(), roleId.getId());
    }

    @Override
    public int countUsersByTenantIdAndRoleIdAndIdNotIn(TenantId tenantId, RoleId roleId, List<UserId> userIds) {
        return userRepository.countUsersByTenantIdAndRoleIdAndIdNotIn(tenantId.getId(), roleId.getId(), DaoUtil.toUUIDs(userIds));
    }

    @Override
    public PageData<User> findUsersByCustomerIds(UUID tenantId, List<CustomerId> customerIds, PageLink pageLink) {
        return DaoUtil.toPageData(
                userRepository
                        .findTenantAndCustomerUsers(
                                tenantId,
                                DaoUtil.toUUIDs(customerIds),
                                pageLink.getTextSearch(),
                                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<User> findAll(PageLink pageLink) {
        return DaoUtil.toPageData(userRepository.findAll(DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<User> findAllByAuthority(Authority authority, PageLink pageLink) {
        return DaoUtil.toPageData(userRepository.findAllByAuthority(authority, DaoUtil.toPageable(pageLink)));
    }

    @Override
    public boolean existsByAuthority(Authority authority) {
        return userRepository.existsByAuthority(authority);
    }

    @Override
    public PageData<User> findByAuthorityAndTenantsIds(Authority authority, List<TenantId> tenantsIds, PageLink pageLink) {
        return DaoUtil.toPageData(userRepository.findByAuthorityAndTenantIdIn(authority, DaoUtil.toUUIDs(tenantsIds), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<User> findByAuthorityAndTenantProfilesIds(Authority authority, List<TenantProfileId> tenantProfilesIds, PageLink pageLink) {
        return DaoUtil.toPageData(userRepository.findByAuthorityAndTenantProfilesIds(authority, DaoUtil.toUUIDs(tenantProfilesIds),
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public List<User> findUsersByCustomMenuId(CustomMenuId customMenuId) {
        return DaoUtil.convertDataList(userRepository.findByCustomMenuId(customMenuId.getId()));
    }

    @Override
    public void updateUsersCustomMenuId(List<UserId> userIds, CustomMenuId customMenuId) {
        if (customMenuId == null) {
            userRepository.updateCustomMenuIdToNull(toUUIDs(userIds));
        } else {
            userRepository.updateCustomMenuId(toUUIDs(userIds), customMenuId.getId());
        }
    }

    @Override
    public boolean existsInEntityGroup(UserId id, EntityGroupId entityGroupId) {
        return userRepository.existsInEntityGroup(id.getId(), entityGroupId.getId());
    }

    @Override
    public UserAuthDetails findUserAuthDetailsByUserId(UUID tenantId, UUID userId) {
        TbPair<UserEntity, Boolean> result = userRepository.findUserAuthDetailsByUserId(userId);
        return result != null ? new UserAuthDetails(result.getFirst().toData(), result.getSecond()) : null;
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return userRepository.countByTenantId(tenantId.getId());
    }

    @Override
    public PageData<User> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return findByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public List<UserFields> findNextBatch(UUID id, int batchSize) {
        return userRepository.findNextBatch(id, Limit.of(batchSize));
    }

    @Override
    public User findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(userRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public User findByTenantIdAndName(UUID tenantId, String name) {
        // User.getName() returns email, so name-based lookups for users are email lookups.
        return findByTenantIdAndEmail(TenantId.fromUUID(tenantId), name);
    }

    @Override
    public UserId getExternalIdByInternal(UserId internalId) {
        return Optional.ofNullable(userRepository.getExternalIdById(internalId.getId()))
                .map(UserId::new).orElse(null);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.USER;
    }

}
