// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.customtranslation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sql.CustomTranslationCompositeKey;
import org.thingsboard.server.dao.model.sql.CustomTranslationEntity;

import java.util.List;
import java.util.Set;
import java.util.UUID;


public interface CustomTranslationRepository extends JpaRepository<CustomTranslationEntity, CustomTranslationCompositeKey> {

    @Query(value = "SELECT DISTINCT c.localeCode FROM CustomTranslationEntity c WHERE c.tenantId = :tenantId AND c.customerId = :customerId")
    Set<String> findLocalesByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId, @Param("customerId") UUID customerId);

    @Transactional
    @Modifying
    @Query("DELETE FROM CustomTranslationEntity r WHERE r.tenantId = :tenantId")
    void deleteByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT new org.thingsboard.server.dao.model.sql.CustomTranslationCompositeKey(ct.tenantId, ct.customerId, ct.localeCode) FROM CustomTranslationEntity ct WHERE ct.tenantId = :tenantId ")
    List<CustomTranslationCompositeKey> findByTenantId(@Param("tenantId") UUID tenantId);

    Page<CustomTranslationEntity> findAllByTenantId(UUID tenantId, Pageable pageable);

}
