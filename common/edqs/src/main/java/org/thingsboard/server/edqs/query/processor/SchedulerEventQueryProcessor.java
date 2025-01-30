/**
 * Copyright © 2016-2024 ThingsBoard, Inc.
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
package org.thingsboard.server.edqs.query.processor;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.SchedulerEventFilter;
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.repo.TenantRepo;

public class SchedulerEventQueryProcessor extends AbstractSimpleQueryProcessor<SchedulerEventFilter> {

    public SchedulerEventQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query) {
        super(repo, ctx, query, (SchedulerEventFilter) query.getEntityFilter(), EntityType.SCHEDULER_EVENT);
    }

    @Override
    protected boolean matches(EntityData<?> ed) {
        return super.matches(ed) && (filter.getEventType() == null || filter.getEventType().equals(ed.getFields().getType()))
                && (filter.getOriginator() == null || filter.getOriginator().equals(ed.getFields().getOriginatorId()));
    }

}
