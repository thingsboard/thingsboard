// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.rule;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.edqs.fields.RuleChainFields;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.RuleChainEntity;

import java.util.List;
import java.util.UUID;

public interface RuleChainRepository extends JpaRepository<RuleChainEntity, UUID>, ExportableEntityRepository<RuleChainEntity> {

    @Query("SELECT rc FROM RuleChainEntity rc WHERE rc.tenantId = :tenantId " +
            "AND (:searchText IS NULL OR ilike(rc.name, CONCAT('%', :searchText, '%')) = true)")
    Page<RuleChainEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                         @Param("searchText") String searchText,
                                         Pageable pageable);

    @Query("SELECT rc FROM RuleChainEntity rc WHERE rc.tenantId = :tenantId " +
            "AND rc.type = :type " +
            "AND (:searchText IS NULL OR ilike(rc.name, CONCAT('%', :searchText, '%')) = true)")
    Page<RuleChainEntity> findByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                                @Param("type") RuleChainType type,
                                                @Param("searchText") String searchText,
                                                Pageable pageable);

    @Query("SELECT rc FROM RuleChainEntity rc, RelationEntity re WHERE rc.tenantId = :tenantId " +
            "AND rc.id = re.toId AND re.toType = 'RULE_CHAIN' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = 'Contains' AND re.fromId = :edgeId AND re.fromType = 'EDGE' " +
            "AND (:searchText IS NULL OR ilike(rc.name, CONCAT('%', :searchText, '%')) = true)")
    Page<RuleChainEntity> findByTenantIdAndEdgeId(@Param("tenantId") UUID tenantId,
                                                  @Param("edgeId") UUID edgeId,
                                                  @Param("searchText") String searchText,
                                                  Pageable pageable);

    @Query("SELECT rc FROM RuleChainEntity rc, RelationEntity re WHERE rc.tenantId = :tenantId " +
            "AND rc.id = re.toId AND re.toType = 'RULE_CHAIN' AND re.relationTypeGroup = 'EDGE_AUTO_ASSIGN_RULE_CHAIN' " +
            "AND re.relationType = 'Contains' AND re.fromId = :tenantId AND re.fromType = 'TENANT' " +
            "AND (:searchText IS NULL OR ilike(rc.name, CONCAT('%', :searchText, '%')) = true)")
    Page<RuleChainEntity> findAutoAssignByTenantId(@Param("tenantId") UUID tenantId,
                                                   @Param("searchText") String searchText,
                                                   Pageable pageable);


    RuleChainEntity findByTenantIdAndTypeAndRootIsTrue(UUID tenantId, RuleChainType ruleChainType);

    Long countByTenantId(UUID tenantId);

    List<RuleChainEntity> findByTenantIdAndTypeAndName(UUID tenantId, RuleChainType type, String name);

    @Query("SELECT externalId FROM RuleChainEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(rc.id, 'RULE_CHAIN', rc.name) " +
            "FROM RuleChainEntity rc WHERE rc.tenantId = :tenantId AND EXISTS " +
            "(SELECT 1 FROM RuleNodeEntity rn WHERE rn.ruleChainId = rc.id AND cast(rn.configuration as string) LIKE CONCAT('%', :resourceId, '%'))")
    List<EntityInfo> findRuleChainsByTenantIdAndResource(@Param("tenantId") UUID tenantId,
                                                         @Param("resourceId") String resourceId,
                                                         PageRequest of);

    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(rc.id, 'RULE_CHAIN', rc.name) " +
            "FROM RuleChainEntity rc WHERE EXISTS " +
            "(SELECT 1 FROM RuleNodeEntity rn WHERE rn.ruleChainId = rc.id AND cast(rn.configuration as string) LIKE CONCAT('%', :resourceId, '%'))")
    List<EntityInfo> findRuleChainsByResource(@Param("resourceId") String resourceId,
                                              Pageable pageable);

    @Query("SELECT new org.thingsboard.server.common.data.edqs.fields.RuleChainFields(r.id, r.createdTime, r.tenantId," +
            "r.name, r.version, r.additionalInfo) FROM RuleChainEntity r WHERE r.id > :id ORDER BY r.id")
    List<RuleChainFields> findNextBatch(@Param("id") UUID id, Limit limit);
}
