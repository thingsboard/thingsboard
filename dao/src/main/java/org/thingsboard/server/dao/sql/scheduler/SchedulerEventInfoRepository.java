// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.scheduler;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.SchedulerEventInfoEntity;
import org.thingsboard.server.dao.model.sql.SchedulerEventWithCustomerInfoEntity;

import java.util.List;
import java.util.UUID;

public interface SchedulerEventInfoRepository extends JpaRepository<SchedulerEventInfoEntity, UUID> {

    @Query("SELECT new org.thingsboard.server.dao.model.sql.SchedulerEventWithCustomerInfoEntity(s, c.title, c.additionalInfo) " +
            "FROM SchedulerEventInfoEntity s " +
            "LEFT JOIN CustomerEntity c on c.id = s.customerId " +
            "WHERE s.id = :schedulerEventId")
    SchedulerEventWithCustomerInfoEntity findSchedulerEventWithCustomerInfoById(@Param("schedulerEventId") UUID schedulerEventId);

    List<SchedulerEventInfoEntity> findSchedulerEventInfoEntitiesByTenantId(UUID tenantId);

    List<SchedulerEventInfoEntity> findSchedulerEventInfoEntitiesByTenantIdAndEnabled(UUID tenantId, boolean enabled);

    @Query("SELECT s.id FROM SchedulerEventInfoEntity s " +
            "WHERE s.tenantId = :tenantId " +
            "AND s.customerId = :customerId")
    List<UUID> findIdsByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                              @Param("customerId") UUID customerId);

    @Query("SELECT s.id FROM SchedulerEventInfoEntity s " +
            "WHERE s.tenantId = :tenantId")
    List<UUID> findIdsByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.SchedulerEventWithCustomerInfoEntity(s, c.title, c.additionalInfo) " +
            "FROM SchedulerEventInfoEntity s " +
            "LEFT JOIN CustomerEntity c on c.id = s.customerId " +
            "WHERE s.tenantId = :tenantId " +
            "AND (:customerId IS NULL OR s.customerId = :customerId) " +
            "AND (:type IS NULL OR s.type = :type) " +
            "AND (:searchText IS NULL OR ilike(s.name, CONCAT('%', :searchText, '%')) = true " +
            "OR ilike(s.type, CONCAT('%', :searchText, '%')) = true " +
            "OR ilike(c.title, CONCAT('%', :searchText, '%')) = true)")
    Page<SchedulerEventWithCustomerInfoEntity> findByTenantIdAndCustomerIdAndTypeAndSearchText(@Param("tenantId") UUID tenantId,
                                                                                               @Param("customerId") UUID customerId,
                                                                                               @Param("type") String type,
                                                                                               @Param("searchText") String searchText,
                                                                                               Pageable pageable);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.SchedulerEventWithCustomerInfoEntity(s, c.title, c.additionalInfo) " +
            "FROM SchedulerEventInfoEntity s " +
            "LEFT JOIN CustomerEntity c on c.id = s.customerId " +
            "JOIN RelationEntity r ON r.toId = s.id AND r.toType = 'SCHEDULER_EVENT' " +
            "WHERE s.tenantId = :tenantId  AND r.relationTypeGroup = 'EDGE' " +
            "AND r.relationType = 'Contains' AND r.fromId = :edgeId AND r.fromType = 'EDGE' " +
            "AND (:customerId IS NULL OR s.customerId = :customerId) " +
            "AND (:type IS NULL OR s.type = :type) " +
            "AND (:searchText IS NULL OR ilike(s.name, CONCAT('%', :searchText, '%')) = true " +
            "OR ilike(s.type, CONCAT('%', :searchText, '%')) = true " +
            "OR ilike(c.title, CONCAT('%', :searchText, '%')) = true)")
    Page<SchedulerEventWithCustomerInfoEntity> findByTenantIdAndCustomerIdAndTypeAndEdgeIdAndSearchText(@Param("tenantId") UUID tenantId,
                                                                                                        @Param("customerId") UUID customerId,
                                                                                                        @Param("type") String type,
                                                                                                        @Param("edgeId") UUID edgeId,
                                                                                                        @Param("searchText") String searchText,
                                                                                                        Pageable pageable);

    List<SchedulerEventInfoEntity> findSchedulerEventsByTenantIdAndIdIn(UUID tenantId, List<UUID> schedulerEventIds);

    @Query("SELECT sei FROM SchedulerEventInfoEntity sei, RelationEntity re WHERE sei.tenantId = :tenantId " +
            "AND sei.id = re.toId AND re.toType = 'SCHEDULER_EVENT' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = 'Contains' AND re.fromId = :edgeId AND re.fromType = 'EDGE' " +
            "AND (:searchText IS NULL OR ilike(sei.name, CONCAT('%', :searchText, '%')) = true)")
    Page<SchedulerEventInfoEntity> findByTenantIdAndEdgeId(@Param("tenantId") UUID tenantId,
                                                           @Param("edgeId") UUID edgeId,
                                                           @Param("searchText") String searchText,
                                                           Pageable pageable);

    @Query("SELECT sei FROM SchedulerEventInfoEntity sei, RelationEntity re WHERE sei.tenantId = :tenantId " +
            "AND sei.id = re.toId AND re.toType = 'SCHEDULER_EVENT' AND re.relationTypeGroup = 'EDGE' " +
            "AND sei.customerId = :customerId AND re.relationType = 'Contains' AND re.fromId = :edgeId AND re.fromType = 'EDGE' " +
            "AND (:searchText IS NULL OR ilike(sei.name, CONCAT('%', :searchText, '%')) = true)")
    Page<SchedulerEventInfoEntity> findByTenantIdAndEdgeIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                                        @Param("edgeId") UUID edgeId,
                                                                        @Param("customerId") UUID customerId,
                                                                        @Param("searchText") String searchText,
                                                                        Pageable pageable);

}
