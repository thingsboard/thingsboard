// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
