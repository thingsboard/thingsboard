// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao;

import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.UUID;

public interface ExportableCustomerEntityDao<T extends ExportableEntity<I>, I extends EntityId> extends ExportableEntityDao<I, T> {

    PageData<I> findIdsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink);

}
