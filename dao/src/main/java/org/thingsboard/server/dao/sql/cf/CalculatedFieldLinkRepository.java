// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.cf;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.thingsboard.server.dao.model.sql.CalculatedFieldLinkEntity;

import java.util.List;
import java.util.UUID;

public interface CalculatedFieldLinkRepository extends JpaRepository<CalculatedFieldLinkEntity, UUID> {

    List<CalculatedFieldLinkEntity> findAllByTenantIdAndCalculatedFieldId(UUID tenantId, UUID calculatedFieldId);

    List<CalculatedFieldLinkEntity> findAllByTenantIdAndEntityId(UUID tenantId, UUID entityId);

    List<CalculatedFieldLinkEntity> findAllByTenantId(UUID tenantId);

    Page<CalculatedFieldLinkEntity> findAllByTenantId(UUID tenantId, Pageable pageable);

}
