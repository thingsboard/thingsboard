// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.page;

import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

public class PageDataIterableByTenantIdEntityId<T> extends BasePageDataIterable<T> {

    private final FetchFunction<T> function;
    private final TenantId tenantId;
    private final EntityId entityId;

    public PageDataIterableByTenantIdEntityId(FetchFunction<T> function, TenantId tenantId, EntityId entityId, int fetchSize) {
        super(fetchSize);
        this.function = function;
        this.tenantId = tenantId;
        this.entityId = entityId;
    }

    @Override
    PageData<T> fetchPageData(PageLink link) {
        return function.fetch(tenantId, entityId, link);
    }

    public interface FetchFunction<T> {
        PageData<T> fetch(TenantId tenantId, EntityId entityId, PageLink link);
    }

}
