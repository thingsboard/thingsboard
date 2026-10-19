// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sql.user;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.edqs.fields.UserFields;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.UserEntity;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID>, ExportableEntityRepository<UserEntity> {

    UserEntity findByEmail(String email);

    UserEntity findByTenantIdAndEmail(UUID tenantId, String email);

    @Query("SELECT u FROM UserEntity u WHERE u.tenantId = :tenantId " +
            "AND u.customerId = :customerId AND u.authority = :authority " +
            "AND (:searchText IS NULL OR ilike(u.email, CONCAT('%', :searchText, '%')) = true)")
    Page<UserEntity> findUsersByAuthority(@Param("tenantId") UUID tenantId,
                                          @Param("customerId") UUID customerId,
                                          @Param("searchText") String searchText,
                                          @Param("authority") Authority authority,
                                          Pageable pageable);

    @Query("SELECT u FROM UserEntity u WHERE u.tenantId = :tenantId " +
            "AND u.customerId IN (:customerIds) " +
            "AND (:searchText IS NULL OR ilike(u.email, CONCAT('%', :searchText, '%')) = true)")
    Page<UserEntity> findTenantAndCustomerUsers(@Param("tenantId") UUID tenantId,
                                                @Param("customerIds") Collection<UUID> customerIds,
                                                @Param("searchText") String searchText,
                                                Pageable pageable);

    @Query("SELECT u FROM UserEntity u WHERE u.tenantId = :tenantId " +
            "AND u.authority = :authority " +
            "AND (:searchText IS NULL OR ilike(u.email, CONCAT('%', :searchText, '%')) = true)")
    Page<UserEntity> findAllTenantUsersByAuthority(@Param("tenantId") UUID tenantId,
                                                   @Param("searchText") String searchText,
                                                   @Param("authority") Authority authority,
                                                   Pageable pageable);

    @Query("SELECT u FROM UserEntity u, " +
            "RelationEntity re " +
            "WHERE u.id = re.toId AND re.toType = 'USER' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId = :groupId AND re.fromType = 'ENTITY_GROUP' " +
            "AND (:textSearch IS NULL OR ilike(u.email, CONCAT('%', :textSearch, '%')) = true)")
    Page<UserEntity> findByEntityGroupId(@Param("groupId") UUID groupId,
                                         @Param("textSearch") String textSearch,
                                         Pageable pageable);

    @Query("SELECT u FROM UserEntity u, " +
            "RelationEntity re " +
            "WHERE u.id = re.toId AND re.toType = 'USER' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId in :groupIds AND re.fromType = 'ENTITY_GROUP' " +
            "AND (:textSearch IS NULL OR ilike(u.email, CONCAT('%', :textSearch, '%')) = true)")
    Page<UserEntity> findByEntityGroupIds(@Param("groupIds") List<UUID> groupIds,
                                          @Param("textSearch") String textSearch,
                                          Pageable pageable);

    List<UserEntity> findUsersByTenantIdAndIdIn(UUID tenantId, List<UUID> userIds);

    @Query("SELECT u FROM UserEntity u WHERE u.tenantId = :tenantId " +
            "AND (:searchText IS NULL OR ilike(u.email, CONCAT('%', :searchText, '%')) = true)")
    Page<UserEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                    @Param("searchText") String searchText,
                                    Pageable pageable);

    Page<UserEntity> findAllByAuthority(Authority authority, Pageable pageable);

    boolean existsByAuthority(Authority authority);

    Page<UserEntity> findByAuthorityAndTenantIdIn(Authority authority, Collection<UUID> tenantsIds, Pageable pageable);

    @Query("SELECT u FROM UserEntity u INNER JOIN TenantEntity t ON u.tenantId = t.id AND u.authority = :authority " +
            "INNER JOIN TenantProfileEntity p ON t.tenantProfileId = p.id " +
            "WHERE p.id IN :profiles")
    Page<UserEntity> findByAuthorityAndTenantProfilesIds(@Param("authority") Authority authority,
                                                         @Param("profiles") Collection<UUID> tenantProfilesIds,
                                                         Pageable pageable);

    Long countByTenantId(UUID tenantId);

    @Query("SELECT u FROM UserEntity u WHERE u.id IN " +
            "(SELECT r.toId FROM RelationEntity r WHERE r.fromType = 'ENTITY_GROUP' AND r.toType = 'USER' AND r.fromId IN " +
            "(SELECT p.userGroupId FROM GroupPermissionEntity p WHERE (p.tenantId = :tenantId OR :tenantId = org.thingsboard.server.common.data.id.EntityId.NULL_UUID) " +
            "AND p.roleId IN :rolesIds))")
    Page<UserEntity> findByTenantIdAndRolesIds(@Param("tenantId") UUID tenantId,
                                               @Param("rolesIds") List<UUID> rolesIds,
                                               Pageable pageable);

    @Query("SELECT u FROM UserEntity u WHERE u.tenantId IN :tenantsIds AND u.id IN " +
            "(SELECT r.toId FROM RelationEntity r WHERE r.fromType = 'ENTITY_GROUP' AND r.toType = 'USER' AND r.fromId IN " +
            "(SELECT p.userGroupId FROM GroupPermissionEntity p WHERE p.tenantId IN :tenantsIds AND p.roleId = :roleId))")
    Page<UserEntity> findByTenantsIdsAndRoleId(@Param("tenantsIds") List<UUID> tenantsIds,
                                               @Param("roleId") UUID roleId,
                                               Pageable pageable);

    @Query("SELECT count(u) > 0 FROM UserEntity u WHERE u.id = :id AND u.tenantId IN :tenantsIds AND u.id IN " +
            "(SELECT r.toId FROM RelationEntity r WHERE r.fromType = 'ENTITY_GROUP' AND r.toType = 'USER' AND r.fromId IN " +
            "(SELECT p.userGroupId FROM GroupPermissionEntity p WHERE p.tenantId IN :tenantsIds AND p.roleId = :roleId))")
    boolean existsByIdAndTenantsIdsAndRoleId(UUID id,
                                             List<UUID> tenantsIds,
                                             UUID roleId);

    @Query("SELECT u FROM UserEntity u INNER JOIN TenantEntity t ON u.tenantId = t.id " +
            "WHERE t.tenantProfileId IN :tenantProfilesIds AND u.id IN " +
            "(SELECT r.toId FROM RelationEntity r WHERE r.fromType = 'ENTITY_GROUP' AND r.toType = 'USER' AND r.fromId IN " +
            "(SELECT p.userGroupId FROM GroupPermissionEntity p INNER JOIN TenantEntity te ON p.tenantId = te.id " +
            "WHERE te.tenantProfileId IN :tenantProfilesIds AND p.roleId = :roleId))")
    Page<UserEntity> findByTenantProfilesIdsAndRoleId(@Param("tenantProfilesIds") List<UUID> tenantProfilesIds,
                                                      @Param("roleId") UUID roleId,
                                                      Pageable pageable);

    @Query("SELECT count(u) > 0 FROM UserEntity u INNER JOIN TenantEntity t ON u.tenantId = t.id " +
            "WHERE u.id = :id AND t.tenantProfileId IN :tenantProfilesIds AND u.id IN " +
            "(SELECT r.toId FROM RelationEntity r WHERE r.fromType = 'ENTITY_GROUP' AND r.toType = 'USER' AND r.fromId IN " +
            "(SELECT p.userGroupId FROM GroupPermissionEntity p INNER JOIN TenantEntity te ON p.tenantId = te.id " +
            "WHERE te.tenantProfileId IN :tenantProfilesIds AND p.roleId = :roleId))")
    boolean existsByIdAndTenantProfilesIdsAndRoleId(UUID id,
                                                    List<UUID> tenantProfilesIds,
                                                    UUID roleId);

    @Query("SELECT u FROM UserEntity u WHERE u.id IN " +
            "(SELECT r.toId FROM RelationEntity r WHERE r.fromType = 'ENTITY_GROUP' AND r.toType = 'USER' AND r.fromId IN " +
            "(SELECT p.userGroupId FROM GroupPermissionEntity p WHERE p.roleId = :roleId))")
    Page<UserEntity> findByRoleId(@Param("roleId") UUID roleId,
                                  Pageable pageable);

    @Query("SELECT count(u) > 0 FROM UserEntity u WHERE u.id = :id AND u.id IN " +
            "(SELECT r.toId FROM RelationEntity r WHERE r.fromType = 'ENTITY_GROUP' AND r.toType = 'USER' AND r.fromId IN " +
            "(SELECT p.userGroupId FROM GroupPermissionEntity p WHERE p.roleId = :roleId))")
    boolean existsByIdAndRoleId(UUID id,
                                UUID roleId);

    @Query(value = "SELECT count(u.id) FROM tb_user u WHERE u.id IN " +
            "(SELECT r.to_id FROM relation r WHERE r.from_type = 'ENTITY_GROUP' AND r.to_type = 'USER' AND r.from_id IN " +
            "(SELECT p.user_group_id FROM group_permission p WHERE p.tenant_id = :tenantId AND p.role_id = :roleId)) AND u.id NOT IN :userIds", nativeQuery = true)
    int countUsersByTenantIdAndRoleIdAndIdNotIn(@Param("tenantId") UUID tenantId,
                                                @Param("roleId") UUID roleId,
                                                @Param("userIds") List<UUID> userIds);

    @Query("SELECT u FROM UserEntity u WHERE u.customMenuId = :customMenuId")
    List<UserEntity> findByCustomMenuId(@Param("customMenuId") UUID customMenuId);

    @Modifying
    @Transactional
    @Query("UPDATE UserEntity u SET u.customMenuId = :customMenuId WHERE u.id IN :ids")
    void updateCustomMenuId(@Param("ids") List<UUID> ids, @Param("customMenuId") UUID customMenuId);

    @Modifying
    @Transactional
    @Query("UPDATE UserEntity u SET u.customMenuId = NULL WHERE u.id IN :ids")
    void updateCustomMenuIdToNull(@Param("ids") List<UUID> ids);

    @Query("SELECT new org.thingsboard.server.common.data.edqs.fields.UserFields(u.id, u.createdTime, u.tenantId," +
            "u.customerId, u.version, u.firstName, u.lastName, u.email, u.phone, u.additionalInfo) " +
            "FROM UserEntity u WHERE u.id > :id ORDER BY u.id")
    List<UserFields> findNextBatch(@Param("id") UUID id, Limit limit);

    @Query(value = "SELECT EXISTS ("
            + "  SELECT 1 "
            + "  FROM tb_user u "
            + "  INNER JOIN relation re ON u.id = re.to_id "
            + "  WHERE u.id = :userId "
            + "    AND re.to_type = 'USER' "
            + "    AND re.relation_type_group = 'FROM_ENTITY_GROUP' "
            + "    AND re.relation_type = 'Contains' "
            + "    AND re.from_id = :groupId "
            + "    AND re.from_type = 'ENTITY_GROUP' "
            + ")", nativeQuery = true)
    boolean existsInEntityGroup(@Param("userId") UUID userId, @Param("groupId") UUID groupId);

    @Query("SELECT new org.thingsboard.server.common.data.util.TbPair(u, uc.enabled) " +
            "FROM UserEntity u JOIN UserCredentialsEntity uc ON u.id = uc.userId WHERE u.id = :userId ")
    TbPair<UserEntity, Boolean> findUserAuthDetailsByUserId(@Param("userId") UUID userId);

    @Query("SELECT u.externalId FROM UserEntity u WHERE u.id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

}
