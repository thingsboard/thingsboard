// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
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
