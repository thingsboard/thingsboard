// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.cf;

import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.CalculatedFieldLinkId;
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

    CalculatedField findByEntityIdAndName(EntityId entityId, String name);

    List<CalculatedFieldId> findCalculatedFieldIdsByEntityId(TenantId tenantId, EntityId entityId);

    List<CalculatedField> findCalculatedFieldsByEntityId(TenantId tenantId, EntityId entityId);

    PageData<CalculatedField> findAllCalculatedFields(PageLink pageLink);

    PageData<CalculatedField> findCalculatedFieldsByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<CalculatedField> findAllCalculatedFieldsByEntityId(TenantId tenantId, EntityId entityId, PageLink pageLink);

    void deleteCalculatedField(TenantId tenantId, CalculatedFieldId calculatedFieldId);

    int deleteAllCalculatedFieldsByEntityId(TenantId tenantId, EntityId entityId);

    CalculatedFieldLink saveCalculatedFieldLink(TenantId tenantId, CalculatedFieldLink calculatedFieldLink);

    CalculatedFieldLink findCalculatedFieldLinkById(TenantId tenantId, CalculatedFieldLinkId calculatedFieldLinkId);

    List<CalculatedFieldLink> findAllCalculatedFieldLinksById(TenantId tenantId, CalculatedFieldId calculatedFieldId);

    List<CalculatedFieldLink> findAllCalculatedFieldLinksByEntityId(TenantId tenantId, EntityId entityId);

    PageData<CalculatedFieldLink> findAllCalculatedFieldLinksByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<CalculatedFieldLink> findAllCalculatedFieldLinks(PageLink pageLink);

    boolean referencedInAnyCalculatedField(TenantId tenantId, EntityId referencedEntityId);

}
