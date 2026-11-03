// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.cf;

import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;

public interface CalculatedFieldDao extends Dao<CalculatedField> {

    List<CalculatedField> findAllByTenantId(TenantId tenantId);

    List<CalculatedFieldId> findCalculatedFieldIdsByEntityId(TenantId tenantId, EntityId entityId);

    List<CalculatedField> findCalculatedFieldsByEntityId(TenantId tenantId, EntityId entityId);

    List<CalculatedField> findAll();

    CalculatedField findByEntityIdAndName(EntityId entityId, String name);

    PageData<CalculatedField> findAll(PageLink pageLink);

    PageData<CalculatedField> findAllByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<CalculatedField> findAllByEntityId(TenantId tenantId, EntityId entityId, PageLink pageLink);

    List<CalculatedField> removeAllByEntityId(TenantId tenantId, EntityId entityId);

    long countCFByEntityId(TenantId tenantId, EntityId entityId);

}
