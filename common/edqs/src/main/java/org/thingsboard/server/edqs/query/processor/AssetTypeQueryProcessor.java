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
package org.thingsboard.server.edqs.query.processor;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.AssetTypeFilter;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.repo.TenantRepo;

import java.util.List;

public class AssetTypeQueryProcessor extends AbstractEntityProfileQueryProcessor<AssetTypeFilter> {

    public AssetTypeQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query) {
        super(repo, ctx, query, (AssetTypeFilter) query.getEntityFilter(), EntityType.ASSET);
    }

    @Override
    protected String getEntityNameFilter(AssetTypeFilter filter) {
        return filter.getAssetNameFilter();
    }

    @Override
    protected List<String> getProfileNames(AssetTypeFilter filter) {
        return filter.getAssetTypes();
    }

    @Override
    protected EntityType getProfileEntityType() {
        return EntityType.ASSET_PROFILE;
    }

}
