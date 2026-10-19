// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sql.dashboard;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.edqs.fields.DashboardFields;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.DashboardEntity;

import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
public interface DashboardRepository extends JpaRepository<DashboardEntity, UUID>, ExportableEntityRepository<DashboardEntity> {

    Long countByTenantId(UUID tenantId);

    List<DashboardEntity> findByTenantIdAndTitle(UUID tenantId, String title);

    Page<DashboardEntity> findByTenantId(UUID tenantId, Pageable pageable);

    @Query("SELECT d.id FROM DashboardEntity d WHERE d.tenantId = :tenantId AND (d.customerId is null OR d.customerId = org.thingsboard.server.common.data.id.EntityId.NULL_UUID)")
    Page<UUID> findIdsByTenantIdAndNullCustomerId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT d.id FROM DashboardEntity d WHERE d.tenantId = :tenantId AND d.customerId = :customerId")
    Page<UUID> findIdsByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                              @Param("customerId") UUID customerId,
                                              Pageable pageable);

    @Query("SELECT externalId FROM DashboardEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

    @Query("SELECT d.id FROM DashboardEntity d WHERE d.tenantId = :tenantId")
    Page<UUID> findIdsByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT d.id FROM DashboardEntity d")
    Page<UUID> findAllIds(Pageable pageable);

    @Query("SELECT new org.thingsboard.server.common.data.edqs.fields.DashboardFields(d.id, d.createdTime, d.tenantId, " +
            "d.customerId, d.title, d.version) FROM DashboardEntity d WHERE d.id > :id ORDER BY d.id")
    List<DashboardFields> findNextBatch(@Param("id") UUID id, Limit limit);

    @Modifying
    @Query(nativeQuery = true,
            value = """
                    UPDATE dashboard
                    SET configuration = REPLACE(configuration, :pattern, :replacement)
                    WHERE configuration LIKE CONCAT('%', :pattern, '%')
                    """
    )
    int replaceStringInAllDashboardConfigs(
            @Param("pattern") String pattern, @Param("replacement") String replacement
    );

    @Query(
            value = "SELECT COUNT(*) " +
                    "FROM dashboard d " +
                    "WHERE d.configuration LIKE CONCAT('%\"layoutType\":\"', :layoutType, '\"%')",
            nativeQuery = true
    )
    long countAllDashboardsByLayoutType(@Param("layoutType") String layoutType);

    @Modifying
    @Query(nativeQuery = true,
            value = """
            UPDATE dashboard
            SET configuration = CAST(jsonb_set(
              CAST(configuration AS jsonb),
              '{widgets}',
              (
                SELECT jsonb_object_agg(e.key,
                  CASE
                    WHEN e.value->>'typeFullFqn' = :oldFqn
                    THEN jsonb_set(e.value, '{typeFullFqn}', to_jsonb(CAST(:newFqn AS text)), true)
                    ELSE e.value
                  END
                )
                FROM jsonb_each((CAST(configuration AS jsonb))->'widgets') e
              ),
              true
            ) AS text)
            WHERE configuration IS NOT NULL
              AND configuration LIKE CONCAT('%', :oldFqn, '%')
              AND (CAST(configuration AS jsonb)->'widgets') IS NOT NULL
            """
    )
    int replaceWidgetTypeFullFqn(@Param("oldFqn") String oldFqn, @Param("newFqn") String newFqn);

    @Modifying
    @Query(nativeQuery = true,
            value = """
            UPDATE dashboard
            SET configuration = CAST(
                (CAST(configuration AS jsonb) - 'widgets') || 
                jsonb_build_object('widgets', (
                    SELECT jsonb_object_agg(k,
                        CASE
                            WHEN (v->>'typeFullFqn') = :systemFqn
                            THEN v || '{"type": "latest"}'::jsonb
                            ELSE v
                        END
                    )
                    FROM jsonb_each(CAST(configuration AS jsonb)->'widgets') AS e(k, v)
                ))
            AS text)
            WHERE configuration IS NOT NULL
              AND configuration LIKE CONCAT('%', :systemFqn, '%')
              AND (CAST(configuration AS jsonb)->'widgets') IS NOT NULL
              AND EXISTS (
                  SELECT 1 
                  FROM jsonb_each(CAST(configuration AS jsonb)->'widgets') AS e2(k, v2)
                  WHERE v2->>'typeFullFqn' = :systemFqn 
                    AND (v2->>'type') IS DISTINCT FROM 'latest'
              )
            """)
    int setTrendzWidgetsTypeLatestBySystemFqn(@Param("systemFqn") String systemFqn);

}
