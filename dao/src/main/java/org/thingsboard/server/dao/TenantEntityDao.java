// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

public interface TenantEntityDao<T> {

    default Long countByTenantId(TenantId tenantId) {
        throw new UnsupportedOperationException();
    }

    default PageData<T> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        throw new UnsupportedOperationException();
    }

}
