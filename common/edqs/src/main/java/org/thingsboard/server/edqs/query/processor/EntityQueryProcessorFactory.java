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

import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.repo.TenantRepo;

public class EntityQueryProcessorFactory {

    public static EntityQueryProcessor create(TenantRepo repo, QueryContext ctx, EdqsQuery query) {
        return switch (query.getEntityFilter().getType()) {
            case SINGLE_ENTITY -> new SingleEntityQueryProcessor(repo, ctx, query);
            case ENTITY_LIST -> new EntityListQueryProcessor(repo, ctx, query);
            case ENTITY_NAME -> new EntityNameQueryProcessor(repo, ctx, query);
            case ENTITY_TYPE -> new EntityTypeQueryProcessor(repo, ctx, query);
            case DEVICE_TYPE -> new DeviceTypeQueryProcessor(repo, ctx, query);
            case ASSET_TYPE -> new AssetTypeQueryProcessor(repo, ctx, query);
            case ENTITY_VIEW_TYPE -> new EntityViewTypeQueryProcessor(repo, ctx, query);
            case EDGE_TYPE -> new EdgeTypeQueryProcessor(repo, ctx, query);
            case RELATIONS_QUERY -> new RelationQueryProcessor(repo, ctx, query);
            case API_USAGE_STATE -> new ApiUsageStateQueryProcessor(repo, ctx, query);
            case ASSET_SEARCH_QUERY -> new AssetSearchQueryProcessor(repo, ctx, query);
            case DEVICE_SEARCH_QUERY -> new DeviceSearchQueryProcessor(repo, ctx, query);
            case ENTITY_VIEW_SEARCH_QUERY -> new EntityViewSearchQueryProcessor(repo, ctx, query);
            case EDGE_SEARCH_QUERY -> new EdgeTypeSearchQueryProcessor(repo, ctx, query);
            default -> throw new RuntimeException("Not Implemented!");
        };
    }

}
