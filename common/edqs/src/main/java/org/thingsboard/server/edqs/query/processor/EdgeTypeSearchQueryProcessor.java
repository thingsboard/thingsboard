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
package org.thingsboard.server.edqs.query.processor;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.EdgeSearchQueryFilter;
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.data.RelationInfo;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.repo.TenantRepo;

public class EdgeTypeSearchQueryProcessor extends AbstractEntitySearchQueryProcessor<EdgeSearchQueryFilter> {

    public EdgeTypeSearchQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query) {
        super(repo, ctx, query, (EdgeSearchQueryFilter) query.getEntityFilter());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.EDGE;
    }

    @Override
    protected boolean check(RelationInfo relationInfo) {
        EntityData<?> ed = relationInfo.getTarget();
        return super.check(relationInfo) &&
                (filter.getEdgeTypes() == null || filter.getEdgeTypes().contains(ed.getFields().getType()));
    }

}
