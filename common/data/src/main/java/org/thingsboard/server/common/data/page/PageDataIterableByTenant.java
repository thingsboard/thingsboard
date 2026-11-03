// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.page;

import org.thingsboard.server.common.data.id.TenantId;

public class PageDataIterableByTenant<T> extends BasePageDataIterable<T> {

    private final FetchFunction<T> function;
    private final TenantId tenantId;

    public PageDataIterableByTenant(FetchFunction<T> function, TenantId tenantId, int fetchSize) {
        super(fetchSize);
        this.function = function;
        this.tenantId = tenantId;
    }

    @Override
    PageData<T> fetchPageData(PageLink link) {
        return function.fetch(tenantId, link);
    }

    public interface FetchFunction<T> {
        PageData<T> fetch(TenantId tenantId, PageLink link);
    }
}
