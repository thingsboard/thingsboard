/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.asset;

import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetInfo;
import org.thingsboard.server.common.data.asset.AssetSearchQuery;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.service.importing.BulkImportRequest;
import org.thingsboard.server.service.importing.BulkImportResult;

import java.util.List;

public interface AssetService {

    Asset getAssetById(String strAssetId) throws ThingsboardException;

    AssetInfo getAssetInfoById(String strAssetId) throws ThingsboardException;

    Asset saveAsset(Asset asset) throws ThingsboardException;

    BulkImportResult<Asset> processAssetsBulkImport(BulkImportRequest request) throws Exception;

    void deleteAsset(String strAssetId) throws ThingsboardException;

    Asset assignAssetToCustomer(String strCustomerId, String strAssetId) throws ThingsboardException;

    Asset unassignAssetFromCustomer(String strAssetId) throws ThingsboardException;

    Asset assignAssetToPublicCustomer(String strAssetId) throws ThingsboardException;

    PageData<Asset> getTenantAssets(int pageSize, int page, String type, String textSearch, String sortProperty,
                                    String sortOrder) throws ThingsboardException;

    PageData<AssetInfo> getTenantAssetInfos(int pageSize, int page, String type, String textSearch, String sortProperty,
                                            String sortOrder) throws ThingsboardException;

    Asset getTenantAsset(String assetName) throws ThingsboardException;

    PageData<Asset> getCustomerAssets(String strCustomerId, int pageSize, int page, String type, String textSearch,
                                      String sortProperty, String sortOrder) throws ThingsboardException;

    PageData<AssetInfo> getCustomerAssetInfos(String strCustomerId, int pageSize, int page, String type, String textSearch,
                                              String sortProperty, String sortOrder) throws ThingsboardException;

    List<Asset> getAssetsByIds(String[] strAssetIds) throws ThingsboardException;

    List<Asset> findByQuery(AssetSearchQuery query) throws ThingsboardException;

    List<EntitySubtype> getAssetTypes() throws ThingsboardException;

    Asset assignAssetToEdge(String strEdgeId, String strAssetId) throws ThingsboardException;

    Asset unassignAssetFromEdge(String strEdgeId, String strAssetId) throws ThingsboardException;

    PageData<Asset> getEdgeAssets(String strEdgeId, int pageSize, int page, String type, String textSearch,
                                  String sortProperty, String sortOrder, Long startTime, Long endTime) throws ThingsboardException;
}
