// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.whitelabeling;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;
import org.thingsboard.server.dao.model.sql.WhiteLabelingCompositeKey;
import org.thingsboard.server.dao.model.sql.WhiteLabelingEntity;

import java.util.List;
import java.util.Set;
import java.util.UUID;


public interface WhiteLabelingRepository extends JpaRepository<WhiteLabelingEntity, WhiteLabelingCompositeKey> {

    @Query("SELECT wl " +
            "FROM WhiteLabelingEntity wl " +
            "LEFT JOIN DomainEntity d ON wl.domainId = d.id " +
            "WHERE d.name = :domain AND wl.type = :type")
    WhiteLabelingEntity findByDomainAndType(@Param("domain") String domain, @Param("type") WhiteLabelingType type);

    @Query(nativeQuery = true,
            value = "SELECT * FROM white_labeling wl WHERE wl.tenant_id = :tenantId " +
                    "and wl.settings ILIKE CONCAT('%\"', :imageLink, '\"%') limit :lmt"
    )
    List<WhiteLabelingEntity> findByTenantAndImageLink(@Param("tenantId") UUID tenantId, @Param("imageLink") String imageLink, @Param("lmt") int lmt);

    @Query(nativeQuery = true,
            value = "SELECT * FROM white_labeling wl WHERE wl.settings ILIKE CONCAT('%\"', :imageLink, '\"%') limit :lmt"
    )
    List<WhiteLabelingEntity> findByImageLink(@Param("imageLink") String imageLink, @Param("lmt") int lmt);

    @Query("SELECT w FROM WhiteLabelingEntity w WHERE w.type IN :types")
    Page<WhiteLabelingEntity> findAllByTypeIn(@Param("types") Set<WhiteLabelingType> types, Pageable pageable);

    Page<WhiteLabelingEntity> findByTenantId(UUID tenantId, Pageable pageable);

}
