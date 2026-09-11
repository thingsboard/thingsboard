// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.cf;

import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldFilter;
import org.thingsboard.server.common.data.cf.CalculatedFieldInfo;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;

public interface CalculatedFieldService extends EntityDaoService {

    CalculatedField save(CalculatedField calculatedField);

    CalculatedField save(CalculatedField calculatedField, boolean doValidate);

    CalculatedField findById(TenantId tenantId, CalculatedFieldId calculatedFieldId);

    CalculatedField findByEntityIdAndTypeAndName(EntityId entityId, CalculatedFieldType type, String name);

    List<CalculatedFieldId> findCalculatedFieldIdsByEntityId(TenantId tenantId, EntityId entityId);

    List<CalculatedField> findCalculatedFieldsByEntityId(TenantId tenantId, EntityId entityId);

    PageData<CalculatedField> findAllCalculatedFields(PageLink pageLink);

    PageData<CalculatedField> findCalculatedFieldsByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<CalculatedFieldInfo> findCalculatedFieldsByTenantIdAndFilter(TenantId tenantId, CalculatedFieldFilter filter, PageLink pageLink);

    PageData<String> findCalculatedFieldNamesByTenantIdAndType(TenantId tenantId, CalculatedFieldType type, PageLink pageLink);

    PageData<CalculatedField> findCalculatedFieldsByEntityId(TenantId tenantId, EntityId entityId, CalculatedFieldType type, PageLink pageLink);

    void deleteCalculatedField(TenantId tenantId, CalculatedFieldId calculatedFieldId);

    int deleteAllCalculatedFieldsByEntityId(TenantId tenantId, EntityId entityId);

    boolean referencedInAnyCalculatedField(TenantId tenantId, EntityId referencedEntityId);

}
