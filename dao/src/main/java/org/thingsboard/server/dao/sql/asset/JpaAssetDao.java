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
package org.thingsboard.server.dao.sql.asset;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.asset.AssetDao;
import org.thingsboard.server.dao.model.sql.AssetEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;

/**
 * Created by Valerii Sosliuk on 5/19/2017.
 */
@Component
@ConditionalOnProperty(prefix = "sql", value = "enabled", havingValue = "true", matchIfMissing = false)
public class JpaAssetDao extends JpaAbstractSearchTextDao<AssetEntity, Asset> implements AssetDao {

    @Autowired
    private AssetRepository assetRepository;

    @Override
    protected Class getEntityClass() {
        return AssetEntity.class;
    }

    @Override
    protected CrudRepository getCrudRepository() {
        return assetRepository;
    }

    @Override
    public List<Asset> findAssetsByTenantId(UUID tenantId, TextPageLink pageLink) {
        if (pageLink.getIdOffset() == null) {
            return DaoUtil.convertDataList(assetRepository.findByTenantIdFirstPage(pageLink.getLimit(), tenantId,
                    pageLink.getTextSearch()));
        } else {
            return DaoUtil.convertDataList(assetRepository.findByTenantIdNextPage(pageLink.getLimit(), tenantId,
                    pageLink.getTextSearch(), pageLink.getIdOffset()));
        }
    }

    @Override
    public ListenableFuture<List<Asset>> findAssetsByTenantIdAndIdsAsync(UUID tenantId, List<UUID> assetIds) {
        ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
        return service.submit(() ->
                DaoUtil.convertDataList(assetRepository.findByTenantIdAndIdIn(tenantId, assetIds)));
    }

    @Override
    public List<Asset> findAssetsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, TextPageLink pageLink) {
        if (pageLink.getIdOffset() == null) {
            return DaoUtil.convertDataList(assetRepository.findByTenantIdAndCustomerIdFirstPage(pageLink.getLimit(), tenantId,
                    customerId, pageLink.getTextSearch()));
        } else {
            return DaoUtil.convertDataList(assetRepository.findByTenantIdAndCustomerIdNextPage(pageLink.getLimit(), tenantId,
                    customerId, pageLink.getTextSearch(), pageLink.getIdOffset()));
        }
    }

    @Override
    public ListenableFuture<List<Asset>> findAssetsByTenantIdAndCustomerIdAndIdsAsync(UUID tenantId, UUID customerId, List<UUID> assetIds) {
        ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
        return service.submit(() ->
                DaoUtil.convertDataList( assetRepository.findByTenantIdAndCustomerIdAndIdIn(tenantId, customerId, assetIds)));
    }

    @Override
    public Optional<Asset> findAssetsByTenantIdAndName(UUID tenantId, String name) {
        Asset asset = DaoUtil.getData(assetRepository.findByTenantIdAndName(tenantId, name));
        return Optional.ofNullable(asset);
    }
}
