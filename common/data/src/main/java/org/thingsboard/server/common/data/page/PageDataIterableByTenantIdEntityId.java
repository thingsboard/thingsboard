/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
