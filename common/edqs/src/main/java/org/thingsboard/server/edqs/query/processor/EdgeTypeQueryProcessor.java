// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.query.processor;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.EdgeTypeFilter;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.repo.TenantRepo;

import java.util.List;

public class EdgeTypeQueryProcessor extends AbstractEntityProfileNameQueryProcessor<EdgeTypeFilter> {

    public EdgeTypeQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query) {
        super(repo, ctx, query, (EdgeTypeFilter) query.getEntityFilter(), EntityType.EDGE);
    }

    @Override
    protected String getEntityNameFilter(EdgeTypeFilter filter) {
        return filter.getEdgeNameFilter();
    }

    @Override
    protected List<String> getProfileNames(EdgeTypeFilter filter) {
        return filter.getEdgeTypes();
    }

}
