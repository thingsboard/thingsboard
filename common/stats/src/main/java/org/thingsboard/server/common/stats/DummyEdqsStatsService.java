/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.common.stats;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityDataQuery;

@Service
@ConditionalOnMissingBean(value = EdqsStatsService.class, ignored = DummyEdqsStatsService.class)
public class DummyEdqsStatsService implements EdqsStatsService {

    @Override
    public void reportAdded(ObjectType objectType) {}

    @Override
    public void reportRemoved(ObjectType objectType) {}

    @Override
    public void reportEntityDataQuery(TenantId tenantId, EntityDataQuery query, long timingNanos) {}

    @Override
    public void reportEntityCountQuery(TenantId tenantId, EntityCountQuery query, long timingNanos) {}

    @Override
    public void reportEdqsDataQuery(TenantId tenantId, EntityDataQuery query, long timingNanos) {}

    @Override
    public void reportEdqsCountQuery(TenantId tenantId, EntityCountQuery query, long timingNanos) {}

    @Override
    public void reportStringCompressed() {}

    @Override
    public void reportStringUncompressed() {}

}
