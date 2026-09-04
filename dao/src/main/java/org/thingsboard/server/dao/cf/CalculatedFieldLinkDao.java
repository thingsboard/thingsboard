// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.cf;

import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;

public interface CalculatedFieldLinkDao extends Dao<CalculatedFieldLink> {

    List<CalculatedFieldLink> findCalculatedFieldLinksByCalculatedFieldId(TenantId tenantId, CalculatedFieldId calculatedFieldId);

    List<CalculatedFieldLink> findCalculatedFieldLinksByEntityId(TenantId tenantId, EntityId entityId);

    List<CalculatedFieldLink> findCalculatedFieldLinksByTenantId(TenantId tenantId);

    List<CalculatedFieldLink> findAll();

    PageData<CalculatedFieldLink> findAll(PageLink pageLink);

    PageData<CalculatedFieldLink> findAllByTenantId(TenantId tenantId, PageLink pageLink);

}
