// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao;

import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.UUID;

public interface ExportableEntityDao<I extends EntityId, T extends ExportableEntity<I>> extends Dao<T> {

    T findByTenantIdAndExternalId(UUID tenantId, UUID externalId);

    default T findByTenantIdAndName(UUID tenantId, String name) { throw new UnsupportedOperationException(); }

    PageData<T> findByTenantId(UUID tenantId, PageLink pageLink);

    default PageData<I> findIdsByTenantId(UUID tenantId, PageLink pageLink) {
        return findByTenantId(tenantId, pageLink).mapData(ExportableEntity::getId);
    }

    I getExternalIdByInternal(I internalId);

    default T findDefaultEntityByTenantId(UUID tenantId) { throw new UnsupportedOperationException(); }

}
