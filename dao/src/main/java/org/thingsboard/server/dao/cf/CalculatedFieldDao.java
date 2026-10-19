// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.cf;

import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldFilter;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.Set;

public interface CalculatedFieldDao extends Dao<CalculatedField> {

    List<CalculatedField> findAllByTenantId(TenantId tenantId);

    List<CalculatedFieldId> findCalculatedFieldIdsByEntityId(TenantId tenantId, EntityId entityId);

    List<CalculatedField> findCalculatedFieldsByEntityId(TenantId tenantId, EntityId entityId);

    List<CalculatedField> findAll();

    CalculatedField findByEntityIdAndTypeAndName(EntityId entityId, CalculatedFieldType type, String name);

    PageData<CalculatedField> findAll(PageLink pageLink);

    PageData<CalculatedField> findAllByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<CalculatedField> findByEntityIdAndTypes(TenantId tenantId, EntityId entityId, Set<CalculatedFieldType> types, PageLink pageLink);

    List<CalculatedField> removeAllByEntityId(TenantId tenantId, EntityId entityId);

    long countByEntityIdAndTypeNot(TenantId tenantId, EntityId entityId, CalculatedFieldType type);

    PageData<CalculatedField> findByTenantIdAndFilter(TenantId tenantId, CalculatedFieldFilter filter, PageLink pageLink);

    PageData<String> findNamesByTenantIdAndType(TenantId tenantId, CalculatedFieldType type, PageLink pageLink);

}
