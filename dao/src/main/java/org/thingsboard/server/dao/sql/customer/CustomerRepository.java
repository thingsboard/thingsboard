// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sql.customer;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.EntityInfo;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.edqs.fields.CustomerFields;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.CustomerEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID>, ExportableEntityRepository<CustomerEntity> {

    @Query("SELECT c FROM CustomerEntity c WHERE c.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(c.title, CONCAT('%', :textSearch, '%')) = true)")
    Page<CustomerEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                        @Param("textSearch") String textSearch,
                                        Pageable pageable);

    CustomerEntity findByTenantIdAndTitle(UUID tenantId, String title);

    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(a.id, 'CUSTOMER', a.title) " +
            "FROM CustomerEntity a WHERE a.tenantId = :tenantId AND a.title LIKE CONCAT(:prefix, '%')")
    List<EntityInfo> findEntityInfosByNamePrefix(UUID tenantId, String prefix);

    @Query("SELECT c FROM CustomerEntity c, " +
            "RelationEntity re " +
            "WHERE c.id = re.toId AND re.toType = 'CUSTOMER' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId = :groupId AND re.fromType = 'ENTITY_GROUP' " +
            "AND (:textSearch IS NULL OR ilike(c.title, CONCAT('%', :textSearch, '%')) = true)")
    Page<CustomerEntity> findByEntityGroupId(@Param("groupId") UUID groupId,
                                             @Param("textSearch") String textSearch,
                                             Pageable pageable);

    @Query("SELECT c FROM CustomerEntity c, " +
            "RelationEntity re " +
            "WHERE ((c.id = re.toId AND re.toType = 'CUSTOMER' " +
            "AND re.relationTypeGroup = 'FROM_ENTITY_GROUP' " +
            "AND re.relationType = 'Contains' " +
            "AND re.fromId in :groupIds AND re.fromType = 'ENTITY_GROUP') " +
            "OR (:additionalCustomerIds IS NOT NULL AND c.id in :additionalCustomerIds)) " +
            "AND (:textSearch IS NULL OR ilike(c.title, CONCAT('%', :textSearch, '%')) = true)")
    Page<CustomerEntity> findByEntityGroupIds(@Param("groupIds") List<UUID> groupIds,
                                              @Param("additionalCustomerIds") List<UUID> additionalCustomerIds,
                                              @Param("textSearch") String textSearch,
                                              Pageable pageable);

    List<CustomerEntity> findCustomersByTenantIdAndIdIn(UUID tenantId, List<UUID> customerIds);

    @Query("SELECT c.id FROM CustomerEntity c WHERE c.tenantId = :tenantId AND (c.parentCustomerId is null OR c.parentCustomerId = org.thingsboard.server.common.data.id.EntityId.NULL_UUID)")
    Page<UUID> findIdsByTenantIdAndNullCustomerId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT c.id FROM CustomerEntity c WHERE c.tenantId = :tenantId AND c.parentCustomerId = :customerId")
    Page<UUID> findIdsByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                              @Param("customerId") UUID customerId,
                                              Pageable pageable);

    Page<CustomerEntity> findByTenantIdAndParentCustomerId(UUID tenantId, UUID parentCustomerId, Pageable pageable);

    @Query("SELECT c FROM CustomerEntity c WHERE c.tenantId = :tenantId AND (c.parentCustomerId IS NULL " +
           "OR c.parentCustomerId = org.thingsboard.server.common.data.id.EntityId.NULL_UUID)")
    Page<CustomerEntity> findByTenantIdAndNullParentCustomerId(UUID tenantId, Pageable pageable);

    @Query(value = "SELECT * FROM customer c WHERE c.tenant_id = :tenantId AND c.is_public IS TRUE AND " +
            "(c.parent_customer_id IS NULL OR c.parent_customer_id = '13814000-1dd2-11b2-8080-808080808080') ORDER BY c.id ASC LIMIT 1", nativeQuery = true)
    CustomerEntity findPublicCustomerByTenantIdAndNullCustomerId(@Param("tenantId") UUID tenantId);

    @Query(value = "SELECT * FROM customer c WHERE c.tenant_id = :tenantId AND c.is_public IS TRUE AND " +
            "c.parent_customer_id = :customerId ORDER BY c.id ASC LIMIT 1", nativeQuery = true)
    CustomerEntity findPublicCustomerByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                             @Param("customerId") UUID customerId);

    Long countByTenantId(UUID tenantId);

    @Query("SELECT externalId FROM CustomerEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

    @Query(value = "SELECT c.* FROM customer c " +
            "INNER JOIN (SELECT tenant_id, title FROM customer GROUP BY tenant_id, title HAVING COUNT(title) > 1) dc " +
            "ON c.tenant_id = dc.tenant_id AND c.title = dc.title " +
            "ORDER BY c.tenant_id, c.title, c.id",
            nativeQuery = true)
    Page<CustomerEntity> findCustomersWithTheSameTitle(Pageable pageable);

    @Query("SELECT c FROM CustomerEntity c WHERE c.customMenuId = :customMenuId")
    List<CustomerEntity> findByCustomMenuId(@Param("customMenuId") UUID customMenuId);

    @Modifying
    @Transactional
    @Query("UPDATE CustomerEntity c SET c.customMenuId = :customMenuId WHERE c.id IN :ids")
    void updateCustomMenuId(@Param("ids") List<UUID> ids, @Param("customMenuId") UUID customMenuId);

    @Modifying
    @Transactional
    @Query("UPDATE CustomerEntity u SET u.customMenuId = NULL WHERE u.id IN :ids")
    void updateCustomMenuIdToNull(@Param("ids") List<UUID> ids);


    @Query("SELECT new org.thingsboard.server.common.data.edqs.fields.CustomerFields(c.id, c.createdTime, c.tenantId, c.parentCustomerId, " +
            "c.title, c.version, c.additionalInfo, c.country, c.state, c.city, c.address, c.address2, c.zip, c.phone, c.email) " +
            "FROM CustomerEntity c WHERE c.id > :id ORDER BY c.id")
    List<CustomerFields> findNextBatch(@Param("id") UUID id, Limit limit);

    @Query("SELECT new org.thingsboard.server.common.data.EntityInfo(a.id, 'CUSTOMER', a.title) " +
            "FROM CustomerEntity a WHERE a.tenantId = :tenantId AND a.title = :name")
    EntityInfo findEntityInfoByName(UUID tenantId, String name);

    @Query(value = "WITH RECURSIVE customers_ids(id) AS " +
            "(SELECT id id FROM customer ce WHERE ce.tenant_id = :tenantId and id = :customerId " +
            "UNION SELECT ce1.id id FROM customer ce1, customers_ids parent WHERE ce1.tenant_id = :tenantId " +
            "and ce1.parent_customer_id = parent.id) SELECT id FROM customers_ids", nativeQuery = true)
    List<UUID> findSubCustomerIds(@Param("tenantId") UUID tenantId, @Param("customerId") UUID customerId);

    /**
     * Resolves {@code customerId} together with its sub-customer ids for a Citus "including sub-customers" query.
     * Returns an empty {@link Optional} when there is nothing to query (missing/stale/cross-tenant customer): the
     * caller must then short-circuit to an empty page, because an empty id list would render {@code IN ()} and fail
     * under Citus. A present Optional always holds a non-empty list, matching the plain path's empty-result behavior.
     * Centralizes the empty-list guard that the {@code *InfoDao} "including sub-customers" methods would otherwise repeat.
     */
    default Optional<List<UUID>> findSubCustomerIdsOrEmpty(UUID tenantId, UUID customerId) {
        List<UUID> customerIds = findSubCustomerIds(tenantId, customerId);
        return customerIds.isEmpty() ? Optional.empty() : Optional.of(customerIds);
    }
}
