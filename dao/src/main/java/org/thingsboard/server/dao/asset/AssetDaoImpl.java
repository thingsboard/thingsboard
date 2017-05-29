/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.dao.asset;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.mapping.Result;
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.AbstractSearchTextDao;
import org.thingsboard.server.dao.model.AssetEntity;
import org.thingsboard.server.dao.model.TenantAssetTypeEntity;

import javax.annotation.Nullable;
import java.util.*;

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;
import static org.thingsboard.server.dao.model.ModelConstants.*;

@Component
@Slf4j
public class AssetDaoImpl extends AbstractSearchTextDao<AssetEntity> implements AssetDao {

    @Override
    protected Class<AssetEntity> getColumnFamilyClass() {
        return AssetEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return ASSET_COLUMN_FAMILY_NAME;
    }

    @Override
    public AssetEntity save(Asset asset) {
        log.debug("Save asset [{}] ", asset);
        return save(new AssetEntity(asset));
    }

    @Override
    public List<AssetEntity> findAssetsByTenantId(UUID tenantId, TextPageLink pageLink) {
        log.debug("Try to find assets by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<AssetEntity> assetEntities = findPageWithTextSearch(ASSET_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Collections.singletonList(eq(ASSET_TENANT_ID_PROPERTY, tenantId)), pageLink);

        log.trace("Found assets [{}] by tenantId [{}] and pageLink [{}]", assetEntities, tenantId, pageLink);
        return assetEntities;
    }

    @Override
    public List<AssetEntity> findAssetsByTenantIdAndType(UUID tenantId, String type, TextPageLink pageLink) {
        log.debug("Try to find assets by tenantId [{}], type [{}] and pageLink [{}]", tenantId, type, pageLink);
        List<AssetEntity> assetEntities = findPageWithTextSearch(ASSET_BY_TENANT_BY_TYPE_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(ASSET_TYPE_PROPERTY, type),
                        eq(ASSET_TENANT_ID_PROPERTY, tenantId)), pageLink);
        log.trace("Found assets [{}] by tenantId [{}], type [{}] and pageLink [{}]", assetEntities, tenantId, type, pageLink);
        return assetEntities;
    }

    @Override
    public ListenableFuture<List<AssetEntity>> findAssetsByTenantIdAndIdsAsync(UUID tenantId, List<UUID> assetIds) {
        log.debug("Try to find assets by tenantId [{}] and asset Ids [{}]", tenantId, assetIds);
        Select select = select().from(getColumnFamilyName());
        Select.Where query = select.where();
        query.and(eq(ASSET_TENANT_ID_PROPERTY, tenantId));
        query.and(in(ID_PROPERTY, assetIds));
        return findListByStatementAsync(query);
    }

    @Override
    public List<AssetEntity> findAssetsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, TextPageLink pageLink) {
        log.debug("Try to find assets by tenantId [{}], customerId[{}] and pageLink [{}]", tenantId, customerId, pageLink);
        List<AssetEntity> assetEntities = findPageWithTextSearch(ASSET_BY_CUSTOMER_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(ASSET_CUSTOMER_ID_PROPERTY, customerId),
                        eq(ASSET_TENANT_ID_PROPERTY, tenantId)),
                pageLink);

        log.trace("Found assets [{}] by tenantId [{}], customerId [{}] and pageLink [{}]", assetEntities, tenantId, customerId, pageLink);
        return assetEntities;
    }

    @Override
    public List<AssetEntity> findAssetsByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, TextPageLink pageLink) {
        log.debug("Try to find assets by tenantId [{}], customerId [{}], type [{}] and pageLink [{}]", tenantId, customerId, type, pageLink);
        List<AssetEntity> assetEntities = findPageWithTextSearch(ASSET_BY_CUSTOMER_BY_TYPE_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(ASSET_TYPE_PROPERTY, type),
                        eq(ASSET_CUSTOMER_ID_PROPERTY, customerId),
                        eq(ASSET_TENANT_ID_PROPERTY, tenantId)),
                pageLink);

        log.trace("Found assets [{}] by tenantId [{}], customerId [{}], type [{}] and pageLink [{}]", assetEntities, tenantId, customerId, type, pageLink);
        return assetEntities;
    }

    @Override
    public ListenableFuture<List<AssetEntity>> findAssetsByTenantIdCustomerIdAndIdsAsync(UUID tenantId, UUID customerId, List<UUID> assetIds) {
        log.debug("Try to find assets by tenantId [{}], customerId [{}] and asset Ids [{}]", tenantId, customerId, assetIds);
        Select select = select().from(getColumnFamilyName());
        Select.Where query = select.where();
        query.and(eq(ASSET_TENANT_ID_PROPERTY, tenantId));
        query.and(eq(ASSET_CUSTOMER_ID_PROPERTY, customerId));
        query.and(in(ID_PROPERTY, assetIds));
        return findListByStatementAsync(query);
    }

    @Override
    public Optional<AssetEntity> findAssetsByTenantIdAndName(UUID tenantId, String assetName) {
        Select select = select().from(ASSET_BY_TENANT_AND_NAME_VIEW_NAME);
        Select.Where query = select.where();
        query.and(eq(ASSET_TENANT_ID_PROPERTY, tenantId));
        query.and(eq(ASSET_NAME_PROPERTY, assetName));
        return Optional.ofNullable(findOneByStatement(query));
    }

    @Override
    public ListenableFuture<List<TenantAssetTypeEntity>> findTenantAssetTypesAsync() {
        Select statement = select().distinct().column(ASSET_TYPE_PROPERTY).column(ASSET_TENANT_ID_PROPERTY).from(ASSET_TYPES_BY_TENANT_VIEW_NAME);
        statement.setConsistencyLevel(cluster.getDefaultReadConsistencyLevel());
        ResultSetFuture resultSetFuture = getSession().executeAsync(statement);
        ListenableFuture<List<TenantAssetTypeEntity>> result = Futures.transform(resultSetFuture, new Function<ResultSet, List<TenantAssetTypeEntity>>() {
            @Nullable
            @Override
            public List<TenantAssetTypeEntity> apply(@Nullable ResultSet resultSet) {
                Result<TenantAssetTypeEntity> result = cluster.getMapper(TenantAssetTypeEntity.class).map(resultSet);
                if (result != null) {
                    return result.all();
                } else {
                    return Collections.emptyList();
                }
            }
        });
        return result;
    }

}
